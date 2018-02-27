package eu.europa.ecdc.enauploader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

public class Sample extends DatabaseEntity {

	private String taxon;
	HashMap<String,String> attributes;
	
	

	Sample(String c, String a) {
		super(c, a);
		type = "SAMPLE";
		attributes = new HashMap<String,String>();
	}
	
	public void setTaxon(String tax) {
		taxon = tax;
	}
	
	public void setAttribute(String key, String value) {
		attributes.put(key, value);
	}
	

	@Override
	public void writeXml(File f) {
		
		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new FileWriter(f));
			bw.write("<?xml version = '1.0' encoding = 'UTF-8'?>\n");
			bw.write("<SAMPLE_SET xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"ftp://ftp.sra.ebi.ac.uk/meta/xsd/sra_1_5/SRA.sample.xsd\">\n");
			bw.write("<SAMPLE alias=\""+alias+"\" center_name=\""+centerName+"\">\n");
			bw.write("<TITLE>"+title+"</TITLE>\n");
			bw.write("<SAMPLE_NAME>\n");
			bw.write("<TAXON_ID>"+ENAUtils.getTaxid(taxon)+"</TAXON_ID>\n");
			bw.write("<SCIENTIFIC_NAME>"+taxon+"</SCIENTIFIC_NAME>\n");
			bw.write("</SAMPLE_NAME>\n");
			bw.write("<SAMPLE_ATTRIBUTES>\n");
			
			bw.write("<SAMPLE_ATTRIBUTE>\n");
			bw.write("<TAG>organism</TAG>\n");
			bw.write("<VALUE>"+taxon+"</VALUE>\n");
			bw.write("</SAMPLE_ATTRIBUTE>\n");
			
			bw.write("<SAMPLE_ATTRIBUTE>\n");
			bw.write("<TAG>strain</TAG>\n");
			bw.write("<VALUE>"+alias+"</VALUE>\n");
			bw.write("</SAMPLE_ATTRIBUTE>\n");
			
			//This is the GMI checklist for metadata, might need to change this.
			bw.write("<SAMPLE_ATTRIBUTE>\n");
			bw.write("<TAG>ENA_CHECKLIST</TAG>\n");
			bw.write("<VALUE>ERC000028</VALUE>\n");
			bw.write("</SAMPLE_ATTRIBUTE>\n");
			
			
			for (String attribute : attributes.keySet()) {
				String value = attributes.get(attribute);
				bw.write("<SAMPLE_ATTRIBUTE>\n");
				bw.write("<TAG>"+attribute+"</TAG>\n");
				bw.write("<VALUE>"+value+"</VALUE>\n");
				bw.write("</SAMPLE_ATTRIBUTE>\n");
			}
			
			bw.write("</SAMPLE_ATTRIBUTES>\n");
			bw.write("</SAMPLE>\n");
			bw.write("</SAMPLE_SET> \n");
			
			
			bw.close();
		} catch (IOException e) {
			
			e.printStackTrace();
		}
	}

	@Override
	public String getSubmitRow() {
		return "<ADD schema=\"sample\" source=\""+xmlFile.toString()+"\"/>";
	}

}
