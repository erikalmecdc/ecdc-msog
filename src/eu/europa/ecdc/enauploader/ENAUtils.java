package eu.europa.ecdc.enauploader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import it.sauronsoftware.ftp4j.FTPAbortedException;
import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPDataTransferException;
import it.sauronsoftware.ftp4j.FTPException;
import it.sauronsoftware.ftp4j.FTPIllegalReplyException;

public class ENAUtils {





	public static String getTaxid(String taxon) {

		String out = "";
		
		try {
			String line;
			BufferedReader br = new BufferedReader(new FileReader("taxids.tsv"));

			while((line = br.readLine())!=null) {
				String[] fields = line.split("\t");
				String name = fields[0];
				if (name.toLowerCase().equals(taxon.toLowerCase())) {
					br.close();
					return fields[1];
				}
			}
			
			br.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return out;
		
	}

	public static String[] readCsvHeader(File csvFile) {
		String line;


		try {
			BufferedReader br = new BufferedReader(new FileReader(csvFile));

			line = br.readLine();
			br.close();
			String[] header = line.split(",",-1);
			return header;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String[][] readCsv(File csvFile, boolean header) {
		
		
		String line;
		ArrayList<String[]> rowData = new ArrayList<String[]>();
		int cols = 0;
		try {
			BufferedReader br = new BufferedReader(new FileReader(csvFile));
			if (header) {
				line = br.readLine();
				String[] fields = line.split(",",-1);
				if (fields.length>cols) {
					cols = fields.length; 
				}
			}
			while ((line = br.readLine())!=null) {
				if (line.equals("")) {
					break;
				}
				String[] fields = line.split(",",-1);
				if (fields.length>cols) {
					cols = fields.length; 
				}
				if (fields[0].equals("")) {
					break;
				}
				rowData.add(fields);
			}

			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		String[][] data = new String[rowData.size()+1][cols];
		int i = 0;
		for (String[] rowdat : rowData) {
			
			for (int j = 0;j<rowdat.length;j++) {
				data[i][j] = rowdat[j];
			}
			i++;
			
		}
		return data;

	}

	public static void writeCsv(File file, String title, String[][] data, String[] header) {
		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new FileWriter(file));
			
			if (title!=null) {
				bw.write(title+"\n");
			}
			
			String row = String.join(",", header);
			bw.write(row+"\n");
			for (int i = 0; i< data.length;i++) {
				if (data[i][0]==null) {
					break;
				}
				String drow = String.join(",", data[i]);
				bw.write(drow+"\n");
			}
			
			bw.close();
		} catch (IOException e) {
			
			e.printStackTrace();
		}
	}
}
