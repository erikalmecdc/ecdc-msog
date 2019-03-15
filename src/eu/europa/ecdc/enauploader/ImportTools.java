package eu.europa.ecdc.enauploader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

// Class for importing data from various sources
public class ImportTools {

	private static final String SQL_DRIVER_PREFIX = "jdbc:";
	private static final String SQLITE_PREFIX = "sqlite://";




	// Map the values using the configured valueMap
	static ArrayList<String[]> mapValues(ArrayList<String[]> data,
			ArrayList<String> fields, ArrayList<Integer> indices, LinkedHashMap<String, LinkedHashMap<String, String>> valueMap) {

		// Iterate through the data rows
		for (int j = 0;j<data.size();j++) {
			String[] row = data.get(j);
			boolean updated = false;

			// Iterate through the fields
			for (int i = 0;i<fields.size();i++) {

				// Get the field
				int ind = indices.get(i);
				String field = fields.get(i);

				// Get the valueMap for this field
				LinkedHashMap<String, String> valueMapLocal = valueMap.get(field);

				// If there is no value map, do nothing
				if (valueMapLocal==null) {
					continue;
				}
				// if the old value is in the valueMap, map it and note the entry as updated
				if (valueMapLocal.containsKey(row[i])) {
					row[i] = valueMapLocal.get(row[i]);
					updated = true;
				}

			}

			// If this entry is updated, write into memory
			if (updated) {
				data.set(j, row);
			}
		}

		return data;
	}


	// Import from CSV
	static ArrayList<String[]> importCsv(ImportConfig cfg, ArrayList<String> oldEntries, ArrayList<String> fields, EcdcJob job, File importFile) {
		File file = importFile;
		ArrayList<String[]> data = new ArrayList<String[]>();

		try {
			BufferedReader br = new BufferedReader(new FileReader(file));

			String line;

			while ((line = br.readLine())!=null) {
				String[] rowData = parseLine(line);
				data.add(rowData);		
			}
			br.close();

		} catch (IOException e) {
			if (job!=null) {
				job.log("IOException, import failed");
				job.log(e.getMessage());
			}
			e.printStackTrace();
		}
		return data;
	}


	// Import from Excel
	static ArrayList<String[]> importExcel(ImportConfig cfg, ArrayList<String> oldEntries, ArrayList<String> fields, EcdcJob job, File importFile) {

		File file = importFile;
		String sheetName = cfg.getDatatable();
		ArrayList<String[]> data = new ArrayList<String[]>();
		System.out.println(fields.size());
		ArrayList<Integer> indices = new ArrayList<Integer>();
		for (int i = 0;i<fields.size();i++) {
			indices.add(0);
		}

		FileInputStream fs;
		try {
			fs = new FileInputStream(file);
			XSSFWorkbook wb = new XSSFWorkbook(fs);
			XSSFSheet sheet = wb.getSheet(sheetName);
			XSSFRow row;
			XSSFCell cell;
			DataFormatter objDefaultFormat = new DataFormatter();
			FormulaEvaluator objFormulaEvaluator = wb.getCreationHelper().createFormulaEvaluator();

			int rows; // No of rows
			rows = sheet.getPhysicalNumberOfRows();

			int cols = 0; // No of columns
			int tmp = 0;
			int startRow = 0;

			// This trick ensures that we get the data properly even if it doesn't start from first few rows
			for(int i = 0; i < 5 && i < rows; i++) {
				row = sheet.getRow(i);
				if(row != null) {
					tmp = sheet.getRow(i).getPhysicalNumberOfCells();
					if(tmp > cols){
						cols = tmp;
						startRow = i;
					}
				}
			}
			System.out.println(startRow);
			row = sheet.getRow(startRow);
			if(row != null) {
				for(int c = 0; c < cols; c++) {
					cell = row.getCell((short)c);
					if(cell != null) {
						String value = cell.getStringCellValue();
						int index = fields.indexOf(value);
						if (index!=-1) {
							indices.set(index, c);
						}
					}
				}
			}
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

			for(int i = startRow + 1; i < rows; i++) {
				row = sheet.getRow(i);
				if(row != null) {
					cols = sheet.getRow(i).getPhysicalNumberOfCells();
					String[] datarow = new String[indices.size()];
					int n = 0;
					for(int c : indices) {
						cell = row.getCell((short)c);
						if(cell != null) {
							String value;

							if (cell.getCellType() == CellType.NUMERIC && HSSFDateUtil.isCellDateFormatted(cell)) {

								value =  sdf.format(cell.getDateCellValue());
							} else {
								objFormulaEvaluator.evaluate(cell); // This will evaluate the cell, And any type of cell will return string value
								value = objDefaultFormat.formatCellValue(cell,objFormulaEvaluator);
							}

							datarow[n] = value;
							System.out.print(value+"\t");

						}
						n++;
						System.out.println(n);
					}
					data.add(datarow);
				}
			}

			wb.close();
			return data;
		} catch (IOException e) {
			if (job!=null) {
				job.log("IOException, import failed");
				job.log(e.getMessage());
			}
			e.printStackTrace();
		}

		return null;

	}



	static ArrayList<String[]> importSql(ImportConfig cfg, ArrayList<String> oldEntries, ArrayList<String> fields, EcdcJob job) {
		try {
			Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
		} catch (ClassNotFoundException e) {	
			if (job!=null) {
				job.log("ClassNotFoundException, import failed");
				job.log(e.getMessage());
			}
			e.printStackTrace();
		}

		ArrayList<String[]> data = new ArrayList<String[]>();

		try {
			String database = cfg.getDatabase();
			String databaseServer = cfg.getDatabaseServer().toLowerCase();
			if (databaseServer.startsWith("sqlserver")) {
				database = SQL_DRIVER_PREFIX+databaseServer+";DatabaseName="+database;
			} else if (databaseServer.startsWith("mysql")) {
				database = SQL_DRIVER_PREFIX+databaseServer+"?DatabaseName="+database;
			}


			System.out.println(database);
			Connection connection;

			if (cfg.getAuthType()==ImportConfig.AUTH_NONE) {
				connection = DriverManager.getConnection(database+";integratedSecurity=true");
			} else if (cfg.getAuthType()==ImportConfig.AUTH_PASSWORD) {
				connection = DriverManager.getConnection(database, cfg.getUsername(), new String(cfg.getPassword()));
			} else {
				return null;
			}

			String query = constructQuery(cfg,fields);

			Statement stmt  = connection.createStatement();
			ResultSet resultSet    = stmt.executeQuery(query);
			while (resultSet.next()) {

				String[] row = new String[fields.size()];
				for (int i=1;i<= fields.size();i++) {
					row[i-1] = resultSet.getString(i);
				}
				data.add(row);

			}

		} catch (SQLException e) {
			if (job!=null) {
				job.log("SQLException, import failed");
				job.log(e.getMessage());
			}
			e.printStackTrace();
			return null;
		}

		return data;
	}

	static ArrayList<String[]> importSqlite(ImportConfig cfg, ArrayList<String> oldEntries, ArrayList<String> fields, EcdcJob job) {
		ArrayList<String[]> data = new ArrayList<String[]>();
		Connection connection;
		try {

			connection = DriverManager.getConnection(SQL_DRIVER_PREFIX+SQLITE_PREFIX+cfg.getDatafile().replace("\\","/"));


			String query = constructQuery(cfg,fields);

			Statement stmt  = connection.createStatement();
			ResultSet resultSet    = stmt.executeQuery(query);
			while (resultSet.next()) {

				String[] row = new String[fields.size()];
				for (int i=1;i<= fields.size();i++) {
					row[i-1] = resultSet.getString(i);
				}
				data.add(row);

			}

		} catch (SQLException e) {
			if (job!=null) {
				job.log("SQLException, import failed");
				job.log(e.getMessage());
			}
			e.printStackTrace();
			return null;
		}

		return data;
	}


	private static String constructQuery(ImportConfig cfg, ArrayList<String> fields) {
		String query = "SELECT ";
		boolean first = true;
		for (String f : fields) {
			if (!first) {
				query = query + ", ";
			} else {
				first = false;
			}
			query = query +"["+ f+"]";
		}
		query = query + " FROM "+cfg.getDatatable();
		if (!cfg.getSqlQuery().equals("")) {
			String filter = cfg.getSqlQuery();
			if (filter.contains(";")) {
				filter = filter.substring(0, filter.indexOf(";"));
			}
			
			query = query + " WHERE "+filter;
		}
		query = query + ";";
		return query;
	}



	public static ArrayList<String> readCsvHeader(File file) {
		ArrayList<String> headers = new ArrayList<String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line = br.readLine();
			String[] fields = parseLine(line);
			for (String f : fields) {
				headers.add(f);
			}			
			br.close();
			return headers;
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	public static ArrayList<String> readExcelHeader(File file, String sheetName) {
		ArrayList<String> headers = new ArrayList<String>();
		FileInputStream fs;
		try {
			fs = new FileInputStream(file);
			XSSFWorkbook wb = new XSSFWorkbook(fs);
			XSSFSheet sheet = wb.getSheet(sheetName);
			XSSFRow row;
			XSSFCell cell;

			int rows; // No of rows
			rows = sheet.getPhysicalNumberOfRows();

			int cols = 0; // No of columns
			int tmp = 0;
			int startRow = 0;

			// This trick ensures that we get the data properly even if it doesn't start from first few rows
			for(int i = 0; i < 5 && i < rows; i++) {
				row = sheet.getRow(i);
				if(row != null) {
					tmp = sheet.getRow(i).getPhysicalNumberOfCells();
					if(tmp > cols){
						cols = tmp;
						startRow = i;
					}
				}
			}
			System.out.println(startRow);
			row = sheet.getRow(startRow);
			if(row != null) {
				for(int c = 0; c < cols; c++) {
					cell = row.getCell((short)c);
					if(cell != null) {
						String value = cell.getStringCellValue();
						headers.add(value);
					}
				}
			}
			wb.close();
			return headers;
		} catch (IOException e) {

			e.printStackTrace();
		}

		return null;
	}

	public static ArrayList<String> readSqliteHeader(String path, String table) {
		ArrayList<String> headers = new ArrayList<String>();

		Connection connection;
		try {
			System.out.println(SQL_DRIVER_PREFIX+SQLITE_PREFIX+path.replace("\\","/"));
			connection = DriverManager.getConnection(SQL_DRIVER_PREFIX+SQLITE_PREFIX+path.replace("\\","/"));
			DatabaseMetaData md = connection.getMetaData();
			ResultSet rset = md.getColumns(null, null, table, null);
			while (rset.next())
			{
				headers.add(rset.getString("COLUMN_NAME"));
			}
			return headers;
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static ArrayList<String> readSqlHeader(String databaseServer,String database, String table, ImportSqlAuth auth) {

		try {
			Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
		} catch (ClassNotFoundException e) {			
			e.printStackTrace();
		}

		String dbName = database;
		databaseServer = databaseServer.toLowerCase();
		if (databaseServer.startsWith("sqlserver")) {
			database = SQL_DRIVER_PREFIX+databaseServer+";DatabaseName="+database;
		} else if (databaseServer.startsWith("mysql")) {
			database = SQL_DRIVER_PREFIX+databaseServer+"?DatabaseName="+database;
		}


		System.out.println(database);
		Connection connection;
		try {
			if (auth.getAuthMethod()==ImportConfig.AUTH_NONE) {
				connection = DriverManager.getConnection(database+";integratedSecurity=true");
			} else if (auth.getAuthMethod()==ImportConfig.AUTH_PASSWORD) {
				connection = DriverManager.getConnection(database, auth.getUsername(), new String(auth.getPassword()));
			} else {
				return null;
			}

			return readGeneralSqlHeaders(connection,dbName,table);
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}


	private static ArrayList<String> readGeneralSqlHeaders(Connection connection, String db, String table) throws SQLException {
		ArrayList<String> headers = new ArrayList<String>();

		DatabaseMetaData md = connection.getMetaData();
		ResultSet rset = md.getColumns(null, null, table, null);
		while (rset.next())
		{
			headers.add(rset.getString("COLUMN_NAME"));
		}


		return headers;
	}

	private static String[] parseLine(String l2) {
		String otherThanQuote = " [^\"] ";
		String quotedString = String.format(" \" %s* \" ", otherThanQuote);
		String regex = String.format("(?x) "+ // enable comments, ignore white spaces
				",                         "+ // match a comma
				"(?=                       "+ // start positive look ahead
				"  (?:                     "+ //   start non-capturing group 1
				"    %s*                   "+ //     match 'otherThanQuote' zero or more times
				"    %s                    "+ //     match 'quotedString'
				"  )*                      "+ //   end group 1 and repeat it zero or more times
				"  %s*                     "+ //   match 'otherThanQuote'
				"  $                       "+ // match the end of the string
				")                         ", // stop positive look ahead
				otherThanQuote, quotedString, otherThanQuote);

		String[] tokens = l2.split(regex, -1);
		return tokens;
	}
} 