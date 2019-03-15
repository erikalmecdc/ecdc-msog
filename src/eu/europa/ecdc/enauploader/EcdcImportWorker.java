package eu.europa.ecdc.enauploader;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

public class EcdcImportWorker extends EcdcJob {

	private JTable table;
	private ImportConfig importConfig;
	private File importFile;
	
	EcdcImportWorker(EcdcUploaderGUI gui, TessyBatch batch, String name, UploadConfig cfg, String[] headers,
			String[][] data) {
		super(gui, batch, name, cfg, headers, data);
		
	}

	
	public void setImportConfig (ImportConfig cfg) {
		this.importConfig = cfg;
	}
	
	public void setTable (JTable table) {
		this.table = table;
	}
	
	@Override
	protected Object doInBackground() throws Exception {
		
		if (importFile==null) {
			importFile = new File(importConfig.getDatafile());
		}
		
		setTitle("Importing data for: "+name);
		setStatus("Performing import");
		setProgress(10);
		
		doImport(table, importConfig, this);
		
		setStatus("Import finished.");
		setProgress(100);
		
		return null;
	}

	
	// Method for import using a ImportConfig, will select the source from the config parameters
		private ArrayList<Integer> doImport(JTable table, ImportConfig cfg, EcdcJob job) {

			if (job!=null) {
				job.log("Starting import...");
			}
			
			ArrayList<Integer> importedRows = new ArrayList<Integer>();
			ArrayList<String> oldEntries = new ArrayList<String>();
			
			// Read the existing entries
			for (int i = 0;i<table.getRowCount();i++) {
				String recordId = (String)table.getValueAt(i, 0);
				if (recordId==null || recordId.equals("")) {
					continue;
				}			
				oldEntries.add(recordId);
			}

			if (job!=null) {
				job.log(Integer.toString(oldEntries.size())+" entries in data table before import.");
			}
			
			
			ArrayList<String> importFields = new ArrayList<String>();
			ArrayList<Integer> tableIndices = new ArrayList<Integer>();
			HashMap<Integer,String> fixed = new HashMap<Integer,String>();

			// Get any constant field values from the config, these will not be imported but rather set to the fixed value
			for (String k: cfg.getConstants().keySet()) {
				String value = cfg.getConstants().get(k);
				for (int i = 0;i<table.getColumnCount();i++) {
					if (k.equals(table.getColumnName(i))) {
						fixed.put(i, value);
						if (job!=null) {
							job.log("For field "+table.getColumnName(i)+", setting fixed value: "+value);
						}
					}
				}
			}

			
			
			// Get field name mappings
			// Fields are the columns to import and indices are the corresponding table column numbers to put the values in
			// Iterate over metadata field names
			for (String k : cfg.getMap().keySet()) {
				// Get the corresponding import field name
				String value = cfg.getMap().get(k);
				if (!value.equals("")) {
					importFields.add(value);
					// FInd the correct table column to put this imported column in
					for (int i = 0;i<table.getColumnCount();i++) {
						if (k.equals(table.getColumnName(i))) {
							tableIndices.add(i);
							if (job!=null) {
								job.log("For field "+table.getColumnName(i)+", source field "+value+" will be imported to table column "+Integer.toString(i));
							}
						}
					}
				}
			}

			// Init import data
			ArrayList<String[]> data=null;
			
			if (job!=null) {
				job.log("Checking import source type");
			}

			// Choose import source type and call the corresponding import method 
			switch (cfg.getImportType()) {
			case ImportConfig.IMPORT_SQL:
				if (job!=null) {
					job.log("SQL");
				}
				data = ImportTools.importSql(cfg,oldEntries, importFields, job);
				break;

			case ImportConfig.IMPORT_SQLITE:
				if (job!=null) {
					job.log("SQLite");
				}
				data = ImportTools.importSqlite(cfg,oldEntries, importFields, job);
				break;

			case ImportConfig.IMPORT_CSV:
				if (job!=null) {
					job.log("CSV");
				}
				
				data = ImportTools.importCsv(cfg, oldEntries, importFields, job, importFile);
				break;

			case ImportConfig.IMPORT_EXCEL:
				if (job!=null) {
					job.log("Excel");
				}
				data = ImportTools.importExcel(cfg, oldEntries, importFields, job, importFile);
				break;

			}

			
			if (data==null) {
				if (job!=null) {
					job.log("Null data returned by import routine");
				}
				return importedRows;
			}

			// Map the values
			if (job!=null) {
				job.log("Mapping values");
			}
		
			data = ImportTools.mapValues(data,importFields,tableIndices,cfg.getValueMap());

			
			DefaultTableModel model =(DefaultTableModel)table.getModel();
			
			// Make a HashMap from RecordId to row number for the existing entries
			HashMap<String,Integer> oldIds = new HashMap<String,Integer>();
			for (int i = 0;i<model.getRowCount();i++) {
				oldIds.put((String)model.getValueAt(i, 0), i);
			}


			if (job!=null) {
				job.setStatus("Mapping data");
				setProgress(19);
			}

			int cols = model.getColumnCount();

			int newRows = 0;
			int updatedRows = 0;
			// Go through the data to import
			
			for (int i = 0;i<data.size();i++) {
				
				int progress = 19+(80*i)/data.size();
				setProgress(progress);
				
				String[] dataVector = data.get(i);
				if (dataVector[0]==null || dataVector[0].equals("")) {
					continue;
				}
				
				// If there is a match to an old entry, update rather than creating a new row
				if (oldIds.containsKey(dataVector[0])) {
					boolean updated = false;
					int oldRowNumber = oldIds.get(dataVector[0]);
					for (int j = 0;j<dataVector.length;j++) {
						if (dataVector[j]!=null) {
							if (model.getValueAt(oldRowNumber, tableIndices.get(j))==null || !dataVector[j].equals(model.getValueAt(oldRowNumber, tableIndices.get(j)))) {							
								model.setValueAt(dataVector[j], oldRowNumber, tableIndices.get(j));
								updated = true;
							}
						}
					}
					
					if (updated) {
						//if (job!=null) {
						//	job.log("Entry "+dataVector[0]+" already exists and will be updated with new data.");
						//}
						updatedRows++;
						importedRows.add(oldRowNumber);
					}
					// Do not add a new row if an existing entry was added
					continue;
				}

				// Create a String array of the data to add to the table
				String[] rowdata = new String[cols];
				
				// Set fixed values
				for (int k : fixed.keySet()) {
					rowdata[k] = fixed.get(k);
				}
				
				// Put the imported data into the corresponding table columns
				for (int j = 0; j< tableIndices.size();j++) {
					rowdata[tableIndices.get(j)] = dataVector[j];
				}

				// Add row to table data model
				model.addRow(rowdata);
				newRows++;
				//if (job!=null) {
				//	job.log("Entry "+dataVector[0]+" created.");
				//}
				
				// Add this row to the importedRows which is later returned
				importedRows.add(model.getRowCount()-1);
			}
			if (job!=null) {
				job.log(Integer.toString(newRows)+" entries created.");
				job.log(Integer.toString(updatedRows)+" entries updated.");
				job.log(Integer.toString(model.getRowCount())+" entries in data table after import.");
			}
			table.clearSelection();
			for (int i : importedRows) {
				int tableIndex = table.getRowSorter().convertRowIndexToView(i);
				table.addRowSelectionInterval(tableIndex, tableIndex);
			}
			
			return importedRows;
		}


		public void setFile(File f) {
			this.importFile = f;
			
		}

	
}
