package eu.europa.ecdc.enauploader;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.Selectors;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.impl.StandardFileSystemManager;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;

import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPException;

public class SubmissionWorker extends SwingWorker {

	private String center;
	private String projectId;
	//private JTable table;
	private File dataDir;
	private String login;
	private String pass;
	private boolean prod;
	private boolean anonymize;
	private String delimiter;
	private String enaChecklist;
	private JTextArea logArea;
	private String ftpHost;
	private String curlPath;
	private String tmpPath;
	private boolean ftpExist;
	private String[][] metadata;
	private String[] header;
	private OutputHandler outHandler;
	private TessyBatch batch;
	private boolean submitEna;
	private boolean submitTessy;
	private ENAuploaderGUI gui;
	private boolean submitSftp;
	private String sftpHost;
	private String sftpLogin;
	private String sftpPass;

	//	String center, String projectId, JTable table, File dataDir,String login,String pass, boolean prod, boolean anonymize, String delimiter, String enaChecklist
	SubmissionWorker (String cent, String proj, String[][] metad,String[] head, File dataD,String log,String p, boolean pr, boolean anon, boolean existFtp, String delim, String enaCheck, JTextArea logA, String tmpP, String curlP, String ftpH, OutputHandler outH, TessyBatch bat, String sftpH, String sftpL, String sftpP, boolean ftpB, boolean enaB, boolean tessyB, ENAuploaderGUI gu) {

		sftpHost = sftpH;
		sftpLogin = sftpL;
		sftpPass = sftpP;
		submitSftp = ftpB;
		submitEna = enaB;
		submitTessy = tessyB;
		batch = bat;
		outHandler = outH;
		center = cent;
		projectId = proj;
		//table = tab;
		metadata = metad;
		header = head;
		dataDir = dataD;
		login = log;
		pass = p;
		prod = pr;
		anonymize = anon;
		ftpExist = existFtp;
		delimiter = delim;
		enaChecklist = enaCheck;
		logArea = logA;
		ftpHost = ftpH;
		curlPath = curlP;
		tmpPath = tmpP;
		gui = gu;

	}

	@Override
	protected void done() {
		if (gui != null) {
			gui.save();
		}
	}

	@Override
	protected Object doInBackground() throws Exception {

		SimpleDateFormat timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		if (logArea!=null) {
			logArea.append("Submission started...\n");
		}










		if (submitEna) {
			File[] dataFiles = dataDir.listFiles();

			if (logArea!=null) {
				logArea.append("Submitting to ENA: "+projectId+"\n");
			} else {
				System.out.println("Submitting to ENA: "+projectId);
			}


			boolean notDone = true;
			int i = -1;
			while (notDone) {


				i++;
				int row = i;

				if (i>=metadata.length) {
					break;
				}
				String id = metadata[i][0];


				String acc1 = metadata[i][header.length-8];
				if (logArea!=null) {
					logArea.append(acc1+"\n");
				} else {
					System.out.println(acc1);
				}
				String acc2 = metadata[i][header.length-9];
				String acc3 = metadata[i][header.length-10];
				if (id == null || id.equals("")) {
					break;
				}
				if (!(acc1 == null || acc1.equals(""))) {
					continue;
				}
				if (!(acc2 == null || acc3.equals(""))) {
					continue;
				}
				if (!(acc2 == null || acc3.equals(""))) {
					continue;
				}

				UUID uuid = UUID.randomUUID();
				String randomUUIDString = uuid.toString();
				String sname = center+"_"+randomUUIDString;

				//Create new submission
				Submission s = new Submission(center,"sub_"+sname);
				s.setFtpExist(ftpExist);
				s.setFtpHost(ftpHost);
				s.setCurlPath(curlPath);
				s.setTmpPath(tmpPath);
				s.setlogArea(logArea);

				if (prod) {
					s.useProductionServer(true);
				}
				if (!login.equals("") && !pass.equals("")) {
					s.setLogin(login, pass);
				}





				if (logArea!=null) {
					logArea.append(id+"\n");
				} else {
					System.out.println(id);
				}

				String filePattern = metadata[i][header.length-12];
				String rUUIDString;


				if (anonymize) {
					rUUIDString = UUID.randomUUID().toString();

				} else {
					rUUIDString = id;
				}


				String instrument = metadata[i][header.length-14];
				String taxon = metadata[i][header.length-13];
				SampleWrapper wrap = new SampleWrapper(center,projectId, rUUIDString,s);
				wrap.experiment.setInstrument(instrument);
				wrap.sample.setTaxon(taxon);
				wrap.sample.setAttribute("checklist",enaChecklist);


				for (int k = 1;k<header.length;k++) {
					String head = header[k];
					String value = metadata[i][k];
					if (head.endsWith("*") || !head.matches(".*[;].*") || value.equals("")) {
						continue;
					}

					String[] fields = head.split(";",-1);
					String key = fields[1];
					if (key.equals("")) {
						continue;
					}
					if (fields[0].equals("DateUsedForStatistics")) {

						value = value.substring(0, 4);


					}
					if (fields[0].equals("ReportingCountry")) {
						value = codeToCountry(value);
					}

					wrap.sample.setAttribute(key,value);
				}
				wrap.sample.setAttribute("host scientific name","Homo sapiens");
				int foundFiles = 0;

				if (ftpExist) {
					boolean failed = true;
					FTPClient client = new FTPClient();
					int tries = 5;
					while (failed && tries > 0) {
						try {
							System.out.println("Using FTP for files");




							client.connect(s.FTP_HOST);	




							client.login(s.LOGIN, s.PASSWORD);


							String[] filesList = client.listNames();
							for(String f: filesList) {

								if (!f.matches(".*fastq.*") || f.matches(".*md5$")) {
									continue;
								}
								boolean foundFile = false;
								if (f.toLowerCase().startsWith(filePattern.toLowerCase()+delimiter)) {
									System.out.println(f);
									foundFile = true;
								}
								if (foundFile) {
									if (logArea==null) {
										System.out.println(id+"\tAdding file: "+f);
									} else {
										logArea.append(id+"\tAdding file: "+f+"\n");
									}
									File ff = new File(f);
									wrap.run.addFile(ff,ff);
									foundFiles++;
									File md5Local = new File(s.TMP_PATH+"/tmp.md5");

									try {
										client.download(f+".md5", md5Local);
										String line;
										BufferedReader br = new BufferedReader(new FileReader(md5Local));
										while ((line = br.readLine())!=null) {
											if (!line.equals("")) {
												wrap.run.setMd5Hex(ff.getName(),line.trim());
											}
										}
										br.close();
									} catch (FTPException fe) {
										if (logArea==null) {
											System.out.println("MD5-files must exist on the FTP when using pre-uploaded FTP data files. Aborting submission.");
										} else {
											logArea.append("MD5-files must exist on the FTP when using pre-uploaded FTP data files.\nAborting submission.\n");
										}
										fe.printStackTrace();
										return null;
									} catch (Exception e) {
										wrap.run.setMd5Hex(ff.getName(),"");
										e.printStackTrace();
									}

								}
							}
							client.disconnect(true);
							failed = false;
						} catch (FTPException fe) {
							if (logArea==null) {
								System.out.println("FTP error.");
							} else {
								logArea.append("FTP error.");
							}
							fe.printStackTrace();
							failed = true;
							tries--;
							System.out.println(tries);
							Thread.sleep(5000);
						} 
					}
				} else {
					for (File f : dataFiles) {
						if (!f.getName().toLowerCase().matches(".*fastq.*")) {
							continue;
						}

						boolean foundFile = false;

						System.out.println(f.getName().toLowerCase());
						System.out.println(filePattern.toLowerCase()+delimiter);
						if (f.getName().toLowerCase().startsWith(filePattern.toLowerCase()+delimiter)) {
							foundFile = true;
						}

						if (foundFile) {if (logArea!=null) {
							logArea.append(id+"\tAdding file: "+f.getName()+"\n");
						} else {
							System.out.println(id+"\tAdding file: "+f.getName());
						}



						FileInputStream fis = new FileInputStream(f);
						String checksum = org.apache.commons.codec.digest.DigestUtils.md5Hex(fis);
						fis.close();
						wrap.run.addFile(f);
						wrap.run.setMd5Hex(f.getName(), checksum);
						foundFiles++;
						}
					}
				}

				if (foundFiles==0) {
					outHandler.write("No files found",i,header.length-1);

					if (logArea!=null) {
						logArea.append("No files found\n");
					} else {
						System.out.println("No files found\n");
					}

					continue;
				}



				s.addEntry(wrap);


				s.uploadFiles();

				String sampleAcc = "";
				String experimentAcc = "";
				String runAcc = "";

				if (s.submit()) {
					sampleAcc = wrap.getSample().getAccession();
					experimentAcc = wrap.getExperiment().getAccession();
					runAcc = wrap.getRun().getAccession();
					if (sampleAcc.equals("")) {
						if (logArea!=null) {
							logArea.append("ERROR: Submission failed.\n");
						} else {
							System.out.println("ERROR: Submission failed.");
						}
					} else {

						if (logArea!=null) {
							logArea.append("Submission complete.\n");
						} else {
							System.out.println("Submission complete.");
						}
					}

				} else {

					if (logArea!=null) {
						logArea.append("ERROR: Submission failed. Aborting.\n");
					} else {
						System.out.println("ERROR: Submission failed. Aborting.");
					}
					return null;
				}






				String fileStr = "";
				ArrayList<File> origFiles = wrap.getRun().getOriginalFiles();
				for (File f : origFiles) {
					if (!fileStr.equals("")) {
						fileStr = fileStr + "|";
					}
					fileStr = fileStr + f.getName();
				}

				String ts = timestamp.format(new Date());
				outHandler.write(fileStr,row,header.length-7);
				outHandler.write(runAcc,row,header.length-8);
				outHandler.write(experimentAcc,row,header.length-9);
				outHandler.write(sampleAcc,row,header.length-10);
				outHandler.write(rUUIDString,row,header.length-11);
				outHandler.write(ts,row,1);
				metadata[row][header.length-7] = fileStr;
				metadata[row][header.length-8] = runAcc;
				metadata[row][header.length-9] = experimentAcc;
				metadata[row][header.length-10] = sampleAcc;
				metadata[row][header.length-11] = rUUIDString;
				metadata[row][1] = ts;


				outHandler.close();
				if (gui != null) {
					gui.save();
				}


			}

		}

		if (submitTessy) {
			boolean submit = true;
			boolean notDone = true;
			int i = -1;
			ArrayList<Integer> submittedRows = new ArrayList<Integer>();

			if (logArea!=null) {
				logArea.append("Submitting to TESSy: "+batch.getCredentials().getHostname()+batch.getCredentials().getTarget()+"\n");
			} else {
				System.out.println("Submitting to TESSy: "+batch.getCredentials().getHostname()+batch.getCredentials().getTarget());
			}
			File[] dataFiles = dataDir.listFiles();
			while (notDone) {


				i++;
				int row = i;

				System.out.println(i);
				if (i>=metadata.length) {
					break;
				}


				String filePattern = metadata[i][header.length-12];
				String runAcc = metadata[i][header.length-8];

				if (runAcc==null) {
					runAcc = "";
				}

				String id = metadata[i][0];
				String tessyId = metadata[i][header.length-2];

				System.out.println("TESSy: "+id+" "+runAcc+" TESSy id: "+tessyId);
				if (tessyId != null && !tessyId.equals("")) {
					String val1 = metadata[i][header.length-4];
					String val2 = metadata[i][1];
					try {
						if (val1!=null && val2!=null) {
							Date d1 = timestamp.parse(val1);
							Date d2 = timestamp.parse(val2);
							if (d2.after(d1)) {
								String val1b = metadata[i][header.length-3];
								Date d1b = timestamp.parse(val1b);
								if (d1.after(d1b)) {
									continue;
								}
							} else {
								continue;		
							}
						}
					} catch (Exception e) {
						continue;
					}
				}

				submittedRows.add(row);

				HashMap<String,String> metaD = new HashMap<String,String>();
				for (int k = 1;k<header.length;k++) {
					String head = header[k];
					String value = metadata[i][k];
					if (head.endsWith("*") || !head.matches(".*[;].*") || value.equals("")) {
						continue;
					}

					String[] fields = head.split(";");
					head = fields[0];


					metaD.put(head, value);
				}

				outHandler.write("",row,header.length-5);

				String prot = getWgsProtocol(metadata[i][header.length-14]);

				System.out.println(prot);
				if (prot.equals("ERROR")) {
					outHandler.write("Instrument model not recognized, use from list",row,header.length-5);
					submit = false;
					continue;
				}


				if(!runAcc.equals("")) {
					metaD.put("WgsEnaId",runAcc);
					metaD.put("WgsProtocol",prot);
				}


				for (String k : metaD.keySet()) {
					System.out.println(k+" => "+metaD.get(k));
				}

				if (!metaD.containsKey("DateUsedForStatistics") || !metaD.get("DateUsedForStatistics").matches("[0-9][0-9][0-9][0-9][-][0-9][0-9][-][0-9][0-9]")) {
					outHandler.write("DateUsedForStatistics Missing/Misformatted",row,header.length-5);

					submit = false;
					continue;
				}
				if (!metaD.containsKey("ReportingCountry") || metaD.get("ReportingCountry").equals("")) {
					outHandler.write("ReportingCountry Missing",row,header.length-5);
					submit = false;
					continue;
				}

				String countryFull = metaD.get("ReportingCountry");
				String country = countryToCode(countryFull);
				//metaD.put("ReportingCountry", country);



				TessyIsolate obc = new TessyIsolate(id,metaD.get("DateUsedForStatistics"),country);
				metaD.remove("DateUsedForStatistics");
				metaD.remove("ReportingCountry");
				for (String key: metaD.keySet()) {
					obc.setField(key, metaD.get(key));
				}

				if (metaD.containsKey("WgsAssembler") && !metaD.get("WgsAssembler").equals("")) {


					for (File f : dataFiles) {
						if (!f.getName().toLowerCase().matches(".*fasta.*")) {
							continue;
						}

						if (f.getName().toLowerCase().startsWith(filePattern.toLowerCase()+delimiter)) {

							File f2 = f;
							if (!f.getName().endsWith(".gz")) {
								f2 = new File(f.toString()+".gz");
								compressGzipFile(f,f2);
							}

							byte[] fileContents =  Files.readAllBytes(f2.toPath());

							String assemblyBase64String = DatatypeConverter.printBase64Binary(fileContents);
							obc.setField("WgsAssembly", assemblyBase64String);
							obc.setField("WgsProtocol",prot);

							if (logArea!=null) {
								logArea.append(id+"\tAdding assembly file: "+f.getName()+"\n");
							} else {
								System.out.println(id+"\tAdding assembly file: "+f.getName());
							}
							break;
						}
					}


				}

				if (logArea!=null) {
					logArea.append("Adding to batch "+batch.getBatchId()+": "+id+"\n");
				} else {
					System.out.println("Submitting to TESSy: "+batch.getCredentials().getHostname()+batch.getCredentials().getTarget());
				}

				batch.addIsolate(obc);


			}

			System.out.println("SUBMIT: "+Boolean.toString(submit));
			if (!submit) {
				if (logArea!=null) {
					logArea.append("There are pre-validation errors, aborting.\n");
				} else {
					System.out.println("There are pre-validation errors, aborting.");
				}
				if (gui!=null) {
					gui.save();
				}
				return null;
			}

			if (logArea!=null) {
				logArea.append("Getting unused batch Id\n");
			} else {
				System.out.println("Getting unused batch Id");
			}
			batch.setId(getBatchid());



			if (logArea!=null) {
				logArea.append("Testing batch: "+batch.getBatchId()+"\n");
			} else {
				System.out.println("Testing batch: "+batch.getBatchId());
			}
			HashMap<String,TessyValidationResult> res = batch.test();


			for (String k : res.keySet()) {
				String val = "";
				int num = -1; 
				for (int ii = 0;ii<metadata.length;ii++) {
					System.out.println(metadata[ii][0]);
					if (metadata[ii][0].equals(k)) {
						num = ii;
						System.out.println(num);
					}
				}
				TessyValidationResult r = res.get(k);
				System.out.println(k);
				System.out.print(r.getErrorNum());
				System.out.print("\t");
				System.out.print(r.getWarningNum());
				System.out.print("\t");
				System.out.print(r.getRemarkNum());
				System.out.println("");
				for (String e : r.getErrors()) {
					System.out.println(e);
					val = val + "ERROR: "+ e+"\t";
				}
				for (String e : r.getWarnings()) {
					System.out.println(e);
					val = val +"WARNING: "+e+"\t";
				}
				for (String e : r.getRemarks()) {
					System.out.println(e);
					val = val +"REMARK:: "+ e+"\t";
				}
				if (val.equals("")) {
					val = "OK";
				}
				if (num>=0) {

					val = val.replace(",","");
					outHandler.write(val,num,header.length-5);
				} else {
					if (logArea!=null) {
						logArea.append(val+"\n");
					} else {
						System.out.println(val);
					}
				}
			}
			for (String k : res.keySet()) {
				TessyValidationResult r = res.get(k);
				if (!r.pass()) {
					System.out.println("There are errors, quitting.");
					if (logArea!=null) {
						logArea.append("There are errors in batch : "+batch.getBatchId()+", aborting\n");
					} else {
						System.out.println("There are errors in batch : "+batch.getBatchId()+", aborting");
					}
					if (gui!=null) {
						gui.save();
					}
					return null;
				}
			}

			for (int srow : submittedRows) {
				outHandler.write(batch.getBatchId(),srow,header.length-6);
			}
			if (gui != null) {
				gui.save();
			}
			boolean uploaded = batch.upload();
			if (!uploaded) {
				if (logArea!=null) {
					logArea.append("TESSy upload failed, aborting"+"\n");
				} else {
					System.out.println("TESSy upload failed, aborting");
				}
				for (int srow : submittedRows) {
					outHandler.write("",srow,header.length-6);
				}
				if (gui != null) {
					gui.save();
				}

				return null;
			}
			
			



			boolean done = false;
			while (!done) {


				done = batch.checkValidation();

				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {

					e.printStackTrace();
				}
			}

			System.out.println("Validation results available.");




			res = batch.getValidation();
			for (String k : res.keySet()) {
				TessyValidationResult r = res.get(k);
				System.out.println(k);
				String guid = r.getGuid();
				System.out.println(guid);
				System.out.print(r.getErrorNum());
				System.out.print("\t");
				System.out.print(r.getWarningNum());
				System.out.print("\t");
				System.out.print(r.getRemarkNum());
				System.out.println("");
				if (!r.pass()) {
					System.out.println("There are errors, quitting.");
					return null;
				}
			}

			System.out.println("Validation OK, ready for approval.");

			String ts = timestamp.format(new Date());
			for (String k : res.keySet()) {
				TessyValidationResult r = res.get(k);
				int num = -1; 
				for (int ii = 0;ii<metadata.length;ii++) {
					if (metadata[ii][0].equals(k)) {
						num = ii;
					}
				}
				if (num!=-1) {
					outHandler.write(r.getGuid(),num,header.length-2);

					outHandler.write(ts,num,header.length-4);

				}

			}

			if (logArea!=null) {
				logArea.append("Approving batch: "+batch.getBatchId()+"\n");
			} else {
				System.out.println("Approving batch: "+batch.getBatchId());
			}

			if (gui != null) {
				gui.save();
			}

			boolean approved = batch.approve();


			if (!approved) {
				return null;
			}
			for (String k : res.keySet()) {
				int num = -1; 
				for (int ii = 0;ii<metadata.length;ii++) {
					if (metadata[ii][0].equals(k)) {
						num = ii;
					}
				}
				if (num!=-1) {
					outHandler.write(timestamp.format(new Date()),num,header.length-3);
				}
			}
			if (logArea!=null) {
				logArea.append("Batch approved, submission to TESSy complete."+"\n");
			} else {
				System.out.println("Batch approved, submission to TESSy complete.");
			}

		}

		if (submitSftp) {
			if (logArea!=null) {
				logArea.append("Submitting to SFTP: "+sftpHost+"\n");
			} else {
				System.out.println("Submitting to SFTP: "+sftpHost);
			}
			File[] dataFiles = dataDir.listFiles();
			boolean notDone = true;
			int i = -1;
			while (notDone) {


				i++;
				int row = i;

				if (i>=metadata.length) {
					break;
				}
				String uid = metadata[i][header.length-1];
				if (uid!=null && !uid.equals("") && !uid.equals("no files found") && !uid.equals("no TESSy id found") && !uid.equals("upload error")) {
					continue;
				}
				String id = metadata[i][0];
				String filebase = metadata[i][header.length-12];
				ArrayList<File> uploadFiles = new ArrayList<File>();
				for (File f : dataFiles) {
					if (f.getName().startsWith(filebase+delimiter)) {
						uploadFiles.add(f);
					}
				}
				if (uploadFiles.isEmpty()) {
					outHandler.write("no files found",i,header.length-1);
					continue;
				}

				String ftpUUIDString = metadata[i][header.length-2];

				if (ftpUUIDString==null || ftpUUIDString.equals("")) {
					outHandler.write("no TESSy id found",i,header.length-1);
					continue;
				}

				int ii = 0;
				FILE: for (File f : uploadFiles) {
					ii++;
					String newName = ftpUUIDString;
					if (uploadFiles.size()>1) {
						newName = newName + ".R"+Integer.toString(ii);
					}
					if (f.getName().toLowerCase().matches(".*fastq.*")) {
						newName = newName+".fastq";
					} else if (f.getName().toLowerCase().matches(".*fasta.*")) {
						newName = newName+".fasta";
					}

					if (f.getName().endsWith("gz")) {
						newName = newName+".gz";
					}

					long filesizeLocal = f.length();
					FileInputStream fis = new FileInputStream(f);
					String checksum = org.apache.commons.codec.digest.DigestUtils.md5Hex(fis);
					fis.close();
					String md5File = tmpPath+"/"+newName+".md5";
					BufferedWriter bw = new BufferedWriter(new FileWriter(md5File));
					bw.write(checksum);
					bw.close();

					//StandardFileSystemManager manager = new StandardFileSystemManager();
					FileSystemManager fsManager;
					try {
						
						FileSystemOptions fsOptions = new FileSystemOptions();
						SftpFileSystemConfigBuilder.getInstance().setStrictHostKeyChecking(fsOptions, "no");
						SftpFileSystemConfigBuilder.getInstance().setTimeout(fsOptions, 30000);
						fsManager = VFS.getManager();


						String uri = "sftp://"+sftpLogin+":"+sftpPass+"@"+sftpHost+":22/autoupload/"+newName;
						String uriMd5 = "sftp://"+sftpLogin+":"+sftpPass+"@"+sftpHost+":22/autoupload/"+(new File(md5File)).getName();
						FileObject localFile = fsManager.resolveFile(f.getAbsolutePath());
						FileObject fo = fsManager.resolveFile(uri, fsOptions);

						if (logArea!=null) {
							logArea.append("Uploading to SFTP: "+localFile.toString()+"\n");
						} else {
							System.out.println("Uploading to SFTP: "+localFile.toString());
						}

						fo.copyFrom(localFile, Selectors.SELECT_SELF);

						FileObject localFileMd5 = fsManager.resolveFile(md5File);
						FileObject foMd5 = fsManager.resolveFile(uriMd5, fsOptions);
						foMd5.copyFrom(localFileMd5, Selectors.SELECT_SELF);

						long filesizeUploaded = fo.getContent().getSize();
						if (filesizeUploaded != filesizeLocal) {
							outHandler.write("upload error",i,header.length-1);
							break FILE;
						}
						if (!foMd5.exists()) {
							outHandler.write("upload error",i,header.length-1);
							break FILE;
						}


					} catch (Exception e) {
						e.printStackTrace();
						outHandler.write("upload error",i,header.length-1);
						break FILE;

					} 



				}

				String ts = timestamp.format(new Date());
				outHandler.write(ts,i,header.length-1);
				metadata[i][header.length-1] = ts;

			}


		}





		return null;
	}

	private String getWgsProtocol(String instrument) {
		instrument = instrument.toLowerCase();
		System.out.println(instrument);

		if (instrument.startsWith("illumina hiseq") || instrument.startsWith("hiseq")) {
			return "HISEQ_2x100";
		} else if (instrument.startsWith("illumina miseq") || instrument.startsWith("miseq")) {
			return "MISEQ_2x250";
		} else if (instrument.startsWith("illumina nextseq") || instrument.startsWith("nextseq")) {
			return "NEXTSEQ_2x150";
		} else if (instrument.startsWith("illumina novaseq") || instrument.startsWith("novaseq")) {
			return "NEXTSEQ_2x150";
		} else if (instrument.startsWith("ion")) {
			return "IONTORRENT";
		} else { 
			return "ERROR";
		} 
	}

	private String codeToCountry(String countryCode) {
		if (countryCode.length()>2) {
			return countryCode;
		}
		countryCode = countryCode.toUpperCase();
		switch (countryCode) {
		case "SE":
			return "Sweden";
		case "FI":
			return "Finland";
		case "DK":
			return "Denmark";
		case "AT":
			return "Austria";
		case "FR":
			return "France";
		case "UK":
			return "United Kingdom";
		case "NL":
			return "The Netherlands";
		case "ES":
			return "Spain";
		case "IT":
			return "Italy";
		case "PL":
			return "Poland";
		case "IE":
			return "Ireland";
		case "IS":
			return "Iceland";
		case "EL":
			return "Greece";
		case "NO":
			return "Norway";
		case "PT":
			return "Portugal";
		case "SI":
			return "Switzerland";
		case "DE":
			return "Germany";
		case "CZ":
			return "Czech republic";
		case "BG":
			return "Bulgaria";
		case "LU":
			return "Luxembourg";
		case "BE":
			return "Belgium";
		case "HU":
			return "Hungary";
		case "EE":
			return "Estonia";
		case "LT":
			return "Lithuania";
		case "LV":
			return "Latvia";
		case "HR":
			return "Croatia";
		case "MT":
			return "Malta";
		case "CY":
			return "Cyprus";
		case "RO":
			return "Romania";
		case "SK":
			return "Slovakia";




		default:
			return "";
		}

	}

	private String countryToCode(String countryFull) {
		if (countryFull.length()==2) {
			return countryFull;
		}
		countryFull = countryFull.toLowerCase();
		switch (countryFull) {
		case "sweden":
			return "SE";
		case "finland":
			return "FI";
		case "denmark":
			return "DK";
		case "austria":
			return "AT";
		case "france":
			return "FR";
		case "united kingdom":
			return "UK";
		case "scotland":
			return "UK";
		case "england":
			return "UK";
		case "wales":
			return "UK";
		case "the netherlands":
			return "NL";
		case "netherlands":
			return "NL";
		case "spain":
			return "ES";
		case "italy":
			return "IT";
		case "poland":
			return "PL";
		case "ireland":
			return "IE";
		case "iceland":
			return "IS";
		case "greece":
			return "EL";
		case "norway":
			return "NO";
		case "portugal":
			return "PT";
		case "switzerland":
			return "SI";
		case "germany":
			return "DE";
		case "czech republic":
			return "CZ";
		case "bulgaria":
			return "BG";
		case "luxembourg":
			return "LU";
		case "belgium":
			return "BE";
		case "hungary":
			return "HU";
		case "estonia":
			return "EE";
		case "lithuania":
			return "LT";
		case "latvia":
			return "LV";
		case "croatia":
			return "HR";
		case "malta":
			return "MT";
		case "cyprus":
			return "CY";
		case "romania":
			return "RO";
		case "slovakia":
			return "SK";




		default:
			return "";
		}

	}

	private String getBatchid() {



		TessyIsolate iso = new TessyIsolate("","","");
		iso.setCredentials(batch.getCredentials());
		ArrayList<String> content = new ArrayList<String>();
		content.add("<GetLastBatchId xmlns=\"http://ecdc.europa.eu/tessy/v2\"></GetLastBatchId>");
		String response = iso.submitXml(content);
		String[] lines = response.split("\n",-1);
		for (String l : lines) {
			if (l.matches(".*<batchId>.*")) {
				String batchStr = l.replaceAll(".*<batchId>","").replaceAll("</batchId>.*","");
				int batchnum = Integer.parseInt(batchStr);
				batchnum++;
				batchStr = Integer.toString(batchnum);
				return batchStr;
			}
		}
		return "";


	}

	private void compressGzipFile(File file, File gzipFile) {
		try {
			FileInputStream fis = new FileInputStream(file);
			FileOutputStream fos = new FileOutputStream(gzipFile);
			GZIPOutputStream gzipOS = new GZIPOutputStream(fos);
			byte[] buffer = new byte[1024];
			int len;
			while((len=fis.read(buffer)) != -1){
				gzipOS.write(buffer, 0, len);
			}
			//close resources
			gzipOS.close();
			fos.close();
			fis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
