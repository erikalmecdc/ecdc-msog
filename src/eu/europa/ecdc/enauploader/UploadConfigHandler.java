package eu.europa.ecdc.enauploader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class UploadConfigHandler {

	public static UploadConfig loadConfig(File configFile) throws IOException, ClassNotFoundException {
		FileInputStream streamIn = new FileInputStream(configFile);
		ObjectInputStream objectinputstream = new ObjectInputStream(streamIn);
		UploadConfig config = (UploadConfig) objectinputstream.readObject();
		objectinputstream.close();
		return config;
	}
	
	public static void saveConfig(File configFile, UploadConfig config) throws IOException {
		FileOutputStream fout = new FileOutputStream(configFile);
		ObjectOutputStream oos = new ObjectOutputStream(fout);
		oos.writeObject(config);
		oos.close();
	}
}
