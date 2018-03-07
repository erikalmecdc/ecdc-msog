package eu.europa.ecdc.enauploader;
import java.io.File;

public abstract class DatabaseEntity {

	String title;
	String accession;
	String centerName;
	String alias;
	File xmlFile;
	String type;
	
	
	public String TMP_PATH;
	
	
	public void init() {
		File tmpDir = new File(TMP_PATH);
		tmpDir.mkdirs();
	}
	
	DatabaseEntity(String c, String a, Submission s) {
		title = a;
		accession = "";
		centerName=c;
		alias=a;
		if (s!=null) {
			TMP_PATH = s.getTmpPath();
		}
	}
	
	public void setAccession(String a) {
		accession = a;
	}
	public String getAccession() {
		return accession;
	}
	
	public void writeXml() {
		xmlFile = new File(TMP_PATH+"/"+type+"_"+alias+".xml");
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
	
	public void setTmpPath(String path) {
		TMP_PATH = path;
	}
	
	public String getTmpPath() {
		return TMP_PATH;
	}

	
}
