package eu.europa.ecdc.enauploader;

import java.io.Serializable;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;

import eu.europa.ecdc.enauploader.TessyIsolate.DefaultTrustManager;

public class TessyBatch implements Serializable {

	
	private static final long serialVersionUID = 1658151869052753489L;
	ArrayList<TessyIsolate> isolates;
	private static final int TEST = 1;
	private static final int UPLOAD = 2;
	private static final int CHECK_VALIDATION = 3;
	private static final int VALIDATION = 4;
	private static final int APPROVE = 5;
	private static final int REJECT = 6;

	private String reportingCountry;
	private String contact;
	private String version;
	private String subject;
	private String dataSource;
	private String batchId;
	private TessyCredentials tessyCredentials;
	private boolean haltWarn;
	private boolean haltRemark;
	private String state;
	private HashMap<String,TessyValidationResult> validation;
	private String lastResultString;
	private String lastMessageString;
	
	
	public void setValidationResults(HashMap<String,TessyValidationResult> validation) {
		this.validation = validation;
	}
	
	public HashMap<String,TessyValidationResult> getValidationResults() {
		return validation;
	}

	public static void main(String[] args) {

		TessyCredentials cred = new TessyCredentials();
		cred.setUsername("Provider_PT");
		char[] pwd = {'T','e','s','t','0','0','0','1'};
		cred.setPassword(pwd);
		cred.setDomain("idmdevdmz");
		cred.setHostname("tessy.idmdevdmz.local");
		cred.setTarget("/TessyTESTWebService/TessyUpload.asmx");




		TessyIsolate obc = new TessyIsolate("Test0028","2018-02-13","PT");
		obc.setField("Gender", "F");
		obc.setField("Age", "56");
		obc.setField("Imported", "N");
		obc.setField("DateOfSampling", "2018-02-13");
		obc.setField("DateOfReceiptReferenceLab", "2018-02-16");

		TessyIsolate obc2 = new TessyIsolate("Test0029","2018-02-03","PT");
		obc2.setField("Gender", "M");
		obc2.setField("Age", "71");
		obc2.setField("Imported", "Y");
		obc2.setField("DateOfSampling", "2018-02-03");
		obc2.setField("DateOfReceiptReferenceLab", "2018-02-07");

		TessyBatch bat = new TessyBatch("124","PT","Erik Alm","PT-MOLSURV","3","SALMISO");
		bat.setTessyCredentials(cred);
		bat.addIsolate(obc);
		bat.addIsolate(obc2);

		HashMap<String,TessyValidationResult> res = bat.test();

		for (String k : res.keySet()) {
			TessyValidationResult r = res.get(k);
			System.out.println(k);
			System.out.print(r.getErrorNum());
			System.out.print("\t");
			System.out.print(r.getWarningNum());
			System.out.print("\t");
			System.out.print(r.getRemarkNum());
			System.out.println("");
			for (String e : r.getErrors()) {
				System.out.println(e);
			}
			for (String e : r.getWarnings()) {
				System.out.println(e);
			}
			for (String e : r.getRemarks()) {
				System.out.println(e);
			}
		}
		for (String k : res.keySet()) {
			TessyValidationResult r = res.get(k);
			if (!r.pass()) {
				System.out.println("There are errors, quitting.");
				return;
			}
		}

		boolean uploaded = bat.upload();
		if (!uploaded) {
			System.out.println("Upload failed, aborting.");
			return;
		}

		boolean done = false;
		while (!done) {


			done = bat.checkValidation();

			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {

				e.printStackTrace();
			}
		}

		System.out.println("Validation results available.");

		res = bat.getValidation();
		for (String k : res.keySet()) {
			TessyValidationResult r = res.get(k);
			System.out.println(k);
			String guid = r.getGuid();
			System.out.println(guid);
			System.out.print(r.getErrorNum());
			System.out.print("\t");
			System.out.print(r.getWarningNum());
			System.out.print("\t");
			System.out.print(r.getRemarkNum());
			System.out.println("");
			if (!r.pass()) {
				System.out.println("There are errors, quitting.");
				return;
			}
		}
		
		System.out.println("Validation OK, ready for approval.");
		
		
		


		boolean approved = bat.approve();


		if (!approved) {
			System.out.println("Approval failed, rejecting batch...");
			bat.reject();
		}

		System.out.println("Approval ok. Finished.\n");
		 
	}







	TessyBatch(String bId,String country,String cont, String dataSou, String ver, String subj) {
		batchId = bId;
		contact = cont;
		reportingCountry = country;
		dataSource = dataSou;
		version = ver;
		subject = subj;
		isolates = new ArrayList<TessyIsolate>();
		haltRemark = false;
		haltWarn = false;
	}
	
	public String getState() {
		return state;
	}
	
	public void setId(String id) {
		batchId = id;
	}

	public void addIsolate(TessyIsolate iso) {
		isolates.add(iso);
	}

	public void setTessyCredentials(TessyCredentials c) {
		tessyCredentials = c;
	}

	public HashMap<String,TessyValidationResult> test() {
		
		ArrayList<String> xml = generateXml(TEST);
		
		String resultString = submitXml(xml);
		
		if (resultString==null) {
			return null;
		}
		
		HashMap<String,TessyValidationResult> results = parseValidationString(resultString);
		state = "TESTED";
		setValidationResults(results);
		return results;
	}
	
	public void reject() {
		ArrayList<String> xml = generateXml(REJECT);
		submitXml(xml);
		state = "REJECTED";
		
		
	}
	
	
	public boolean approve() {
		boolean approved = false;
		ArrayList<String> xml = generateXml(APPROVE);
		String resultString = submitXml(xml);
		//System.out.println(resultString);

		String[] rows = resultString.split("\n",-1);
		
	
		for (String l : rows) {
			
			if (l.matches(".*<operationSuccessful>.*")) {
				approved = true;
				state = "APPROVED";
			}
		}
		
		return approved;
	}
	
	public boolean upload() {

		boolean success = false;
		ArrayList<String> xml = generateXml(UPLOAD);
		String resultString = submitXml(xml);

		String[] rows = resultString.split("\n",-1);


		for (String l : rows) {
			if (l.matches(".*<operationSuccessful>.*")) {
				success = true;
			}
		}
		state = "UPLOADED";
		return success;
	}

	public HashMap<String,TessyValidationResult> getValidation() {

		ArrayList<String> xml = generateXml(VALIDATION);
		String resultString = submitXml(xml);
		System.out.println(resultString);
		if (resultString==null) {
			return null;
		}
		
		HashMap<String,TessyValidationResult> results = parseValidationString(resultString);
		state = "VALIDATED";
		setValidationResults(results);
		return results;
	}

	public boolean checkValidation() {
		ArrayList<String> xml = generateXml(CHECK_VALIDATION);
		String resultString = submitXml(xml);
		//System.out.println(resultString);
		return parseValidationAvailable(resultString);
	}


	private HashMap<String, TessyValidationResult> parseValidationString(String resultString) {
		HashMap<String,TessyValidationResult> results = new HashMap<String,TessyValidationResult>();

		for (TessyIsolate iso : isolates) {
			TessyValidationResult res = new TessyValidationResult("");
			results.put(iso.getRecordId(), res);
		}
		TessyValidationResult res2 = new TessyValidationResult("");
		results.put("GENERAL", res2);

		String[] rows = resultString.split("\n");
		boolean header = false;
		boolean validationResult = false;
		String type = "";
		String message = "";
		String isolate = "GENERAL";

		Pattern pat = Pattern.compile(".*<recordGuid>(.*)</recordGuid>.*");
		Pattern pat2 = Pattern.compile(".*<recordId>(.*)</recordId>.*");

		int errors = 0;
		int warnings = 0;
		int remarks = 0;

		for (String l : rows) {

			Matcher mat = pat.matcher(l);
			if (mat.find()) {
				String guid = mat.group(1);
				results.get(isolate).setGuid(guid);
			}

			if (l.matches(".*</header>.*")) {
				header = false;
			}
			Matcher mat2 = pat2.matcher(l);
			if (mat2.find()) {
				isolate = mat2.group(1);
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
					results.get(isolate).addRemark(message);
					break;
				case "warning":
					results.get(isolate).addWarning(message);
					break;
				case "error":
					results.get(isolate).addError(message);
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
				default:
				}
			}


			if (l.matches(".*<validationResult>.*")) {
				validationResult = true;
				type = "";
				message = "";
			}

			if (l.matches(".*<header>.*")) {
				header = true;
			}


		}
		if (errors==0 && (warnings==0 || !haltWarn) && (remarks==0 || !haltRemark)) {
			for (String k : results.keySet()) {
				results.get(k).setPass(true);
				System.out.println("Set pass.");
			}
		}


		return results;
	}
	
	public boolean passedValidation() {
		return passedValidation(null);
	}

	public boolean passedValidation(EcdcJob job) {
		for (String k : validation.keySet()) {
			TessyValidationResult val = validation.get(k);
			int errors = val.getErrorNum();
			int warnings = val.getWarningNum();
			int remarks = val.getRemarkNum();
			
			if ((errors==0) && (warnings==0 || !haltWarn) && (remarks==0 || !haltRemark)) {
				if (job!=null) {
					job.log("Batch validation OK");
				}
			} else {
				if (job!=null) {
					job.log("Batch validation not OK");
					job.log("Halt on warnings: "+Boolean.toString(haltWarn));
					job.log("Halt on remarks: "+Boolean.toString(haltRemark));
					job.log("Entry (GENERAL means overall issues with the batch): "+k);
					job.log("Errors: "+Integer.toString(errors));
					for (String e : val.getErrors()) {
						job.log(e);
					}
					job.log("Warnings: "+Integer.toString(warnings));
					for (String e : val.getWarnings()) {
						job.log(e);
					}
					job.log("Remarks: "+Integer.toString(remarks));
					for (String e : val.getRemarks()) {
						job.log(e);
					}
				}
			
				
				return false;
			}
		}
		
		
		return true;
	}
	
	private ArrayList<String> generateXml(int action) {



		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat sdf2 = new SimpleDateFormat("HH:mm:ss");
		String dateStr = sdf.format(new Date())+"T"+sdf2.format(new Date()); 

		ArrayList<String> xml1 = new ArrayList<String>();
		ArrayList<String> xml = new ArrayList<String>();

		switch (action) {
		case TEST:	
			xml1.add("<TestData xmlns=\"http://ecdc.europa.eu/tessy/v2\"><xml>");
			break;
		case UPLOAD:	
			xml1.add("<UploadData xmlns=\"http://ecdc.europa.eu/tessy/v2\"><batchId>"+batchId+"</batchId><xml>");
			break;
		case CHECK_VALIDATION:	
			xml1.add("<AreValidationResultsAvailable xmlns=\"http://ecdc.europa.eu/tessy/v2\"><batchId>"+batchId+"</batchId>");
			break;
		case VALIDATION:	
			xml1.add("<GetValidationResults xmlns=\"http://ecdc.europa.eu/tessy/v2\"><batchId>"+batchId+"</batchId>");
			break;
		case APPROVE:	
			xml1.add("<ApproveBatch xmlns=\"http://ecdc.europa.eu/tessy/v2\"><batchId>"+batchId+"</batchId>");
			break;
		case REJECT:	
			xml1.add("<RejectBatch xmlns=\"http://ecdc.europa.eu/tessy/v2\"><batchId>"+batchId+"</batchId>");
			break;
		default:

		}

		xml.add("<TESSyDataUpload xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns=\"http://tessy.ecdc.europa.eu/schemas/TESSyDataUploadV2\">\n<header>");
		xml.add("<dateSent>"+dateStr+"</dateSent>");
		xml.add("<contactPerson>"+contact+"</contactPerson>");
		xml.add("</header>\n<body>\n<recordGroup>");

		xml.add("<recordType>"+subject+"</recordType>");
		xml.add("<version>"+version+"</version>");
		xml.add("<subject>"+subject+"</subject>");
		xml.add("<action>update</action>");

		xml.add("<dataSource>"+dataSource+"</dataSource>");
		xml.add("<records>");



		for (TessyIsolate iso : isolates) {
			xml.addAll(iso.generateIsolateXml());
		}


		xml.add("</records>\n</recordGroup>\n</body>\n</TESSyDataUpload>");



		//COMPILE xml
		StringBuilder sb = new StringBuilder();
		
		for (String s : xml)
		{
			System.out.println(s);
			sb.append(s);
		}
		
		String testData = sb.toString();
		testData.replaceAll("\n","");
		byte[] encodedBytes = Base64.encodeBase64(testData.getBytes());
		testData = new String(encodedBytes);


		switch (action) {
		case TEST:	
			xml1.add(testData);
			xml1.add("</xml></TestData>");
			break;
		case UPLOAD:
			xml1.add(testData);
			xml1.add("</xml></UploadData>");
			break;
		case CHECK_VALIDATION:	
			xml1.add("</AreValidationResultsAvailable>");
			break;
		case VALIDATION:	
			xml1.add("</GetValidationResults>");
			break;
		case APPROVE:	
			xml1.add("</ApproveBatch>");
			break;
		case REJECT:	
			xml1.add("</RejectBatch>");
			break;
		default:

		}



		return xml1;
	}

	private String submitXml (ArrayList<String> contentArray) {

		StringBuilder sb = new StringBuilder();
		for (String s : contentArray)
		{
			sb.append(s);
			sb.append("\n");
		}
		String content = sb.toString();
		
		lastMessageString = content;

		try {

			SSLContextBuilder builder = new SSLContextBuilder();
			/*builder.loadTrustMaterial(null, new TrustStrategy(){
			    public boolean isTrusted(X509Certificate[] chain, String authType)
			            throws CertificateException {
			            return true;
			        }});
*/
				SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
						builder.build());

			SSLContext ctx = SSLContext.getInstance("TLS");

				
				ctx.init(new KeyManager[0], new TrustManager[] {new DefaultTrustManager()}, new SecureRandom());
				SSLContext.setDefault(ctx);

				
				
				RequestConfig requestConfig = RequestConfig.custom()
						.setTargetPreferredAuthSchemes(Arrays.asList(AuthSchemes.NTLM))
						.setProxyPreferredAuthSchemes(Arrays.asList(AuthSchemes.BASIC))
						.build();

				CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
				credentialsProvider.setCredentials(AuthScope.ANY,
						new NTCredentials(tessyCredentials.getUsername(), new String(tessyCredentials.getPassword()), "", tessyCredentials.getDomain()));
				
				HttpClient httpclient = HttpClients.custom()
						.setDefaultCredentialsProvider(credentialsProvider)
						.setDefaultRequestConfig(requestConfig)					
						.setSSLSocketFactory(sslsf).build();


			HttpHost target = new HttpHost(tessyCredentials.getHostname(), 443, "https");
			HttpPost httppost = new HttpPost(tessyCredentials.getTarget());


			httppost.setHeader("Content-type", "application/soap+xml");


			String soapBody = "<?xml version=\"1.0\" encoding=\"utf-8\"?><soap12:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap12=\"http://www.w3.org/2003/05/soap-envelope\"><soap12:Body>"+content+"</soap12:Body></soap12:Envelope>";			
			StringEntity strEntity = new StringEntity(soapBody, "UTF-8");
			httppost.setEntity(strEntity);


			HttpResponse r = httpclient.execute(target, httppost);
			HttpEntity e = r.getEntity();
			String responseString = EntityUtils.toString(e, "UTF-8").replace("&gt;",">").replace("&lt;","<");
			lastResultString = responseString;
			return responseString;


		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Returning null result");
		lastResultString = null;
		return null;
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

	private boolean parseValidationAvailable(String resultString) {

		String[] rows = resultString.split("\n",-1);
		for (String l : rows) {
			if (l.matches(".*<areValidationResultsAvailable>true</areValidationResultsAvailable>.*")) {
				return true;
			}
		}


		return false;
	}







	public String getBatchId() {
		return batchId;
	}







	public TessyCredentials getCredentials() {
		return tessyCredentials;
	}







	public void setHaltWarn(boolean haltWarn) {
		this.haltWarn = haltWarn;
		
	}







	public void setHaltRemark(boolean haltRemark) {
		this.haltRemark = haltRemark;
		
	}

	public String getLastResponse() {
		return lastResultString;
	}
	
	public String getLastMessage() {
		return lastMessageString;
	}
}
