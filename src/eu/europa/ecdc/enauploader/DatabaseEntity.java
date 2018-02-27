package eu.europa.ecdc.enauploader;
import java.io.File;

public abstract class DatabaseEntity {

	String title;
	String accession;
	String centerName;
	String alias;
	File xmlFile;
	String type;
	
	DatabaseEntity(String c, String a) {
		title = a;
		accession = "";
		centerName=c;
		alias=a;
		
	}
	
	public void setAccession(String a) {
		accession = a;
	}
	public String getAccession() {
		return accession;
	}
	
	public void writeXml() {
		xmlFile = new File(ENAUtils.TMP_PATH+type+"_"+alias+".xml");
		writeXml(xmlFile);
	}
	
	public abstract void writeXml(File f);
	
	public abstract String getSubmitRow();

	public Object getXmlFile() {
		return xmlFile;
	}

	public String getType() {
		return type;
	}

	public String getAlias() {
		return alias;
	}
	
	
	
	
}
