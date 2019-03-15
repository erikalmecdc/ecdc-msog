package eu.europa.ecdc.enauploader;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.JComboBox;
import javax.swing.JTextArea;

public class JTextActionArea extends JTextArea implements ActionListener {



	public JTextActionArea(String string, boolean edit) {
		super(string);
		this.setEditable(edit);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		setText("");
		JComboBox<String> box = (JComboBox<String>)e.getSource();
		String value = box.getSelectedItem().toString();
		try {
			String line;
			BufferedReader br = new BufferedReader(new FileReader(new File(value)));
			while ((line = br.readLine())!=null) {
				if (!line.equals("")) {
					append(line+"\n");
				}
			}
			br.close();
		} catch (IOException e2) {

			e2.printStackTrace();
		}
	}

}
