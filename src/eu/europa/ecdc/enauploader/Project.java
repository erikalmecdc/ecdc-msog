package eu.europa.ecdc.enauploader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Project extends DatabaseEntity {

	
	String description;
	
	
	Project(String c, String a, Submission s) {
		super(c,a,s);
		description = a;
		type = "PROJECT";
	}
	
	public void writeXml(File projectFile) {

		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new FileWriter(projectFile));
			bw.write("<?xml version = '1.0' encoding = 'UTF-8'?>\n");
			bw.write("<PROJECT_SET>\n");
			bw.write("<PROJECT alias=\""+alias+"\" center_name=\""+centerName+"\">\n");
			bw.write("<TITLE>"+title+"</TITLE>\n");
			bw.write("<DESCRIPTION>"+description+"</DESCRIPTION>\n");
			bw.write("<SUBMISSION_PROJECT>\n");
			bw.write("<SEQUENCING_PROJECT/>\n");
			bw.write("</SUBMISSION_PROJECT>\n");
			bw.write("</PROJECT>\n");
			bw.write("</PROJECT_SET>\n");
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public String getSubmitRow() {
		return "<ADD schema=\"project\" source=\""+xmlFile.toString()+"\"/>";
	}
	
}
