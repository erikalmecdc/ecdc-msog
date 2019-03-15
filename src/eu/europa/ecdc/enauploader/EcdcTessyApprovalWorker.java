package eu.europa.ecdc.enauploader;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

// Class for submitting reads and assembly to SFTP
public class EcdcTessyApprovalWorker extends EcdcJob {

	// Mandatory constructor
	EcdcTessyApprovalWorker(EcdcUploaderGUI gui, TessyBatch batch, String name, UploadConfig cfg, String[] headers,
			String[][] data) {
		super(gui, batch, name, cfg, headers, data);
		
	}

	// Main process for the event
	@Override
	protected Object doInBackground() throws Exception {
		
		String batchId = batch.getBatchId();
		
		// Check if process was interrupted
		if (isStopped()) {
			log("Process cancelled");
			return null;
		}
		log("Approving batch "+batchId);
		
		setTitle("Approving batch "+batchId);
		setStatus("Retrieving validation...");
		setProgress(5);
		
		// Batch must have passed validation before approval
		if (!batch.passedValidation()) {
			log("Batch did not pass validation, it cannot be approved. See deails below. Reject the batch and restart.");

			HashMap<String,TessyValidationResult> results = batch.getValidationResults();
			for (String k : results.keySet()) {
				TessyValidationResult val = results.get(k);
				log(val.toString());
			}

			setStatus("Error, batch did not pass validation");
			setProgress(10);
			return null;
		}
		
		// Check if process is interrupted
		if (isStopped()) {
			log("Process cancelled");
			return null;
		}
		
		setStatus("Approving...");
		setProgress(30);
		
		// Send approve request to TESSy
		log("Sending approve request");
		boolean approved = batch.approve();
		
		// Get TESSy reply
		String msg = batch.getLastMessage();
		log("Uploaded XML: "+msg);
		log("TESSy response: "+batch.getLastResponse());
		
		// If approval failed, exit with error
		if (!approved) {
			setStatus("Error, approval failed.");
			log("Error, approval failed");
			setProgress(35);
			gui.error("TESSy approval of batch "+batchId+" failed");	
			return null;
		}
		
		// Update GUI with TESSy approval date and save
		log("Saving...");
		setStatus("Saving...");
		setProgress(95);
	
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String dateStr = df.format(new Date());
		log("Updating GUI with active batch");
		gui.setBatch(name, batch);
		log("Updating Isolate table");
		gui.setValidationResults(name, batch.getBatchId(),"A",null,dateStr,true);
		log("Saving");
		gui.save();
		
		// Exit
		setStatus("Finished!");
		setProgress(100);	
		return null;
	}
}
