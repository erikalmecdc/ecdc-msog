package eu.europa.ecdc.enauploader;

public class EcdcFullUploadWorker extends EcdcJob {

	// Rows to submit
	private int[] selectedRows;

	// Mandatory constructor
	EcdcFullUploadWorker(EcdcUploaderGUI gui, TessyBatch batch, String name, UploadConfig cfg, String[] headers,
			String[][] data) {
		super(gui, batch, name, cfg, headers, data);
		
	}
	
	// Method for settings the rows to submit, should be called before starting the job
	public void setRows(int[] selected) {
		selectedRows = selected;
		
	}

	// Main routine for this EcdcJob
	@Override
	protected Object doInBackground() {
		
		log("Submission to multiple systems started.");		
		setTitle("Submission to multiple systems");
		setStatus("Initializing");
		setProgress(2);
		
		if (selectedRows==null || selectedRows.length==0) {
			setStatus("Nothing to do, exiting.");
			setProgress(100);
			return null;
		}
		
		// This section is for submitting to ENA
		if (cfg.isSubmitEna()) {
			setStatus("Submitting to ENA");
			setProgress(5);
			log("Launching ENA job");
			
			// Create EnaSubmissionWorker
			EnaSubmissionWorker enaWorker = gui.getEnaSubmissionWorker(name, selectedRows);
			
			// Check if this job was interrupted
			if (isStopped()) {
				log("Job interrupted, exiting");
				setStatus("Error, job interrupted");
				setProgress(30);
				return null;
			}
			
			// If the worker was successfully created, run it and wait for the job to finish
			if (enaWorker!=null) {
				enaWorker.execute();
				boolean done=false;
				while (!done) {
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
					}
					
					// Check if this job was interrupted
					if (enaWorker.isStopped()) {
						log("Job interrupted, exiting");
						setStatus("Error, job interrupted");
						setProgress(30);
						return null;
					}
					if (enaWorker.isDone()) {
						done=true;
					}
				}
			} else {
				log("GUI returned null enaWorker, continuing without ENA");
			}
			
			
		}
		
		
		setStatus("Submitting to ECDC SFTP");
		setProgress(33);
		log("Launching SFTP UPLOAD job");
		
		// Create and run a job for submitting to SFTP
		EcdcSftpUploadWorker sftpWorker = gui.getSftpWorker(name, selectedRows);
		if (sftpWorker!=null) {
			sftpWorker.execute();
		} else {
			log("GUI returned null sftpWorker, likely an error in the isolate table");
			return null;
		}
		
		// Wait for the job to finish
		boolean done=false;
		while (!done) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
			}
			
			// Check if this job was interrupted
			if (sftpWorker.isStopped()) {
				log("Job interrupted, exiting");
				setStatus("Error, job interrupted");
				setProgress(45);
				return null;
			}
			if (sftpWorker.isDone()) {
				done=true;
			}
		}
		
		
		
		// This section is for submitting to TESSy
		if (cfg.isSubmitTessy()) {
			// Check if this job was interrupted
			if (isStopped()) {
				log("Job interrupted, exiting");
				setStatus("Error, job interrupted");
				setProgress(47);
				return null;
			}
			setStatus("Testing at TESSy");
			setProgress(50);
			log("TESSy test job");
			
			// Create and run a job for testing at TESSy
			EcdcTessyCreateAndTestWorker tessyTestWorker = gui.getEcdcTessyTestWorker(name, selectedRows);
			if (tessyTestWorker!=null) {
				tessyTestWorker.execute();
			} else {
				setStatus("Error, no valied entries selected");
				setProgress(55);
				log("GUI returned null tessyTestWorker, likely an error in the isolate table");
				return null;
			}
			
			// Wait for job to finish
			done=false;
			while (!done) {
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
				}
				
				// Check if this job was interrupted
				if (tessyTestWorker.isStopped()) {
					log("Job interrupted, exiting");
					setStatus("Error, job interrupted");
					setProgress(55);
					return null;
				}
				if (tessyTestWorker.isDone()) {
					done=true;
				}
			}
			
			// Check the validation results and decide whether to continue
			batch = tessyTestWorker.getBatch();
			String batchId = batch.getBatchId();
			if (!batch.passedValidation()) {
				log("Batch did not pass validation, quitting");
				setStatus("Error, the batch did not pass TESSy validation");
				setProgress(55);
				return null;
			}
			
			// Check if this job was interrupted
			if (isStopped()) {
				log("Job interrupted, exiting");
				setStatus("Error, job interrupted");
				setProgress(55);
				return null;
			}
			
			// Create and run a job for uploading to TESSy
			setStatus("Uploading to TESSy");
			setProgress(65);
			log("TESSy upload job");
			EcdcTessyUploadWorker tessyUploadWorker = gui.getEcdcTessyUploadWorker(batchId);
			if (tessyUploadWorker!=null) {
				tessyUploadWorker.execute();
			} else {
				log("GUI returned null tessyUploadWorker, likely an error in the isolate table");
				return null;
			}
			
			// Wait for job to finish
			done=false;
			while (!done) {
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
				}
				
				// Check if this job was interrupted
				// with batches going out of synch between the app and TESSy
				if (tessyUploadWorker.isStopped()) {
					log("Job interrupted, exiting");
					setStatus("Error, job interrupted");
					setProgress(67);
					return null;
				}
				if (tessyUploadWorker.isDone()) {
					done=true;
				}
			}
			
			// Check the validation results and decide whether to continue. This should pretty much never fail as the batch has been tested.
			// If the batch somehow fails at this stage, it must be rejected manually before retrying.
			batch = tessyUploadWorker.getBatch();
			if (!batch.passedValidation()) {
				log("Batch did not pass validation after Upload, quitting");
				setStatus("Error, the batch did not pass TESSy validation");
				setProgress(67);
				return null;
			}
			
			// Check if this job was interrupted
			// with batches going out of synch between the app and TESSy
			if (isStopped()) {
				log("Job interrupted, exiting");
				setStatus("Error, job interrupted");
				setProgress(67);
				return null;
			}
			setStatus("Approving in TESSy");
			setProgress(75);
			log("TESSy approve job");
			EcdcTessyApprovalWorker tessyApprovalWorker = gui.getEcdcTessyApprovalWorker(batchId);
			if (tessyApprovalWorker!=null) {
				tessyApprovalWorker.execute();
			} else {
				log("GUI returned null tessyApprovalWorker, likely an error in the isolate table");
				return null;
			}
			done=false;
			while (!done) {
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
				}
				
				// Check if this job was interrupted
				// with batches going out of synch between the app and TESSy
				if (tessyApprovalWorker.isDone()) {
					done=true;
				}
			}
			
			// Check if batch was actually approved
			batch = tessyApprovalWorker.getBatch();
			if (!batch.getState().equals("APPROVED")) {
				log("Batch Approval failed, quitting");
				setStatus("Error, batch approval failed");
				setProgress(77);
				return null;
			}
			
			
		}
		
		// This section is for submission to SFTP
		if (cfg.isSubmitFtp()) {
			
			// Check if this job was interrupted
			if (isStopped()) {
				log("Job interrupted, exiting");
				setStatus("Error, job interrupted");
				setProgress(77);
				return null;
			}
			
			
			
			
		}
		setStatus("Finished!");
		setProgress(100);
			
		return null;
	}

	

}
