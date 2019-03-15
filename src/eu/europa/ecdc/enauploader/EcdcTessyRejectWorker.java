package eu.europa.ecdc.enauploader;

// Class for sending reject requests to TESSy
public class EcdcTessyRejectWorker extends EcdcJob {

	// Mandatory constructor
	EcdcTessyRejectWorker(EcdcUploaderGUI gui, TessyBatch batch, String name, UploadConfig cfg, String[] headers,
			String[][] data) {
		super(gui, batch, name, cfg, headers, data);
	}

	// Main routine for the job
	@Override
	protected Object doInBackground() throws Exception {
		String batchId = batch.getBatchId();
		
		// Check if the process is interrupted
		if (isStopped()) {
			log("Process cancelled");
			return null;
		}
		log("Rejecting batch "+batchId);
		setTitle("Rejecting batch "+batchId);
		setProgress(15);
		
		// Send reject request to TESSy
		batch.reject();
		
		// Get TESSy request and reply texts
		String msg = batch.getLastMessage();
		log("Uploaded XML: "+msg);
		log("TESSy response: "+batch.getLastResponse());
		
		// Update GUI and save
		log("Saving...");
		setStatus("Saving...");
		setProgress(95);
		
		log("Updating Isolate table");
		gui.setValidationResults(name, batch.getBatchId(),"R",null,null,true);
		log("Updating GUI with active batch");
		gui.setBatch(name, batch);
		
		log("Saving");
		gui.save();
		
		log("Finished.");
		setStatus("Finished!");
		setProgress(100);
		
		return null;
	}

}
