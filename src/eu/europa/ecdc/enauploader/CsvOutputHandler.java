package eu.europa.ecdc.enauploader;

import java.io.File;

public class CsvOutputHandler extends OutputHandler {

	
	String[][] data;
	String[] header;
	File outFile;
	String project;
	
	public CsvOutputHandler(File csvFile, File file, String proj) {
		data = ENAUtils.readCsv(csvFile, true);
		header = ENAUtils.readCsvHeader(csvFile);
		outFile = file;
		project = proj;
	}

	@Override
	public void write(String value, int row, int col) {
		data[row][col] = value;
		
	}

	@Override
	public void close() {
		ENAUtils.writeCsv(outFile,project,data,header);
		
	}

}
