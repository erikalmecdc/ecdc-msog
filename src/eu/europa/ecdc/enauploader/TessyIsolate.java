package eu.europa.ecdc.enauploader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
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
import javax.net.ssl.X509TrustManager;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;

import eu.europa.ecdc.enauploader.TessyUploader.DefaultTrustManager;

public class TessyIsolate implements Serializable {


	private static final int TEST = 1;
	private static final int UPLOAD = 2;
	private static final int CHECK_VALIDATION = 3;
	private static final int VALIDATION = 4;
	private static final int APPROVE = 5;
	private static final int REJECT = 6;
	private String recordId;
	private String reportingCountry;
	private String dateUsedForStatistics;

	private String contact;
	private String version;
	private String subject;
	private String dataSource;
	private HashMap<String,String> fields;
	private String batchId;
	
	private TessyCredentials tessyCredentials;

	

	TessyIsolate(String id, String dateUsed, String country) {
		recordId = id;
		dateUsedForStatistics = dateUsed;
		reportingCountry = country;
		fields = new HashMap<String,String>();
	}
	
	

	

	public void setBatchId(String id) {
		batchId = id;
	}

	public void setField(String key, String value) {
		fields.put(key, value);
	}

	public void removeField(String key) {
		fields.remove(key);
	}

	public String getRecordId() {
		return recordId;
	}


	public TessyValidationResult getValidation() {
		ArrayList<String> xml = generateXml(VALIDATION);
		String resultString = submitXml(xml);
		//System.out.println(resultString);
		TessyValidationResult result = new TessyValidationResult(resultString);
		return result;
	}

	public TessyValidationResult test() {

		ArrayList<String> xml = generateXml(TEST);
		String resultString = submitXml(xml);
		//System.out.println(resultString);
		TessyValidationResult result = new TessyValidationResult(resultString);
		return result;
	}
	
	public void reject() {
		ArrayList<String> xml = generateXml(REJECT);
		submitXml(xml);
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
			}
		}
		
		return approved;
	}
	
	public boolean upload() {

		boolean success = false;
		ArrayList<String> xml = generateXml(UPLOAD);
		String resultString = submitXml(xml);
		//System.out.println(resultString);
		
		String[] rows = resultString.split("\n",-1);
		
		
		for (String l : rows) {
			if (l.matches(".*<operationSuccessful>.*")) {
				success = true;
			}
		}
		
		
		return success;
	}
	
	public boolean checkValidation() {
		ArrayList<String> xml = generateXml(CHECK_VALIDATION);
		String resultString = submitXml(xml);
		//System.out.println(resultString);
		return parseValidationAvailable(resultString);
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


	public ArrayList<String> generateIsolateXml() {
		
		ArrayList<String> xml = new ArrayList<String>();
		
		xml.add("<record>");

		xml.add("<recordId>"+recordId+"</recordId>");
		xml.add("<reportingCountry>"+reportingCountry+"</reportingCountry>");
		xml.add("<status>new/update</status>");
		xml.add("<dateUsedForStatistics>"+dateUsedForStatistics+"</dateUsedForStatistics>");
		for (String k : fields.keySet()) {
			String val = fields.get(k);
			xml.add("<field>");
			xml.add("<fieldName>"+k+"</fieldName>");	
			xml.add("<fieldValue>"+val+"</fieldValue>");	
			xml.add("</field>");	
		}
		xml.add("</record>");
		
		return xml;
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
		
		
		
		xml.add("<record>");

		xml.add("<recordId>"+recordId+"</recordId>");
		xml.add("<reportingCountry>"+reportingCountry+"</reportingCountry>");
		xml.add("<status>new/update</status>");
		xml.add("<dateUsedForStatistics>"+dateUsedForStatistics+"</dateUsedForStatistics>");
		for (String k : fields.keySet()) {
			String val = fields.get(k);
			xml.add("<field>");
			xml.add("<fieldName>"+k+"</fieldName>");	
			xml.add("<fieldValue>"+val+"</fieldValue>");	
			xml.add("</field>");	
		}
		xml.add("</record>");
		
		
		xml.add("</records>\n</recordGroup>\n</body>\n</TESSyDataUpload>");



		//COMPILE xml
		StringBuilder sb = new StringBuilder();
		for (String s : xml)
		{
			sb.append(s);
			//sb.append("\n");
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

	public String submitXml (ArrayList<String> contentArray) {

		StringBuilder sb = new StringBuilder();
		for (String s : contentArray)
		{
			sb.append(s);
			sb.append("\n");
		}
		String content = sb.toString();
		System.out.println(content);


		try {

			
			
		SSLContextBuilder builder = new SSLContextBuilder();
		builder.loadTrustMaterial(null, new TrustStrategy(){
		    public boolean isTrusted(X509Certificate[] chain, String authType)
		            throws CertificateException {
		            return true;
		        }});
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
			//String soapBody = "<?xml version=\"1.0\" encoding=\"utf-8\"?><soap12:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap12=\"http://www.w3.org/2003/05/soap-envelope\"><soap12:Body><GetBatchState xmlns=\"http://ecdc.europa.eu/tessy/v2\"><batchId>"+batchId+"</batchId></GetBatchState></soap12:Body></soap12:Envelope>";
			//String soapBody = "<?xml version=\"1.0\" encoding=\"utf-8\"?><soap12:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap12=\"http://www.w3.org/2003/05/soap-envelope\"><soap12:Body><GetAllBatches xmlns=\"http://ecdc.europa.eu/tessy/v2\"></GetAllBatches></soap12:Body></soap12:Envelope>";
			StringEntity strEntity = new StringEntity(soapBody, "UTF-8");
			httppost.setEntity(strEntity);

			System.out.println("TARGET: "+target);
			System.out.println("POSTDATA: "+httppost);
			HttpResponse r = httpclient.execute(target, httppost);
			HttpEntity e = r.getEntity();
			String responseString = EntityUtils.toString(e, "UTF-8").replace("&gt;",">").replace("&lt;","<");
			return responseString;


		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Returning null result");
		return null;
	}


	public static class DefaultTrustManager implements X509TrustManager {

		@Override
		public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}

		@Override
		public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return new X509Certificate[0];
		}



	}


	public void setCredentials(TessyCredentials cred) {
		tessyCredentials = cred;
		
	}





}
