package eu.europa.ecdc.enauploader;

public abstract class OutputHandler {

	
	
	public abstract void write(String value, int row, int col);
	public abstract void close();
	
}
