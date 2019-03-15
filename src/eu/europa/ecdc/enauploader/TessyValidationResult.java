package eu.europa.ecdc.enauploader;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TessyValidationResult implements Serializable {

	
	private static final long serialVersionUID = 3570429346152774993L;
	private int errors;
	private int warnings;
	private int remarks;
	private ArrayList<String> errorArray;
	private ArrayList<String> warningArray;
	private ArrayList<String> remarkArray;
	private boolean pass;
	private String guid;
	
	
	public String toString() {
		String out = "";
		String title = guid;
		if (title==null) {
			title = "GENERAL";
		}
		
		out = out + "Validation result for "+title+"\n";
		for (String s :errorArray) {
			out = out+"ERROR: "+s+"\n";
		}
		for (String s :warningArray) {
			out = out+"WARNING: "+s+"\n";
		}
		for (String s :remarkArray) {
			out = out+"REMARK: "+s+"\n";
		}
		out = out + "PASS: "+Boolean.toString(pass)+"\n";
		return out;
	}
	
	public void addError(String e) {
		errors++;
		errorArray.add(e);
	}
	
	public void addWarning(String e) {
		warnings++;
		warningArray.add(e);
	}
	
	public void addRemark(String e) {
		remarks++;
		remarkArray.add(e);
	}
	
	public void setGuid(String g) {
		guid = g;
	}
	
	public String getGuid() {
		return guid;
	}
	
	public int getErrorNum() {
		return errors;
	}
	public int getWarningNum() {
		return warnings;
	}
	public int getRemarkNum() {
		return remarks;
	}
	
	public ArrayList<String> getRemarks() {
		return remarkArray;
	}
	public ArrayList<String> getWarnings() {
		return warningArray;
	}
	public ArrayList<String> getErrors() {
		return errorArray;
	}
	
	public TessyValidationResult(String resultString) {
		pass = false;
		errors = 0;
		warnings = 0;
		remarks = 0;
		errorArray = new ArrayList<String>();
		warningArray = new ArrayList<String>();
		remarkArray = new ArrayList<String>();
		parse(resultString);
		
	}

	private void parse(String resultString) {
		
		
		String[] rows = resultString.split("\n");
		boolean header = false;
		boolean validationResult = false;
		String type = "";
		String message = "";
		String messageKey = "";
		
		
	
		Pattern pat = Pattern.compile(".*<recordGuid>(.*)</recordGuid>.*");
		
		
		
		for (String l : rows) {
			
			Matcher mat = pat.matcher(l);
			if (mat.find()) {
				guid = mat.group(1);
			}
			
			if (l.matches(".*</header>.*")) {
				header = false;
			}
			
			if (header) {
				String[] res = parseField(l);
				switch (res[0]) {
				case "noErrors":
					errors = Integer.parseInt(res[1]);
					break;
				case "noWarnings":
					warnings = Integer.parseInt(res[1]);
					break;
				case "noRemarks":
					remarks = Integer.parseInt(res[1]);
					break;
					default:
				}
			}
			
			if (l.matches(".*</validationResult>.*")) {
				message = message.replace("&amp;apos;","");
				switch (type) {
				case "remark":
					remarkArray.add(message+" ("+messageKey+")");
					break;
				case "warning":
					warningArray.add(message+" ("+messageKey+")");
					break;
				case "error":
					errorArray.add(message+" ("+messageKey+")");
					break;
				
				default:
				}
				
				validationResult = false;
			}
			
			
			if (validationResult) {
				String[] res = parseField(l);
				switch (res[0]) {
				case "type":
					type = res[1];
					break;
				case "message":
					message = res[1];
					break;
				case "messageKey":
					messageKey = res[1];
					break;
					default:
				}
			}
			

			if (l.matches(".*<validationResult>.*")) {
				validationResult = true;
				type = "";
				message = "";
				messageKey = "";
			}
			
			if (l.matches(".*<header>.*")) {
				header = true;
			}
			
			
		}
		
	}

	private String[] parseField(String l) {
		
		String[] res = new String[2];
		res[0] = "";res[1] = "";
		Pattern pat = Pattern.compile("<(.*)>(.*)<(.*)>");
		Matcher mat = pat.matcher(l);
		if (mat.find()) {
			
			res[0] = mat.group(1);
			res[1] = mat.group(2);
		}
		
		
		return res;
	}

	public void setPass(boolean b) {
		pass = true;
		
	}

	public boolean pass() {
		return pass;
	}
	

}
