package eu.europa.ecdc.enauploader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.SwingWorker;

public abstract class EcdcJob extends SwingWorker<Object,Void> {

	// This abstract class should be used for background jobs launched by the Ecdc upload app
	// The constructor automatically adds the job to the GUI's job handler
	
	// doInbackground() must be implemented by the class inheriting this class, that is where the actual code goes
	//
	// A constructor also needs to be implemented, similar to the one below:
	// 	EcdcSftpUploadWorker(EcdcUploaderGUI gui, TessyBatch batch, String name, UploadConfig cfg, String[] headers, String[][] data) {
	//		super(gui, batch, name, cfg, headers, data);
	//	} 
	//
	// To set the status shown in the job manager, these three methods are used, the GUI is only updated on setProgress(int), so do that last.
	//	setTitle(String title);
	//	setStatus(String status);
	//	setProgress(int progress); progress goes from 0 (just started) to 100 (finished)
	//
	// To add to the job log, use the method:
	// log(String message)
	//
	
	protected String name;
	protected UploadConfig cfg;
	protected String[] headers;
	protected String[][] data;
	protected int batchId;
	protected EcdcUploaderGUI gui;
	protected TessyBatch batch;
	private String status;
	private String title;
	private String logText;
	private SimpleDateFormat dateFormat;
	private boolean stopped = false;
	private String id;
	
	EcdcJob (EcdcUploaderGUI gui, TessyBatch batch, String name, UploadConfig cfg, String[] headers,String[][] data) {
		this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		this.logText = "";
		this.gui = gui;
		this.batch = batch;
		this.name = name;
		this.cfg = cfg;
		this.headers = headers;
		this.data = data;
		gui.getJobHandler().addJob(this);
		this.addPropertyChangeListener(gui.getJobHandler());
		this.setProgress(1);
		SimpleDateFormat logDateFormat = new SimpleDateFormat("yyyy-MM-dd_HHmmss_SS");
		id = logDateFormat.format(new Date());
	}
	
	public boolean isStopped() {
		return stopped;
	}
	
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void log(String text){
		if (!logText.equals("")) {
			logText = logText+"\n";
		}
		logText=logText+"["+dateFormat.format(new Date())+"] "+text;
	}
	
	public String getLogText() {
		return logText;
	}
	
	public TessyBatch getBatch() {
		return batch;
	}

	public void stopJob(boolean b) {
		stopped  = true;
	}
	

	
	public void done() {
		
		File logDir = new File("./logs");
		if (!logDir.exists()) {
			logDir.mkdirs();
		}
		File logFile = new File(logDir.toString()+"/"+id+".log");
		
		log("Writing logfile: "+logFile.toString());
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(logFile));
			bw.write(logText);
			bw.close();
		} catch(IOException e) {
			log("Failed to write logfile to disk.");
		} 
		
		
	}
	
}
