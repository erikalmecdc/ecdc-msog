package eu.europa.ecdc.enauploader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

import it.sauronsoftware.ftp4j.FTPClient;

public class Submission extends DatabaseEntity {

	//Default account settings
	public String LOGIN = "Webin-NNN";
	public String PASSWORD = "xxxxx";
	public String API_URL = "https://www-test.ebi.ac.uk/ena/submit/drop-box/submit/?auth=ENA";
	
	
	
	
	HashMap<String,DatabaseEntity> entries;
	private boolean uploaded; 
	
	Submission(String c, String a) {
		super(c, a);
		entries = new HashMap<String,DatabaseEntity>();
		type = "SUBMISSION";
		uploaded = false;
	}

	public void useProductionServer(boolean p) {
		if (p = true) {
			// production API
			API_URL = "https://www.ebi.ac.uk/ena/submit/drop-box/submit/?auth=ENA";
		} else {
			// test API
			API_URL = "https://www-test.ebi.ac.uk/ena/submit/drop-box/submit/?auth=ENA";
		}
	}
	
	public void SetLogin(String l, String pwd) {
		LOGIN = l;
		PASSWORD = pwd;
	}
	
	public void addEntry(DatabaseEntity e) {
		entries.put(e.getAlias(),e);
	}
	
	public void addEntry(SampleWrapper e) {
		entries.put(e.sample.getAlias(),e.sample);
		entries.put(e.experiment.getAlias(),e.experiment);
		entries.put(e.run.getAlias(),e.run);
	}
	
	public boolean upload(ArrayList<File> files) {

		
		
		try {
			FTPClient client = new FTPClient();
			client.connect(ENAUtils.FTP_HOST);
			client.login(LOGIN, PASSWORD);
			for (File f : files) {
				boolean success = false;
				while (!success) {
					try {
						System.out.println("Uploading " + f.toString());
						client.upload(f);
						success=true;
					} catch (Exception e) {
						success=false;
						Thread.sleep(5000);
						client.connect(ENAUtils.FTP_HOST);
						client.login(LOGIN, PASSWORD);
						e.printStackTrace();
					}
				}
			}
			client.disconnect(true);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	
	@Override
	public void writeXml(File f) {
	
		BufferedWriter bw2;
		try {
			bw2 = new BufferedWriter(new FileWriter(f));

			bw2.write("<?xml version='1.0' encoding='UTF-8'?>\n");
			bw2.write("<SUBMISSION alias=\""+alias+"\" center_name=\""+centerName+"\">\n");
			bw2.write("<ACTIONS>\n");
			
			for (String k : entries.keySet()) {
				DatabaseEntity e = entries.get(k);
				e.writeXml();
				bw2.write("<ACTION>\n");
				bw2.write(e.getSubmitRow());
				bw2.write("</ACTION>\n");
			}
			
			bw2.write("</ACTIONS>\n");
			bw2.write("</SUBMISSION>\n");

			bw2.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	
	public boolean submit() {
		
		
		
		if (!uploaded) {
			System.out.println("DATA FILES HAVE NOT SUCCESSFULLY BEEN UPLOADED!");
			return false;
		}
		writeXml();
		curlFiles();
		return true;
		
	}
	
	
	
	public void uploadFiles() {
	
		ArrayList<File> files = new ArrayList<File>();
		for (String k: entries.keySet()) {
			DatabaseEntity e = entries.get(k);
			if(e.getType().equals("RUN")) {
				ArrayList<File> localFiles = ((Run)e).getFiles();
				files.addAll(localFiles);
			}
		}
		boolean success = upload(files);
		if (success) {
			uploaded = true;	
		} else {
			uploaded = false;
		}
		
	}

	private void curlFiles() {
		
		ArrayList<String> commands = new ArrayList<String>();
		commands.add(ENAUtils.CURL_PATH);
		commands.add("-k");
		commands.add("-F");
		commands.add(type+"=@"+xmlFile.toString());
		
		for (String k: entries.keySet()) {
			DatabaseEntity e = entries.get(k);
			commands.add("-F");
			String curlStr = "\""+e.getType()+"=@"+e.getXmlFile().toString()+"\"";
			commands.add(curlStr);
		}
		commands.add("\""+API_URL+"%20"+LOGIN+"%20"+PASSWORD+"\"");
		
		ProcessBuilder pb = new ProcessBuilder(commands);
		pb.redirectErrorStream(true);
		
		Process p;
		try {
			p = pb.start();
			InputStream processStdOutput = p.getInputStream();
			Reader r = new InputStreamReader(processStdOutput);
			BufferedReader br = new BufferedReader(r);
			String line;
			
			while ((line = br.readLine()) != null) {
				System.out.println(line);
				if (line.matches(".*accession=.*")) {
					Pattern aliasPat = Pattern.compile("alias=\"([^\"]+)\"");
					Matcher aliasMat = aliasPat.matcher(line);
					Pattern accPat = Pattern.compile("accession=\"([^\"]+)\"");
					Matcher accMat = accPat.matcher(line);
					Pattern typePat = Pattern.compile("<([A-Z]+)");
					Matcher typeMat = typePat.matcher(line);
					if (aliasMat.find()) {
						accMat.find();
						typeMat.find();
						String al = aliasMat.group(1);
						String acc = accMat.group(1);
						String ty = typeMat.group(1);
						if (ty.equals("SUBMISSION")) {
							setAccession(acc);
						} else {
							DatabaseEntity e = entries.get(al);
							e.setAccession(acc);
						}
					}
					
				}
				
			}
			p.waitFor();
		} catch (IOException e) {

			e.printStackTrace();
		} catch (InterruptedException e) {

			e.printStackTrace();
		}
		
	}

	@Override
	public String getSubmitRow() {
		return "";
	}

	

}
