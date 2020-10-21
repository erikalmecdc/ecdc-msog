package eu.europa.ecdc.enauploader;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.InetAddress;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.security.cert.X509Certificate;
import javax.swing.JTextArea;

import org.apache.commons.io.FileUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.util.TrustManagerUtils;

import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPConnector;
import it.sauronsoftware.ftp4j.connectors.DirectConnector;

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
		if (p) {
			// production API
			API_URL = "https://www.ebi.ac.uk/ena/submit/drop-box/submit/?auth=ENA";
			System.out.println(API_URL);
		} else {
			// test API
			API_URL = "https://www-test.ebi.ac.uk/ena/submit/drop-box/submit/?auth=ENA";
		}
		System.out.println(API_URL);
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


	public boolean upload(ArrayList<File> files, EcdcJob job) {
		FTPSClient ftpClient = new FTPSClient("TLS", false);


		try {
			TrustManager trustManager = TrustManagerUtils.getAcceptAllTrustManager();
			ftpClient.setTrustManager(trustManager);
			
			ftpClient.setBufferSize(1024 * 1024);
			ftpClient.setConnectTimeout(100000);
			if (job== null) {
				System.out.println("Connecting to ENA using FTPS");
			} else {
				job.log("Connecting to ENA using FTPS");
			}
			ftpClient.connect(InetAddress.getByName(FTP_HOST), 21);
			ftpClient.setSoTimeout(100000);


			if (ftpClient.login(LOGIN, PASSWORD)) {
				if (job== null) {
					System.out.println("Login successful");
				} else {
					job.log("Login successful");
				}
				ftpClient.changeWorkingDirectory("/");

				ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
				ftpClient.enterLocalPassiveMode();
				
				for (File f : files) {
					
					boolean success = false;
					while (!success) {
						if (job== null) {
							System.out.println("Uploading " + f.toString());
						} else {
							job.log("Uploading " + f.toString());
						}
						InputStream input = new FileInputStream(f);
						success = ftpClient.storeFile(f.getName(), input);
						if (!success) {
							if (job== null) {
								System.out.println("Upload of " + f.toString() + " failed!");
							} else {
								job.log("Upload of " + f.toString() + " failed!");
							}
						}
					}
					
				}
			} else {
				if (job== null) {
					System.out.println("Login failed, check your credentials for ENA");
				} else {
					job.log("Login failed, check your credentials for ENA");
				}
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			if (job== null) {
				System.out.println("Upload failed: " + e.toString());
			} else {
				job.log("Upload failed: " + e.toString());
			}
			return false;
		} finally {
			try {
				if (job== null) {
					System.out.println("Disconnecting from ENA");
				} else {
					job.log("Disconnecting from ENA");
				}
				ftpClient.disconnect();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return true;
	}


	public boolean uploadOld(ArrayList<File> files) {


		System.out.println("Connecting to ENA");
		try {
			TrustManager[] trustManager = new TrustManager[] { new X509TrustManager() {
				@Override
				public void checkClientTrusted(java.security.cert.X509Certificate[] arg0, String arg1)
						throws CertificateException {
					// TODO Auto-generated method stub

				}
				@Override
				public void checkServerTrusted(java.security.cert.X509Certificate[] arg0, String arg1)
						throws CertificateException {
					// TODO Auto-generated method stub

				}
				@Override
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					// TODO Auto-generated method stub
					return null;
				}
			} };
			SSLContext sslContext = null;
			try {
				sslContext = SSLContext.getInstance("SSL");
				sslContext.init(null, trustManager, new SecureRandom());
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			} catch (KeyManagementException e) {
				e.printStackTrace();
			}
			SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
			FTPClient client = new FTPClient();
			client.setSSLSocketFactory(sslSocketFactory);
			client.setSecurity(FTPClient.SECURITY_FTPES); // or client.setSecurity(FTPClient.SECURITY_FTPES);
			FTPConnector connector = new DirectConnector();
			connector.setConnectionTimeout(300);
			connector.setReadTimeout(300);
			connector.setCloseTimeout(300);
			client.setConnector(connector);

			client.setPassive(true);
			System.setProperty("ftp4j.activeDataTransfer.acceptTimeout", "0");
			client.connect(FTP_HOST, 21);
			System.out.println("Connected to ENA");
			client.login(LOGIN, PASSWORD);
			System.out.println("Logged in");
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
		return submit(null);
	}

	public boolean submit(EcdcJob sourceJob) {



		if (!uploaded) {
			System.out.println("DATA FILES HAVE NOT SUCCESSFULLY BEEN UPLOADED!");
			if (sourceJob!=null) {
				sourceJob.log("Data files have not been successfully uploaded to ENA, aborting ENA submission");
			}
			return false;
		}
		if (sourceJob!=null) {
			sourceJob.log("Initializing ENA submission");
		}
		init();
		if (sourceJob!=null) {
			sourceJob.log("Writing ENA xml files");
		}
		writeXml();
		if (sourceJob!=null) {
			sourceJob.log("Uploading ENA xml files");
		}
		curlFiles(sourceJob);
		return true;

	}

	public void uploadFiles() {
		uploadFiles(null);
	}

	public void uploadFiles(EcdcJob job) {
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
				boolean success = upload(files, job);
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


	private void curlFiles(EcdcJob sourceJob) {

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

		for (String c : commands) {
			if (logArea==null) {
				System.out.print(c+" ");
			} else {
				logArea.append(c+" ");
			}
			if (sourceJob!=null) {
				sourceJob.log(c);
			}
		}
		if (logArea==null) {
			System.out.println("");
		} else {
			logArea.append("\n");
		}


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
				if (sourceJob!=null) {
					sourceJob.log(line);
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
			if (sourceJob!=null) {
				sourceJob.log("CURL program could not be found, please check CURL path in upload config and check that curl is in the specified location and runs under your operating system.");
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
