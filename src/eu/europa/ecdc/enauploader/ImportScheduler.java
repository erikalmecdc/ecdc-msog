package eu.europa.ecdc.enauploader;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import javax.swing.JTable;
import javax.swing.SwingWorker;

// This class starts a background thread which imports and submits data at scheduled intervals
public class ImportScheduler extends EcdcJob {

	// Time intervals in seconds for: NA, minute, hours, day
	private long[] unitAmounts = {-1, 60, 3600, 86400};
	private HashMap<String,Date> nextActions;
	private HashMap<String,String> importSubjects;
	private HashMap<String,Long> intervals;
	
	private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	
	// Constructor, ties the scheduler to a GUI
	ImportScheduler(EcdcUploaderGUI gui) {
		super(gui, null, null, null, null, null);
		
		intervals = new HashMap<String,Long>();
		nextActions = new HashMap<String,Date>();
		importSubjects = new HashMap<String,String>();
	}
	
	// Go through all the import configs and find any requests for scheduling
	// Only update if they have changed, to not interrupt running schedules
	public int setImportConfigHash(HashMap<String,ImportConfig> importConfigHash) {
	
		log("Loading scheduled imports and submissions...");
		int schedules = 0;
		for (String configName : importConfigHash.keySet()) {
			ImportConfig cfg = importConfigHash.get(configName);
			
			// Calculate interval in seconds
			long interval = cfg.getScheduleAmount() * unitAmounts[cfg.getScheduleUnit()];
			Long oldInterval = intervals.get(configName);
			if (oldInterval != null && interval == oldInterval) {
				//Interval is unchanged, do not update
				continue;
			}
			
			// If interval is negative, no scheduling has been configured. Get rid of any existing schedule for this config
			if (interval<=0) {
				intervals.put(configName, null);
				nextActions.put(configName, null);
				importSubjects.put(configName,null);
			
			// If scheduling, calculate the time for next import/upload 
			} else {
				Date nextAction = new Date( System.currentTimeMillis() + interval * 1000);
				intervals.put(configName, interval);
				nextActions.put(configName, nextAction);
				importSubjects.put(configName,cfg.getSubject());
				schedules++;
				SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				log("Scheduled "+configName+" for "+df.format(nextAction));
			}
		}
		log("Done loading. "+Integer.toString(schedules)+" different configurations scheduled.");
		return schedules;
	}
	
	// Main method for this worker, it waits until it is time to import/upload and then does so
	@Override
	protected Object doInBackground() throws Exception {
		
		setTitle("Import and submission scheduler");
		setStatus("Waiting for scheduled jobs");
		setProgress(5);
		
		// Run forever
		while (true) {
			
			Date now = new Date();
			
			// Iterate through nextActions HashMap
			for (String configName : nextActions.keySet()) {
				setStatus("Checking scheduled jobs");
				setProgress(15);
				// Check if the time for the next import/upload is non-null and in the past, if so, perform it
				Date nextAction = nextActions.get(configName);
				if (nextAction == null) {
					continue;
				}
				if (now.after(nextAction)) {
					setStatus("Performing import and submission for config "+configName);
					log("Performing import and submission for config "+configName);
					setProgress(45);
					Long interval = intervals.get(configName);
					importData(configName);
					
					
					// Calculate time for next action and schedule it
					nextAction = new Date(now.getTime() + interval * 1000);
					log("Setting next scheduled time for "+configName+" to "+dateFormat.format(nextAction));
					nextActions.put(configName, nextAction);
					
					//TODO TEMP for DEV
					//return null;
				}
				
			}
			
			setStatus("Waiting for scheduled jobs");
			setProgress(5);
			// Wait 10 seconds until next check
			Thread.sleep(10000);
		}
	}

	// Trigger import and upload from the GUI
	private void importData(String configName) {
		// TODO
		EcdcAutomationWorker worker = gui.getEcdcAutomationWorker(configName);
		if (worker!=null) {
			System.out.println("Starting scheduled activity");
			worker.execute();
			log("Job launched, waiting for results");
			setStatus("Job launched for "+configName+" waiting...");
			setProgress(60);
			boolean done = false;
			while(!done) {
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				if (worker.isDone()) {
					done = true;
					log("Finished scheduled activity");
				}
				
			}
			
		} else {
			log("Null AutomationWorker, job aborted. Please check import config for errors.");
		}
		
	}
}
