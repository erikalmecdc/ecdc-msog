package eu.europa.ecdc.enauploader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

// Read and write routine for ImportConfig files.
// These can be replaced if another file format than serialized Java objects is to be implemented

public class ImportConfigHandler {

	public static ImportConfig loadConfig(File configFile) throws IOException, ClassNotFoundException {
		FileInputStream streamIn = new FileInputStream(configFile);
		ObjectInputStream objectinputstream = new ObjectInputStream(streamIn);
		ImportConfig config = (ImportConfig) objectinputstream.readObject();
		objectinputstream.close();
		return config;
	}
	
	public static void saveConfig(File configFile, ImportConfig config) throws IOException {
		FileOutputStream fout = new FileOutputStream(configFile);
		ObjectOutputStream oos = new ObjectOutputStream(fout);
		oos.writeObject(config);
		oos.close();
	}
}
