package eu.europa.ecdc.enauploader;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;

// Class for holding the configuration for Data Import and scheduling
// It contains fields and getters+setters for them
public class ImportConfig implements Serializable {

	private static final long serialVersionUID = -7632508112626524840L;
	public static final int IMPORT_SQL = 0;
	public static final int IMPORT_SQLITE = 1;
	public static final int IMPORT_EXCEL = 2;
	public static final int IMPORT_CSV = 3;
	
	public static final int AUTH_NONE = 0;
	public static final int AUTH_PASSWORD = 1;
	
	public static final int SOURCE_STATIC = 0;
	public static final int SOURCE_FLEXIBLE = 1;
	
	public static final int SCHEDULE_NONE = 0;
	public static final int SCHEDULE_MINUTE = 1;
	public static final int SCHEDULE_HOUR = 2;
	public static final int SCHEDULE_DAY = 3;
	
	private int importType=0;
	private int authType=0;
	private int dataSourceFlexible=0;
	private String databaseServer="";
	private String database="";
	private String datafile="";
	private String datatable="";
	private String sqlQuery="";
	private LinkedHashMap<String,String> map;
	private LinkedHashMap<String,String> constants;
	private LinkedHashMap<String,LinkedHashMap<String,String>> valueMap;
	private String username="";
	private char[] password=new char[0];
	private String subject="";
	private ArrayList<String> datasourceHeaders;
	
	private int scheduleUnit=0;
	private int scheduleAmount=0;
	
	ImportConfig() {
		setMap(new LinkedHashMap<String,String>());
		setConstants(new LinkedHashMap<String,String>());
		setValueMap(new LinkedHashMap<String,LinkedHashMap<String,String>>());
	}

	public int getImportType() {
		return importType;
	}

	public void setImportType(int importType) {
		this.importType = importType;
	}

	public int getAuthType() {
		return authType;
	}

	public void setAuthType(int authType) {
		this.authType = authType;
	}

	public int getDataSourceFlexible() {
		return dataSourceFlexible;
	}

	public void setDataSourceFlexible(int dataSourceFlexible) {
		this.dataSourceFlexible = dataSourceFlexible;
	}

	public String getSqlQuery() {
		return sqlQuery;
	}

	public void setSqlQuery(String sqlQuery) {
		this.sqlQuery = sqlQuery;
	}
	
	public LinkedHashMap<String,String> getConstants() {
		return constants;
	}

	public void setConstants(LinkedHashMap<String,String> map) {
		this.constants = map;
	}

	public LinkedHashMap<String,String> getMap() {
		return map;
	}

	public void setMap(LinkedHashMap<String,String> map) {
		this.map = map;
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

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getDatabase() {
		return database;
	}

	public void setDatabase(String database) {
		this.database = database;
	}

	public String getDatafile() {
		return datafile;
	}

	public void setDatafile(String datafile) {
		this.datafile = datafile;
	}

	public String getDatatable() {
		return datatable;
	}

	public void setDatatable(String datatable) {
		this.datatable = datatable;
	}

	public String getDatabaseServer() {
		return databaseServer;
	}

	public void setDatabaseServer(String databaseServer) {
		this.databaseServer = databaseServer;
	}

	public LinkedHashMap<String,LinkedHashMap<String,String>> getValueMap() {
		return valueMap;
	}

	public void setValueMap(LinkedHashMap<String,LinkedHashMap<String,String>> valueMap) {
		this.valueMap = valueMap;
	}

	public int getScheduleUnit() {
		return scheduleUnit;
	}

	public void setScheduleUnit(int scheduleUnit) {
		this.scheduleUnit = scheduleUnit;
	}

	public int getScheduleAmount() {
		return scheduleAmount;
	}

	public void setScheduleAmount(int scheduleAmount) {
		this.scheduleAmount = scheduleAmount;
	}

	public ArrayList<String> getDatasourceHeaders() {
		return datasourceHeaders;
	}
	
	public void setDataSourceHeaders(ArrayList<String> headers) {
		datasourceHeaders = headers;
	}
	
	
}
