package eu.europa.ecdc.enauploader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JOptionPane;
import javax.swing.JTable;

public class EcdcLinkWorker extends EcdcJob {

	private JTable table;
	private int[] selected;
	private String READ_FILES_COLUMN;
	private String ASSEMBLY_FILE_COLUMN;
	private ArrayList<Integer> linkedRows;
	private String BASE_FILES_COLUMN;


	EcdcLinkWorker(EcdcUploaderGUI gui, TessyBatch batch, String name, UploadConfig cfg, String[] headers,
			String[][] data) {
		super(gui, batch, name, cfg, headers, data);
		selected = null;
	}

	public void setTable (JTable table) {
		this.table = table;
	}

	public void setSelected (int[] selected) {
		this.selected = selected;
	}

	public void setColumnNames(String baseName, String rawName, String assemblyName) {
		BASE_FILES_COLUMN = baseName;
		READ_FILES_COLUMN = rawName;
		ASSEMBLY_FILE_COLUMN = assemblyName;
	}


	@Override
	protected Object doInBackground() throws Exception {

		setTitle("Linking sequence files");
		setStatus("Initializing");
		setProgress(5);

		// Get the selected table if not supplied

		// use selected rows if not supplied
		if (selected==null) {
			selected = table.getSelectedRows();
		}

		if (cfg == null) {
			log("Error, no config found for "+name);
			setStatus("Initialization error");
			setProgress(10);
			return null;
		}



		String rawDir = cfg.getRawdataDir();
		String assemblyDir = cfg.getAssemblyDir();
		String rawDelim = cfg.getRawdataDelimiter();
		String assemblyDelim = cfg.getAssemblyDelimiter();

		// Check if data directories are supplied in the config
		if (rawDir.equals("") && assemblyDir.equals("")) {
			log("At least one of 'Assembly directory' or 'Raw reads directory' must be defined in the configuration.");
			setStatus("Initialization error");
			setProgress(10);
			return null;	
		}



		// Init variables
		linkedRows = new ArrayList<Integer>();

		int numberLinkedRaw = 0;
		int numberLinkedAssembly = 0;

		setStatus("Reading raw data directory");
		log("Reading raw data directory: "+rawDir);
		setProgress(10);
		// List the raw data directory recursively
		ArrayList<File> foundFiles = new ArrayList<File>();
		File rawDirFile = new File(rawDir);
		if (!rawDir.equals("") && rawDirFile.exists() && rawDirFile.isDirectory()) {
			try {
				foundFiles = EcdcUtils.search(new File(rawDir), foundFiles);
			} catch (IOException e) {
				log(e.getMessage());
				setStatus("IO Error");
				setProgress(12);
			}
		} else {
			log("No raw reads directory found. Skipping import of raw reads.");
		}

		setStatus("Reading assembly data directory");
		log("Reading assembly data directory: "+assemblyDir);
		setProgress(35);
		ArrayList<File> foundFilesAssembly;
		if (!assemblyDir.equals(rawDir)) {
			// List the assembly data directory recursively
			foundFilesAssembly = new ArrayList<File>();
			File assemblyDirFile = new File(assemblyDir);
			if (!assemblyDir.equals("") && assemblyDirFile.exists() && assemblyDirFile.isDirectory()) {
				try {
					foundFilesAssembly = EcdcUtils.search(new File(assemblyDir), foundFilesAssembly);
				} catch (IOException e) {
					log(e.getMessage());
					setStatus("IO Error");
					setProgress(37);
				}
			} else {
				log("No assembly directory found. Skipping import of assemblies.");
			}
		} else {
			log("Assembly directory same as raw read directory, reusing index.");
			foundFilesAssembly = (ArrayList<File>)foundFiles.clone();
		}

		// Init column indices
		int baseCol = gui.getColumn(table,BASE_FILES_COLUMN);
		int fileCol = gui.getColumn(table,READ_FILES_COLUMN);
		int assemblyCol = gui.getColumn(table,ASSEMBLY_FILE_COLUMN);
		log("Raw reads table column: "+Integer.toString(fileCol));
		log("Assembly table column: "+Integer.toString(assemblyCol));
		
		setStatus("Matching entries with files");
		log("Matching entries with files");
		setProgress(60);
		// Iterate through the table rows
		log("Iterating over "+Integer.toString(foundFiles.size())+" files in reads direcotyr.");
		log("Iterating over "+Integer.toString(foundFilesAssembly.size())+" files in assembly directory.");
		log("Iterating over "+Integer.toString(selected.length)+" entries.");
		for (int i : selected) {
			
			String id;
			String baseVal = (String)table.getValueAt(i, baseCol);
			if (baseVal==null || baseVal.equals("")) {
				String recordId = (String)table.getValueAt(i, 0);
				id = recordId;
			} else {
				id = baseVal;
			}
			
			
			
			
			String files = (String)table.getValueAt(i, fileCol);
			String filesAssembly = (String)table.getValueAt(i, assemblyCol);

			// Skip rows missing RecordId
			if (id == null || id.equals("")) {
				continue;
			}

			// If the raw read links are empty, try to find links
			
				String pattern = id+rawDelim+".*[.]?fastq[.]?g?z?";
				String fileStr = "";
				
				for (File f : foundFiles) {
					if (!f.getName().matches(pattern)) {
						continue;
					}
					if (!fileStr.equals("")) {
						fileStr = fileStr + ";";
					}
					fileStr = fileStr + f.toString();
				}
				
				if (!fileStr.equals("") && (files==null || files.equals("") || !files.equals(fileStr))) {
					linkedRows.add(i);
					table.setValueAt(fileStr,i, fileCol);
					numberLinkedRaw++;
				}
			

			setProgress(80);
			// If the assembly links are empty, try to find links
			
				pattern = id+assemblyDelim+".*[.]?fasta[.]?g?z?";

				fileStr = "";
				
				for (File f : foundFilesAssembly) {

					if (!f.getName().matches(pattern)) {
						continue;
					}
					if (!fileStr.equals("")) {
						fileStr = fileStr + ";";
					}
					fileStr = fileStr + f.toString();
				}
				if (!fileStr.equals("") && (filesAssembly==null || filesAssembly.equals("") || !filesAssembly.equals(fileStr))) {
					table.setValueAt(fileStr,i, assemblyCol);
					if (linkedRows.indexOf(i)==-1) {
						linkedRows.add(i);
					}
					numberLinkedAssembly++;
				}
			}
		
		
		
		
		log("Finished, "+Integer.toString(numberLinkedRaw)+"(reads) "+Integer.toString(numberLinkedAssembly)+"(assembly) "+" linked files");
		setStatus("Finished, "+Integer.toString(numberLinkedRaw)+"(reads) "+Integer.toString(numberLinkedAssembly)+"(assembly) "+" linked files");
		setProgress(100);
		return null;
	}

	public ArrayList<Integer> getLinkedEntries() {
		return linkedRows;
	}

}
