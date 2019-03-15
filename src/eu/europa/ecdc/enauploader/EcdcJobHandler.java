package eu.europa.ecdc.enauploader;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

//This class is the GUI-component and actual job manager for the Ecdc upload app 
public class EcdcJobHandler extends JFrame implements PropertyChangeListener, ActionListener{

	private static final long serialVersionUID = 6686812811438715750L;
	
	
	JPanel mainPanel;
	private JPanel jobPanel;
	private LinkedHashMap<EcdcJob, EcdcJobPanel> jobs;
	private LinkedHashMap<EcdcJobPanel, EcdcJob> panels;


	EcdcJobHandler() {
		
		// Init UI components
		mainPanel = new JPanel(new BorderLayout());
		
		JButton clearButton = new JButton("Clear all finished jobs");
		clearButton.setActionCommand("clear");
		clearButton.addActionListener(this);
		JPanel clearPanel = new JPanel();
		clearPanel.add(clearButton);
		
		mainPanel.add(clearPanel,BorderLayout.NORTH);
		
		jobPanel = new JPanel();
		jobs = new LinkedHashMap<EcdcJob, EcdcJobPanel>();
		panels = new LinkedHashMap<EcdcJobPanel,EcdcJob>();

		setSize(800,600);
		mainPanel.setSize(800,600);
		jobPanel.setSize(800,600);
		jobPanel.setLayout(new BoxLayout(jobPanel, BoxLayout.PAGE_AXIS));
		mainPanel.add(jobPanel,BorderLayout.CENTER);
		this.add(mainPanel);
		setTitle("ECDC WGS upload app - Job handler");
		this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		try {
			String imagePath = "media/ECDC2.png";
			InputStream imgStream = UploadConfigGUI.class.getResourceAsStream(imagePath );
			BufferedImage myImg;
			myImg = ImageIO.read(imgStream);
			setIconImage(myImg);
		} catch (Exception e) {
			e.printStackTrace();
		}
		pack();
	}

	
	// Add a new job. This is called by the EcdcJob constructor and does not explicitly need to be called
	public void addJob(EcdcJob job) {
		EcdcJobPanel panel = new EcdcJobPanel(job.toString(),this);
		jobs.put(job,panel);
		panels.put(panel,job);
		jobPanel.add(panel);
		job.log("Added job to job manager");

		// Update UI and make it pop to the front
		pack();
		jobPanel.repaint();
		setVisible(true);
		toFront();
	}
	
	// Remove job from handler
	public void removeJob(EcdcJobPanel panel) {

		EcdcJob job = panels.get(panel);
		if (job.isDone()) {
			jobPanel.remove(panel);
			jobs.remove(job);
			panels.remove(panel);
		}
		if (panels.isEmpty()) {
			setVisible(false);
		}
		pack();
		repaint();
	}

	// Try to interrupt job
	public void cancelJob(EcdcJobPanel panel) {
		EcdcJob job = panels.get(panel);
		
		// If the job is already gone, show that it is cancelled 
		if (job==null) {
			panel.cancel();
			return;
		}
		
		// Try to cancel the job, if successful, show that it is cancelled
		// Note that jobs may be impossible to cancel at some stages, the individual EcdcJob
		// implementations have to handle the signal
		job.stopJob(false);
		if (job.isDone()) {
			panel.cancel();
		}

	}

	// When any job is updated, update UI
	@Override
	public void propertyChange(PropertyChangeEvent evt) {

		// Get the source job
		EcdcJob source = (EcdcJob) evt.getSource();
		System.out.println("job event! "+source.toString());
		
		// Get the visual job panel
		EcdcJobPanel panel = jobs.get(source);
		if (panel==null) {
			return;
		}
		
		// If job made progress, update progress
		if (evt.getPropertyName().equals("progress")) {
			int intVal = (Integer)evt.getNewValue();
			panel.setProgress(intVal);
		}

		// Set title and status
		String status = source.getStatus();
		String title = source.getTitle();
		panel.setLabel(status);
		if (title!=null) {
			panel.setTitle(title);
		}
		
		// Check if job finished, if so, update panel reflecting this information
		if (source.isDone()) {
			panel.setDone();
		}

	}

	// This method opens a window where the log for a certain job is shown.
	public void showLog(String title, EcdcJobPanel panel) {
		EcdcJob job = panels.get(panel);
		String logText = job.getLogText();

		JFrame f = new JFrame("Log for "+title);
		JPanel logPanel = new JPanel();

		f.getContentPane().add(logPanel, "Center");
		JTextArea ta = new JTextArea(logText);

		JScrollPane scroller = new JScrollPane(ta);
		scroller.setPreferredSize(new Dimension(1200, 800));
		logPanel.add(scroller);

		try {
			String imagePath = "media/ECDC2.png";
			InputStream imgStream = UploadConfigGUI.class.getResourceAsStream(imagePath );
			BufferedImage myImg;
			myImg = ImageIO.read(imgStream);
			f.setIconImage(myImg);
		} catch (Exception e) {
			e.printStackTrace();
		}

		f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		f.setSize(1300, 900);
		f.setVisible(true);
	}

	// Remove finished jobs from UI
	@Override
	public void actionPerformed(ActionEvent e) {
		LinkedHashSet<EcdcJobPanel> jobPanels = new LinkedHashSet<EcdcJobPanel>(panels.keySet());
		for (EcdcJobPanel p : jobPanels) {
			removeJob(p);
		}
	}




}
