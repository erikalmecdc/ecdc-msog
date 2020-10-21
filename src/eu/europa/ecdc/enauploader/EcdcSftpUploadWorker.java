package eu.europa.ecdc.enauploader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.Selectors;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

public class EcdcSftpUploadWorker extends EcdcJob {

	private OutputHandler outputHandler;
	private int ftpColumn;
	private static final int WAIT_RETRY = 10;
	private static final int MAX_TRIES = 5;
	private static final String XML_API_VERSION = "1.0.1";

	// Mandatory constructor
	EcdcSftpUploadWorker(EcdcUploaderGUI gui, TessyBatch batch, String name, UploadConfig cfg, String[] headers,
			String[][] data) {
		super(gui, batch, name, cfg, headers, data);

	}

	// Set the handler for output.
	// TODO: This API is only used for this class, harmonisation for all EcDCJobs would be nice 
	public void setOutputHandler(OutputHandler handler) {
		this.outputHandler = handler;
	}

	// Set the table column for output
	public void setFtpColumn(int col) {
		ftpColumn = col;
	}


	// Main routine for this job
	@Override
	protected Object doInBackground() {
		
		// Check if cancelled
		if (isStopped()) {
			log("Process cancelled");
			return null;
		}
		
		log("Uploading raw reads through SFTP");
		setTitle("Uploading reads using SFTP");
		setStatus("Connecting to SFTP host");
		setProgress(5);

		// Init parameters from config
		String sftpLogin = cfg.getSftpLogin();
		String sftpHost = cfg.getSftpHost();
		String sftpPass = new String(cfg.getSftpPass());
		String sftpPath = cfg.getSftpPath();
		String tmpPath = cfg.getTmpPath();
		File dataDir = new File(cfg.getRawdataDir());
		if (!dataDir.exists()) {
			log("Data directory "+dataDir.toString()+" does not exist.");
			setStatus("Error, data dir not found");
			setProgress(10);
			return null;
		}

		int step = 80/data.length;

		// Iterate over the entries to submit
		int total = data.length;
		int fail = 0;
		
		if (total==0) {
			setStatus("Error, no valid entries selected");
			setProgress(20);
			return null;
		}
		
		ISOLATE: for (int i = 0; i<data.length;i++) {
			
			// Get information from data table. The fields are currently hard-coded
			// TODO: Improve the API for transferring data tables to jobs
			int row = Integer.parseInt(data[i][0]);
			String recordId = data[i][1];  	
			String fileStr = data[i][2];  
			String wgsProtocol = data[i][4];
			String ui = data[i][5];

			setStatus("Preparing upload for "+recordId);
			setProgress(10+i*step);

			// Check that entry has all neccessary fields
			if (wgsProtocol==null || wgsProtocol.equals("") || recordId==null || recordId.equals("") || fileStr==null || fileStr.equals("")) {
				log("Error, RecordId, Wgs protocol and raw data files must be filled in.");
				fail++;
				continue ISOLATE;
			}

			String[] fileNames = fileStr.split(";",-1);
			ArrayList<File> files = new ArrayList<File>();
			ArrayList<File> tmpFiles = new ArrayList<File>();
			for (String fn: fileNames) {
				File file = new File(fn);
				if (file.exists()) {

					if (fn.toLowerCase().endsWith(".fastq.gz") || fn.toLowerCase().endsWith(".fastq") || fn.toLowerCase().endsWith(".fq") || fn.toLowerCase().endsWith(".fq.gz")|| fn.toLowerCase().endsWith(".fasta") || fn.toLowerCase().endsWith(".fa")) {
						if (fn.endsWith(".gz") || fn.endsWith(".fasta") || fn.endsWith(".fa")) {
							files.add(file);
							log("Record "+recordId+", found file: "+fn);
						} else {
							log("Record "+recordId+", found uncompressed file: "+fn);
							String tmpFilePath = tmpPath+"/"+file.getName()+".gz";
							File tmpFile = new File(tmpFilePath);
							log("Compressing "+fn+" to "+tmpFilePath);
							EcdcUtils.compressGzipFile(file,tmpFile);
							tmpFiles.add(tmpFile);
							files.add(tmpFile);
						}
					}


				} else {
					log("Error, for record "+recordId+", file: "+fn+" does not exist.");
					fail++;
					continue ISOLATE;
				}
			}

			if (files.isEmpty()) {
				log("No raw data files found for "+recordId+", skipping.");	
				fail++;
				continue ISOLATE;
			} else {
				log(Integer.toString(files.size()) + " raw data files found for "+recordId);	
			}

			String remotePath = sftpPath+"/"+cfg.getTessySubject();
		
			int count = 0;
			int step2 = step/files.size();

			
			FileSystemManager fsManager;
			FileSystemOptions fsOptions;
			try {
				fsOptions = new FileSystemOptions();
				SftpFileSystemConfigBuilder.getInstance().setStrictHostKeyChecking(fsOptions, "no");
				SftpFileSystemConfigBuilder.getInstance().setTimeout(fsOptions, 30000);
				fsManager = VFS.getManager();
			} catch (Exception e) {
				log("Failed to initialize SFTP filesystem, quitting.");
				fail++;
				continue ISOLATE;
			}
			
			// Create a MD5 digest of concatenated data provider and TESSy GUID
			// This is used to anonymize the file names for the SFTP 
			String baseName = org.apache.commons.codec.digest.DigestUtils.md5Hex(cfg.getTessyProvider().toUpperCase()+"_"+recordId);
			String xmlName = baseName+".xml";
			String xmlFile = tmpPath+"/"+xmlName;

			// Init data structures for the info that is going into the XML waybill
			HashMap<File,String> newFileNames = new HashMap<File,String>();
			ArrayList<String> fileNamesXml = new ArrayList<String>();
			ArrayList<String> checksumsXml = new ArrayList<String>();

			int fileCount = 1;
			// Iterate over the files
			for (File f : files) {
				String newName;
				// Anonymize and standardise filename
				// TODO: Not great to have IONTORRENT hardcoded
				if (f.getName().endsWith(".fa") || f.getName().endsWith(".fasta")) {
					newName = baseName+".fasta";
				} else {
					if (wgsProtocol.toUpperCase().equals("IONTORRENT")) {
						newName = baseName+"."+Integer.toString(fileCount)+".fastq.gz";
					} else {
						newName = baseName+".R"+Integer.toString(fileCount)+".fastq.gz";
					}
				}

				// Calculate checksum for file, to put in XML
				FileInputStream fis;
				String checksum;
				try {
					fis = new FileInputStream(f);
					checksum = org.apache.commons.codec.digest.DigestUtils.md5Hex(fis);
					fis.close();
				} catch (IOException e) {
					log("Checksum failed");
					fail++;
					continue ISOLATE;
				}
				newFileNames.put(f,newName);
				fileNamesXml.add(newName);
				checksumsXml.add(checksum);
				fileCount++;
			}

			
			boolean xmlSuccess = makeSftpXml(xmlFile,baseName,fileNamesXml,wgsProtocol,checksumsXml,ui,cfg.isAnonymizeFtp(),cfg.isShareFtp(),cfg.isShowYearFtp());
			if (!xmlSuccess) {
				log("XML creation failed.");
				fail++;
				continue ISOLATE;
			}
			
			// Upload XML file
			try {
				String uriXml = "sftp://"+sftpLogin+":"+sftpPass+"@"+sftpHost+":22"+remotePath+"/"+xmlName;
				FileObject localFileXml;
				localFileXml = fsManager.resolveFile(xmlFile);
				FileObject foXml = fsManager.resolveFile(uriXml, fsOptions);
				foXml.copyFrom(localFileXml, Selectors.SELECT_SELF);
			} catch (FileSystemException e1) {
				log("XML upload failed.");
				fail++;
				continue ISOLATE;
			}


			// Upload files
			for (File f : files) {
				
				// Check if process is cancelled
				if (isStopped()) {
					log("Process cancelled");
					return null;
				}
				setStatus("File "+f.getName());
				setProgress(10+i*step+count*step2);
				log("File "+f.getName());

				boolean done = false;
				int tries = 0;
				
				// Upload will retry several times, as determined by the MAX_TRIES parameter
				while (!done) {
					done=true;
					count++;

					String newName = newFileNames.get(f);

					// Check local file size, to determine if upload is successful
					long filesizeLocal = f.length();

					try {
						
						// Remote file and folder objects
						String uri = "sftp://"+sftpLogin+":"+sftpPass+"@"+sftpHost+":22"+remotePath+"/"+newName;
						String uriDir = "sftp://"+sftpLogin+":"+sftpPass+"@"+sftpHost+":22"+remotePath;

						FileObject fileObjectLocal = fsManager.resolveFile(f.getAbsolutePath());
						FileObject folderObjectRemote = fsManager.resolveFile(uriDir, fsOptions);
						folderObjectRemote.createFolder();
						FileObject fileObjectRemote = fsManager.resolveFile(uri, fsOptions);

						log("Uploading to SFTP: "+fileObjectLocal.toString()+"\n");
						log("Remote file: "+fileObjectRemote.getPublicURIString()+"\n");

						fileObjectRemote.copyFrom(fileObjectLocal, Selectors.SELECT_SELF);

						long filesizeUploaded = fileObjectRemote.getContent().getSize();
						
						// Check if the file is successfully uploaded
						if (filesizeUploaded != filesizeLocal) {
							log("Upload error (wrong file size) for "+fileObjectLocal.toString());
							done = false;
						}

						if (done) {
							log("Upload for "+fileObjectLocal.toString()+" finished");
						} 

					} catch (Exception e) {
						
						// If Exception, log it and retry
						e.printStackTrace();
						log(e.getStackTrace().toString());
						done = false;
					} 

					if (!done) {
						tries++;
						// Check if the maximum number of tries have been reached
						if (tries<MAX_TRIES) {
							
							// Retry after WAIT_RETRY seconds
							log("Upload failed, will retry #"+Integer.toString(tries)+" in "+Integer.toString(WAIT_RETRY)+" seconds");
							try {
								Thread.sleep(1000*WAIT_RETRY);
							} catch (InterruptedException e) {
							}
						} else {
							
							// If giving up, delete temporary files
							for (File delF : tmpFiles) {
								delF.delete();
							}
							done = true;

							log("SFTP Upload failed, giving up.");
							fail++;
							continue ISOLATE;
						}
					}
				}
			}
			
			// Delete temporary files when finished
			for (File delF : tmpFiles) {
				delF.delete();
			}
			
			// Write output to outputHandler (generally to the isolate table)
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			outputHandler.write(df.format(new Date()), row, ftpColumn);
			log("Upload for "+recordId+" finished");
		
			// Save GUI
			gui.save();
		}

		String numberOkText = Integer.toString(total-fail) + "/" + Integer.toString(total)+" successful";
		log("Finished "+numberOkText);
		setStatus("Finished! "+numberOkText);
		setProgress(100);
		return null;
	}

	// This methods creates the waybill XML
	private boolean makeSftpXml(String xmlFile, String baseName, ArrayList<String> fileNames, String wgsProtocol, ArrayList<String> checksums, String ui, boolean anonymize, boolean share, boolean showYear) {

		try {

			Element ecdcWgs = new Element("ECDCWgs");
			Element sequence = new Element("sequence");

			Element ecdcWgsUploadClient = new Element("ecdcWgsUploadClient");

			// Add XML API version to XML, must match back-end version
			// TODO: Incorrect version should trigger user notification from back-end using email
			ecdcWgsUploadClient.setAttribute(new Attribute("version", XML_API_VERSION));
			Document doc = new Document(ecdcWgs);
			ecdcWgs.addContent(sequence);
			sequence.addContent(ecdcWgsUploadClient);

			// Add current date and time
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Element uploadedDate = new Element("submittedDate");
			uploadedDate.setText(df.format(new Date()));
			ecdcWgsUploadClient.addContent(uploadedDate);

			// Add the anonymized id
			Element anonymizedId = new Element("anonymizedId");
			anonymizedId.setText(baseName);
			ecdcWgsUploadClient.addContent(anonymizedId);

			// Add info for each sequence file
			for (int i = 0; i< fileNames.size();i++) {
				Element sequenceReads = new Element("sequenceReads");
				sequenceReads.addContent(new Element("fileName").setText(fileNames.get(i)));
				sequenceReads.addContent(new Element("wgsProtocol").setText(wgsProtocol));
				sequenceReads.addContent(new Element("md5Checksum").setText(checksums.get(i)));
				ecdcWgsUploadClient.addContent(sequenceReads);
			}

			// Add parameters for the back-ens processing
			Element config = new Element("ecdcWgsConfig");
			ecdcWgsUploadClient.addContent(config);
			
			// Is this associated with an event id?
			config.addContent(new Element("event").setText(ui));
			
			// Should the file names be anonymized if shared
			config.addContent(new Element("anonymise").setText(Boolean.toString(anonymize)));
			
			// Should the files be shared?
			config.addContent(new Element("share").setText(Boolean.toString(share)));
			
			// Should the year be shown if shared?
			config.addContent(new Element("showYear").setText(Boolean.toString(showYear)));

			// Write XML
			XMLOutputter xmlOutput = new XMLOutputter();
			xmlOutput.setFormat(Format.getPrettyFormat());
			xmlOutput.output(doc, new FileWriter(xmlFile));
			log("xmlFile written: "+xmlFile);
			
		} catch (IOException io) {
			log("Error, could not write xmlFile: "+xmlFile);
			return false;
		}
		return true;
	}
}
