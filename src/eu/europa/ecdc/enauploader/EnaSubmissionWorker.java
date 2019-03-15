package eu.europa.ecdc.enauploader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;

// This class submits sequence data and minimal metadata (year, country, sequencing instrument) to ENA 
public class EnaSubmissionWorker extends EcdcJob {

	private static final int EXPECTED_FIELDS = 5;
	private int enaColumn;
	private OutputHandler outputHandler;

	// Mandatory constructor
	EnaSubmissionWorker(EcdcUploaderGUI gui, TessyBatch batch, String name, UploadConfig cfg, String[] headers,
			String[][] data) {
		super(gui, batch, name, cfg, headers, data);

	}

	// Set output handler object
	public void setOutputHandler(OutputHandler handler) {
		this.outputHandler = handler;
	}

	// Set table column for output to GUI
	public void setEnaColumn(int col) {
		enaColumn = col;
	}


	// Main routine for the job
	@Override
	protected Object doInBackground() {

		// Get all config info
		String studyAcc = cfg.getEnaProjectAcc();
		String center = cfg.getEnaCenter();
		String checklist = cfg.getEnaChecklist();
		String login = cfg.getEnaLogin();
		String organism = cfg.getOrganism();
		char[] password = cfg.getEnaPassword();
		boolean anonymize = cfg.getEnaAnonymize();
		boolean prod = cfg.getEnaProd();
		System.out.println("PROD: "+Boolean.toString(prod));

		String ftpHost = cfg.getEnaFtpHost();
		String curlPath = cfg.getCurlPath();
		String tmpPath = cfg.getTmpPath();

		// Log parameters used
		log("ENA parameters");
		log("Study: "+studyAcc);
		log("Center: "+center);
		log("Checklist: "+checklist);
		log("Login: "+login);
		log("Organism: "+organism);
		log("Anonymize: "+anonymize);
		log("ENA prodcution: "+prod);
		log("FTP: "+ftpHost);
		log("CURL: "+curlPath);
		log("TMP: "+tmpPath);

		log("Starting...");
		setTitle("Uploading to ENA study "+studyAcc);
		setStatus("Checking configs...");
		setProgress(5);

		// Check that all required config fields are there
		if (password.length==0 || studyAcc.equals("") || organism.equals("") || center.equals("") || login.equals("") || checklist.equals("")) {
			log("Organism, Study accession, Center, Login, Password, and Checklist are mandatory fields for ENA submission.");
			log("Aborting ENA submission.");
			
			setStatus("Error, missing fields in config");
			log("Config is missing information, aborting.");
			setProgress(10);
			return null;
		}

		// Check that there is data to submit
		if (data == null || data.length==0) {
			log("There are no data to submit.");
			log("Aborting ENA submission.");
			
			setStatus("Error, no data selected for submission");
			log("No data, aborting.");
			setProgress(10);
			return null;
		}

		// Check that data has the correct format
		if (headers.length!=EXPECTED_FIELDS || data[0].length!=EXPECTED_FIELDS) {
			log("Unexpected number of columns in data or headers. Expected number: "+Integer.toString(EXPECTED_FIELDS));
			log("Aborting ENA submission.");
			
			setStatus("Error, data formatting not recongnized");
			log("Data formatting error");
			setProgress(10);
			return null;
		}

		int failed = 0;
		int success = 0;
		int increment = 75/data.length;
		int total = data.length;
		
		if (total==0) {
			setStatus("Error, no valid entries selected");
			setProgress(20);
			return null;
		}
		
		// Iterate over each isolate entry
		ISOLATE: for (int i = 0;i<data.length;i++) {
			
			// Extract fields from tabular data
			// TODO: Improve tabular data transfer API
			String indexStr = data[i][0];
			String name = data[i][1];
			String year = data[i][2];
			String fileStr = data[i][3];
			String wgsProtocol = data[i][4];
			String instrument = getInstrument(wgsProtocol);

			log("Preparing ENA submission for "+name+" (index "+indexStr+")");
			
			setStatus("Checking "+name);
			setProgress(15+increment*i+increment/4);
			
			// RecordId is required
			if (name.equals("")) {
				log("RecordId is missing, skipping");
				continue;
			}
			
			// Year is required
			if (year.equals("")) {
				log("Year is missing, skipping");
				continue;
			}
			
			// Read files required
			if (fileStr.equals("")) {
				log("Read files missing, skipping");
				continue;
			}
			
			// WgsProtocol required
			if (wgsProtocol.equals("")) {
				log("WGS protocol missing, skipping");
				continue;
			}
			
			String sname = name;
			
			// If anonymize, generate a random GUID for the submission
			if (anonymize) {
				UUID uuid = UUID.randomUUID();
				String randomUUIDString = uuid.toString();
				sname = center+"_"+randomUUIDString;
				log("Anonymized: "+randomUUIDString);
			}
			log("Creating new submission "+"sub_"+sname);
			
			// Create a submission object and set config parameters
			Submission s = new Submission(center,"sub_"+sname);
			s.setFtpExist(false);
			s.setFtpHost(ftpHost);
			s.setCurlPath(curlPath);
			s.setTmpPath(tmpPath);
			log("Setting production to "+prod);
			s.useProductionServer(prod);
			log("Setting credentials for "+login);
			s.setLogin(login, new String(password));

			// Create a Wrapper object which contains both Sample, Experiment and Run for ENA
			SampleWrapper wrap = new SampleWrapper(center,studyAcc, sname,s);

			// Set ENA checklist
			wrap.sample.setAttribute("checklist",checklist);
			
			
			// Iretare through the read files
			String[] readFiles = fileStr.split(";");
			int fileNum = 0;
			for (String fname : readFiles) {
				File f = new File(fname);
				if (!f.exists()) {
					log("For isolate: "+name+", file "+fname+" is missing, skipping this isolate.");
					continue ISOLATE;
				}

				
				// Calculate MD5 checksum for the reads file
				FileInputStream fis;
				String checksum;
				try {
					fis = new FileInputStream(f);
					checksum = org.apache.commons.codec.digest.DigestUtils.md5Hex(fis);
					log("Checksum (MD5): "+checksum);
					fis.close();
				} catch (IOException e) {
					log("Failed to get checksum for "+f.toString());
					continue ISOLATE;
				}

				// Add the file to the Run object
				wrap.run.addFile(f);
				log("Added read file "+f.toString());
				fileNum++;

				// Set the MD5 checksum for the file
				wrap.run.setMd5Hex(f.getName(), checksum);
			}
			
			// Set taxid
			wrap.sample.setTaxon(organism);

			// Set instrument
			log("Setting instrument to: "+instrument);
			wrap.experiment.setInstrument(instrument);

			setStatus("Uploading "+Integer.toString(fileNum)+" files for "+name);
			setProgress(15+increment*i+increment/3);

			// Add the entry to the submission
			s.addEntry(wrap);
			
			// Upload the read files
			log("Uploading files");
			s.uploadFiles();

			setStatus("Submitting XML for "+name);
			setProgress(15+increment*i+increment);

			// Submit XML and check whether ENA returned a success message or an error and whether a run accession was returned
			String runAcc = "";
			log("Submitting XML");
			if (s.submit(this)) {
				runAcc = wrap.getRun().getAccession();
				if (runAcc.equals("")) {
					failed++;
					log("Submission of isolate "+name+", failed.");
					continue ISOLATE;
				} else {
					success++;
					log("Submission of isolate "+name+", complete.");
				}
			} else {
				failed++;
				log("Submission of isolate "+name+", failed.");
				continue ISOLATE;
			}
			
			// Write output to GUI and save
			log("Writing accession to isolate table");
			outputHandler.write(runAcc, Integer.parseInt(indexStr), enaColumn);
			gui.save();
		}

		// Exit
		setStatus("Finished, "+Integer.toString(success)+"/"+Integer.toString(success+failed)+" successful.");
		setProgress(100);
		log("Finished.");

		return null;
	}


	// TODO: This needs to be moved to config at some point
	// This translates between TESSy WgsProtocol and ENA Instruments, both are coded value lists
	private String getInstrument(String wgsProtocol) {

		switch (wgsProtocol.toUpperCase()) {
		case "IONTORRENT":
			return "Ion Torrent S5";
		case "MISEQ_2X150":
		case "MISEQ_2X250":
		case "MISEQ_2X300":
			return "Illumina MiSeq";
		case "HISEQ_2X100":
			return "Illumina HiSeq 1000";
		case "NEXTSEQ_2X150":
			return "NextSeq 500";
		default:
			return "Illumina MiSeq";

		}
	}
}
