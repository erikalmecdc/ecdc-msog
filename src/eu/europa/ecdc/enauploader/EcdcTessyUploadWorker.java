package eu.europa.ecdc.enauploader;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

// Class for uploading a created and tested TESSy batch
public class EcdcTessyUploadWorker extends EcdcJob {

	
	// Mandatory constructor
	EcdcTessyUploadWorker(EcdcUploaderGUI gui, TessyBatch batch, String name, UploadConfig cfg, String[] headers,
			String[][] data) {
		super(gui, batch, name, cfg, headers, data);
		
	}

	// Constants
	private static final long WAITTIME = 5000; // Time to wait between tries, in ms
	private static final int MAXTRIES = 100; // How many times to wait for validation before giving up
	// Multiply these together to get the maximum total time before validation timeout
	
	// Main process for the job
	@Override
	protected Object doInBackground() {
		
		//Check if interrupted
		if (isStopped()) {
			log("Process cancelled");
			return null;
		}
		
		log("Started");
		String batchId = batch.getBatchId();
		log("batchId "+batchId);
		
		setTitle("Uploading batch "+batchId);
		setStatus("Uploading batch...");
		setProgress(5);
		
		// Check that batch has passed the test
		if (!batch.passedValidation()) {
			log("Batch did not pass test, it cannot be uploaded.");
			setStatus("Error, batch did not pass test");
			setProgress(10);
			return null;
		}
		
		log("Uploading");
		
		//Upload batch
		boolean uploaded = batch.upload();
		
		log("TESSy response: "+batch.getLastResponse());
		
		// Check if upload was successful
		if (!uploaded) {
			setStatus("Error, upload failed.");
			log("Error, upload failed.");
			setProgress(10);
			gui.error("TESSy upload of batch "+batchId+" failed");
			return null;
		}
		
		// Update UI with information that the batch has been uploaded
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String dateStr = df.format(new Date());
		log("Batch uploaded at "+ dateStr);
		gui.setBatch(name, batch);
		gui.setValidationResults(name, batch.getBatchId(),"U",dateStr,null,true);
		
		setStatus("Waiting for validation results...");
		log("Waiting for validation...");
		setProgress(30);
		
		// Wait for validation results
		boolean done = false;
		int tries = 0;
		while (!done) {
			
			// If giving up, show error in GUI
			if (tries > MAXTRIES) {
				setStatus("Error, validation results timeout after "+Long.toString(MAXTRIES*WAITTIME/1000)+" seconds");
				setProgress(35);
				gui.error("TESSy validation of batch "+batchId+" timed out after "+Long.toString(MAXTRIES*WAITTIME/1000)+" seconds.");
				return null;
			}
			
			// Check if validation is ready
			tries++;
			log("Validation check #"+Integer.toString(tries));
			done = batch.checkValidation();
			log("Results available: "+Boolean.toString(done));
			
			// Just some funky code to move the progressbar back and forth while waiting
			if (tries%2==0) {
				setProgress(40);
			} else {
				setProgress(35);
			}
			
			// Wait before next attempt to retrieve validations
			try {
				Thread.sleep(WAITTIME);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		log("Retrieving validation results.");
		setStatus("Retrieving validation results");
		setProgress(65);
		
		// Retrieve validation results
		HashMap<String,TessyValidationResult> results = batch.getValidation();
		String msg = batch.getLastMessage();
		log("Uploaded XML: "+msg);
		
		// Check if results are present, else show an error
		if (results==null) {
			log("Error, null results retrieved.");
			setStatus("Error while retrieving validation results");
			setProgress(65);
			gui.error("ERROR while retrieving validation results. You must retrieve validation results before proceeding.");
			return null;
		}
		
		// Log the validation results
		for (String k : results.keySet()) {
			TessyValidationResult val = results.get(k);
			log(val.toString());
		}
		
		setStatus("Saving...");
		setProgress(95);
		
		// Update GUI with batch info and validation results
		log("Updating active batch in GUI");
		gui.setBatch(name, batch);
		log("Updating isolate table");
		gui.setValidationResults(name, batch.getBatchId(),"V",dateStr,null,true);
		
		// Save GUI
		log("Saving...");
		gui.save();
		
		// Exit
		setStatus("Finished!");
		setProgress(100);
		log("Finished.");
		return null;
	}

}
