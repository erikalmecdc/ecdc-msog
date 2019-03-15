package eu.europa.ecdc.enauploader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.auth.params.AuthPNames;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.AuthPolicy;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicSchemeFactory;
import org.apache.http.impl.auth.DigestSchemeFactory;
import org.apache.http.impl.auth.KerberosSchemeFactory;
import org.apache.http.impl.auth.SPNegoSchemeFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;

import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

public class TessyUploader {

	
	
	
	
	
	
	
	
	
	
	
	
	

	public static void main(String[] args) {

		
		try {

			SSLContextBuilder builder = new SSLContextBuilder();
			builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
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
	                new NTCredentials("Provider_PT", "Test0001", "", "idmdevdmz"));
	         
	            HttpClient httpclient = HttpClients.custom()
	                .setDefaultCredentialsProvider(credentialsProvider)
	                .setDefaultRequestConfig(requestConfig)
	                .setSSLSocketFactory(sslsf).build();

	            String batchId = "10";
	            String testData = "";
	            File testFile = new File("C:/users/ealm/desktop/LISTISOtest.xml");
	            BufferedReader br = new BufferedReader(new FileReader(testFile));
	            String line;
	            while ((line = br.readLine())!=null) {
	            	testData = testData + line;
	            	System.out.println(line);
	            }
	            byte[] encodedBytes = Base64.encodeBase64(testData.getBytes());
	            testData = new String(encodedBytes);
	            
	            br.close();
	            
	            HttpHost target = new HttpHost("tessy.idmdevdmz.local", 443, "https");
	            HttpPost httppost = new HttpPost("/TessyTESTWebService/TessyUpload.asmx");
	            
	           
	            httppost.setHeader("Content-type", "application/soap+xml");
	          
	            String soapBody = "<?xml version=\"1.0\" encoding=\"utf-8\"?><soap12:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap12=\"http://www.w3.org/2003/05/soap-envelope\"><soap12:Body><TestData xmlns=\"http://ecdc.europa.eu/tessy/v2\"><xml>"+testData+"</xml></TestData></soap12:Body></soap12:Envelope>";
	            StringEntity strEntity = new StringEntity(soapBody, "UTF-8");
	            httppost.setEntity(strEntity);
	            
	            
	            HttpResponse r = httpclient.execute(target, httppost);
	            HttpEntity e = r.getEntity();
	            String responseString = EntityUtils.toString(e, "UTF-8").replace("&gt;",">").replace("&lt;","<");
	            System.out.println(responseString);

	           

		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	
	
	public static class DefaultTrustManager implements X509TrustManager {

		@Override
		public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}

		@Override
		public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}



	}

	





}
