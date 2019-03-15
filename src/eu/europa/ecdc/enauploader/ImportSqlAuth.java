package eu.europa.ecdc.enauploader;

// Class for holding authentication information for importing from SQL
public class ImportSqlAuth {

	private int authMethod;
	private String username;
	private char[] password;

	public ImportSqlAuth(int authMethod, String username, char[] password) {
		this.setAuthMethod(authMethod);
		this.setUsername(username);
		this.setPassword(password);
	}

	public int getAuthMethod() {
		return authMethod;
	}

	public void setAuthMethod(int authMethod) {
		this.authMethod = authMethod;
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

	public void setPassword(char[] password) {
		this.password = password;
	}

}
