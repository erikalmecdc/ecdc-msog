package eu.europa.ecdc.enauploader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.bind.DatatypeConverter;

// Class for creating a batch object and testing it in TESSy
public class EcdcTessyCreateAndTestWorker extends EcdcJob {

	// Mandatory constructor
	EcdcTessyCreateAndTestWorker(EcdcUploaderGUI gui, TessyBatch batch, String name, UploadConfig cfg, String[] headers,
			String[][] data) {
		super(gui, batch, name, cfg, headers, data);
	}

	// Main routine for job
	@Override
	protected Object doInBackground() {
		
		// Check if job is interrupted
		if (isStopped()) {
			log("Process cancelled");
			return null;
		}
		log("Started");
		setStatus("Requesting new batchId...");
		setProgress(5);
		
		// Create a TESSy request for checking last batchId
		// A little bit of a hack to use the TessyIsolate class like this, but it works nicely.
		TessyIsolate iso = new TessyIsolate("","","");
		iso.setCredentials(batch.getCredentials());
		ArrayList<String> content = new ArrayList<String>();
		content.add("<GetLastBatchId xmlns=\"http://ecdc.europa.eu/tessy/v2\"></GetLastBatchId>");
		log("XML submitted: "+content);
		String response = iso.submitXml(content);
		log("TESSy response: "+response);
		if (response==null) {
			setStatus("Error, failed to get next batchId");
			setProgress(10);
			gui.error("Retrieving last used batch id from TESSy failed.");
			log("No batchId returned.");
			return null;
		}
		
		// Check if job is interrupted
		if (isStopped()) {
			log("Process cancelled");
			return null;
		}

		setStatus("Parsing batchId...");
		setProgress(30);
		
		// Parse the batchId
		// TODO: The entire batchId retrieval could be moved to a method and improved
		String[] lines = response.split("\n",-1);
		boolean foundId = false;
		for (String l : lines) {
			if (l.matches(".*<batchId>.*")) {
				String batchStr = l.replaceAll(".*<batchId>","").replaceAll("</batchId>.*","");
				log("Last used batchId: "+batchStr);
				int batchnum = Integer.parseInt(batchStr);
				batchnum++;
				batchStr = Integer.toString(batchnum);
				log("Next batchId: "+batchStr);
				batch.setId(batchStr);
				foundId = true;
			}
		}
		
		// If fail to get next batchId from TESSy, exit
		if (!foundId) {
			setStatus("Error, failed to get next batchId");
			setProgress(35);
			gui.error("Retrieving last used batch id from TESSy failed.");
			log("No batchId returned.");
			return null;
		}
		
		// Check if interrupted
		if (isStopped()) {
			log("Process cancelled");
			return null;
		}
		
		setStatus("Creating batch...");
		log("Creating batch...");
		setTitle("Testing batch "+batch.getBatchId());
		setProgress(35);
		
		// Put data table in arraylist
		// TODO: Data table API between GUI and jobs should be improved
		ArrayList<Integer> rows = new ArrayList<Integer>();
		for (int i = 0;i<data.length;i++) {
			rows.add(Integer.parseInt((data[i][0])));
			log("Adding row from isolate table: "+data[i][0]);
		}
		
		//TODO: This is the parsing of the tabular data needed for submission
		// It clearly needs improvement.
		for (int i = 0;i<data.length;i++) {
			
			
			
			TessyIsolate isolate = new TessyIsolate(data[i][4],data[i][1],cfg.getTessyCountry());
			log("Adding isolate: "+data[i][4]+" "+data[i][1]+" "+cfg.getTessyCountry());

			for (int j = 5;j<headers.length;j++) {
				
				// For fields that are not null/empty and do not contain a date, set them as fields in the TESSy isolate record
				if (data[i][j]!=null && (!data[i][j].equals("") || headers[j].startsWith("Date"))) {
					isolate.setField(headers[j],data[i][j]);
				}
			}
			
			//If ENA accession is supplied, set it as a field
			if (data[i][2]!=null && !data[i][2].equals("")) {
				isolate.setField(headers[2],data[i][2]);
			}
			
			// Check if assembly data is supplied
			if (data[i][3]!=null && !data[i][3].equals("")) {
				File fastaFile = new File(data[i][3]);
				log("Adding FASTA file "+fastaFile.toString());
				
				//Gzip, Encode and add assembly data 
				byte[] fileContents;

				//Read FASTA data
				try {
					fileContents = Files.readAllBytes(fastaFile.toPath());
				} catch (IOException e) {	
					log("Error reading FASTA file "+fastaFile.toString());
					setStatus("Error, reading FASTA file");
					setProgress(45);
					return null;
				}
				
				// Gzip data
				byte[] gzippedContents = EcdcUtils.gzip(fileContents);
				
				// Encode as Base64 and set as a field
				String assemblyBase64String = DatatypeConverter.printBase64Binary(gzippedContents);
				isolate.setField(headers[3],assemblyBase64String);
				
			}
			// Add isolate entry to batch
			batch.addIsolate(isolate);
		}
		
		// Check if interrupted
		if (isStopped()) {
			log("Process cancelled");
			return null;
		}
		
		log("Testing batch...");
		setStatus("Testing batch...");
		setProgress(45);
		
		// Test batch at TESSy
		HashMap<String,TessyValidationResult> results = batch.test();
		
		// If results are somehow null, give an error
		if (results == null) {
			setStatus("Error, failed to find validation results");
			setProgress(50);
			log("Error, failed to find validation results (null response from batch.test()).");
			return null;
		}
		
		// Log the validation results
		for (String k : results.keySet()) {
			TessyValidationResult val = results.get(k);
			log(val.toString());
		}
		
		
		setStatus("Saving...");
		setProgress(90);
		log("Setting current batch: "+name+": "+batch.getBatchId()+" ("+batch.getState()+")");
		
		// Update the GUI with batch information
		log("Updating isolate table with batch information");
		gui.setBatch(name, batch);
		
		// Update the GUI with validation results
		log("Updating isolate table with validation results");
		gui.setValidationResults(name, batch.getBatchId(),"T",null,null,false);
		
		// Save
		log("Saving results...");
		gui.save();

		
		// Exit
		log("Finished!");
		setStatus("Finished!");
		setProgress(100);
		
		return null;
	}
}
