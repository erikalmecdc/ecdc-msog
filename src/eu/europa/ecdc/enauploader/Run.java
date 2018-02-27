package eu.europa.ecdc.enauploader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;

public class Run extends DatabaseEntity {

	ArrayList<File> files;
	ArrayList<File> originalFiles;
	private String experiment;
	int fileNum;

	Run(String c, String a, String exp) {
		super(c, a);
		type = "RUN";
		files = new ArrayList<File>();
		originalFiles = new ArrayList<File>();;
		experiment = exp;
		fileNum = 0;
	}

	public ArrayList<File> getOriginalFiles() {
		return originalFiles;
	}
	

	public void addFile(File f) {
		fileNum++;
		File fout = new File(ENAUtils.TMP_PATH+alias+"_"+Integer.toString(fileNum)+".fastq.gz");
		try {
			FileUtils.copyFile(f, fout);
			files.add(fout);
			originalFiles.add(f);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public ArrayList<File> getFiles() {
		return files;
	}
	

	@Override
	public void writeXml(File f) {
		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new FileWriter(f));
			bw.write("<?xml version = '1.0' encoding = 'UTF-8'?>\n");
			bw.write("<RUN_SET  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"ftp://ftp.sra.ebi.ac.uk/meta/xsd/sra_1_5/SRA.run.xsd\">\n");
			bw.write("<RUN alias=\""+alias+"\" center_name=\""+centerName+"\">\n");
			bw.write(" <EXPERIMENT_REF refname=\""+experiment+"\"/>\n");
			bw.write("<DATA_BLOCK>\n");
			bw.write("<FILES>\n");
			for (File ff : files) {
				String fp = ff.getName();
				FileInputStream fis = new FileInputStream(ff);
				String checksum = org.apache.commons.codec.digest.DigestUtils.md5Hex(fis);
				fis.close();
				bw.write("<FILE filename=\""+fp+"\" filetype=\""+"fastq"+"\" checksum_method=\"MD5\" checksum=\""+checksum+"\"/>\n");
			}
			
			bw.write("</FILES>\n");
			bw.write("</DATA_BLOCK>\n");
			bw.write("</RUN>\n");
			bw.write("</RUN_SET>\n");
			bw.close();
		} catch (IOException e) {

			e.printStackTrace();
		}

	}

	@Override
	public String getSubmitRow() {
		return "<ADD schema=\"run\" source=\""+xmlFile.toString()+"\"/>";
	}

}
