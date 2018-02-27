package eu.europa.ecdc.enauploader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import it.sauronsoftware.ftp4j.FTPAbortedException;
import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPDataTransferException;
import it.sauronsoftware.ftp4j.FTPException;
import it.sauronsoftware.ftp4j.FTPIllegalReplyException;

public class ENAUtils {

	public static final String FTP_HOST = "webin.ebi.ac.uk";
	public static final String TMP_PATH = "C:/ENAtmp/";
	public static final String CURL_PATH = "T:/Epidemiological Methods/MolecularSurveillance/Software/curl.exe";
	
	
	public static void init() {
		File tmpDir = new File(TMP_PATH);
		tmpDir.mkdirs();
	}

	
	
	public static String getTaxid(String taxon) {

		switch (taxon) {

		case "Listeria monocytogenes":
			return "1639";
		case "Salmonella enterica":
			return "28901";
		case "Escherichia coli":
			return "562";
		default:
			return "0";
		}
	}
}
