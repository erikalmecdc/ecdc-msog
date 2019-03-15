package eu.europa.ecdc.enauploader;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.TitledBorder;

public class EcdcJobPanel extends JPanel implements ActionListener {

	private static final long serialVersionUID = 8549159337996248380L;
	private JProgressBar bar;
	private JLabel label;
	private EcdcJobHandler handler;
	private JButton closeButton;
	private JButton logButton;

	EcdcJobPanel(String title, EcdcJobHandler handler) {

		// Init UI components
		this.handler=handler;
		setBorder(BorderFactory.createTitledBorder(title));
		label = new JLabel(title);
		bar = new JProgressBar();
		bar.setMaximum(100);
		bar.setMinimum(0);
		bar.setValue(0);
		add(label);
		add(bar);
		setSize(100,500);
		closeButton = new JButton("Cancel");
		closeButton.setActionCommand("Cancel");
		closeButton.addActionListener(this);
		add(closeButton);
		logButton = new JButton("Show log");
		logButton.setActionCommand("log");
		logButton.addActionListener(this);
		add(logButton);
	}

	// Set progressbar, from 0 to 100
	public void setProgress(int intVal) {
		bar.setValue(intVal);

		// If progress at max, change button text and action
		if (intVal==100) {
			setDone();	
		}
		repaint();
	}

	// Change action and text on button to reflect that the job is finished
	public void setDone() {
		closeButton.setActionCommand("Remove");
		closeButton.setText("Remove");
		repaint();
	}

	// Update the text next to the progressbar
	public void setLabel(String l) {
		if (l==null) {
			return;
		}
		label.setText(l);
		if (l.startsWith("Error")) {
			closeButton.setText("Remove");
			closeButton.setActionCommand("Remove");
		}
		repaint();
	}

	// Update the title of the job
	public void setTitle(String l) {
		TitledBorder b = (TitledBorder)this.getBorder();
		b.setTitle(l);
	}


	// Reflect visually that a job has been cancelled
	public void cancel() {
		closeButton.setText("Remove");
		closeButton.setActionCommand("Remove");
		setLabel("Cancelled");
		repaint();
	}

	// Action for the buttons
	@Override
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		action(cmd);
	}

	
	// The various actions possible for the buttons
	// The handler should perform all operations on jobs and then call this class for updating UI
	public void action(String cmd) {

		// Cancel the job
		if (cmd.equals("Cancel")) {
			handler.cancelJob(this);

		// Remove the job
		} else if (cmd.equals("Remove")) {
			handler.removeJob(this);

		// Open log
		} else if (cmd.equals("log")) {
			String t = ((TitledBorder)this.getBorder()).getTitle();
			handler.showLog(t,this);

		}
	}

}
