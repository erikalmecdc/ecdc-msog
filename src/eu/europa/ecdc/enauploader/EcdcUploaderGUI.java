package eu.europa.ecdc.enauploader;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

public class EcdcUploaderGUI extends JFrame implements ActionListener {

	private static final long serialVersionUID = 1268136970945881244L;

	// Column names for reserved columns in the isolate table
	private static final String BASE_FILE_COLUMN = "File base name";
	private static final String ASSEMBLY_FILE_COLUMN = "Assembly file";
	private static final String READ_FILES_COLUMN = "Read files";
	private static final String MODIFIED_COLUMN = "Modified date";
	private static final String UPLOAD_COLUMN = "Ready for upload";
	private static final String UI_COLUMN = "ECDC event (UI)";
	private static final String TESSY_ID_COLUMN = "TESSy id";
	private static final String TESSY_BATCH_COLUMN = "TESSy batch";
	private static final String TESSY_TEST_COLUMN = "Selected for TESSy upload";
	private static final String TESSY_UPLOADED_COLUMN = "TESSy last uploaded";
	private static final String ENA_RUN_COLUMN = "ENA run id";
	private static final String TESSY_VALIDATION_COLUMN = "TESSy validation";
	private static final String TESSY_APPROVED_COLUMN = "TESSy last approved";
	private static final String SFTP_COLUMN = "ECDC SFTP uploaded";
	
	private static final String VERSION = "1.0.8";
	
	
	

	private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	// Config directory = same as working directory, 
	private static final String CONFIG_DIR = ".";

	protected static final int EDITABLE_EXTRA_FIELDS = 5;
	private static final int LOCKED_FIELDS = 8;

	// Is this class run using the main method?
	private boolean isMain;

	// HashMap of isolate tables
	private HashMap<String, JTable> tableHash;

	// HashMap of upload configurations for each table and any number of import configurations
	private HashMap<String, UploadConfig> configHash;
	private HashMap<String, ImportConfig> importConfigHash;

	// HashMap of TESSy batches in progress and a HashMap with the TESSy SUBJECTs for those batches.
	// TODO: Improve the handling of batches, old batches remain sometimes when the program is closed during upload. 
	private HashMap<String,TessyBatch> batches;
	private HashMap<String,String> batchSubjects;

	// Fields in addition to the metadata fields (reserved fields)
	private ArrayList<String> extraFields;

	// UI elements that need to be accessed as fields
	private JTabbedPane tabs;
	private JMenu importQuickMenu;
	private JScrollPane scroller;
	private JLabel totalEntriesLabel;
	private JLabel selectedEntriesLabel;	

	// Import scheduler for automated submissions, and job handler for submission jobs
	private ImportScheduler scheduler;
	private EcdcJobHandler jobHandler;

	private ImportConfigGUI importDialogWindow;

	private UploadConfigGUI uploadDialogWindow;




	public EcdcUploaderGUI(boolean isMain) {
		this.isMain = isMain;
	}

	public static void main(String[] args) {

		// Set System L&F
		try {
			UIManager.setLookAndFeel(
					UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}

		// Create UI, with the isMain flag set to true
		EcdcUploaderGUI gui = new EcdcUploaderGUI(true);

		// Init UI
		gui.init();

	}

	private void init() {

		// Initialize variables
		batches = new HashMap<String,TessyBatch>();
		batchSubjects = new HashMap<String,String>();

		importConfigHash = new HashMap<String,ImportConfig>();

		

		// Add all the global reserved fields to the extraFields list
		extraFields = new ArrayList<String>();
		extraFields.add(MODIFIED_COLUMN);
		extraFields.add(UPLOAD_COLUMN);
		extraFields.add(UI_COLUMN);
		extraFields.add(BASE_FILE_COLUMN);
		extraFields.add(READ_FILES_COLUMN);
		extraFields.add(ASSEMBLY_FILE_COLUMN);
		extraFields.add(TESSY_TEST_COLUMN );
		extraFields.add(TESSY_BATCH_COLUMN );
		extraFields.add(TESSY_VALIDATION_COLUMN);
		extraFields.add(TESSY_UPLOADED_COLUMN);
		extraFields.add(TESSY_APPROVED_COLUMN);
		extraFields.add(TESSY_ID_COLUMN);
		extraFields.add(ENA_RUN_COLUMN);
		extraFields.add(SFTP_COLUMN);


		// Init UI elements
		totalEntriesLabel = new JLabel("");
		selectedEntriesLabel = new JLabel("");
		JPanel mainPanel = new JPanel(new BorderLayout());
		JMenuBar menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu("Data");
		JMenu configMenu = new JMenu("Setup");
		JMenu uploadMenu = new JMenu("Submission");
		JMenu viewMenu = new JMenu("View");
		menuBar.add(fileMenu);
		menuBar.add(configMenu);
		menuBar.add(uploadMenu);
		menuBar.add(viewMenu);

		JMenuItem addrowItem = new JMenuItem("Add row");
		addrowItem.addActionListener(this);
		addrowItem.setActionCommand("addrow");
		KeyStroke ctrlAKeyStroke = KeyStroke.getKeyStroke("control ENTER");
		addrowItem.setAccelerator(ctrlAKeyStroke);
		fileMenu.add(addrowItem);

		JMenuItem addrowsItem = new JMenuItem("Add several rows");
		addrowsItem.addActionListener(this);
		addrowsItem.setActionCommand("addrows");
		KeyStroke ctrlAAKeyStroke = KeyStroke.getKeyStroke("control alt ENTER");
		addrowsItem.setAccelerator(ctrlAAKeyStroke);
		fileMenu.add(addrowsItem);

		JMenuItem removerowsItem = new JMenuItem("Delete selected rows");
		removerowsItem.addActionListener(this);
		removerowsItem.setActionCommand("removerows");
		fileMenu.add(removerowsItem);

		JMenuItem saveItem = new JMenuItem("Save data");
		saveItem.addActionListener(this);
		saveItem.setActionCommand("save");
		KeyStroke ctrlSKeyStroke = KeyStroke.getKeyStroke("control S");
		saveItem.setAccelerator(ctrlSKeyStroke);
		fileMenu.add(saveItem);

		JMenuItem loadItem = new JMenuItem("Revert to saved");
		loadItem.addActionListener(this);
		loadItem.setActionCommand("revert");
		KeyStroke ctrlLKeyStroke = KeyStroke.getKeyStroke("control L");
		loadItem.setAccelerator(ctrlLKeyStroke);
		fileMenu.add(loadItem);

		fileMenu.addSeparator();

		importQuickMenu = new JMenu("Import using template");
		fileMenu.add(importQuickMenu);

		loadImportAndScheduleConfigs(importQuickMenu);

		JMenuItem importMenuItem = new JMenuItem("Manage imports and scheduling...");
		importMenuItem.addActionListener(this);
		importMenuItem.setActionCommand("importDialog");
		configMenu.add(importMenuItem);

		JMenuItem configItem = new JMenuItem("Configure upload parameters...");
		configItem.addActionListener(this);
		configItem.setActionCommand("config");
		configMenu.add(configItem);

		JMenuItem linkItem = new JMenuItem("Link sequence files for selected isolates");
		linkItem.addActionListener(this);
		linkItem.setActionCommand("link");
		uploadMenu.add(linkItem);

		uploadMenu.addSeparator();

		JMenuItem allUploadItem = new JMenuItem("Submit data to configured systems");
		allUploadItem.addActionListener(this);
		allUploadItem.setActionCommand("uploadSelected");
		uploadMenu.add(allUploadItem);

		uploadMenu.addSeparator();

		JMenuItem enaUploadSelectedItem = new JMenuItem("ENA - Submit all selected isolates");
		enaUploadSelectedItem.addActionListener(this);
		enaUploadSelectedItem.setActionCommand("uploadEnaSelected");
		uploadMenu.add(enaUploadSelectedItem);

		uploadMenu.addSeparator();

		JMenuItem testSelectedItem = new JMenuItem("TESSy - Create batch from selected and test");
		testSelectedItem.addActionListener(this);
		testSelectedItem.setActionCommand("testSelected");
		uploadMenu.add(testSelectedItem);

		JMenuItem uploadAllItem = new JMenuItem("TESSy - Upload batch...");
		uploadAllItem.addActionListener(this);
		uploadAllItem.setActionCommand("uploadBatch");
		uploadMenu.add(uploadAllItem);

		JMenuItem approveAllItem = new JMenuItem("TESSy - Approve batch...");
		approveAllItem.addActionListener(this);
		approveAllItem.setActionCommand("approveBatch");
		uploadMenu.add(approveAllItem);

		JMenuItem rejectItem = new JMenuItem("TESSy - Reject batch...");
		rejectItem.addActionListener(this);
		rejectItem.setActionCommand("rejectBatch");
		uploadMenu.add(rejectItem);

		uploadMenu.addSeparator();
		uploadMenu.addSeparator();

		JMenuItem sftpUploadItem = new JMenuItem("SFTP - Upload sequences");
		sftpUploadItem.addActionListener(this);
		sftpUploadItem.setActionCommand("uploadFtpSelected");
		uploadMenu.add(sftpUploadItem);

		JMenuItem viewJobsItem = new JMenuItem("Show job manager window");
		viewJobsItem.addActionListener(this);
		viewJobsItem.setActionCommand("viewJobs");
		viewMenu.add(viewJobsItem);

		this.setJMenuBar(menuBar);

		// Set program Icon and title
		setTitle("ECDC WGS upload app v"+VERSION);
		try {
			String imagePath = "media/ECDC2.png";
			InputStream imgStream = UploadConfigGUI.class.getResourceAsStream(imagePath );
			BufferedImage myImg;
			myImg = ImageIO.read(imgStream);
			setIconImage(myImg);
		} catch (Exception e) {
			e.printStackTrace();
		}


		// load isolate table contents from files
		loadData();

		// load configs from files
		loadUploadConfigs();

		JPanel titlePanel = new JPanel();
		JLabel titleLabel = new JLabel("Isolate table");



		updateEntriesLabels();


		titlePanel.add(titleLabel);
		JLabel totalText = new JLabel("   Total entries: ");
		titlePanel.add(totalText);
		titlePanel.add(totalEntriesLabel);
		JLabel selectedText = new JLabel("   Selected: ");
		titlePanel.add(selectedText);
		titlePanel.add(selectedEntriesLabel);
		mainPanel.add(titlePanel,BorderLayout.NORTH);

		mainPanel.add(tabs,BorderLayout.CENTER);
		this.add(mainPanel);

		// If this is the main class, close program on window close. 
		// (This class could be instantiated from elsewhere, then it should not exit on close)
		if (isMain) {
			this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		}



		// Init the job handler
		jobHandler = new EcdcJobHandler(); 

		// Init the automated job scheduler
		scheduler = new ImportScheduler(this);
		scheduler.execute();
		
		// Send the config information to the scheduler which can import and upload at regular intervals if so configured
				int scheduleNumber = scheduler.setImportConfigHash(importConfigHash);

				// Notify the user of scheduled submissions.
				if (scheduleNumber>0) {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							JOptionPane.showMessageDialog(null,
									"<html>Automatic import and submission has been configured for "+Integer.toString(scheduleNumber)+" different import configuration(s). This will automatically submit data to configured systems. Go to the 'Manage imports and scheduling...' wizard if you wish to change this.<br>Note that scheduled import and submission only works if you keep the ECDC WGS Upload app open.</html>");
						}

					});

				}

		// Set UI size and make it visible
		this.setSize(1024,800);
		this.setExtendedState(JFrame.MAXIMIZED_BOTH);
		this.setVisible(true);
	}

	private void updateEntriesLabels() {
		String name= tabs.getTitleAt(tabs.getSelectedIndex());
		JTable table = tableHash.get(name);
		totalEntriesLabel.setText(Integer.toString(table.getRowCount()));
		selectedEntriesLabel.setText(Integer.toString(table.getSelectedRowCount()));

	}

	private void loadUploadConfigs() {

		// Init empty HashMap for configs
		configHash = new HashMap<String, UploadConfig>();

		// Iterate through all the tabs and look for a config associated with the SUBJECT
		int totalTabs = tabs.getTabCount();
		for(int i = 0; i < totalTabs; i++)
		{	
			String name= tabs.getTitleAt(i);
			File configFile = new File(name+".cfg");

			// If config does not exist, it must be created before any upload can be performed, warn the user
			if (!configFile.exists()) {
				JOptionPane.showMessageDialog(null,
						"No config found for "+name+", you must create a config called \'"+name+".cfg\' before you can submit any data for "+name+".\nConfigs are created from the \'Setup\' menu");
				continue;
			}

			//Load config from file and add it to HashMap
			try {
				UploadConfig cfg = UploadConfigHandler.loadConfig(configFile);
				configHash.put(name, cfg);
			} catch (Exception e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(this,
						"Loading config "+name+".cfg failed.");
				continue;
			} 


		}
	}


	private void loadData() {

		// Init the actual TabbedPane and a HashMap for associating SUBJECT and tab
		tabs = new JTabbedPane();
		tableHash = new HashMap<String,JTable>();

		tabs.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				updateEntriesLabels();

			}
		});


		// List all files in config directory and iterate through them
		File dir = new File(CONFIG_DIR);
		File[] files = dir.listFiles();
		for (File f : files) {

			// If a batch file is found, load batches from it
			if (f.getName().endsWith(".batch")) {
				String name = f.getName().substring(0,f.getName().length()-6);
				loadBatchFiles(name);
			}

			// If this is a metadata file (supplied by ECDC), create a tab including an isolate table
			if (f.getName().endsWith(".meta")) {
				String[] headers;
				String[][] data;

				// Name of tab derived from metadata file name
				String name = f.getName().substring(0,f.getName().length()-5);

				// Initialize controlled vocabularies use for some fields (coded value lists)
				HashMap<String,String[]> controlledVocabularies = new HashMap<String,String[]>();

				BufferedReader br;
				ArrayList<String> fields = new ArrayList<String>();
				try {
					// Open file
					br = new BufferedReader(new FileReader(f));
					String line;

					// Each row contains a fieldname, and potentially a comma-separated coded value list after =
					while ((line=br.readLine())!=null) {
						fields.add(line);
					}
					// Add the global reserved fields to the list of fields
					for (String s : extraFields) {
						if (fields.indexOf(s)==-1) {
							fields.add(s);
						}
					}

					// Create a table header with size equal to the number of fields
					headers = new String[fields.size()];

					// For every field that contains '=', split it and use the comma-separated values after the = as a controlled vocabulary
					for (int i=0;i<fields.size();i++) {
						String field = fields.get(i);
						int dividerIndex = field.indexOf("=");
						if (dividerIndex!=-1) {
							String[] options = field.substring(dividerIndex+1).split(",",-1);
							field = field.substring(0, dividerIndex);
							controlledVocabularies.put(field,options);
						}
						headers[i] = field;
					}

					// Close metadata file
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
					continue;

				}

				// Load old table data from file
				data = load(name,headers);

				// Create a table model with the headers and data
				DefaultTableModel model = new DefaultTableModel(data,headers) {

					private static final long serialVersionUID = -21870723835576685L;

					// A bit of a hack, sets all extra fields except the first two ones to non-editable
					// TODO: Use the filed name constants instead to identify the filename-fields that should be editable
					public boolean isCellEditable(int row, int column) {
						
						
						if (column>this.getColumnCount()-extraFields.size()+EDITABLE_EXTRA_FIELDS) {
							return false;
						} else {
							return true;
						}
					}
				};

				// Create the isolate table
				JTable table = new JTable(model){

					private static final long serialVersionUID = -7197366759070120859L;


					// Set the tooltip text so that mouse-over shows the contents. For content containing ';', split over several rows
					public String getToolTipText(MouseEvent e) {
						String tip = "";
						java.awt.Point p = e.getPoint();
						int rowIndex = rowAtPoint(p);
						int colIndex = columnAtPoint(p);


						try {
							String[] tips = getValueAt(rowIndex, colIndex).toString().split(";");
							for (String t : tips) {
								tip = tip+t+"<br>";
							}
						} catch (RuntimeException e1) {
							// Do nothing on error (it is hard to envision what could go wrong here anyway)
						}
						if (tip.equals("")) {
							return null;
						}
						tip = "<html>"+tip+"</html>";
						return tip;
					}
				};




				// When new rows are added, scroll to bottom of table. Must be done using invokeLater, as the table is not updated visually
				// on the same thread as the TableModel modification
				table.getModel().addTableModelListener(new TableModelListener()  {
					@Override
					public void tableChanged(TableModelEvent e) {
						if (e.getType()==TableModelEvent.DELETE) {
							System.out.println("DELETE");
							return;
						}
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								if (e.getType()==TableModelEvent.INSERT) {
									table.scrollRectToVisible(table.getCellRect(table.getRowCount()-1, 0, true));
									updateEntriesLabels();
								}
								int col = e.getColumn();
								int dateCol=getColumn(table,MODIFIED_COLUMN);
								if (col<table.getColumnCount()-LOCKED_FIELDS && col!=dateCol) {
									String dString = dateFormat.format(new Date());
									for (int i = e.getFirstRow(); i<=e.getLastRow();i++) {	
										try {
											table.getModel().setValueAt(dString, i, dateCol);
										} catch (Exception e) {
											
										}
									}
								}
							}
						});

					}
				});

				// Add copy/paste functionality using Ctrl+C/Ctrl+V
				new ExcelAdapter(table);

				// Set cell selection mode and disallow reordering of columns
				// it was decided to do row selction only as it is more intuitive for seeing what is going to be submitted
				//table.setCellSelectionEnabled(true); 

				table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION); 
				table.getTableHeader().setReorderingAllowed(false);

				ListSelectionModel selectionModel = table.getSelectionModel();
				selectionModel.addListSelectionListener(new ListSelectionListener() {

					@Override
					public void valueChanged(ListSelectionEvent e) {
						updateEntriesLabels();
					}
				});

				// For the fields with controlled vocabularies, add a JComboBox instead of a normal editable field
				// TODO: Copy/Paste behaviour is a bit weird if you paste something that is not in the vocabulary
				for (String k : controlledVocabularies.keySet()) {
					table.getColumnModel().getColumn(getColumn(table,k)).setCellEditor(new DefaultCellEditor(generateBox(controlledVocabularies.get(k))));
				}

				//Hardcoded vocabulary for the upload column
				String[] options = {"Yes","No"};
				table.getColumnModel().getColumn(getColumn(table,UPLOAD_COLUMN)).setCellEditor(new DefaultCellEditor(generateBox(options)));


				// Set cell renderer to color cells
				table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {

					private static final long serialVersionUID = 1639408938168390421L;

					@Override
					public Component getTableCellRendererComponent(
							JTable table, Object value, boolean isSelected,
							boolean hasFocus, int row, int column) {

						Component c = super.getTableCellRendererComponent(
								table, value, isSelected, hasFocus, row, column);

						String columnName = table.getColumnName(column);

						// Color cell red if TESSy validation has errors
						if (columnName.equals(TESSY_VALIDATION_COLUMN)&& (String)table.getValueAt(row, column)!=null && ((String)table.getValueAt(row, column)).startsWith("E:")) {
							c.setBackground(new Color(235, 100, 100));
							// Color yellow if TESSy validation has warnings
						} else if (columnName.equals(TESSY_VALIDATION_COLUMN) && (String)table.getValueAt(row, column)!=null && ((String)table.getValueAt(row, column)).startsWith("W:")) {
							c.setBackground(new Color(235, 235, 90));
							// A bit of a hack, set color depending on where in the table we are
							// TODO: This should be redone to instead use the reserved fields column numbers, but without looking it up every time using getColumn()
						} else if (column>=table.getColumnCount()-extraFields.size()+1+EDITABLE_EXTRA_FIELDS) {

							if (isSelected) {
								c.setBackground(new Color(155, 155, 155));
							} else {

								c.setBackground(new Color(175, 235, 235));

							}
						} else if (column>=table.getColumnCount()-extraFields.size()+3) {
							if (isSelected) {
								c.setBackground(new Color(155, 155, 155));
							} else {
								c.setBackground(new Color(235, 175, 235));
							}
						} else if (column>=table.getColumnCount()-extraFields.size()) {
							if (isSelected) {
								c.setBackground(new Color(155, 155, 155));
							} else {
								c.setBackground(new Color(225, 225, 225));
							}
						} else {
							if (isSelected) {
								c.setBackground(new Color(175, 175, 175));
							} else {
								c.setBackground(Color.white);
							}
						}

						return c;
					}
				});


				// Encapsulate table in a JScrollPane to allow scrolling
				TableColumn column = null;
				for (int i = 0; i < table.getColumnCount(); i++) {
					column = table.getColumnModel().getColumn(i);
					if (i == 0) {
						column.setPreferredWidth(160);
					} else {
						column.setPreferredWidth(100);
					}
				}    

				TableRowSorter sorter = new TableRowSorter(table.getModel());
				table.setRowSorter(sorter);
				
				// Add MouseListener for onClick event
				table.getTableHeader().addMouseListener(new MouseAdapter() {
					private SortOrder currentOrder = SortOrder.UNSORTED;

					@Override
					public void mouseClicked(MouseEvent e) {
						int column = table.getTableHeader().columnAtPoint(e.getPoint());
						RowSorter sorter = table.getRowSorter();
						List sortKeys = new ArrayList();
						switch (currentOrder) {
						case UNSORTED:
							sortKeys.add(new RowSorter.SortKey(column, currentOrder = SortOrder.ASCENDING));
							break;
						case ASCENDING:
							sortKeys.add(new RowSorter.SortKey(column, currentOrder = SortOrder.DESCENDING));
							break;
						case DESCENDING:
							sortKeys.add(new RowSorter.SortKey(column, currentOrder = SortOrder.UNSORTED));
							break;
						}
						sorter.setSortKeys(sortKeys);
					}
				});

				table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
				
				scroller = new JScrollPane(table);

				JPanel tablePanel = new JPanel(new BorderLayout());


				tablePanel.add(scroller,BorderLayout.CENTER);

				// Add table to HashMap, and add tab to JTabbedPane
				tableHash.put(name, table);
				tabs.add(name, tablePanel);
			}
		}
	}

	// Load the TESSY batch files
	// TODO: This whole functionality is a bit messy, 
	// if the program is closed during TESSy operations, this can go out of synch with the actual batch state in TESSy 
	@SuppressWarnings("unchecked")
	private void loadBatchFiles(String name) {
		File indataFile = new File(name+".batch");

		if (indataFile.exists()) {
			FileInputStream streamIn;
			HashMap<String,TessyBatch> indata;
			try {
				streamIn = new FileInputStream(indataFile);
				ObjectInputStream objectInputstream = new ObjectInputStream(streamIn);
				indata = (HashMap<String,TessyBatch>) objectInputstream.readObject();
				objectInputstream.close();
			} catch (ClassNotFoundException | IOException e) {
				e.printStackTrace();
				indata = new HashMap<String,TessyBatch>();
			}

			// Add loaded batches and batch subjects to corresponding HashMaps
			for (String k : indata.keySet()) {
				TessyBatch bat = indata.get(k);
				batches.put(bat.getBatchId(),bat);
				batchSubjects.put(bat.getBatchId(), name);
			}
		} 

	}

	// Load the isolate table
	// Data is stored as key->value in a serialized ArrayList of HashMaps
	// This is not very efficient but allows for updates of the metadata on the fly
	@SuppressWarnings("unchecked")
	private String[][] load(String name, String[] headers) {

		File indataFile = new File(CONFIG_DIR+"/"+name+".dat");
		ArrayList<HashMap<String,String>> indata;
		if (indataFile.exists()) {
			FileInputStream streamIn;
			try {
				streamIn = new FileInputStream(indataFile);
				ObjectInputStream objectInputstream = new ObjectInputStream(streamIn);
				indata = (ArrayList<HashMap<String,String>>) objectInputstream.readObject();
				objectInputstream.close();
			} catch (ClassNotFoundException | IOException e) {
				e.printStackTrace();
				indata = new ArrayList<HashMap<String,String>>();
			}
		} else {
			indata = new ArrayList<HashMap<String,String>>();
		}
		String[][] data = new String[indata.size()][headers.length];
		int i = 0;
		for (HashMap<String,String> row : indata) {
			int j = 0;
			for (String field : headers) {
				if (row.containsKey(field)) {
					data[i][j] = row.get(field);
				}
				j++;
			}
			i++;
		}
		return data;
	}


	// Load data import templates and add them to the quick import Menu
	public void loadImportAndScheduleConfigs(JMenu menu) {

		File dir = new File(CONFIG_DIR);
		File[] files = dir.listFiles();

		// Init empty HashMap of import configs
		importConfigHash = new HashMap<String,ImportConfig>();
		FILES: for (File f : files) {

			if (f.getName().endsWith(".import")) {
				// Derive the config name from the file name
				String name = f.getName().substring(0,f.getName().length()-7);
				try {
					// Load the config and put it into a HashMap
					ImportConfig cfg = ImportConfigHandler.loadConfig(f);
					importConfigHash.put(name, cfg);
				} catch (ClassNotFoundException | IOException e) {
					JOptionPane.showMessageDialog(this,
							"Config "+name+" could not be loaded from file: "+f.toString()+", the file may be corrupt or locked for reading.");
					e.printStackTrace();
				}

				// Check if a corresponding item already exists in the Menu
				for (int i = 0; i< menu.getItemCount();i++) {
					if (menu.getItem(i) != null && menu.getItem(i).getText().equals(name)){
						continue FILES;
					}
				}

				// if it does not exists, add it to menu
				JMenuItem csvImportItem = new JMenuItem(name);
				csvImportItem.addActionListener(this);
				csvImportItem.setActionCommand("IMPORT_"+name);
				importQuickMenu.add(csvImportItem);
			}
		}
		if (importConfigHash!=null && scheduler!=null) {
			scheduler.setImportConfigHash(importConfigHash);
		}
		
	}

	// Event handler
	@Override
	public void actionPerformed(ActionEvent e) {

		// The importDialog opens an ImportConfigGUI and then loads the importConfigs when it is closed

		if (e.getActionCommand().equals("importDialog")) {
			if (importDialogWindow!=null) {
				importDialogWindow.toFront();
				return;
			}
			ImportConfigGUI importGui = new ImportConfigGUI(false);
			importGui.setLocationRelativeTo(null);
			importGui.init();
			importDialogWindow = importGui;
			importGui.addWindowListener(new WindowAdapter() {
				@Override
				// When the ImportConfigGUI window is closed, reload importConfigs
				public void windowClosing(WindowEvent e) {
					SwingUtilities.invokeLater(new Runnable(){

						@Override
						public void run() {
							importDialogWindow = null;
							loadImportAndScheduleConfigs(importQuickMenu);
						}
					});
				}
			});

			// The config dialog opens a UploadConfigGUI and then loads the upload configs when the window is closed.
		} else if (e.getActionCommand().equals("config")) {
			
			if (uploadDialogWindow!=null) {
				uploadDialogWindow.toFront();
				return;
			}
			
			UploadConfigGUI uploadConfigGui = new UploadConfigGUI(false);
			uploadConfigGui.setLocationRelativeTo(null);
			uploadConfigGui.init();
			uploadDialogWindow = uploadConfigGui;
			
			uploadConfigGui.addWindowListener(new WindowAdapter() {
				@Override
				// When the config window is closed, reload upload configs
				public void windowClosing(WindowEvent e) {
					SwingUtilities.invokeLater(new Runnable(){

						@Override
						public void run() {
							uploadDialogWindow = null;
							loadUploadConfigs();
						}
					});
				}
			});

			// Open job manager
		} else if (e.getActionCommand().equals("viewJobs")) {
			jobHandler.setVisible(true);
			jobHandler.toFront();

			// Save isolate table, note that it is also autosaved on many operations
		} else if (e.getActionCommand().equals("save")) {
			save();

			// revert isolate table to saved version
		} else if (e.getActionCommand().equals("revert")) {
			revert();

			// Add row to active isolate table
		}  else if (e.getActionCommand().equals("addrow")) {
			addSingleRow();

			// Add several rows to active isolate table
		} else if (e.getActionCommand().equals("addrows")) {
			addRowsInteractive();

			// Link sequence data for selected rows
		} else if (e.getActionCommand().equals("removerows")) {
			String name= tabs.getTitleAt(tabs.getSelectedIndex());
			JTable table = tableHash.get(name);
			int num = table.getSelectedRowCount();
			int reply = JOptionPane.showConfirmDialog(null, "This will delete "+Integer.toString(num)+" entries from the "+name+" table. Are you sure?", "Delete rows?", JOptionPane.YES_NO_OPTION);
			if (reply == JOptionPane.YES_OPTION) {
				removeRows();
			}


			// Link sequence data for selected rows
		} else if (e.getActionCommand().equals("link")) {
			EcdcLinkWorker worker = getEcdcLinkWorker(null, null);
			worker.execute();

			// Upload selected rows to ECDC SFTP, uses EcdcJob base class to perform in background.
		} else if (e.getActionCommand().equals("uploadFtpSelected")) {
			EcdcSftpUploadWorker worker = getSftpWorker(null, null);
			if (worker!=null) {
				worker.execute();
			}

			// Upload selected rows to configured systems, uses EcdcJob base class to perform in background.
		} else if (e.getActionCommand().equals("uploadSelected")) {
			EcdcFullUploadWorker worker = getFullUploadWorker(null, null);
			if (worker!=null) {
				worker.execute();
			}

			// Upload selected rows to ENA, uses EcdcJob base class to perform in background.
		} else if (e.getActionCommand().equals("uploadEnaSelected")) {
			EnaSubmissionWorker worker = getEnaSubmissionWorker(null, null);
			if (worker!=null) {
				worker.execute();
			}

			// Test selected rows at TESSy, uses EcdcJob base class to perform in background.
		} else if (e.getActionCommand().equals("testSelected")) {
			EcdcTessyCreateAndTestWorker worker = getEcdcTessyTestWorker(null, null);
			if (worker!=null) {
				worker.execute();
			}

			// Upload batch to TESSy, uses EcdcJob base class to perform in background.
		} else if (e.getActionCommand().equals("uploadBatch")) {
			EcdcTessyUploadWorker worker = getEcdcTessyUploadWorker(null);
			if (worker!=null) {
				worker.execute();
			}

			// Approve batch in TESSy, uses EcdcJob base class to perform in background.
		} else if (e.getActionCommand().equals("approveBatch")) {
			EcdcTessyApprovalWorker worker = getEcdcTessyApprovalWorker(null);
			if (worker!=null) {
				worker.execute();
			}

			// Reject batch in TESSy, uses EcdcJob base class to perform in background.
		}  else if (e.getActionCommand().equals("rejectBatch")) {
			EcdcTessyRejectWorker worker = getEcdcTessyRejectWorker(null);
			if (worker!=null) {
				worker.execute();
			}

			// Quick import triggered from the Menu
		} else if (e.getActionCommand().startsWith("IMPORT_")) {
			String cfgName = e.getActionCommand().substring(7);

			EcdcImportWorker worker = getEcdcImportWorker(cfgName);
			if (worker!=null) {
				worker.execute();
			}
		}
	}


	private void removeRows() {
		String name= tabs.getTitleAt(tabs.getSelectedIndex());
		JTable table = tableHash.get(name);
		DefaultTableModel model = (DefaultTableModel)table.getModel();
		int[] inds = table.getSelectedRows();
		Integer[] indices = new Integer[inds.length];
		int ii = 0;
		for (int value : inds) {
			indices[ii++] = Integer.valueOf(value);
		}
		Arrays.sort(indices, Collections.reverseOrder());
		for(int i = 0; i < indices.length; i++)
		{
			indices[i] = table.convertRowIndexToModel(indices[i]);                              
			model.removeRow(indices[i]); 
		}
	}

	// This functions adds a single row to the active isolate table
	private void addSingleRow() {
		String name= tabs.getTitleAt(tabs.getSelectedIndex());
		JTable table = tableHash.get(name);
		DefaultTableModel model = (DefaultTableModel)table.getModel();
		model.addRow(new String[table.getColumnCount()]);
	}

	// This functions queries the user for the number of rows and then adds them to the active isolate table
	private void addRowsInteractive() {
		String s = (String)JOptionPane.showInputDialog(
				this,
				"Choose number of rows to add",
				"Add rows",
				JOptionPane.PLAIN_MESSAGE
				);

		if (s==null) {
			return;
		}
		int num;
		try {
			num = Integer.parseInt(s);
		} catch (Exception e2) {
			e2.printStackTrace();
			return;
		}
		String name= tabs.getTitleAt(tabs.getSelectedIndex());
		JTable table = tableHash.get(name);
		DefaultTableModel model = (DefaultTableModel)table.getModel();
		for (int i = 0;i<num;i++) {
			model.addRow(new String[table.getColumnCount()]);
		}

	}

	// Function to revert to the saved state of the isolate tables
	private void revert() {
		int totalTabs = tabs.getTabCount();
		for(int i = 0; i < totalTabs; i++)
		{

			// Get table and tableModel
			String name= tabs.getTitleAt(i);
			JTable table = tableHash.get(name);
			DefaultTableModel model = (DefaultTableModel)table.getModel();
			int cols = table.getColumnCount();

			// Keep current headers, do not reload metadata
			String[] headers = new String[cols];
			for (int j = 0;j<cols;j++) {
				headers[j] = table.getColumnName(j);
			}

			// Load data
			String[][] data = load(name,headers);

			// Replace data
			model.setDataVector(data, headers);
		}
	}

	//This function performs quick import using an import config
	public EcdcImportWorker getEcdcImportWorker(String cfgName) {

		// Get the ImportConfig by name
		ImportConfig cfg;
		cfg = importConfigHash.get(cfgName);

		if (cfg==null) {
			return null;
		}
		
		File f = null;
		if (cfg.getDataSourceFlexible()==1) {
			JFileChooser chooser = new JFileChooser(); 
			chooser.setCurrentDirectory(new java.io.File("."));

			if (cfg.getImportType()==ImportConfig.IMPORT_CSV) {
				FileFilter filterCsv = new FileNameExtensionFilter("CSV file (.csv)","csv");
				chooser.addChoosableFileFilter(filterCsv);
			} else if (cfg.getImportType()==ImportConfig.IMPORT_EXCEL) {
				FileFilter filterExcel = new FileNameExtensionFilter("Excel file (.xlsx)","xlsx");
				chooser.addChoosableFileFilter(filterExcel);
			} else {
				return null;
			}

			chooser.setAcceptAllFileFilterUsed(true);
			chooser.setDialogTitle("Choose file for import");
			chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			chooser.setAcceptAllFileFilterUsed(true);

			if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) { 		   
				f = chooser.getSelectedFile();
			}
		}
		
		
		JTable table = tableHash.get(cfg.getSubject());

		
		EcdcImportWorker worker = new EcdcImportWorker(this, null, cfg.getSubject(), null, null, null);
		worker.setTable(table);
		worker.setImportConfig(cfg);
		worker.setFile(f);
		return worker;

	}



	public EcdcAutomationWorker getEcdcAutomationWorker(String cfgName) {


		ImportConfig importConfig;
		importConfig = importConfigHash.get(cfgName);

		if (importConfig==null) {
			return null;
		}
		JTable table = tableHash.get(importConfig.getSubject());

		System.out.println(importConfig.getSubject());


		UploadConfig uploadConfig = configHash.get(importConfig.getSubject());

		if (uploadConfig==null) {
			return null;
		}


		EcdcAutomationWorker worker = new EcdcAutomationWorker(this, null, cfgName, uploadConfig, null, null);
		worker.setTable(table);
		worker.setImportConfig(importConfig);

		return worker;
	}

	// Function for generating a dialog box where you can choose a batch with a given state
	private String getBatchIdInteractive(String batchState) {
		if (batches.isEmpty()) {
			JOptionPane.showMessageDialog(this,
					"No batch have been created yet.");
			return null;
		}
		JComboBox<String> box = new JComboBox<String>();
		for (String id : batches.keySet()) {
			TessyBatch bat = batches.get(id);
			if (bat.getState().matches(batchState)) {
				box.addItem(id+" ("+bat.getState()+")");
			}
		}
		box.setSelectedIndex(box.getItemCount()-1);
		int res = JOptionPane.showConfirmDialog( null, box, "Select batch", JOptionPane.OK_CANCEL_OPTION);
		String choice = (String)box.getSelectedItem();
		if (res != JOptionPane.OK_OPTION || choice==null) {
			return null;
		}
		return(choice.split(" ")[0]);
	}


	// This function is interactive if the argument is null, otherwise it will use the supplied batchId
	// It creates an EcdcJob for rejecting a batch in TESSy
	public EcdcTessyRejectWorker getEcdcTessyRejectWorker(String batchId) {

		// Interactive dialog for selecting batch to reject
		if (batchId==null) {
			batchId = getBatchIdInteractive("VALIDATED");
			if (batchId==null) {
				return null;
			}
		}


		// Get batch
		TessyBatch batch = batches.get(batchId);
		if (batch==null) {
			JOptionPane.showMessageDialog(this, "No such batch "+batchId+" found!");
			return null;
		}
		String batchSubject = batchSubjects.get(batchId);

		// Create and return job, note that it is not executed automatically
		EcdcTessyRejectWorker worker = new 	EcdcTessyRejectWorker(this, batch, batchSubject, null, null, null);
		return worker;
	}



	// This function is interactive if the argument is null, otherwise it will use the supplied batchId
	// It creates an EcdcJob for approving a batch in TESSy
	public EcdcTessyApprovalWorker getEcdcTessyApprovalWorker(String batchId) {

		// Interactive dialog for selecting batch to reject
		if (batchId==null) {
			batchId = getBatchIdInteractive("VALIDATED");
			if (batchId==null) {
				return null;
			}
		}

		// Get batch
		TessyBatch batch = batches.get(batchId);
		if (batch==null) {
			JOptionPane.showMessageDialog(this, "No such batch "+batchId+" found!");
			return null;
		}
		String batchSubject = batchSubjects.get(batchId);

		// Create and return job, note that it is not executed automatically
		EcdcTessyApprovalWorker worker = new 	EcdcTessyApprovalWorker(this,batch,batchSubject,null,null,null);
		return worker;
	}

	// This function is interactive if the argument is null, otherwise it will use the supplied batchId
	// It creates an EcdcJob for uploading a batch to TESSy
	public EcdcTessyUploadWorker getEcdcTessyUploadWorker(String batchId) {

		// Interactive dialog for selecting batch to reject
		if (batchId==null) {
			batchId = getBatchIdInteractive("TESTED");
			if (batchId==null) {
				return null;
			}
		}

		// Get batch
		TessyBatch batch = batches.get(batchId);
		if (batch==null) {
			JOptionPane.showMessageDialog(this, "No such batch "+batchId+" found!");
			return null;
		}
		String batchSubject = batchSubjects.get(batchId);

		// Create and return job, note that it is not executed automatically
		EcdcTessyUploadWorker worker = new 	EcdcTessyUploadWorker(this,batch,batchSubject,null,null,null);
		return worker;
	}


	// This function uses the current table selection if the arguments are null, otherwise it will use the supplied table and rows
	// It creates an EcdcJob for generating a batch object and testing it in TESSy
	// This is a requirement before upload in this application
	public EcdcTessyCreateAndTestWorker getEcdcTessyTestWorker(String tabname, int[] selected) {
		if (tabname==null) {
			// If not supplied, use active tab
			tabname = tabs.getTitleAt(tabs.getSelectedIndex());
		}
		JTable table = tableHash.get(tabname);

		// If not supplied, use the selected rows.
		boolean interactive=false;
		if (selected==null) {
			interactive = true;
			selected = getUploadRows(table,true);	
		}


		ArrayList<Integer> rows = new ArrayList<Integer>();

		int tessyBatchColumn = getColumn(table,TESSY_BATCH_COLUMN);
		if (tessyBatchColumn==-1) {
			return null;
		}

		// Check which isolates that have been submitted to TESSy before and separate them into different lists 
		// depending on whether they are approved or not. Resubmitting isolates that are already in progress in TESSy will not work.
		ArrayList<Integer> rowsOld = new ArrayList<Integer>();
		ArrayList<Integer> rowsInProgress = new ArrayList<Integer>();
		for (int i : selected) {
			if(table.getModel().getValueAt(i,0)==null || table.getModel().getValueAt(i,0).equals("")) {
				continue;
			}
			if (table.getModel().getValueAt(i,tessyBatchColumn)==null || table.getModel().getValueAt(i,tessyBatchColumn).equals("") || ((String)table.getModel().getValueAt(i,tessyBatchColumn)).endsWith("T") || ((String)table.getModel().getValueAt(i,tessyBatchColumn)).endsWith("R")) {
				rows.add(i);
			} else if (((String)table.getModel().getValueAt(i,tessyBatchColumn)).endsWith("A")) {
				rowsOld.add(i);
			} else {
				rowsInProgress.add(i);
			}
		}


		if (interactive) {

			// For interactive submission, give the user an error that the selection 
			// includes isolates that have been uploaded but not approved nor rejected.
			if (!rowsInProgress.isEmpty()) {
				JOptionPane.showMessageDialog(this,
						"Selection includes isolates that are parts of batches that have been uploaded but not approves, these cannot be included. Try again.");
				return null;
			}

			// If the selection includes isolates that have been approved before, warn the user that they will be replaced.
			if (!rowsOld.isEmpty()) {
				int reply = JOptionPane.showConfirmDialog(null, "Some isolates have already been approved in TESSy, include them anyway?", "Resubmit isolates?", JOptionPane.YES_NO_OPTION);
				if (reply == JOptionPane.YES_OPTION) {
					rows.addAll(rowsOld);
				}
			}
		} else {
			// For non-interactive session, quietly resubmit old approved or rejected isolates but never include those in the uploaded state.
			rows.addAll(rowsOld);
		}

		// If no isolates are included, warn if interactive and then return.
		if (rows.isEmpty()) {
			if (interactive) {
				JOptionPane.showMessageDialog(this, "No isolates selected for submission.");
			}
			return null;
		}

		// Set TESSy credentials and options for the batch
		UploadConfig cfg = configHash.get(tabname);
		TessyCredentials cred = cfg.getTessyCredentials();
		TessyBatch batch = new TessyBatch("",cfg.getTessyCountry(),cfg.getTessyContact(), cfg.getTessyProvider(), cfg.getTessyMeta(), cfg.getTessySubject());
		batch.setTessyCredentials(cred);
		batch.setHaltWarn(cfg.getTessyHaltWarn());
		batch.setHaltRemark(cfg.getTessyHaltRemark());

		// TODO: This is a bit of a hack. Basically the code creates a table with headers, the first four columns are fixed,
		// RecordId, DateUsedForStatistics, EnaId, Assembly file. The rest are any supplied fields in the data table.
		// The code can be improved
		int extrafields = 4;

		String[] headers = new String[table.getColumnCount()-extraFields.size()+extrafields];
		String[][] data = new String[rows.size()][table.getColumnCount()-extraFields.size()+extrafields];

		int enaColumn = getColumn(table,ENA_RUN_COLUMN);
		int assemblyColumn = getColumn(table,ASSEMBLY_FILE_COLUMN);

		headers[0] = "id";
		headers[1] = "DateUsedForStatistics";
		headers[2] = "WgsEnaId";
		headers[3] = "WgsAssembly";
		for (int j = extrafields ;j<data[0].length;j++) {
			headers[j] = table.getColumnName(j-extrafields);
		}

		for (int i = 0;i<rows.size();i++) {
			int row = rows.get(i);
			data[i][0] = Integer.toString(row);				// Row index
			data[i][1] = EcdcUtils.calcDate(table,row,"yyyy-MM-dd");	// DateUsedForStatistics
			data[i][2] = (String)table.getModel().getValueAt(row, enaColumn);	//ENA run ID							
			data[i][3] = (String)table.getModel().getValueAt(row, assemblyColumn); //Assembly

			for (int j = extrafields ;j<data[0].length;j++) {
				data[i][j] = (String)table.getModel().getValueAt(row, j-extrafields);
			}
		}

		// Generate an EcdcJob that will create a batch object and test it,
		EcdcTessyCreateAndTestWorker worker = new EcdcTessyCreateAndTestWorker(this,batch,tabname,cfg,headers,data);
		return worker;
	}


	private int[] getUploadRows(JTable table, boolean interactive) {
		int[] inds = table.getSelectedRows();
		ArrayList<Integer> uploadList = new ArrayList<Integer>();
		ArrayList<Integer> nonUploadList = new ArrayList<Integer>();
		int uploadColumn = getColumn(table,UPLOAD_COLUMN);
		for (int i : inds) {
			int modelInd = table.convertRowIndexToModel(i);
			String uploadVal = (String)table.getModel().getValueAt(modelInd, uploadColumn);
			if (uploadVal!=null && uploadVal.equals("Yes")) {
				uploadList.add(modelInd);
			} else {
				nonUploadList.add(modelInd);
			}
		}
		if (interactive && nonUploadList.size()>0) {
			int reply = JOptionPane.showConfirmDialog(null, "The selection includes "+Integer.toString(nonUploadList.size())+" isolates not flagged as 'Ready for upload', change the flag and upload?", "Submit?", JOptionPane.YES_NO_OPTION);
			if (reply == JOptionPane.YES_OPTION) {
				for (int i : nonUploadList) {
					table.getModel().setValueAt("Yes",i, uploadColumn);
				}
				uploadList.addAll(nonUploadList);
			}
		}
		
		
		int[] out = new int[uploadList.size()];
		for (int i = 0;i<uploadList.size();i++) {
			out[i] = uploadList.get(i);
		}
		return out;
	}

	// This function uses the current table selection if the arguments are null, otherwise it will use the supplied table and rows
	// It creates an EcdcJob for uploading to ENA
	public EnaSubmissionWorker getEnaSubmissionWorker(String tabname, int[] selectedRows) {

		if (tabname==null) {
			// If not supplied, use active tab
			tabname = tabs.getTitleAt(tabs.getSelectedIndex());
		}

		JTable dataTable = tableHash.get(tabname);

		// Init column indices
		int recordIdCol = 0;
		int filesCol = getColumn(dataTable,READ_FILES_COLUMN);
		int enaColumn = getColumn(dataTable,ENA_RUN_COLUMN);
		int wgsProtocolCol = getColumn(dataTable,"WgsProtocol");

		// Metadata is required to have a WgsProtocol column for ENA submission
		if (wgsProtocolCol==-1) {
			JOptionPane.showMessageDialog(this,
					"No WGSPROTOCOL field found in metadata. Cannot submit to ENA without this.");
			return null;
		}

		ArrayList<Integer> rows = new ArrayList<Integer>();

		// If not supplied, use selected rows
		if (selectedRows==null) {
			selectedRows = getUploadRows(dataTable, true);
		}

		// Check that the selected rows have a recordId and raw read files, and that they do not already have an ENA run id.
		for (int i : selectedRows) {
			if (dataTable.getModel().getValueAt(i, recordIdCol)!=null && !dataTable.getModel().getValueAt(i, recordIdCol).equals("") && dataTable.getModel().getValueAt(i, filesCol)!=null && !dataTable.getModel().getValueAt(i, filesCol).equals("") && (dataTable.getModel().getValueAt(i, enaColumn)==null || dataTable.getModel().getValueAt(i, enaColumn).equals(""))) {
				rows.add(i);
			}
		}

		// return if nothing to do
		if (rows.isEmpty()) {
			JOptionPane.showMessageDialog(this,
					"No valid entries selected for submission to ENA.");
			return null;
		}


		// Create a table with the required data for ENA submission
		String[] headers = {"id","RecordId","Year","Files","WgsProtocol"};
		String[][] data = new String[rows.size()][headers.length];
		int c = 0;
		for (int i : rows) {

			// Get year from supplied dates, if no date is supplied, skip.
			String year = EcdcUtils.calcDate(dataTable,i,"yyyy");
			if (year.equals("")) {
				JOptionPane.showMessageDialog(this,
						"No date found for one or more entries, ENA submission halted.");
				return null;
			}

			data[c][0]=Integer.toString(i);
			data[c][1]=(String)dataTable.getModel().getValueAt(i, recordIdCol);
			data[c][2]=year;
			data[c][3]=(String)dataTable.getModel().getValueAt(i, filesCol);
			data[c][4]=(String)dataTable.getModel().getValueAt(i, wgsProtocolCol);
			c++;
		}


		// Create and return, an EcdcJob for submitting to ENA. It does not automatically run.
		EnaSubmissionWorker worker = new EnaSubmissionWorker(this,null,tabname,configHash.get(tabname),headers,data);

		// Set table column for worker to output ENA accession in
		worker.setEnaColumn(enaColumn);
		worker.setOutputHandler(new JTableOutputHandler(dataTable));
		return worker;
	}


	// This function uses the current table selection if the arguments are null, otherwise it will use the supplied table and rows
	// It creates an EcdcJob for uploading to all configured systems
	public EcdcFullUploadWorker getFullUploadWorker(String tabname, int[] selected) {

		// Use selected table if argument is not supplied
		if (tabname==null) {
			tabname = tabs.getTitleAt(tabs.getSelectedIndex());
		}
		JTable table = tableHash.get(tabname);

		// use selected rows if not supplied
		if (selected==null) {
			selected = getUploadRows(table, true);	
		} 

		// Create and return an EcdcJob for submitting to all configured systems. I still needs to be executed
		EcdcFullUploadWorker worker = new EcdcFullUploadWorker(this,null,tabname, configHash.get(tabname),null,null);
		worker.setRows(selected);
		return worker;
	}

	// This function uses the current table selection if the arguments are null, otherwise it will use the supplied table and rows
	// It creates an EcdcJob for uploading to SFTP
	public EcdcSftpUploadWorker getSftpWorker(String tabname, int[] selected) {

		// Use selected table if argument is not supplied
		if (tabname==null) {
			tabname = tabs.getTitleAt(tabs.getSelectedIndex());
		}
		JTable table = tableHash.get(tabname);

		// use selected rows if not supplied
		if (selected==null) {
			selected = getUploadRows(table, true);
		}

		// Create a data table with information needed for SFTP upload 
		int n = selected.length;
		String[] headers = new String[6];
		String[][] data = new String[n][6];

		int recordIdCol = 0;
		int filesCol  = getColumn(table, READ_FILES_COLUMN);
		int assemblyCol  = getColumn(table, ASSEMBLY_FILE_COLUMN);
		int tessyIdCol = getColumn(table, TESSY_ID_COLUMN);
		int protocolCol = getColumn(table, "WgsProtocol");
		int uiCol = getColumn(table, UI_COLUMN);
		int ftpCol = getColumn(table, SFTP_COLUMN);


		int count = 0;
		for (int i : selected) {
			data[count][0] = Integer.toString(i);
			data[count][1] = (String)table.getModel().getValueAt(i, recordIdCol);
			data[count][2] = (String)table.getModel().getValueAt(i, filesCol);

			String assemblyFiles = (String)table.getModel().getValueAt(i, assemblyCol);
			if (assemblyFiles!=null && !assemblyFiles.equals("")) {
				if (data[count][2]==null) {
					data[count][2] = "";
				}
				if (!data[count][2].equals("")) {
					data[count][2] = data[count][2]+";";
				}
				data[count][2] = data[count][2] + assemblyFiles;
			}
			data[count][3] = (String)table.getModel().getValueAt(i, tessyIdCol);
			data[count][4] = (String)table.getModel().getValueAt(i, protocolCol);
			data[count][5] = (String)table.getModel().getValueAt(i, uiCol); 

			count++;
		}

		if (count==0) {
			JOptionPane.showMessageDialog(this,
					"No valid entries selected for submission using SFTP.");
			return null;
		}

		// Create and return a worker for SFTP upload. It still needs to be executed
		EcdcSftpUploadWorker worker = new EcdcSftpUploadWorker(this,null,tabname,configHash.get(tabname),headers,data);

		// Set output handling for this worker
		worker.setFtpColumn(ftpCol);
		worker.setOutputHandler(new JTableOutputHandler(table));

		return worker;
	}


	// Save isolate table and batch data
	void save() {

		// Iterate over all tabs
		int totalTabs = tabs.getTabCount();
		for(int i = 0; i < totalTabs; i++)
		{
			ArrayList<HashMap<String,String>> data = new ArrayList<HashMap<String,String>>();
			String name= tabs.getTitleAt(i);
			System.out.println(name);
			File outFile = new File(name+".dat");
			File outFileBatch = new File(name+".batch");

			// Get the batches associated with this subject
			// TODO: This can be done more efficiently
			HashMap<String,TessyBatch> saveBatches = new HashMap<String,TessyBatch>();
			for (String k : batches.keySet()) {
				if (batchSubjects.get(k).equals(name)) {
					TessyBatch bat = batches.get(k);
					if (!bat.getState().equals("APPROVED")) {
						saveBatches.put(k,bat);
					}
				}
			}

			// Iterate through the data table, add all non-empty rows to the List for saving
			JTable table = tableHash.get(name);
			int rows = table.getRowCount();
			int cols = table.getColumnCount();
			for (int j = 0; j< rows; j++) {
				HashMap<String,String> row=null;
				boolean save = false;

				for (int k = 0; k< cols; k++) {
					String val = (String)table.getModel().getValueAt(j, k);
					if (val!=null) {
						if (!save) {
							save = true;
							row = new HashMap<String,String>();
						}
						row.put((String)table.getColumnName(k), (String)table.getModel().getValueAt(j, k));
					}
				}
				if (save) {
					data.add(row);
				}
			}

			// Write table data to file
			FileOutputStream foutData;
			try {
				foutData = new FileOutputStream(outFile);
				ObjectOutputStream oos = new ObjectOutputStream(foutData);
				oos.writeObject(data);
				oos.close();
			} catch (IOException e1) {
				JOptionPane.showMessageDialog(this,
						"Save failed.");
				e1.printStackTrace();
			}

			// Write batch data to file
			FileOutputStream foutBatch;
			try {
				foutBatch = new FileOutputStream(outFileBatch);
				ObjectOutputStream oosB = new ObjectOutputStream(foutBatch);
				oosB.writeObject(saveBatches);
				oosB.close();
			} catch (IOException e1) {
				JOptionPane.showMessageDialog(this,
						"Saving TESSy batch data failed.");
				e1.printStackTrace();
			}

		}

	}


	// This function uses the current table selection if the arguments are null, otherwise it will use the supplied table and rows
	// it links sequence read files and assembly files using the RecordId and the UploadConfig file pattern
	public EcdcLinkWorker getEcdcLinkWorker(String name, int[] selected) {

		// Get the selected table if not supplied
		if (name==null) {
			name= tabs.getTitleAt(tabs.getSelectedIndex());
		}

		JTable table = tableHash.get(name);

		// A config is required,
		if (!configHash.containsKey(name)) {
			JOptionPane.showMessageDialog(this, "No upload configuration found for subject "+name+", cannot link data.");
			return null;
		}
		UploadConfig cfg = configHash.get(name);


		EcdcLinkWorker linkWorker = new EcdcLinkWorker(this, null, name, cfg, null, null);
		linkWorker.setColumnNames(BASE_FILE_COLUMN, READ_FILES_COLUMN, ASSEMBLY_FILE_COLUMN);
		linkWorker.setTable(table);
		linkWorker.setSelected(selected);
		return linkWorker;

	}


	// This functions handles the TESSy validation results output, puts them into the isolate table. 
	public void setValidationResults(String name, String batchId, String suffix, String dateStr, String appDateStr, boolean setId) {

		HashMap<String,TessyValidationResult> results = batches.get(batchId).getValidationResults();

		batchId = batchId+suffix;

		JTable table = tableHash.get(name);

		// Init column indices
		int tessyValColumn = getColumn(table,TESSY_VALIDATION_COLUMN);
		int tessyTestColumn = getColumn(table,TESSY_TEST_COLUMN);
		int tessyBatchColumn = getColumn(table,TESSY_BATCH_COLUMN);
		int tessyUploadColumn = getColumn(table,TESSY_UPLOADED_COLUMN);
		int tessyApproveColumn = getColumn(table,TESSY_APPROVED_COLUMN);
		int tessyIdColumn = getColumn(table,TESSY_ID_COLUMN);

		// TESSy validation column required
		if (tessyValColumn==-1) {
			return;
		}


		// Clear all old non-uploaded batches, only one test batch can be active at any time
		for (String k : tableHash.keySet()) {
			JTable t = tableHash.get(k);
			for (int i = 0; i<t.getRowCount();i++) {
				t.getModel().setValueAt("", i, tessyTestColumn);
			}
		}



		for (int i = 0;i<table.getRowCount();i++) {
			String id = (String)table.getModel().getValueAt(i, 0);
			if (id==null) {
				continue;
			}

			// Check if each row of the table matches a RecordId in the tested batch
			if (results.containsKey(id)) {
				TessyValidationResult r = results.get(id);
				String val = "";

				for (String e : r.getErrors()) {
					val = val + "E: "+ e+";";
				}
				for (String e : r.getWarnings()) {
					val = val +"W: "+e+";";
				}
				for (String e : r.getRemarks()) {
					val = val +"R: "+ e+";";
				}
				if (val.equals("")) {
					val = "OK";
				}

				// Set the TESSy validation text
				table.getModel().setValueAt(val, i, tessyValColumn);	
				if (batchId!=null) {
					if (setId) {
						table.getModel().setValueAt(batchId, i, tessyBatchColumn);
					} else {
						table.getModel().setValueAt(batchId, i, tessyTestColumn);
					}
				}

				// If an upload date is supplied, set it
				if (dateStr!=null) {
					table.getModel().setValueAt(dateStr, i, tessyUploadColumn);
				}
				// If an approval date is supplied, set it
				if (appDateStr!=null) {
					table.getModel().setValueAt(appDateStr, i, tessyApproveColumn);
				}

				// If the flag setId is set, update the TESSy isolate GUID in the table
				if (setId) {
					String tessyId = r.getGuid();
					if (tessyId!=null && !tessyId.equals("")) {
						table.getModel().setValueAt(tessyId, i, tessyIdColumn);
					}
				}

			}

		}
	}

	// Generate a JComboBox for the isolate table
	private JComboBox<String> generateBox(String[] options)
	{
		JComboBox<String> bx=null;
		bx=new JComboBox<String>();
		bx.addItem("");	
		for (String option: options) {
			bx.addItem(option);	
		}
		bx.setEditable(false);

		return bx;

	}

	// Get the column index for a certain table and column name, returns -1 if not found
	int getColumn(JTable table, String colname) {
		for (int i = 0;i<table.getColumnCount();i++) {
			String n = table.getColumnName(i);
			if (n.equals(colname)) {
				return i;
			}
		}
		return -1;
	}

	// Add a TESSy batch to the HashMaps batches and batchSubjects
	public void setBatch(String subject, TessyBatch batch) {
		String id = batch.getBatchId();
		batches.put(id,batch);
		batchSubjects.put(id,subject);
		if (batch.getState().equals("REJECTED")) {
			batches.remove(id);
			batchSubjects.remove(id);
		}
	}

	// Show an error dialog box.
	public void error(String msg) {
		JOptionPane.showMessageDialog(this,
				msg);
	}

	// Get the Job handler for this GUI
	public EcdcJobHandler getJobHandler() {
		return jobHandler;

	}

	// Get the table for a SUBJECT
	public JTable getTable(String subject) {
		return tableHash.get(subject);
	}

	
	// This methods returns a List of rows in the tableModel to submit, based on Date logic, mainly used for automated uploads
	public ArrayList<Integer> getUploadList(JTable table, UploadConfig cfg) {
		
		
		ArrayList<Integer> inds = new ArrayList<Integer>();
		int uploadColumn = getColumn(table,UPLOAD_COLUMN);
		int readsColumn = getColumn(table,READ_FILES_COLUMN);
		int assemblyColumn = getColumn(table,ASSEMBLY_FILE_COLUMN);
		int modifiedColumn = getColumn(table,MODIFIED_COLUMN);
		int tessyApprovedColumn = getColumn(table,TESSY_APPROVED_COLUMN);
		int sftpColumn = getColumn(table,SFTP_COLUMN);
		int enaColumn = getColumn(table,ENA_RUN_COLUMN);
		
		for (int i = 0;i<table.getModel().getRowCount();i++) {
			String uploadVal = (String)table.getModel().getValueAt(i, uploadColumn); 
			String readsVal = (String)table.getModel().getValueAt(i, readsColumn); 
			String assemblyVal = (String)table.getModel().getValueAt(i, assemblyColumn); 
			String modifiedVal = (String)table.getModel().getValueAt(i, modifiedColumn); 
			String tessyApprovedVal = (String)table.getModel().getValueAt(i, tessyApprovedColumn); 
			String sftpVal = (String)table.getModel().getValueAt(i, sftpColumn);
			String enaVal = (String)table.getModel().getValueAt(i, enaColumn);
			
			// Only upload entries which have been flagged
			if (uploadVal==null || !uploadVal.equals("Yes")) {
				continue;
			}
			// Only upload entries with reads or assemblies
			if ((readsVal==null || readsVal.equals("")) && (assemblyVal==null || assemblyVal.equals(""))) {
				continue;
			}
			// Always submit entries that have not been submitted to TESSy, or has been modified since last upload
			if (tessyApprovedVal==null || tessyApprovedVal.equals("")) {
				inds.add(i);
				continue;
			} else {
				try {
					Date tessyDate = dateFormat.parse(tessyApprovedVal);
					Date modifiedDate = dateFormat.parse(modifiedVal);
					if (modifiedDate.after(tessyDate)) {
						inds.add(i);
						continue;
					}
				} catch (ParseException e) {
					
					e.printStackTrace();
				}
			}
			// If submit to ENA and entry is not submitted to ENA, submit
			if (cfg.isSubmitEna() && (enaVal==null || enaVal.equals(""))) {
				inds.add(i);
				continue;
			}
			// If submit to sftp and entry is not submitted to sftp, or has been modified since last upload, submit
			if (cfg.isSubmitFtp() && (sftpVal==null || sftpVal.equals(""))) {
				inds.add(i);
				continue;
			} else if (cfg.isSubmitFtp()) {
				try {
					Date sftpDate = dateFormat.parse(sftpVal);
					Date modifiedDate = dateFormat.parse(modifiedVal);
					if (modifiedDate.after(sftpDate)) {
						inds.add(i);
						continue;
					}
				} catch (ParseException e) {
					
					e.printStackTrace();
				}
			}

		}
		
		return inds;
	}


}
