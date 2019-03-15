package eu.europa.ecdc.enauploader;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

// This class creates a GUI for configuring Data import and scheduling 
public class ImportConfigGUI extends JFrame implements ActionListener {

	private static final long serialVersionUID = -7815244026662021007L;
	private HashMap<String, ImportConfig> configs;
	private JButton newButton;
	private JButton renameButton;
	private JButton saveButton;
	private JButton revertButton;
	private JButton deleteButton;
	private JTextArea descriptionArea;
	private JComboBox<String> confignameField;
	private String oldSelection;
	private boolean isMain;
	private JComboBox<String> configTypeBox;
	private JComboBox<String> authTypeBox;
	private JComboBox<String> flexibleSourceBox;
	private JTextField infileField;
	private JTextField indatabaseField;
	private JTextField intableField;
	private JTextField queryField;
	private JTextField usernameField;
	private JPasswordField passwordField;
	private JComboBox<String> subjectBox;

	LinkedHashMap<String,ImportMapPanel> mapPanelHashMap;
	private JPanel mapPanel;
	private JTabbedPane tabs;
	private JTextField databaseServerField;
	private JComboBox<String> scheduleBox;
	private JTextField scheduleField;

	private static final String UPLOAD_COLUMN = "Ready for upload";
	private static final String BASE_FILE_COLUMN = "File base name";
	private static final String ASSEMBLY_FILE_COLUMN = "Assembly file";
	private static final String READ_FILES_COLUMN = "Read files";
	
	// This can be run as main class, in that case the program will exit on GUI close
	ImportConfigGUI(boolean isMain) {
		this.isMain = isMain;
	}

	public void init() {

		// Find all configs and read them
		readConfigs();


		// Init UI elements
		try {
			String imagePath = "media/ECDC2.png";
			InputStream imgStream = ImportConfigGUI.class.getResourceAsStream(imagePath );
			BufferedImage myImg;
			myImg = ImageIO.read(imgStream);
			setIconImage(myImg);
		} catch (Exception e) {
			e.printStackTrace();
		}


		setTitle("ECDC WGS Uploader - Data import configuration");
		this.setResizable(false);
		JPanel mainPanel = new JPanel(new BorderLayout());
		JPanel buttonPanel = new JPanel();
		JPanel topPanel = new JPanel(new BorderLayout());

		newButton = new JButton("New");
		newButton.addActionListener(this);
		newButton.setActionCommand("new");
		renameButton = new JButton("Rename");
		renameButton.addActionListener(this);
		renameButton.setActionCommand("rename");
		saveButton = new JButton("Save");
		saveButton.addActionListener(this);
		saveButton.setActionCommand("save");

		revertButton = new JButton("Revert changes");
		revertButton.addActionListener(this);
		revertButton.setActionCommand("revert");
		deleteButton = new JButton("Delete");
		deleteButton.addActionListener(this);
		deleteButton.setActionCommand("delete");

		buttonPanel.add(newButton);
		buttonPanel.add(renameButton);
		buttonPanel.add(saveButton);

		buttonPanel.add(revertButton);
		buttonPanel.add(deleteButton);

		JLabel descriptionLabel = new JLabel("Description:");
		descriptionArea = new JTextArea();
		descriptionArea.setPreferredSize(new Dimension(360, 80));
		JPanel descriptionPanel = new JPanel();
		descriptionPanel.add(descriptionLabel);
		descriptionPanel.add(descriptionArea);
		buttonPanel.add(descriptionPanel);


		JLabel confignameLabel = new JLabel("Configuration");
		confignameField = new JComboBox<String>();
		for (String k : configs.keySet()) {
			confignameField.addItem(k);
		}
		confignameField.addActionListener(this);
		confignameField.setActionCommand("selection");

		confignameField.setPreferredSize(new Dimension(240,20));
		JPanel confignamePanel = new JPanel ();
		confignamePanel.add(confignameLabel);
		confignamePanel.add(confignameField);


		topPanel.add(buttonPanel,BorderLayout.CENTER);
		topPanel.add(confignamePanel,BorderLayout.NORTH);
		tabs = new JTabbedPane();
		mainPanel.add(tabs,BorderLayout.CENTER);
		mainPanel.add(topPanel,BorderLayout.NORTH);


		JPanel cfgPanel = new JPanel();
		FlowLayout mgr = new FlowLayout(FlowLayout.RIGHT);
		cfgPanel.setLayout(mgr);
		cfgPanel.setBorder(BorderFactory.createEtchedBorder());
		populate(cfgPanel);
		tabs.add("Import configuration", cfgPanel);

		mapPanelHashMap = new LinkedHashMap<String,ImportMapPanel>();
		mapPanel = new JPanel();
		FlowLayout mgr2 = new FlowLayout(FlowLayout.RIGHT);
		mapPanel.setLayout(mgr2);
		mapPanel.setBorder(BorderFactory.createEtchedBorder());
		String subject = (String) subjectBox.getSelectedItem();

		JPanel loadHeadersPanel = new JPanel();
		JButton loadHeadersButton = new JButton("Load column names from data source");
		loadHeadersButton.addActionListener(this);
		loadHeadersButton.setActionCommand("loadHeaders");
		loadHeadersPanel.add(loadHeadersButton);
		mapPanel.add(loadHeadersPanel);

		JPanel constantPanel = new JPanel();
		JLabel constantLabel = new JLabel("Fixed value            Value mapping   ");
		constantPanel.add(constantLabel);
		mapPanel.add(constantPanel);

		
		
		
		tabs.add("Import variable mapping", mapPanel);

		String item = (String)confignameField.getSelectedItem();
		ImportConfig cfg = configs.get(item);
		updateUi(cfg);
		oldSelection = item;
		updateMappingTab(subject,false,false);
		
		
		/*
		JScrollPane mainScroller = new JScrollPane(mainPanel);
		mainScroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		mainScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

		JPanel mainPanel2 = new JPanel(new BorderLayout());
		mainPanel2.add(mainScroller,BorderLayout.CENTER);
		*/
		
		this.add(mainPanel);
		
		if (isMain) {
			this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		}
		
		
		this.setPreferredSize(new Dimension(640,1040));
		this.pack();
		this.setResizable(true);
		this.setVisible(true);
	}
	
	public ImportConfig getActiveConfig() {
		String item = (String)confignameField.getSelectedItem();
		ImportConfig cfg = configs.get(item);
		return cfg;
	}

	private void readConfigs() {
		// Find all configs and read them
		configs = new HashMap<String,ImportConfig>();

		//TODO: Find a suitable location for the CONFIG_DIR constant and make it available to the classes that needs it
		File dir = new File(".");
		File[] files = dir.listFiles();
		for (File f : files) {
			if (f.getName().endsWith(".import")) {
				try {
					ImportConfig cfg = ImportConfigHandler.loadConfig(f);
					configs.put(f.getName(),cfg);
				} catch (ClassNotFoundException | IOException e) {
					e.printStackTrace();
					JOptionPane.showMessageDialog(null,
							"Could not open config file "+f.toString()+". If it is open in another program, please close it and try again.");
				}
			}
		}

	}

	// This methods updates the GUI components from an ImportConfig object
	private void updateUi(ImportConfig cfg) {

		if (cfg==null) {
			return;
		}

		configTypeBox.setSelectedIndex(cfg.getImportType());
		authTypeBox.setSelectedIndex(cfg.getAuthType());
		flexibleSourceBox.setSelectedIndex(cfg.getDataSourceFlexible());

		infileField.setText(cfg.getDatafile());
		databaseServerField.setText(cfg.getDatabaseServer());
		indatabaseField.setText(cfg.getDatabase());
		intableField.setText(cfg.getDatatable());
		queryField.setText(cfg.getSqlQuery());
		usernameField.setText(cfg.getUsername());
		passwordField.setText(new String(cfg.getPassword()));
		subjectBox.setSelectedItem(cfg.getSubject());

		scheduleBox.setSelectedIndex(cfg.getScheduleUnit());
		scheduleField.setText(Integer.toString(cfg.getScheduleAmount()));

		updateMappingTab(cfg.getSubject(),true,false);

		LinkedHashMap<String,String> dataMap = cfg.getMap();
		for (String k : dataMap.keySet()) {
			String value = dataMap.get(k);
			mapPanelHashMap.get(k).setMappingField(value);
		}

		LinkedHashMap<String,String> constants = cfg.getConstants();
		for (String k : constants.keySet()) {
			String value = constants.get(k);
			mapPanelHashMap.get(k).setConstant(value);
		}

	}


	// Populate the first panel, for import configuration.
	private void populate(JPanel cfgPanel) {

		JPanel subjectPanel = new JPanel();
		JLabel subjectLabel = new JLabel("Subject for the import: ");
		subjectBox = new JComboBox<String>();
		File dir = new File(".");
		File[] files = dir.listFiles();

		// Check which subjects that are available and add them to the dropdown
		for (File f : files) {
			if (f.getName().endsWith(".meta")) {
				String name = f.getName().substring(0,f.getName().length()-5);
				subjectBox.addItem(name);
			}
		}

		subjectPanel.add(subjectLabel);
		subjectPanel.add(subjectBox);
		cfgPanel.add(subjectPanel);

		JPanel configTypePanel = new JPanel();
		JLabel configTypeLabel = new JLabel("Type of data source for import: ");
		String[] options = {"SQL","SQLite","Excel","csv"};
		configTypeBox = new JComboBox<String>(options);
		configTypePanel.add(configTypeLabel);
		configTypePanel.add(configTypeBox);
		cfgPanel.add(configTypePanel);

		JPanel authTypePanel = new JPanel();
		JLabel authTypeLabel = new JLabel("Authentication method (for SQL or SQLite): ");
		String[] options2 = {"None/Integrated","Password"};
		authTypeBox = new JComboBox<String>(options2);
		authTypePanel.add(authTypeLabel);
		authTypePanel.add(authTypeBox);
		cfgPanel.add(authTypePanel);

		JPanel flexibleSourcePanel = new JPanel();
		JLabel flexibleSourceLabel = new JLabel("Choose data source (file/database+table) every time the import is run? ");
		String[] options3 = {"No","Yes"};
		flexibleSourceBox = new JComboBox<String>(options3);
		flexibleSourcePanel.add(flexibleSourceLabel);
		flexibleSourcePanel.add(flexibleSourceBox);
		cfgPanel.add(flexibleSourcePanel);

		JPanel infilePanel = new JPanel();
		JLabel infileLabel = new JLabel("File for CSV/Excel/SQLite import ");
		infileField = new JTextField("");
		infileField.setPreferredSize(new Dimension(240, 22));
		JButton chooseInfileButton = new JButton("Browse");
		chooseInfileButton.setActionCommand("browseFile");
		chooseInfileButton.addActionListener(this);
		infilePanel.add(infileLabel);
		infilePanel.add(infileField);
		infilePanel.add(chooseInfileButton);
		cfgPanel.add(infilePanel);

		// For SQL databases, the apropriate drivers must be available. Right now MySQL and Sqlserver drivers are included.
		String serverToolTipText = "<html>Must be prefixed with protocol:// and end with :port<br>For example: mysql://localhost:1433<br>Supported protocols:<br>sqlserver<br>mysql</html>";
		JPanel databaseServerPanel = new JPanel();
		JLabel databaseServerLabel = new JLabel("Database server for SQL import ");
		databaseServerField = new JTextField("");
		databaseServerField.setPreferredSize(new Dimension(240, 22));
		databaseServerField.setToolTipText(serverToolTipText);
		databaseServerPanel.add(databaseServerLabel);
		databaseServerPanel.add(databaseServerField);
		databaseServerPanel.setToolTipText(serverToolTipText);
		cfgPanel.add(databaseServerPanel);

		JPanel indatabasePanel = new JPanel();
		JLabel indatabaseLabel = new JLabel("Database for SQL import ");
		indatabaseField = new JTextField("");
		indatabaseField.setPreferredSize(new Dimension(240, 22));
		indatabasePanel.add(indatabaseLabel);
		indatabasePanel.add(indatabaseField);
		cfgPanel.add(indatabasePanel);


		// TODO: Dynamically read sheet name from Excel
		JPanel intablePanel = new JPanel();
		JLabel intableLabel = new JLabel("Table/view for SQL import, sheet name for Excel import ");
		intableField = new JTextField("");
		intableField.setPreferredSize(new Dimension(140, 22));
		intablePanel.add(intableLabel);
		intablePanel.add(intableField);
		cfgPanel.add(intablePanel);

		JPanel queryPanel = new JPanel();
		JLabel queryLabel = new JLabel("Filter to be used with SQL (using WHERE clause) ");
		queryField = new JTextField("");
		queryField.setPreferredSize(new Dimension(200, 22));
		queryPanel.add(queryLabel);
		queryPanel.add(queryField);
		cfgPanel.add(queryPanel);

		JPanel usernamePanel = new JPanel();
		JLabel usernameLabel = new JLabel("Username (if authentication is used) ");
		usernameField = new JTextField("");
		usernameField.setPreferredSize(new Dimension(200, 22));
		usernamePanel.add(usernameLabel);
		usernamePanel.add(usernameField);
		cfgPanel.add(usernamePanel);

		JPanel passwordPanel = new JPanel();
		JLabel passwordLabel = new JLabel("Password (if authentication is used) ");
		passwordField = new JPasswordField();
		passwordField.setPreferredSize(new Dimension(200, 22));
		passwordPanel.add(passwordLabel);
		passwordPanel.add(passwordField);
		cfgPanel.add(passwordPanel);

		JPanel schedulePanel = new JPanel();
		JLabel scheduleLabel = new JLabel("Import, link, and submit to configured systems every");
		scheduleField = new JTextField("");
		scheduleField.setPreferredSize(new Dimension(80, 22));
		String[] options4 = {"- (Not enabled)","Minutes","Hours","Days"};
		scheduleBox = new JComboBox<String>(options4);
		schedulePanel.add(scheduleLabel);
		schedulePanel.add(scheduleField);
		schedulePanel.add(scheduleBox);
		cfgPanel.add(schedulePanel);
	}

	// Actions
	@Override
	public void actionPerformed(ActionEvent e) {

		String command = e.getActionCommand();

		// Close button, closes the GUI
		if (command.equals("close")) {
			this.dispose();

			// Load the headers from the configured data source into the mappings tab
		} else if (command.equals("loadHeaders")) {
			int tab = tabs.getSelectedIndex();
			if (tab==1) {
				String subject = (String) subjectBox.getSelectedItem();
				updateMappingTab(subject,true,true);
			}

			// Save configs
		} else if (command.equals("save")) {

			ImportConfig cfg = createConfig();
			String selected = (String)confignameField.getSelectedItem();
			if (selected!=null) {
				configs.put(selected,cfg);
			}

			for (String k : configs.keySet()) {
				ImportConfig cfg2 = configs.get(k);
				File f = new File(k);
				if (cfg2!=null && f!=null) {
					try {
						ImportConfigHandler.saveConfig(f, cfg2);
					} catch (IOException e1) {
						e1.printStackTrace();
						JOptionPane.showMessageDialog(null,
								"Could not save to config file "+f.toString()+". If it is open in another program, please close it and try again.");
					}
				}
			}

			// Revert to saved state
		} else if (command.equals("revert")) {
			for (String name : configs.keySet()) {
				File f = new File(name);
				if (f.exists()) {
					try {
						ImportConfig cfg = ImportConfigHandler.loadConfig(f);
						configs.put(name,cfg);
					} catch (ClassNotFoundException | IOException e1) {

						e1.printStackTrace();
					}
				} else {
					configs.put(name,new ImportConfig());
				}
			}
			ImportConfig cfg = configs.get((String)confignameField.getSelectedItem());
			updateUi(cfg);

			// Browse for a file to import from
		} else if (command.equals("browseFile")) {
			JFileChooser chooser = new JFileChooser(); 
			chooser.setCurrentDirectory(new java.io.File("."));

			if (configTypeBox.getSelectedItem().equals("csv")) {
				FileFilter filterCsv = new FileNameExtensionFilter("CSV file (.csv)","csv");
				chooser.addChoosableFileFilter(filterCsv);
			} else if (configTypeBox.getSelectedItem().equals("Excel")) {
				FileFilter filterExcel = new FileNameExtensionFilter("Excel file (.xlsx)","xlsx");
				chooser.addChoosableFileFilter(filterExcel);
			} else if (configTypeBox.getSelectedItem().equals("SQLite")) {
				FileFilter filterExcel = new FileNameExtensionFilter("SQLite database (.db, .sqlite)","db","sqlite");
				chooser.addChoosableFileFilter(filterExcel);
			} else {
				return;
			}

			chooser.setAcceptAllFileFilterUsed(true);
			chooser.setDialogTitle("Choose file for import");
			chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			chooser.setAcceptAllFileFilterUsed(true);

			if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) { 		   
				infileField.setText(chooser.getSelectedFile().toString());
			}

			// If another config is selected, update GUI to reflect the data in this config
		} else if (command.equals("selection")) {

			// Put data for previously selected config into an object
			ImportConfig cfgOld = createConfig();
			if (oldSelection!=null) {
				configs.put(oldSelection,cfgOld);
			}

			String item = (String)confignameField.getSelectedItem();
			ImportConfig cfg = configs.get(item);
			updateUi(cfg);

			// Make the newly selected config the one to save to an object on next selection action
			oldSelection = item;

			// Create new config
		} else if (command.equals("new")) {

			// Get user input for config name
			String s = (String)JOptionPane.showInputDialog(
					this,
					"Choose a name for the config:",
					"Create new config",
					JOptionPane.PLAIN_MESSAGE
					);

			if ((s != null) && (s.length() > 0)) {
				if (!s.endsWith(".import")) {
					s = s + ".import";
				}
				for (String k : configs.keySet()) {
					if (s.equals(k)) {
						JOptionPane.showMessageDialog(this,
								"This config name already exists.");
						return;
					}
				}

				// Create config and update GUI
				ImportConfig cfg = new ImportConfig();
				configs.put(s,cfg);
				confignameField.addItem(s);
				confignameField.setSelectedItem(s);
				updateUi(cfg);
			}


			// Rename config
		} else if (command.equals("rename")) {

			// Get old name
			String name = (String)confignameField.getSelectedItem();
			if (name==null) {
				return;
			}

			// Get user input for new name
			String s = (String)JOptionPane.showInputDialog(
					this,
					"Choose a new name for config "+name,
					"Rename config",
					JOptionPane.PLAIN_MESSAGE
					);

			if ((s != null) && (s.length() > 0)) {
				if (!s.endsWith(".import")) {
					s = s + ".import";
				}
				for (String k : configs.keySet()) {
					if (s.equals(k)) {
						JOptionPane.showMessageDialog(this,
								"This config name already exists, choose another name.");
						return;
					}
				}

				// Create new config from current data shown in GUI
				ImportConfig cfg = createConfig();

				// If config is saved to file, save to new file and get rid of the old one
				// TODO: Potential issue, if the new file is created and the delete then fails, there is a duplication.
				File oldFile = new File(name);
				File newFile = new File(s);
				if (oldFile.exists()) {
					try {
						ImportConfigHandler.saveConfig(newFile, cfg);
						oldFile.delete();
					} catch (IOException e1) {
						e1.printStackTrace();
						JOptionPane.showMessageDialog(this,
								"Writing to file "+s+" failed. Aborting rename.");
						return;
					}
				}

				// Replace config in HashMap
				configs.remove(name);
				configs.put(s,cfg);

				// Reflect the name change in the GUI
				confignameField.removeItem(name);
				confignameField.addItem(s);
				confignameField.setSelectedItem(s);
				updateUi(cfg);
			} 


			// Delete config (This is only a logical deletion, file will be renamed to .import.bak and will not be visible any more)
			// It can be restored through the file system by removing the .bak
		} else if (command.equals("delete")) {
			String name = (String)confignameField.getSelectedItem();
			if (name==null) {
				return;
			}

			// Ask user to confirm
			int n = JOptionPane.showConfirmDialog(
					this,
					"Are you sure? This will rename the config file to "+name+".bak\n and it will no longer be visible.",
					"Delete config?",
					JOptionPane.YES_NO_OPTION);
			if (n==JOptionPane.YES_OPTION) {
				// TODO: This is replication of code with the rename command, can be extracted to method
				File oldFile = new File(name);
				File newFile = new File(name+".bak");
				ImportConfig cfg = createConfig();
				if (oldFile.exists()) {
					try {
						ImportConfigHandler.saveConfig(newFile, cfg);
						oldFile.delete();
					} catch (IOException e1) {
						e1.printStackTrace();
						JOptionPane.showMessageDialog(this,
								"Deletion failed.");
						return;

					}
				}


				oldSelection = null;
				confignameField.removeItem(name);
				try {
					// Try to set the selected config to the first item in the dropdown
					confignameField.setSelectedIndex(0);
					ImportConfig cfg2 = configs.get((String)confignameField.getSelectedItem());
					updateUi(cfg2);
					String item = (String)confignameField.getSelectedItem();
					oldSelection = item;
				} catch (Exception e2) {
					// If no other configs exist, generate an empty one
					ImportConfig cfg2 = new ImportConfig();
					updateUi(cfg2);
				}

			}

		}
	}

	// This method updates the mapping tab with the headings from the selected subject metadata file
	// and the headings from the selected data source (if it is valid)
	private void updateMappingTab(String subject, boolean forceUpdate, boolean readHeadersFromSource) {

		//Get the selected config
		ImportConfig cfg = configs.get(confignameField.getSelectedItem());

		//TODO: This base directory should be in global field CONFIG_DIR
		File dir = new File(".");
		File[] files = dir.listFiles();
		for (File f : files) {
			// Find the corresponding subject metadata file
			if (f.getName().equals(subject+".meta")) {

				// Try to read all headers from data source, just add empty if data source returned null header
				ArrayList<String> columns = new ArrayList<String>();
				columns.add("");
				
				ArrayList<String> header=null;
				
				if (readHeadersFromSource) {
					header = readDataHeader();
					ImportConfig activeCfg = getActiveConfig();
					if (activeCfg!=null) {
						activeCfg.setDataSourceHeaders(header);
					}
				} else {
					ImportConfig activeCfg = getActiveConfig();
					if (activeCfg!=null) {
						header = activeCfg.getDatasourceHeaders();
					}
					if (header==null) {
						header = readDataHeader();
						if (activeCfg!=null) {
							activeCfg.setDataSourceHeaders(header);
						}
					}
				}
				
				if (header!=null) {
					columns.addAll(header);
				}

				// Open metadata file and step through the fields, adding a controlled vocabulary when provided
				// TODO: This is a bit hacky, it entagles GUI operations with file parsing, should be rewritten 
				ArrayList<String> fields = new ArrayList<String>();
				ArrayList<String> lines = new ArrayList<String>();
				BufferedReader br;
				try {

					// Read all lines from metadata file
					br = new BufferedReader(new FileReader(f));
					String line;
					while ((line=br.readLine())!=null) {
						lines.add(line);
					}
					br.close();
			
					lines.add(UPLOAD_COLUMN+"=Yes,No");
					lines.add(BASE_FILE_COLUMN);
					lines.add(ASSEMBLY_FILE_COLUMN);
					lines.add(READ_FILES_COLUMN);
					
					// Iterate over the fields
					for (String field : lines) {

						// Check if there is a '=' present, if so, the field name is everything up to it
						int dividerIndex = field.indexOf("=");
						String[] vocabulary=null;
						if (dividerIndex!=-1) {
							vocabulary = field.substring(dividerIndex+1).split(",",-1);
							field = field.substring(0, dividerIndex);
						}
						fields.add(field);

						// Check if this is a field not previously in the GUI, or if the method was called with a force update flag
						if (!mapPanelHashMap.containsKey(field) || forceUpdate) {

							// Generate a new panel, or get an existing one
							ImportMapPanel panel;
							if (mapPanelHashMap.containsKey(field)) {
								// panel already exists, update
								panel = mapPanelHashMap.get(field);
								panel.setMappingFields(columns);

							} else {
								// New panel, generate it

								// Get map between import values and metadata values if there is a controlled vocabulary
								LinkedHashMap<String,String> valueMap = null;
								if (cfg!=null) {
									valueMap = cfg.getValueMap().get(field);
								}
								panel = new ImportMapPanel(field,columns,vocabulary, valueMap);
							}

							// Set the mapping for this panel
							if (cfg!=null) {
								String value = cfg.getMap().get(field);
								if (value!=null) {
									panel.setMappingField(value);
								}
							}

							// Add panel to HashMap and to GUI
							mapPanelHashMap.put(field,panel);
							mapPanel.add(panel);
						}
					}

					// Iterate over all existing panels, if it is not in the metadata set, remove it from GUI and HashMap
					for (String k : new LinkedHashSet<String>(mapPanelHashMap.keySet())) {
						if (fields.indexOf(k)==-1) {
							mapPanel.remove(mapPanelHashMap.get(k));
							mapPanelHashMap.remove(k);
						}
					}

				} catch (IOException e) {
					e.printStackTrace();
					continue;

				}
			}
		}
	}

	// Read the header from the import data source
	private ArrayList<String> readDataHeader() {
		
		System.out.println("Reading headers from source");
		ArrayList<String> header=new ArrayList<String>();

		// Get parameters from GUI
		File infile = new File (infileField.getText());
		String database = indatabaseField.getText();
		String databaseServer = databaseServerField.getText();
		String table = intableField.getText();
		int dataType = configTypeBox.getSelectedIndex();


		switch (dataType) {
		
		// CSV
		case ImportConfig.IMPORT_CSV:
			if (!infile.exists()) {
				error("File does not exist");
				return header;
			}
			header = ImportTools.readCsvHeader(infile);
			break;
		
		//Excel
		case ImportConfig.IMPORT_EXCEL:
			if (!infile.exists()) {
				error("File does not exist");
				return header;
			}
			if (table.equals("")) {
				error("Excel sheet must be chosen");
				return header;
			}
			header = ImportTools.readExcelHeader(infile, table);
			break;

		//SQLite
		case ImportConfig.IMPORT_SQLITE:
			if (!infile.exists()) {
				error("File does not exist");
				return header;
			}

			if (table.equals("")) {
				error("SQLite table must be chosen");
				return header;
			}
			header = ImportTools.readSqliteHeader(infile.toString(), table);
			break;

		// SQL
		case ImportConfig.IMPORT_SQL:
			if (databaseServer.equals("")) {
				return header;
			}
			if (database.equals("")) {
				error("SQL database must be chosen");
				return header;
			}
			if (table.equals("")) {
				error("SQL table must be chosen");
				return header;
			}
			int authMethod = authTypeBox.getSelectedIndex();
			String username = usernameField.getText();
			char[] password = passwordField.getPassword();
			ImportSqlAuth auth = new ImportSqlAuth(authMethod, username, password);
			header = ImportTools.readSqlHeader(databaseServer, database, table, auth);
			break;

		}


		return header;
	}

	private void error(String message) {
		JOptionPane.showMessageDialog(this, message);
	}

	// Generate an ImportConfig object from the current GUI data
	private ImportConfig createConfig() {
		
		ImportConfig activeConfig = getActiveConfig();
		ArrayList<String> datasourceHeaders = activeConfig.getDatasourceHeaders();
		ImportConfig cfg = new ImportConfig();
		cfg.setDataSourceHeaders(datasourceHeaders);
		cfg.setImportType(configTypeBox.getSelectedIndex());
		cfg.setAuthType(authTypeBox.getSelectedIndex());
		cfg.setDataSourceFlexible(flexibleSourceBox.getSelectedIndex());
		cfg.setDatafile(infileField.getText());
		cfg.setDatabaseServer(databaseServerField.getText());
		cfg.setDatabase(indatabaseField.getText());
		cfg.setDatatable(intableField.getText());
		cfg.setSqlQuery(queryField.getText());
		cfg.setUsername(usernameField.getText());
		cfg.setPassword(passwordField.getPassword());
		cfg.setSubject((String)subjectBox.getSelectedItem());

		int scheduleAmount = 0;
		try {
			scheduleAmount = Integer.parseInt(scheduleField.getText());
		} catch (Exception e) {
			scheduleAmount = 0;
		}
		cfg.setScheduleAmount(scheduleAmount);
		cfg.setScheduleUnit(scheduleBox.getSelectedIndex());

		LinkedHashMap<String,LinkedHashMap<String,String>> valueMapMap = new LinkedHashMap<String,LinkedHashMap<String,String>>();
		LinkedHashMap<String,String> dataMap = new LinkedHashMap<String,String>();
		for (String k : mapPanelHashMap.keySet()) {
			dataMap.put(k, mapPanelHashMap.get(k).getMappingFieldname());
			valueMapMap.put(k, mapPanelHashMap.get(k).getValueMap());
		}
		cfg.setMap(dataMap);
		cfg.setValueMap(valueMapMap);

		LinkedHashMap<String,String> constants = new LinkedHashMap<String,String>();
		for (String k : mapPanelHashMap.keySet()) {
			constants.put(k, mapPanelHashMap.get(k).getConstant());
		}
		cfg.setConstants(constants);

		return cfg;
	}

	// Main method for running this GUI stand alone
	public static void main(String[] args) {
		try {
			// Set System L&F
			UIManager.setLookAndFeel(
					UIManager.getSystemLookAndFeelClassName());
		} 
		catch (UnsupportedLookAndFeelException e) {
			
		}
		catch (ClassNotFoundException e) {
			
		}
		catch (InstantiationException e) {
			
		}
		catch (IllegalAccessException e) {
			
		}

		ImportConfigGUI gui = new ImportConfigGUI(true);
		gui.init();
	}


}
