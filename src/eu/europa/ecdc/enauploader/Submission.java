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

import javax.swing.JTextArea;

import org.apache.commons.io.FileUtils;

import it.sauronsoftware.ftp4j.FTPClient;

public class Submission extends DatabaseEntity {

	//Default account settings
	public String LOGIN = "";
	public String PASSWORD = "";
	public String API_URL = "https://www-test.ebi.ac.uk/ena/submit/drop-box/submit/?auth=ENA";

	public String FTP_HOST;
	public String CURL_PATH;


	public void init() {
		File tmpDir = new File(TMP_PATH);
		tmpDir.mkdirs();
	}


	private JTextArea logArea;
	HashMap<String,DatabaseEntity> entries;
	private boolean uploaded;
	public boolean ftpExist;


	Submission(String c, String a) {
		super(c, a, null);
		entries = new HashMap<String,DatabaseEntity>();
		type = "SUBMISSION";
		uploaded = false;
	}

	public void setlogArea(JTextArea logger) {
		logArea = logger;
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

	public void setLogin(String l, String pwd) {
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
			client.connect(FTP_HOST);
			client.login(LOGIN, PASSWORD);
			for (File f : files) {
				boolean success = false;
				while (!success) {
					try {
						if (logArea== null) {
							System.out.println("Uploading " + f.toString());
						} else {
							logArea.append("Uploading " + f.toString()+"\n");
						}

						client.upload(f);
						success=true;
					} catch (Exception e) {
						success=false;
						Thread.sleep(5000);
						if (!client.isConnected()) {
							client.connect(FTP_HOST);
						}
						if (!client.isAuthenticated()) {
							client.login(LOGIN, PASSWORD);
						}
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

			String[] entryTypes = {"PROJECT","SAMPLE","EXPERIMENT","RUN"};
			for (String entryType : entryTypes) {
				for (String k : entries.keySet()) {

					DatabaseEntity e = entries.get(k);
					if (e.getType().equals(entryType)) {
						e.writeXml();
						
						bw2.write(e.getSubmitRow());
					}
				}

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
		init();
		writeXml();
		curlFiles();
		return true;

	}



	public void uploadFiles() {
		if (ftpExist) {
			if (logArea== null) {
				System.out.println("Files already exist on FTP, skipping upload step.");
			} else {
				logArea.append("Files already exist on FTP, skipping upload step."+"\n");
			}
			uploaded = true;	
			return;
		}
		
		boolean globalSuccess = true;
		for (String k: entries.keySet()) {
			DatabaseEntity e = entries.get(k);
			if(e.getType().equals("RUN")) {
				ArrayList<File> files = new ArrayList<File>();
				ArrayList<File> localFiles = ((Run)e).getFiles();
				files.addAll(localFiles);
				boolean success = upload(files);
				if (!success) {
					globalSuccess = false;
				}
			}
		}

		if (globalSuccess) {
			uploaded = true;	
		} else {
			uploaded = false;
		}

	}

	private void curlFiles() {

		if (logArea==null) {
			System.out.println("Submitting XML files...");
		} else {
			logArea.append("Submitting XML files..."+"\n");
		}

		ArrayList<String> commands = new ArrayList<String>();
		commands.add(CURL_PATH);
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
				if (logArea==null) {
					System.out.println(line);
				} else {
					logArea.append(line+"\n");
				}
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

			if (logArea==null) {
				System.out.println("CURL program could not be found, please check paths.txt and check that curl is in the specified location and runs under your operating system.");
			} else {
				logArea.append("CURL program could not be found, please check paths.txt and check that curl is in the specified location and runs under your operating system."+"\n");
			}
		} catch (InterruptedException e) {

			e.printStackTrace();
		}

	}

	@Override
	public String getSubmitRow() {
		return "";
	}



	public void setCurlPath(String path) {
		CURL_PATH = path;
	}

	public String getCurlPath() {
		return CURL_PATH;
	}

	public void setFtpHost(String path) {
		FTP_HOST = path;
	}

	public String getFtpHost() {
		return FTP_HOST;
	}

	public void setFtpExist(boolean ftpE) {
		ftpExist = ftpE;
		
	}

}
