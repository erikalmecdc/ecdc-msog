package eu.europa.ecdc.enauploader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;


// This is a utility class for creating Ghost entries in a Bionumerics database
public class GhostEntryCreator extends JFrame {


	
	
	private static HashMap<String, Integer> usedIds;

	public static void main(String[] args) {
		try {
			Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
		} catch (ClassNotFoundException e) {			
			e.printStackTrace();
		}


		/*	if (args.length<5) {
			System.out.println("Usage: createghostentry <subject> <TEST/PROD> <username> <password> <csv file> ");
			System.exit(0);
		}

		String insubj = args[0].toUpperCase();
		String environ = args[1].toUpperCase();
		String username = args[2];
		String password = args[3];
		String infile = args[4];
		 */

		String insubj = "LISTISO";
		String environ = "PROD";
		String username = "ealm";
		
		
		ArrayList<String> BNfields = new ArrayList<String>();
		BNfields.add("BN_ANN1");
		BNfields.add("BN_ANN2");
		BNfields.add("BN_ANN3");
		BNfields.add("BN_WGS_ECDCID");
		String sourceDb = "jdbc:sqlite:Z:/BioNumerics/Data/ELITE_WGS/ELITE_WGS.sqlite";
		String transferlist = "C:/Users/ealm/Desktop/transfer3.csv";
		String targetDb;
		String targetTable;
		String password;
		
		if (environ.equals("PROD")) {
			targetDb = "jdbc:sqlserver://nsql3:1433;DatabaseName=Molsurv;integratedSecurity=true";
			targetTable = "vSRC_"+insubj+"_ENTRYTABLE";
			password = "hbj024";
		} else {
			targetDb = "jdbc:sqlserver://zdevsql16.idmdevdmz.local:1433;DatabaseName=BNAS_TEST_"+insubj;
			targetTable = "ENTRYTABLE";
			password = "Solna123";
		}
		
		
		
		


		String infile = "C:/Users/ealm/Desktop/testghost.txt";

		try {
			dumpDbToFile(sourceDb, infile);
		} catch (SQLException | IOException e1) {

			e1.printStackTrace();
			System.exit(0);
		}


		ArrayList<String> isolates = new ArrayList<String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(transferlist));
			String line;
			while ((line=br.readLine())!=null) {
				if (line.split(",")[1].toUpperCase().equals("Y")) {
					isolates.add(line.split(",")[0]);
				}

			}
			br.close();

		} catch (IOException e) {
			e.printStackTrace();
		}




		try {
			boolean success = upload(insubj, environ, username, password, infile, BNfields, isolates, targetDb, targetTable);
			if (success) {
				System.out.println("Submission successful.");
			} else {
				System.out.println("Submission failed.");
			}
		} catch (Exception e) {

			e.printStackTrace();
		}

	}

	private static void dumpDbToFile(String sourceDb, String infile) throws SQLException, IOException {

		BufferedWriter bw = new BufferedWriter(new FileWriter(infile));

		Connection conn2 = DriverManager.getConnection(sourceDb);
		Statement m_Statement2 = conn2.createStatement();
		System.out.println("Connection to SQLite has been established.");
		String query2 = "SELECT * FROM entrytable";


		ResultSet rs2 = m_Statement2.executeQuery(query2);
		for (int i = 1; i < rs2.getMetaData().getColumnCount() + 1; i++) {
			String val = rs2.getMetaData().getColumnName(i);
			val = val.replaceAll("\t","");
			if (i>1) {
				bw.write("\t");
			}
			bw.write(val);
		}

		while (rs2.next()) {
			bw.write("\n");
			for (int i = 1; i < rs2.getMetaData().getColumnCount() + 1; i++) {
				String val = rs2.getString(i);
				if (val==null) {
					val = "";
				}
				val = val.replaceAll("\t","");
				if (i>1) {
					bw.write("\t");
				}
				bw.write(val);
			}

		}
		bw.close();
		conn2.close();
	}

	private static boolean upload(String subj, String environ, String login, String password, String infile, ArrayList<String> BNfields, ArrayList<String> isolates, String targetDb, String targetTable) throws Exception {


		String baseUrl;

		if (environ.equals("TEST")) {
			baseUrl = "http://zdevbion16.idmdevdmz.local/bnwebservice/";
		} else if (environ.equals("PROD"))  {
			baseUrl = "http://zbionsts.ecdcdmz.europa.eu/bnwebservice/";
		} else {
			System.out.println("Only TEST or PROD are allowed for the second argument");
			return false;
		}

		File xmlFile = new File("C:/Users/ealm/test3.txt");


		int numrecords = writeXml(xmlFile,new File(infile),subj, BNfields, isolates, targetDb, targetTable);
		if (numrecords == 0) {
			return false;
		}
		//System.exit(0);
		
		
		System.out.print("Number of records read from list: ");
		System.out.println(isolates.size());
		System.out.print("Number of records loaded into xml: ");
		System.out.println(numrecords);
		//System.exit(0);
		//TODO: 

		URL url = new URL(baseUrl+"logon.aspx?userid="+login+"&pwd="+password+"&db=BNAS_"+environ+"_"+subj);
		final long startTime = System.nanoTime();

		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("GET");


		int status = con.getResponseCode();
		BufferedReader in = new BufferedReader(
				new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer content = new StringBuffer();
		while ((inputLine = in.readLine()) != null) {
			content.append(inputLine);
		}
		in.close();
		con.disconnect();
		System.out.println(status);
		System.out.println(content);

		String sessionId = "";
		if (content.toString().startsWith("OK")) {
			sessionId = content.toString().substring(3);
		} else {
			return false;
		}
		System.out.println(sessionId);
		final long interTime = System.nanoTime();

		//Code here
		//runscript.aspx?session=" + SessionId + "&name=" + scriptName + "&arg=" + UrlCodecHelper.Encode(arg);

		String scriptName = "UploadToBnServer.py";
		String arg = "";
		String base64Xml = "";
		String xml = "";


		BufferedReader br = new BufferedReader(new FileReader(xmlFile));
		String l;
		while ((l=br.readLine())!=null) {
			xml = xml+ l;
		}
		br.close();
		base64Xml = Base64.getEncoder().encodeToString(xml.getBytes());


		url = new URL(baseUrl+"runscript.aspx?session="+sessionId+"&name="+scriptName+"&arg="+arg);


		con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("POST");

		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(base64Xml);
		wr.flush();
		wr.close();

		status = con.getResponseCode();
		in = new BufferedReader(
				new InputStreamReader(con.getInputStream()));

		content = new StringBuffer();
		while ((inputLine = in.readLine()) != null) {
			content.append(inputLine);
		}
		in.close();
		con.disconnect();
		System.out.println(status);
		System.out.println(content);
		boolean success = true;
		if (!content.toString().startsWith("OK")) {
			success = false;
		}

		final long interTime2 = System.nanoTime();


		url = new URL(baseUrl+"logoff.aspx?session="+sessionId);
		con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("GET");

		status = con.getResponseCode();
		in = new BufferedReader(
				new InputStreamReader(con.getInputStream()));

		content = new StringBuffer();
		while ((inputLine = in.readLine()) != null) {
			content.append(inputLine);
		}
		in.close();
		con.disconnect();
		System.out.println(status);
		System.out.println(content);
		if (!success) {
			return false;
		}
		final long duration1 = interTime - startTime;
		final long duration2 = interTime2 - interTime;
		final long duration3 = System.nanoTime() - interTime2;
		final long duration = System.nanoTime() - startTime;

		System.out.print("N=");
		System.out.println(numrecords);
		System.out.print("CONNECT: ");
		System.out.println(duration1/1000000);
		System.out.print("SUBMIT: ");
		System.out.println(duration2/1000000);
		System.out.print("DELETE: ");
		System.out.println(duration3/1000000);
		System.out.println("");
		System.out.print("TOTAL: ");
		System.out.println(duration/1000000);

		for (String k : usedIds.keySet()) {
			Connection conn;
			if (targetDb.matches(".*nsql3.*")) {
				conn = DriverManager.getConnection(targetDb);
			} else {
				conn = DriverManager.getConnection(targetDb,"sa","Solna123");
			}
			
			Statement m_Statement = conn.createStatement();
			

			String query = "SELECT [key] FROM "+targetTable+ " WHERE[key]='"+k+"'";

			boolean inTessy = false;
			ResultSet rs = m_Statement.executeQuery(query);
			while (rs.next()) {
				inTessy=true;
			}
			if (!inTessy) {
				System.out.println("Submission of "+k+" failed.");
			}
			conn.close();
		}

		return true;
	}

	private static int writeXml(File xmlFile, File infile, String subj, ArrayList<String> BNfields, ArrayList<String> isolates, String targetDb, String targetTable) {
		int numrecords = 0;
		try {
			Connection conn;
			if (targetDb.matches(".*nsql3.*")) {
				conn = DriverManager.getConnection(targetDb);
			} else {
				conn = DriverManager.getConnection(targetDb,"sa","Solna123");
			}
			
			
			
			BufferedReader br = new BufferedReader(new FileReader(infile));
			ArrayList<String> lines = new ArrayList<String>();
			HashMap<String,Integer> indices = new HashMap<String,Integer>();
			String line = br.readLine();
			String[] headFields = line.split("\t",-1);

			if (headFields.length<2) {
				System.out.println("Infile must be tab delimited");
				br.close();
				return 0;
			}
			for (int i = 0; i< headFields.length;i++) {
				indices.put(headFields[i],i);
			}




			while ((line = br.readLine())!=null) {
				lines.add(line);
			}

			br.close();



			BufferedWriter bw = new BufferedWriter(new FileWriter(xmlFile));
			PrintWriter pw = new PrintWriter(bw);

			pw.println("<TESSyToBNServerDataUpload>");
			pw.println("<header>");
			pw.println("<approvalDate></approvalDate>");
			pw.println("<uploadingUser>ECDC</uploadingUser>");
			pw.println("<uploadingUserADGUID></uploadingUserADGUID>");
			pw.println("<approvingUser></approvingUser>");
			pw.println("<approvingUserADGUID></approvingUserADGUID>");
			pw.println("<batchGuid></batchGuid>");
			pw.println("<batchId></batchId>");
			pw.println("</header>");
			pw.println("<body>");
			pw.println("<recordGroup>");
			pw.println("<recordType>"+subj+"</recordType>");
			pw.println("<version></version>");
			pw.println("<subject>"+subj+"</subject>");
			pw.println("<action>UPDATE</action>");
			pw.println("<dataSource></dataSource>");
			pw.println("<dataSourceFullName></dataSourceFullName>");
			pw.println("<records>");

			System.out.print("Lines read: ");
			System.out.println(lines.size());
			
			usedIds = new HashMap<String,Integer>();
			
			for (String l : lines) {
				
				String[] fields = l.split("\t",-1);
				
				if (isolates!=null && isolates.indexOf(fields[0])==-1) {
					//System.out.println("Skipping "+guid);
					continue;
				}
				String guid = fields[0].toUpperCase();
				String oldGuid = guid;
				if (!guid.startsWith("SEQ-")) {
					guid = "SEQ-"+UUID.randomUUID().toString().toUpperCase();
				}
				
				//System.out.println(oldGuid);
				
				String id = fields[indices.get("PB_RECORDID")];
				if (id.equals("")) {
				
					System.out.println("Entry "+oldGuid+ " is missing a RecordId");
					continue;
				}
				String country = fields[indices.get("PB_REPORTINGCOUNTRY")];
				if (country.equals("")) {
					
					System.out.println("Entry "+oldGuid+ "("+id+") is missing a ReportingCountry");
					country = "UNK";
				}
				
				Statement m_Statement = conn.createStatement();
				

				String query = "SELECT [key] FROM "+targetTable+ " WHERE (PB_RECORDID='"+id+"' AND PB_REPORTINGCOUNTRY='"+country+"') OR [key]='"+oldGuid+"'";

				boolean inTessy = false;
				ResultSet rs = m_Statement.executeQuery(query);
				while (rs.next()) {
					String key = rs.getString(1);
					guid = key;
					//System.out.println(oldGuid+ " already exists in target database, using key: "+key);
					inTessy = true;
				}
				
				String dateSampling = fields[indices.get("PB_DATEOFSAMPLING")];								
				String dateReflab = fields[indices.get("PB_DATEOFRECEIPTREFERENCELAB")];
				String date = fields[indices.get("PB_DATEUSEDFORSTATISTICS")];

				if (usedIds.containsKey(guid)) {
					System.out.println(guid + " is duplicate! "+country+", "+id+", "+oldGuid);
					
					continue;
				} else {
					usedIds.put(guid,1);
					
				}
				
				if (date.equals("")) {
					if (!dateSampling.equals("")) {
						date = dateSampling;
					} else if (!dateReflab.equals("")) {
						date = dateReflab;
					} else {
						String dateOrig = fields[indices.get("BN_ANN3")];
						if (dateOrig==null) {
							dateOrig = "";
						}
						if (dateOrig.matches("[0-9][0-9][0-9][0-9][-][0-9][0-9]-[0-9][0-9]")) {
							date = dateOrig;
						} else if (dateOrig.matches("[0-9][0-9][0-9][0-9][-][0-9][0-9]")) {
							date = dateOrig+"-15";
						} else if (dateOrig.matches("[0-9][0-9][0-9][0-9]")) {
							date = dateOrig+"-07-01";
						} else {
							System.out.println("Entry "+oldGuid+"("+id+", "+country+") is missing Date and Ann3");	
							//continue;
							date = "UNK";
						}
						
					}
					fields[indices.get("PB_DATEUSEDFORSTATISTICS")] = date;
				}
				
				
				String enaId = "";
				if (enaId.equals("") && !fields[indices.get("BN_WGS_ENAID")].equals("")) {
					enaId = fields[indices.get("BN_WGS_ENAID")];
				}
				if (enaId.equals("") && !fields[indices.get("BN_WGS_SRAID")].equals("")) {
					enaId = fields[indices.get("BN_WGS_SRAID")];
				}
				
				
				
				
				pw.println("<record>");
				pw.println("<recordId>"+id+"</recordId>");
				pw.println("<reportingCountry>"+country+"</reportingCountry>");
				pw.println("<status>NEW/UPDATE</status>");
				pw.println("<dateUsedForStatistics>"+date+"</dateUsedForStatistics>");
				pw.println("<guid>"+guid+"</guid>");

				for (int i = 1;i<headFields.length;i++) {
					if (i>=fields.length) {
						pw.close();
						bw.close();
						System.out.println("Entry "+guid+ " has too few columns");
						return 0;
					}
					if (!headFields[i].startsWith("PB_") && !headFields[i].startsWith("BN_")) {
						continue;
					}
					if (fields[i].equals("")) {
						continue;
					}
					if (!enaId.equals("")) {
						pw.println("<field>");
						pw.println("<fieldName>WGSENAID</fieldName>");
						pw.println("<fieldValue>"+enaId+"</fieldValue>");
						pw.println("</field>");
					}
					if (headFields[i].toUpperCase().equals("BN_ANN3")) {
						pw.println("<field>");
						pw.println("<fieldName>DATEOFRECEIPTREFERENCELABORIG</fieldName>");
						pw.println("<fieldValue>"+fields[i]+"</fieldValue>");
						pw.println("</field>");
					} else if (headFields[i].startsWith("PB_") && !inTessy) {
						pw.println("<field>");
						pw.println("<fieldName>"+headFields[i].substring(3)+"</fieldName>");
						pw.println("<fieldValue>"+fields[i]+"</fieldValue>");
						pw.println("</field>");
					} else if (BNfields.indexOf(headFields[i].toUpperCase())!=-1) {
						pw.println("<field>");
						pw.println("<fieldName>"+headFields[i]+"</fieldName>");
						pw.println("<fieldValue>"+fields[i]+"</fieldValue>");
						pw.println("</field>");
					}

				}

				pw.println("</record>");
				numrecords++;
				if (numrecords>90) {
					//break;
				}
			}



			/*
			for (int i = 0; i<n;i++) {
				String id = "test-"+baseid+"-"+Integer.toString(i);
				String guid = "TEST-SEQ-"+UUID.randomUUID().toString();
				pw.println("<record>");
				pw.println("<recordId>"+id+"</recordId>");
				pw.println("<reportingCountry>SE</reportingCountry>");
				pw.println("<status>NEW/UPDATE</status>");
				pw.println("<dateUsedForStatistics>2013-07-01</dateUsedForStatistics>");
				pw.println("<guid>"+guid+"</guid>");
				pw.println("<field>");
				pw.println("<fieldName>Age</fieldName>");
				pw.println("<fieldValue>53</fieldValue>");
				pw.println("</field>");
				pw.println("<field>");
				pw.println("<fieldName>Gender</fieldName>");
				pw.println("<fieldValue>M</fieldValue>");
				pw.println("</field>");
				pw.println("<field>");
				pw.println("<fieldName>DateOfSampling</fieldName>");
				pw.println("<fieldValue>2013-07-01</fieldValue>");
				pw.println("</field>");
				pw.println("<field>");
				pw.println("<fieldName>DateOfReceiptReferenceLab</fieldName>");
				pw.println("<fieldValue>2013-07-15</fieldValue>");
				pw.println("</field>");
				pw.println("<field>");
				pw.println("<fieldName>WgsProtocol</fieldName>");
				pw.println("<fieldValue>MISEQ_2X150</fieldValue>");
				pw.println("</field>");
				pw.println("<field>");
				pw.println("<fieldName>WgsEnaId</fieldName>");
				pw.println("<fieldValue>ERR1276254</fieldValue>");
				pw.println("</field>");



				pw.println("</record>");


			}
			 */

			pw.println("</records>");
			pw.println("</recordGroup>");
			pw.println("</body>");
			pw.println("</TESSyToBNServerDataUpload>");



			pw.close();
			bw.close();
			conn.close();
		} catch (IOException | SQLException e) {

			e.printStackTrace();
		}
		return numrecords;
	}




}
