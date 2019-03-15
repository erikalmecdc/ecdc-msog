package eu.europa.ecdc.enauploader;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

// This class implements a panel for the mapping of  a single data field
public class ImportMapPanel extends JPanel implements ActionListener {

	private static final long serialVersionUID = 6385671140689063455L;
	private String name;
	private JComboBox<String> mappingFieldnameBox;
	private String[] vocabulary;
	private JTextField constantField;
	private JComboBox<String> constantBox;
	boolean multiChoice;
	private LinkedHashMap<String,String> valueMap;
	

	ImportMapPanel(String name, ArrayList<String> options,String[] vocabulary, LinkedHashMap<String,String> valueMap) {
		this.name = name;
		this.vocabulary = vocabulary;
		
		// If the mapping of values between controlled vocabularies is empty, generate a default one
		if (valueMap==null && vocabulary != null) {
			valueMap = new LinkedHashMap<String,String>();
			for (String v : vocabulary) {
				valueMap.put(v,v);
			}
		}
	
		this.valueMap = valueMap;
		
		// Init GUI components
		this.setLayout(new FlowLayout(FlowLayout.RIGHT));
		mappingFieldnameBox = new JComboBox<String>();
		setMappingFields(options);
		
		JLabel label = new JLabel(this.name);
		
		// For free text, use a JtextField, for controlled vocabularies, use a JComboBox for the constant field
		if (vocabulary==null || vocabulary.length==0) {
			constantField = new JTextField("");
			constantField.setPreferredSize(new Dimension(80,25));
			multiChoice=false;
		} else {
			constantBox = new JComboBox<String>();
			constantBox.addItem("");
			for (String c : this.vocabulary) {
				constantBox.addItem(c);
			}
			constantBox.setPreferredSize(new Dimension(80,25));
			multiChoice=true;
		}
		
		
		this.add(label);
		this.add(mappingFieldnameBox);
		if (multiChoice) {
			this.add(constantBox);
		} else {
			this.add(constantField);
		}
		
		// If there is a controlled vocabulary, enable the button for mapping to it
		JButton valueMappingButton = new JButton("Map values");
		if (vocabulary==null || vocabulary.length==0) {
			valueMappingButton.setEnabled(false);
		} else {
			valueMappingButton.setEnabled(true);
		}
		valueMappingButton.setActionCommand("map");
		valueMappingButton.addActionListener(this);
		
		this.add(valueMappingButton);
		
		this.setPreferredSize(new Dimension(600,30));
	}
	
	// Get the mapping field name
	public String getMappingFieldname() {
		return (String) mappingFieldnameBox.getSelectedItem();
	}
	
	// Get the value map
	public LinkedHashMap<String,String> getValueMap() {
		return valueMap;
	}
	
	// Get the constant field value
	public String getConstant() {
		if (multiChoice) {
			return (String)constantBox.getSelectedItem();
		} else {
			return constantField.getText();
		}
	}
	
	// Get the field name (from metadata file)
	public String getName() {
		return name;
	}

	// Set the possible fields to map from
	public void setMappingFields(ArrayList<String> columns) {
		mappingFieldnameBox.removeAllItems();
		for (String option : columns) {
			mappingFieldnameBox.addItem(option);
		}
		repaint();
	}

	
	// Set the choice for field to map from
	public void setMappingField(String value) {
		mappingFieldnameBox.setSelectedItem(value);
	}

	// Set the constant value
	public void setConstant(String value) {
		if (multiChoice) {
			constantBox.setSelectedItem(value);
		} else {
			constantField.setText(value);
		}
		
	}

	
	// Handler for event for the value mapping button, it opens a dialog with a table of mappings
	// TODO: Make this nicer looking and add JComboBox for controlled vocabulary
	@Override
	public void actionPerformed(ActionEvent arg0) {
		
		// Generate data for mapping table
		String[] headers = {"Local value","TESSy value"};
		String[][] data = new String[valueMap.size()][headers.length];
		int c = 0;
		for (String k : valueMap.keySet()) {
			String[] row = new String[headers.length];
			row[0] = k;
			row[1] = valueMap.get(k);
			data[c] = row;
			c++;
		}
		
		// Create table
		DefaultTableModel model = new DefaultTableModel(data,headers);
		JTable table = new JTable(model);
		JScrollPane scroller = new JScrollPane(table);
		JPanel panel = new JPanel();
		panel.add(scroller);
		
		// Button for a dding a new rom to the mapping
		JButton addButton = new JButton("Add row");
		addButton.setActionCommand("add");
		addButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				model.addRow(new String[2]);
				
			}
		});
		panel.add(addButton);
		
		// Button for removing a row from the mapping
		JButton removeButton = new JButton("Remove row");
		removeButton.setActionCommand("remove");
		removeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				model.removeRow(table.getSelectedRow());
			}
		});
		panel.add(removeButton);
		
		//Button for saving the value map
		JButton saveButton = new JButton("Save");
		saveButton.setActionCommand("save");
		saveButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				for (int i = 0 ;i<table.getRowCount();i++) {
					valueMap.put((String)table.getValueAt(i, 0), (String)table.getValueAt(i, 1));
				}
			}
		});
		panel.add(saveButton);
		
		// Generate dialog
		JDialog dialog = new JDialog();
		dialog.setTitle("Mapping of values for field "+name);
		dialog.setContentPane(panel);
		dialog.pack();
		dialog.setVisible(true);
	}
	
	
}
