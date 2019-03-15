package eu.europa.ecdc.enauploader;

import javax.swing.JTable;

public class JTableOutputHandler extends OutputHandler {

	
	private JTable outTable;

	JTableOutputHandler (JTable table) {
		outTable = table;
	}

	@Override
	public void write(String value, int row, int col) {
		outTable.setValueAt(value, row, col);
		System.out.println(value);
	}

	@Override
	public void close() {
		
		
	}
}
