package eu.europa.ecdc.enauploader;

import java.io.Serializable;


public class TessyCredentials implements Serializable {


	private static final long serialVersionUID = -5824802763806173512L;
	private String username="";
	private char[] password=new char[0];
	private String domain="";
	private String hostname="";
	private String target="";

	TessyCredentials () {
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public char[] getPassword() {
		return password;
	}

	public void setPassword(char[] cs) {
		this.password = cs;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}
}
