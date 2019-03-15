package eu.europa.ecdc.enauploader;


import java.io.Serializable;

public class UploadConfig implements Serializable {

	
	private static final long serialVersionUID = -6824355776374586820L;
	
	private String name="";
	private String description="";
	
	private boolean submitTessy=true;
	private boolean submitEna=false;
	private boolean submitFtp=false;
	private boolean shareFtp=true;
	private boolean submitTessyAssembly=false;
	
	
	private String rawdataDir="";
	private String rawdataDelimiter="";
	private String assemblyDir="";
	private String assemblyDelimiter="";
	
	private String sftpHost="";
	private String sftpLogin="";
	private char[] sftpPass=new char[0];
	private String sftpPath="";
	
	private TessyCredentials tessyCredentials=new TessyCredentials();
	/*private String tessyLogin;
	private String tessyPass;
	private String tessyUrl;
	private String tessyDomain;
	private String tessyTarget;*/
	
	
	private String tessyContact="";
	private String tessyProvider="";
	private String tessySubject="";
	private String tessyMeta="";
	private String tessyCountry="";
	private boolean tessyHaltRemark=false;
	private boolean tessyHaltWarn=false;
	
	private String enaProjectAcc="";
	private String enaCenter="";
	private boolean enaAnonymize=true;
	private boolean enaProd=true;
	private boolean enaFtpExist=false;
	private String enaLogin="";
	private char[] enaPassword=new char[0];
	private String enaChecklist="";
	
	
	private String bigsdbAccessToken="";
	private String bigsdbApiUrl="";

	private String enaFtpHost="";

	private String curlPath="";

	private String tmpPath="";
	private String organism = "";

	private boolean anonymizeFtp; 
	private boolean showYearFtp;
	
	
	
	public String getRawdataDir() {
		return rawdataDir;
	}
	public void setRawdataDir(String rawdataDir) {
		this.rawdataDir = rawdataDir;
	}
	public String getRawdataDelimiter() {
		return rawdataDelimiter;
	}
	public void setRawdataDelimiter(String rawdataDelimiter) {
		this.rawdataDelimiter = rawdataDelimiter;
	}
	public String getSftpHost() {
		return sftpHost;
	}
	public void setSftpHost(String sftpHost) {
		this.sftpHost = sftpHost;
	}
	public String getSftpLogin() {
		return sftpLogin;
	}
	public void setSftpLogin(String sftpLogin) {
		this.sftpLogin = sftpLogin;
	}
	public char[] getSftpPass() {
		return sftpPass;
	}
	public void setSftpPass(char[] cs) {
		this.sftpPass = cs;
	}
	
	public String getSftpPath() {
		return sftpPath;
	}
	public void setSftpPath(String sftpPath) {
		this.sftpPath = sftpPath;
	}
	
	public String getTessyContact() {
		return tessyContact;
	}
	public void setTessyContact(String tessyContact) {
		this.tessyContact = tessyContact;
	}
	public String getTessyProvider() {
		return tessyProvider;
	}
	public void setTessyProvider(String tessyProvider) {
		this.tessyProvider = tessyProvider;
	}
	public String getTessySubject() {
		return tessySubject;
	}
	public void setTessySubject(String tessySubject) {
		this.tessySubject = tessySubject;
	}
	public String getTessyMeta() {
		return tessyMeta;
	}
	public void setTessyMeta(String tessyMeta) {
		this.tessyMeta = tessyMeta;
	}
	public String getTessyCountry() {
		return tessyCountry;
	}
	public void setTessyCountry(String tessyCountry) {
		this.tessyCountry = tessyCountry;
	}
	public boolean getTessyHaltRemark() {
		return tessyHaltRemark;
	}
	public void setTessyHaltRemark(boolean tessyHaltRemark) {
		this.tessyHaltRemark = tessyHaltRemark;
	}
	public boolean getTessyHaltWarn() {
		return tessyHaltWarn;
	}
	public void setTessyHaltWarn(boolean tessyHaltWarn) {
		this.tessyHaltWarn = tessyHaltWarn;
	}
	public String getEnaProjectAcc() {
		return enaProjectAcc;
	}
	public void setEnaProjectAcc(String enaProjectAcc) {
		this.enaProjectAcc = enaProjectAcc;
	}
	public boolean getEnaAnonymize() {
		return enaAnonymize;
	}
	public void setEnaAnonymize(boolean enaAnonymize) {
		this.enaAnonymize = enaAnonymize;
	}
	public String getEnaCenter() {
		return enaCenter;
	}
	public void setEnaCenter(String enaCenter) {
		this.enaCenter = enaCenter;
	}
	public boolean getEnaProd() {
		return enaProd;
	}
	public void setEnaProd(boolean enaProd) {
		this.enaProd = enaProd;
	}
	public boolean getEnaFtpExist() {
		return enaFtpExist;
	}
	public void setEnaFtpExist(boolean enaFtpExist) {
		this.enaFtpExist = enaFtpExist;
	}
	public String getEnaLogin() {
		return enaLogin;
	}
	public void setEnaLogin(String enaLogin) {
		this.enaLogin = enaLogin;
	}
	public char[] getEnaPassword() {
		return enaPassword;
	}
	public void setEnaPassword(char[] cs) {
		this.enaPassword = cs;
	}
	public String getEnaChecklist() {
		return enaChecklist;
	}
	public void setEnaChecklist(String enaChecklist) {
		this.enaChecklist = enaChecklist;
	}
	


	
	public String getBigsdbAccessToken() {
		return bigsdbAccessToken;
	}
	public void setBigsdbAccessToken(String bigsdbAccessToken) {
		this.bigsdbAccessToken = bigsdbAccessToken;
	}
	public String getBigsdbApiUrl() {
		return bigsdbApiUrl;
	}
	public void setBigsdbApiUrl(String bigsdbApiUrl) {
		this.bigsdbApiUrl = bigsdbApiUrl;
	}
	public String getAssemblyDir() {
		return assemblyDir;
	}
	public void setAssemblyDir(String assemblyDir) {
		this.assemblyDir = assemblyDir;
	}
	public String getAssemblyDelimiter() {
		return assemblyDelimiter;
	}
	public void setAssemblyDelimiter(String assemblyDelimiter) {
		this.assemblyDelimiter = assemblyDelimiter;
	}
	public TessyCredentials getTessyCredentials() {
		return tessyCredentials;
	}
	public void setTessyCredentials(TessyCredentials tessyCredentials) {
		this.tessyCredentials = tessyCredentials;
	}
	public boolean isSubmitEna() {
		return submitEna;
	}
	public void setSubmitEna(boolean submitEna) {
		this.submitEna = submitEna;
	}
	public boolean isSubmitFtp() {
		return submitFtp;
	}
	public void setSubmitFtp(boolean submitFtp) {
		this.submitFtp = submitFtp;
	}
	public boolean isShareFtp() {
		return shareFtp;
	}
	public void setShareFtp(boolean shareFtp) {
		this.shareFtp = shareFtp;
	}
	public boolean isSubmitTessyAssembly() {
		return submitTessyAssembly;
	}
	public void setSubmitTessyAssembly(boolean submitTessyAssembly) {
		this.submitTessyAssembly = submitTessyAssembly;
	}
	public boolean isSubmitTessy() {
		return submitTessy;
	}
	public void setSubmitTessy(boolean submitTessy) {
		this.submitTessy = submitTessy;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	
	public String getEnaFtpHost() {
		return enaFtpHost;
	}
	
	public String getCurlPath() {
		return curlPath;
	}
	
	public String getTmpPath() {
		return tmpPath;
	}
	
	public void setEnaFtpHost(String enaFtpHost) {
		this.enaFtpHost = enaFtpHost;
	}
	
	public void setCurlPath(String curlPath) {
		this.curlPath = curlPath;
	}
	public void setTmpPath(String tmpPath) {
		this.tmpPath = tmpPath;
	}
	public String getOrganism() {
		return organism;
	}
	public void setOrganism(String organism) {
		this.organism = organism;
	}
	public void setAnonymizeFtp(boolean anonymizeFtp ) {
		this.anonymizeFtp = anonymizeFtp;
	}
	public boolean isAnonymizeFtp() {
		return anonymizeFtp;
	}
	public boolean isShowYearFtp() {
		return showYearFtp;
	}
	public void setShowYearFtp(boolean showYearFtp) {
		this.showYearFtp = showYearFtp;
	}
	
}
