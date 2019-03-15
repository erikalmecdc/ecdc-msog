package eu.europa.ecdc.enauploader;

import java.util.ArrayList;

import javax.swing.JTable;

// This class automates the process of Data import, sequence file linking and data submission
public class EcdcAutomationWorker extends EcdcJob {

	private ImportConfig importConfig;
	private JTable table;

	EcdcAutomationWorker(EcdcUploaderGUI gui, TessyBatch batch, String name, UploadConfig cfg, String[] headers,
			String[][] data) {
		super(gui, batch, name, cfg, headers, data);

	}

	public void setImportConfig (ImportConfig cfg) {
		this.importConfig = cfg;
	}

	public void setTable (JTable table) {
		this.table = table;
	}

	@Override
	protected Object doInBackground() throws Exception {

		setTitle("Automated import, linking and submission");
		setStatus("Initializing");
		setProgress(10);

		// Create EcdcImportWorker for data import
		EcdcImportWorker importWorker = gui.getEcdcImportWorker(name);


		// Check if this job was interrupted
		if (isStopped()) {
			log("Job interrupted, exiting");
			setStatus("Error, job interrupted");
			setProgress(30);
			return null;
		}


		// If the worker was successfully created, run it and wait for the job to finish
		if (importWorker!=null) {
			setStatus("Importing...");
			setProgress(25);
			importWorker.execute();
			boolean done=false;
			while (!done) {
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
				}

				// Check if this job was interrupted
				if (importWorker.isStopped()) {
					log("Job interrupted, exiting");
					setStatus("Error, job interrupted");
					setProgress(30);
					return null;
				}
				if (importWorker.isDone()) {
					done=true;
				}
			}
		} else {
			log("GUI returned null ImportWorker, exiting");
			setStatus("Error, null ImportWorker");
			setProgress(35);
			return null;
		}

		setStatus("Linking sequence data...");
		setProgress(35);
		int[] selected = new int[table.getRowCount()];
		for (int i = 0; i< selected.length;i++) {
			selected[i] = i;
		}
		EcdcLinkWorker linkWorker = gui.getEcdcLinkWorker(importConfig.getSubject(), selected);

		if (isStopped()) {
			log("Job interrupted, exiting");
			setStatus("Error, job interrupted");
			setProgress(40);
			return null;
		}



		// If the worker was successfully created, run it and wait for the job to finish
		if (linkWorker!=null) {
			setProgress(37);
			linkWorker.execute();
			boolean done=false;
			while (!done) {
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
				}

				// Check if this job was interrupted
				if (linkWorker.isStopped()) {
					log("Job interrupted, exiting");
					setStatus("Error, job interrupted");
					setProgress(45);
					return null;
				}
				if (linkWorker.isDone()) {
					done=true;

				}
			}
		} else {
			log("GUI returned null LinkWorker, exiting");
			setStatus("Error, null LinkWorker");
			setProgress(45);
			return null;
		}

		setStatus("Submitting data...");
		setProgress(60);


		ArrayList<Integer> uploadList = gui.getUploadList(table, cfg);


		int[] selectedForUpload = new int[uploadList.size()];
		for (int i = 0; i< uploadList.size(); i++) {
			selectedForUpload[i] = uploadList.get(i);
			System.out.println(uploadList.get(i));
		}
		EcdcFullUploadWorker uploadWorker = gui.getFullUploadWorker(importConfig.getSubject(), selectedForUpload);

		if (isStopped()) {
			log("Job interrupted, exiting");
			setStatus("Error, job interrupted");
			setProgress(70);
			return null;
		}

		// If the worker was successfully created, run it and wait for the job to finish
		if (uploadWorker!=null) {
			setProgress(65);
			uploadWorker.execute();
			boolean done=false;
			while (!done) {
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
				}

				// Check if this job was interrupted
				if (uploadWorker.isStopped()) {
					log("Job interrupted, exiting");
					setStatus("Error, job interrupted");
					setProgress(70);
					return null;
				}
				if (uploadWorker.isDone()) {
					done=true;
				}
			}
		} else {
			log("GUI returned null FullUploadWorker, exiting");
			setStatus("Error, null FullUploadWorker");
			setProgress(70);
			return null;
		}
		log("Job finished");
		setStatus("Automated import and upload finished.");
		setProgress(100);	
		return null;
	}

}
