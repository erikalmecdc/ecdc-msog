package eu.europa.ecdc.enauploader;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.DefaultCaret;


public class ENAuploaderGUI extends JFrame implements ActionListener {

	private static final int MAXROWS = 5000;
	private static final String version="0.30";
	private static final String BASE_TITLE = "ECDC uploader v"+version+" ";
	private DefaultTableModel dataTableModel;
	private JTable dataTable;
	private JTextField projectText;
	private JTextField centerText;
	private JTextField projaccText;
	private JTextField delimiterText;
	private JTextField datadirText;
	private JTextField loginText;
	private JPasswordField passwdText;
	private JCheckBox anonButton;
	private JCheckBox prodButton;
	private JTextArea logArea;

	private String curlPath;
	private String ftpHost;
	private String tmpPath;
	private JCheckBox ftpButton;
	private JTextField holdText;
	private JTextField checklistText;


	private File saveFile;
	private JTextField tessyLoginText;
	private JPasswordField tessyPassText;
	private JTextField tessyHostText;
	private JTextField tessyDomainText;
	private JTextField tessyTargetText;
	private JTextField tessyContactText;
	private JTextField tessyProviderText;
	private JTextField tessySubjectText;
	private JTextField tessyMetaText;
	private JTextField tessyCountryText;
	private JTextField ftpHostText;
	private JTextField ftpLoginText;
	private JPasswordField ftpPassText;
	private JCheckBox ftpCheckBox;
	private JCheckBox enaCheckBox;
	private JCheckBox tessyCheckBox;
	private JCheckBox tessyWarnCheckBox;
	private JCheckBox tessyRemarkCheckBox;
	private SimpleDateFormat timestamp;
	ENAuploaderGUI gui;

	ENAuploaderGUI() {
		gui = this;
		timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		saveFile = null;
		curlPath = "curl.exe";
		String line;

		try {
			BufferedReader br = new BufferedReader(new FileReader(new File("paths.txt")));
			while ((line = br.readLine())!=null) {
				if (!line.equals("")) {
					String[] fields = line.split("=");
					String key = fields[0];
					String value = fields[1];
					if (key.equals("CURL")) {
						curlPath = value;
					} else if (key.equals("FTP")) {
						ftpHost = value;
					} else if (key.equals("TMP")) {
						tmpPath = value;
					}
				}
			}
			br.close();
		} catch (IOException e) {

			e.printStackTrace();
		}

		JLabel checklistLabel = new JLabel("ENA checklist");
		checklistText = new JTextField("ERC000044");
		checklistText.setPreferredSize(new Dimension(60,20));

		/*try {
			BufferedReader br = new BufferedReader(new FileReader(new File("checklist.txt")));
			while ((line = br.readLine())!=null) {
				if (!line.equals("")) {
					checklistText.setText(line.trim());
				}
			}
			br.close();
		} catch (IOException e) {

			e.printStackTrace();
		}*/



		dataTable = new JTable(dataTableModel);
		dataTable.setCellSelectionEnabled(true);
		dataTable.getTableHeader().setReorderingAllowed(false);
		new ExcelAdapter(dataTable);
		//dataTable.putClientProperty("terminateEditOnFocusLost", true);




		dataTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(
					JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column) {
				Component c = super.getTableCellRendererComponent(
						table, value, isSelected, hasFocus, row, column);
				DefaultTableModel model = (DefaultTableModel) table.getModel();
				c.setBackground(Color.white);
				if (isSelected) {
					c.setBackground(new Color(175, 175, 175));
				} else if (table.getColumnName(column).endsWith("*")) {
					if (table.getColumnName(column).startsWith("SFTP")) {
						c.setBackground(new Color(189, 211, 125));
					} else if (table.getColumnName(column).startsWith("TESSy")) {
						c.setBackground(new Color(167, 182, 41));

						if (table.getColumnName(column).equals("TESSy date last approved*")) {
							try {
								String val1 = (String)table.getValueAt(row, column);
								String val2 = (String)table.getValueAt(row, column-1);
								if (val1!=null && val2!=null && !val1.equals("") && !val2.equals("")) {
									Date d1 = timestamp.parse(val1);
									Date d2 = timestamp.parse(val2);
									if (d2.after(d1)) {
										c.setBackground(new Color(255, 180, 120));
									}
								}
							} catch (ParseException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						} else if (table.getColumnName(column).equals("TESSy date last uploaded*")) {
							try {
								String val1 = (String)table.getValueAt(row, column);
								String val2 = (String)table.getValueAt(row, 1);
								if (val1!=null && val2!=null && !val1.equals("") && !val2.equals("")) {
									Date d1 = timestamp.parse(val1);
									Date d2 = timestamp.parse(val2);
									if (d2.after(d1)) {
										c.setBackground(new Color(255, 180, 120));
									}
								}
							} catch (ParseException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}



					} else {
						c.setBackground(new Color(121, 191, 189));
					}


				} else {
					if (table.getColumnName(column).matches(".*[;].+") || table.getColumnName(column).matches("Instrument.*") || table.getColumnName(column).matches("species.*") || table.getColumnName(column).matches("File base.*")) {
						c.setBackground(new Color(235, 235, 255));
					} else if (column<2) {

					}
				}



				return c;
			}
		});



		//loadMetaData(new File("metadata.txt"));

		dataTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);		
		JScrollPane dataScroller = new JScrollPane(dataTable);
		dataScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS );

		JPanel bottomPanel = new JPanel(new BorderLayout());

		JPanel dataPanel = new JPanel(new BorderLayout());
		dataPanel.add(dataScroller,BorderLayout.CENTER);

		JPanel ftpPanel = new JPanel();
		GridLayout ftpLayout = new GridLayout(0,2);
		ftpPanel.setLayout(ftpLayout);

		JPanel controlPanel = new JPanel();
		GridLayout buttonLayout = new GridLayout(0,2);
		controlPanel.setLayout(buttonLayout);

		JPanel tessyPanel = new JPanel();
		GridLayout tessyLayout = new GridLayout(0,2);
		tessyPanel.setLayout(tessyLayout);

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridLayout(0,1));

		ftpCheckBox = new JCheckBox("Submit to ECDC SFTP");
		ftpCheckBox.setSelected(true);
		JLabel fooLabel1 = new JLabel("");
		ftpPanel.add(ftpCheckBox);
		ftpPanel.add(fooLabel1);

		enaCheckBox = new JCheckBox("Submit to ENA");
		enaCheckBox.setSelected(true);
		JLabel fooLabel2 = new JLabel("");
		controlPanel.add(enaCheckBox);
		controlPanel.add(fooLabel2);

		tessyCheckBox = new JCheckBox("Submit to TESSy");
		tessyCheckBox.setSelected(true);
		JLabel fooLabel3 = new JLabel("");
		tessyPanel.add(tessyCheckBox);
		tessyPanel.add(fooLabel3);

		JLabel ftpHostLabel = new JLabel("FTP host");
		ftpHostText = new JTextField("sftp.ecdc.europa.eu");
		ftpHostText.setPreferredSize(new Dimension(190,22));

		JLabel ftpLoginLabel = new JLabel("FTP login");
		ftpLoginText = new JTextField("Moltype");
		ftpLoginText.setPreferredSize(new Dimension(190,22));

		JLabel ftpPassLabel = new JLabel("FTP login");
		ftpPassText = new JPasswordField("");
		ftpPassText.setPreferredSize(new Dimension(190,22));

		ftpPanel.add(ftpHostLabel);
		ftpPanel.add(ftpHostText);

		ftpPanel.add(ftpLoginLabel);
		ftpPanel.add(ftpLoginText);

		ftpPanel.add(ftpPassLabel);
		ftpPanel.add(ftpPassText);

		JLabel centerLabel = new JLabel("Submitting institution");
		centerText = new JTextField("");
		centerText.setPreferredSize(new Dimension(80,20));

		controlPanel.add(centerLabel);
		controlPanel.add(centerText);

		JLabel projectLabel = new JLabel("Study alias");
		projectText = new JTextField("");
		projectText.setPreferredSize(new Dimension(60,20));

		controlPanel.add(projectLabel);
		controlPanel.add(projectText);

		JLabel holdLabel = new JLabel("Release date (empty for immediate)");
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		Calendar c = Calendar.getInstance();
		c.setTime(new Date());
		c.add(Calendar.DATE, 30);
		String dateStr = format.format(c.getTime());
		holdText = new JTextField(dateStr);
		holdText.setPreferredSize(new Dimension(60,20));

		controlPanel.add(holdLabel);
		controlPanel.add(holdText);

		JLabel projaccLabel = new JLabel("Study accession");
		projaccText = new JTextField("");
		projaccText.setPreferredSize(new Dimension(60,20));

		controlPanel.add(projaccLabel);
		controlPanel.add(projaccText);

		JLabel createProjectLabel = new JLabel("");
		JButton createProjectButton = new JButton("Create and submit study to ENA");
		createProjectButton.setActionCommand("newProject");
		createProjectButton.addActionListener(this);



		JLabel releaseProjectLabel = new JLabel("");
		JButton releaseProjectButton = new JButton("Release ENA study");
		releaseProjectButton.setActionCommand("releaseProject");
		releaseProjectButton.addActionListener(this);




		JLabel delimiterLabel = new JLabel("File delimiter");
		delimiterText = new JTextField("_");
		delimiterText.setPreferredSize(new Dimension(60,20));

		controlPanel.add(delimiterLabel);
		controlPanel.add(delimiterText);

		JLabel datadirLabel = new JLabel("Data directory");
		datadirText = new JTextField("");
		datadirText.setPreferredSize(new Dimension(60,20));

		controlPanel.add(datadirLabel);
		controlPanel.add(datadirText);

		JLabel choosedirLabel = new JLabel("");
		JButton choosedirButton = new JButton("Choose data directory");
		choosedirButton.setActionCommand("chooseDir");
		choosedirButton.addActionListener(this);

		controlPanel.add(choosedirLabel);
		controlPanel.add(choosedirButton);

		JLabel submitLabel = new JLabel("");
		JButton submitButton = new JButton("Submit isolates to selected systems");
		submitButton.setActionCommand("submitIsolates");
		submitButton.addActionListener(this);




		buttonPanel.add(submitButton);






	
	



		

		
		JLabel loginLabel = new JLabel("Webin account");
		loginText = new JTextField("");
		loginText.setPreferredSize(new Dimension(60,20));

		controlPanel.add(loginLabel);
		controlPanel.add(loginText);

		JLabel passwdLabel = new JLabel("Webin password");
		passwdText = new JPasswordField("");
		passwdText.setPreferredSize(new Dimension(60,20));

		controlPanel.add(passwdLabel);
		controlPanel.add(passwdText);


		JLabel anonLabel = new JLabel("");
		anonButton = new JCheckBox ("Anonymize sample IDs");
		anonButton.setSelected(true);

		controlPanel.add(anonLabel);
		controlPanel.add(anonButton);

		JLabel prodLabel = new JLabel("");
		prodButton = new JCheckBox ("Use ENA production API");

		controlPanel.add(prodLabel);
		controlPanel.add(prodButton);

		JLabel ftpLabel = new JLabel("");
		ftpButton = new JCheckBox ("Files already exist on ENA FTP");

		controlPanel.add(ftpLabel);
		controlPanel.add(ftpButton);


		controlPanel.add(checklistLabel);
		controlPanel.add(checklistText);


		controlPanel.add(createProjectLabel);
		controlPanel.add(createProjectButton);
		controlPanel.add(releaseProjectLabel);
		controlPanel.add(releaseProjectButton);

		JLabel tessyHostLabel = new JLabel("TESSy URL");
		tessyHostText = new JTextField("tessy.ecdc.europa.eu");
		tessyHostText.setPreferredSize(new Dimension(120,20));
		tessyPanel.add(tessyHostLabel);
		tessyPanel.add(tessyHostText);

		JLabel tessyDomainLabel = new JLabel("TESSy DOMAIN");
		tessyDomainText = new JTextField("ecdcdmz");
		tessyDomainText.setPreferredSize(new Dimension(120,20));
		tessyPanel.add(tessyDomainLabel);
		tessyPanel.add(tessyDomainText);

		JLabel tessyTargetLabel = new JLabel("TESSy TARGET");
		tessyTargetText = new JTextField("/TessyWebService/TessyUpload.asmx");
		tessyTargetText.setPreferredSize(new Dimension(120,20));
		tessyPanel.add(tessyTargetLabel);
		tessyPanel.add(tessyTargetText);


		JLabel tessyLoginLabel = new JLabel("TESSy account");
		tessyLoginText = new JTextField("");
		tessyLoginText.setPreferredSize(new Dimension(120,20));
		tessyPanel.add(tessyLoginLabel);
		tessyPanel.add(tessyLoginText);

		JLabel tessyPassLabel = new JLabel("TESSy password");
		tessyPassText = new JPasswordField("");
		tessyPassText.setPreferredSize(new Dimension(120,20));
		tessyPanel.add(tessyPassLabel);
		tessyPanel.add(tessyPassText);

		JLabel tessyCountryLabel = new JLabel("TESSy country");
		tessyCountryText = new JTextField("");
		tessyCountryText.setPreferredSize(new Dimension(120,20));
		tessyPanel.add(tessyCountryLabel);
		tessyPanel.add(tessyCountryText);

		JLabel tessyContactLabel = new JLabel("TESSy contact");
		tessyContactText = new JTextField("");
		tessyContactText.setPreferredSize(new Dimension(120,20));
		tessyPanel.add(tessyContactLabel);
		tessyPanel.add(tessyContactText);

		JLabel tessyProviderLabel = new JLabel("TESSy data source (e.g. XX-MOLSURV)");
		tessyProviderText = new JTextField("");
		tessyProviderText.setPreferredSize(new Dimension(120,20));
		tessyPanel.add(tessyProviderLabel);
		tessyPanel.add(tessyProviderText);

		JLabel tessySubjectLabel = new JLabel("TESSy SUBJECT (e.g. SALMISO)");
		tessySubjectText = new JTextField("SALMISO");
		tessySubjectText.setPreferredSize(new Dimension(120,20));
		tessyPanel.add(tessySubjectLabel);
		tessyPanel.add(tessySubjectText);

		JLabel tessyMetaLabel = new JLabel("TESSy metadata version");
		tessyMetaText = new JTextField("3");
		tessyMetaText.setPreferredSize(new Dimension(120,20));
		tessyPanel.add(tessyMetaLabel);
		tessyPanel.add(tessyMetaText);


		JLabel tessyWarnLabel = new JLabel("");
		tessyWarnCheckBox = new JCheckBox("Halt submission on warning");
		tessyPanel.add(tessyWarnLabel);
		tessyPanel.add(tessyWarnCheckBox);

		JLabel tessyRemarkLabel = new JLabel("");
		tessyRemarkCheckBox = new JCheckBox("Halt submission on remark");
		tessyPanel.add(tessyRemarkLabel);
		tessyPanel.add(tessyRemarkCheckBox);


		logArea = new JTextArea("");
		DefaultCaret caret = (DefaultCaret)logArea.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

		JScrollPane logScroller = new JScrollPane(logArea);
		logScroller.setPreferredSize(new Dimension(1000,200));
		bottomPanel.add(logScroller);

		JPanel rightPanel = new JPanel();
		rightPanel.setPreferredSize(new Dimension(440,1200));
		rightPanel.add(ftpPanel);
		rightPanel.add(controlPanel);
		rightPanel.add(tessyPanel);
		rightPanel.add(buttonPanel);

		JScrollPane rightScroller = new JScrollPane(rightPanel);


		JPanel mainPanel = new JPanel(new BorderLayout());

		ftpPanel.setBorder(BorderFactory.createTitledBorder("ECDC FTP parameters"));
		controlPanel.setBorder(BorderFactory.createTitledBorder("ENA parameters"));
		tessyPanel.setBorder(BorderFactory.createTitledBorder("TESSy parameters"));
		buttonPanel.setBorder(BorderFactory.createTitledBorder("Controls"));
		dataPanel.setBorder(BorderFactory.createTitledBorder("Isolate table"));


		createMenuBar();


		mainPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));



		mainPanel.add(dataPanel,BorderLayout.CENTER);
		mainPanel.add(rightScroller,BorderLayout.EAST);


		mainPanel.add(bottomPanel,BorderLayout.SOUTH);

		setTitle(BASE_TITLE);
		this.getContentPane().add(mainPanel);
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.setSize(new Dimension(1200,800));

		File stateFile = new File("lastopen.txt");
		if (stateFile.exists()) {
			try {
				BufferedReader br = new BufferedReader(new FileReader(stateFile));

				loadState(new File(br.readLine()));
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	

		}



		setVisible(true);

	}


	private void createMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu("File");

		JMenuItem newMetaMenuItem = new JMenuItem("New metadataset");
		newMetaMenuItem.addActionListener(this);
		newMetaMenuItem.setActionCommand("NewMeta");
		fileMenu.add(newMetaMenuItem);

		fileMenu.addSeparator();

		JMenuItem newMenuItem = new JMenuItem("New dataset");
		newMenuItem.addActionListener(this);
		newMenuItem.setActionCommand("New");
		fileMenu.add(newMenuItem);
		KeyStroke ctrlN = KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit ().getMenuShortcutKeyMask());
		newMenuItem.setAccelerator(ctrlN);



		JMenuItem openMenuItem = new JMenuItem("Open");
		openMenuItem.addActionListener(this);
		openMenuItem.setActionCommand("Open");
		fileMenu.add(openMenuItem);
		KeyStroke ctrlO = KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit ().getMenuShortcutKeyMask());
		openMenuItem.setAccelerator(ctrlO);

		JMenuItem saveMenuItem = new JMenuItem("Save");
		saveMenuItem.addActionListener(this);
		saveMenuItem.setActionCommand("Save");
		fileMenu.add(saveMenuItem);
		KeyStroke ctrlS = KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit ().getMenuShortcutKeyMask());
		saveMenuItem.setAccelerator(ctrlS);

		JMenuItem saveAsMenuItem = new JMenuItem("Save as...");
		saveAsMenuItem.addActionListener(this);
		saveAsMenuItem.setActionCommand("SaveAs");
		fileMenu.add(saveAsMenuItem);

		fileMenu.addSeparator();


		JMenuItem quickSaveMenuItem = new JMenuItem("Quick save");
		quickSaveMenuItem.addActionListener(this);
		quickSaveMenuItem.setActionCommand("qSave");
		fileMenu.add(quickSaveMenuItem);

		JMenuItem quickLoadMenuItem = new JMenuItem("Quick load");
		quickLoadMenuItem.addActionListener(this);
		quickLoadMenuItem.setActionCommand("qLoad");
		fileMenu.add(quickLoadMenuItem);

		fileMenu.addSeparator();
		JMenuItem instrumentsMenuItem = new JMenuItem("Show allowed instruments");
		instrumentsMenuItem.addActionListener(this);
		instrumentsMenuItem.setActionCommand("Instruments");
		fileMenu.add(instrumentsMenuItem);

		JMenuItem taxMenuItem = new JMenuItem("Show allowed species");
		taxMenuItem.addActionListener(this);
		taxMenuItem.setActionCommand("Taxids");
		fileMenu.add(taxMenuItem);

		menuBar.add(fileMenu);
		this.setJMenuBar(menuBar);

	}


	public static void main(String[] args) {
		try {
			// Set System L&F
			UIManager.setLookAndFeel(
					UIManager.getSystemLookAndFeelClassName());
		} 
		catch (UnsupportedLookAndFeelException e) {
			// handle exception
		}
		catch (ClassNotFoundException e) {
			// handle exception
		}
		catch (InstantiationException e) {
			// handle exception
		}
		catch (IllegalAccessException e) {
			// handle exception
		}

		new ENAuploaderGUI();
	}


	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("newProject")) {
			submitProject();
		} else if (e.getActionCommand().equals("releaseProject")) {
			releaseProject();
		} else if (e.getActionCommand().equals("submitIsolates")) {
			submitIsolates(ftpCheckBox.isSelected(),enaCheckBox.isSelected(),tessyCheckBox.isSelected());
		} else if (e.getActionCommand().equals("chooseDir")) {
			chooseDir();
		} else if (e.getActionCommand().equals("qSave")) {
			saveState(new File("qsave.txt"));
		} else if (e.getActionCommand().equals("qLoad")) {
			loadState(new File("qsave.txt"));
		} else if (e.getActionCommand().equals("Instruments")) {
			JFrame frame = new JFrame("Allowed instruments");
			JPanel panel = new JPanel(new BorderLayout());
			JTextArea area = new JTextArea("");
			area.setEditable(false);
			panel.add(area,BorderLayout.CENTER);
			frame.getContentPane().add(panel);
			frame.setSize(400,600);
			frame.setVisible(true);
			area.setText("HiSeq X Five\nHiSeq X Ten\nIllumina Genome Analyzer\nIllumina Genome Analyzer II\nIllumina Genome Analyzer IIx\nIllumina HiScanSQ\nIllumina HiSeq 1000\nIllumina HiSeq 1500\nIllumina HiSeq 2000\nIllumina HiSeq 2500\nIllumina HiSeq 3000\nIllumina HiSeq 4000\nIllumina MiSeq\nIllumina MiniSeq\nIllumina NovaSeq 6000\nNextSeq 500\nNextSeq 550\n");
			area.append("\nIon Torrent PGM\nIon Torrent Proton\nIon Torrent S5\nIon Torrent S5 XL\n");
			area.append("\nPacBio RS\nPacBio RS II\nSequel");


		}else if (e.getActionCommand().equals("Taxids")) {
			JFrame frame = new JFrame("Allowed species");
			JPanel panel = new JPanel(new BorderLayout());
			JTextArea area = new JTextArea("");
			area.setEditable(false);
			JScrollPane scroll = new JScrollPane(area);
			panel.add(scroll,BorderLayout.CENTER);
			frame.getContentPane().add(panel);
			frame.setSize(400,600);
			frame.setVisible(true);
			try {
				String line;
				BufferedReader br = new BufferedReader(new FileReader("taxids.tsv"));

				while((line = br.readLine())!=null) {
					area.append(line+"\n");
				}

				br.close();

			} catch (IOException e2) {
				e2.printStackTrace();
			}


		}  else if (e.getActionCommand().equals("Save")) {
			save();
		} else if (e.getActionCommand().equals("SaveAs")) {
			saveAs();

		} else if (e.getActionCommand().equals("Open")) {
			JFileChooser jfc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());

			int returnValue = jfc.showOpenDialog(null);
			//int returnValue = jfc.showSaveDialog(null);

			if (returnValue == JFileChooser.APPROVE_OPTION) {
				File selectedFile = jfc.getSelectedFile();
				loadState(selectedFile);
				saveFile = selectedFile;

			}
		} else if (e.getActionCommand().equals("New")) {
			JComboBox<String> metaBox = new JComboBox<String>();
			File[] files = (new File(".")).listFiles();
			for (File f : files) {
				if (f.getName().startsWith("ERC")||f.getName().startsWith("meta")||f.getName().endsWith("ISO.txt")) {
					metaBox.addItem(f.getName());
				}
			}
			JTextActionArea textArea = new JTextActionArea("",false);
			textArea.setPreferredSize(new Dimension(140,400));
			metaBox.addActionListener(textArea);
			textArea.actionPerformed(new ActionEvent(metaBox, 0, ""));

			final JComponent[] inputs = new JComponent[] {
					new JLabel("Select metadataset"),
					metaBox,textArea

			};
			int result = JOptionPane.showConfirmDialog(null, inputs, "Choose metadata set", JOptionPane.PLAIN_MESSAGE);
			if (result == JOptionPane.OK_OPTION) {
				String val = metaBox.getSelectedItem().toString();
				loadMetaData(new File(val));
				if (val.length()>=13) {
					checklistText.setText(val.substring(0,9));
				}

				setTitle(BASE_TITLE + " - "+ "untitled");
			} else {

			}

		} else if (e.getActionCommand().equals("NewMeta")) {
			JTextField metaField = new JTextField("");
			JComboBox<String> metaBox = new JComboBox<String>();
			File[] files = (new File(".")).listFiles();
			for (File f : files) {
				if (f.getName().startsWith("ERC")||f.getName().startsWith("meta")) {
					metaBox.addItem(f.getName());
				}
			}
			JTextActionArea textArea = new JTextActionArea("",true);
			textArea.setPreferredSize(new Dimension(140,400));
			metaBox.addActionListener(textArea);
			textArea.actionPerformed(new ActionEvent(metaBox, 0, ""));

			final JComponent[] inputs = new JComponent[] {
					new JLabel("Name of new metadataset"),
					metaField,
					new JLabel("Existing metadatasets"),
					metaBox,
					new JLabel("Metadata fields in new set"),
					textArea

			};
			int result = JOptionPane.showConfirmDialog(null, inputs, "Create metadata set", JOptionPane.PLAIN_MESSAGE);
			if (result == JOptionPane.OK_OPTION) {
				String val = metaField.getText();
				String metastr = textArea.getText();
				String[] fields = metastr.split("\n");
				try {
					BufferedWriter bw = new BufferedWriter(new FileWriter(new File(val)));
					for (String l:fields) {
						bw.write(l+"\n");
					}
					bw.close();
				} catch (IOException e2) {
					e2.printStackTrace();
				}
			} else {

			}

		}

	}


	public void save() {
		if (saveFile==null) {
			saveAs();
			return;
		}
		saveState(saveFile);

	}

	private void saveAs() {
		JFileChooser jfc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());

		int returnValue = jfc.showSaveDialog(null);

		if (returnValue == JFileChooser.APPROVE_OPTION) {
			File selectedFile = jfc.getSelectedFile();
			saveState(selectedFile);
			saveFile = selectedFile;
			setTitle(BASE_TITLE + " - "+ saveFile.toString());
		}

	}


	private void chooseDir() {
		JFileChooser chooser = new JFileChooser(); 
		chooser.setCurrentDirectory(new java.io.File("."));
		chooser.setDialogTitle("Choose directory for fastq files");
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setAcceptAllFileFilterUsed(false);  
		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) { 
			datadirText.setText(chooser.getSelectedFile().toString());
		}
		else {
			System.out.println("No Selection ");
		}

	}


	private void submitIsolates(boolean ftp, boolean ena, boolean tessy) {

		String sftpHost= ftpHostText.getText();
		String sftpLogin = ftpLoginText.getText();
		char[] ftpPasswdchars = ftpPassText.getPassword();
		String sftpPass = new String(ftpPasswdchars);

		String tessyLogin = tessyLoginText.getText();
		String tessyUrl = tessyHostText.getText();
		String tessyDomain = tessyDomainText.getText();
		String tessyTarget = tessyTargetText.getText();
		String tessyContact = tessyContactText.getText();
		String tessyProvider = tessyProviderText.getText();
		String tessySubject = tessySubjectText.getText();
		String tessyMeta = tessyMetaText.getText();
		String tessyCountry = tessyCountryText.getText();

		char[] tessyPasswdchars = tessyPassText.getPassword();
		String tessyPass = new String(tessyPasswdchars);


		TessyCredentials cred = null;
		TessyBatch batch = null;
		if (tessy) {
			if (tessyPass.equals("") || tessyLogin.equals("") || tessyUrl.equals("") || tessyDomain.equals("") || tessyTarget.equals("")) {
				JOptionPane.showMessageDialog(this, "You must enter TESSy login information to submit to TESSy");
				return;
			} else {

				cred = new TessyCredentials();
				cred.setUsername(tessyLogin);
				cred.setPassword(tessyPasswdchars);
				cred.setDomain(tessyDomain);
				cred.setHostname(tessyUrl);
				cred.setTarget(tessyTarget);
			}
			boolean haltRemark = tessyRemarkCheckBox.isSelected();
			boolean haltWarn = tessyWarnCheckBox.isSelected();
			
			
			
			
			batch = new TessyBatch("",tessyCountry,tessyContact, tessyProvider, tessyMeta, tessySubject);
			batch.setTessyCredentials(cred);
			batch.setHaltWarn(haltWarn);
			batch.setHaltRemark(haltRemark);

		}

		String projectId = projaccText.getText();
		String center = centerText.getText();
		File dataDir = new File(datadirText.getText());

		boolean anonymize = anonButton.isSelected();
		boolean prod = prodButton.isSelected();
		boolean ftpExist = ftpButton.isSelected();



		String delimiter = delimiterText.getText();
		String login = loginText.getText();

		String enaChecklist = checklistText.getText();

		char[] passwdchars = passwdText.getPassword();
		String pass = new String(passwdchars);

		if (ena) {
			if (center.equals("")) {
				JOptionPane.showMessageDialog(this, "You must enter an institution");
				return;
			}
			if (projectId.equals("")) {
				JOptionPane.showMessageDialog(this, "You must enter an existing study acc or create a study acc by submitting a study first.");
				return;
			}
			if (!dataDir.exists()) {
				if (!ftpExist) {
					JOptionPane.showMessageDialog(this, "You must choose a valid data directory unless files are already on FTP.");
					return;
				} else {

				}
			}
		}

		if (ftp) {
			if (sftpHost.equals("")) {
				JOptionPane.showMessageDialog(this, "You must enter an SFTP host for SFTP upload.");
				return;
			}
			if (sftpLogin.equals("")) {
				JOptionPane.showMessageDialog(this, "You must enter an SFTP login for SFTP upload.");
				return;
			}
			if (sftpPass.equals("")) {
				JOptionPane.showMessageDialog(this, "You must enter an SFTP password for SFTP upload.");
				return;
			}
		}

		int rows = 0;
		for (int i = 0;i<dataTable.getRowCount();i++) {
			String val = (String)dataTable.getValueAt(i, 0);
			if (val==null || val.equals("") || val.equals("null")) {
				break;
			}
			rows++;
		}

		String[] header = new String[dataTable.getColumnCount()];
		for (int j = 0;j<dataTable.getColumnCount();j++) {
			header[j] = dataTable.getColumnName(j);
		}

		String[][] data = new String[rows][dataTable.getColumnCount()];
		for (int i = 0;i<rows;i++) {
			for (int j = 0;j<dataTable.getColumnCount();j++) {
				String val = (String)dataTable.getValueAt(i, j);
				if (val==null || val.equals("null")) {
					val = "";
				}
				data[i][j] = val;

			}
		}



		JTableOutputHandler outHandler = new JTableOutputHandler(dataTable);
		SubmissionWorker worker = new SubmissionWorker(center, projectId, data, header, dataDir,login,pass, prod, anonymize, ftpExist, delimiter, enaChecklist, logArea, tmpPath, curlPath, ftpHost, outHandler, batch,sftpHost,sftpLogin, sftpPass, ftp, ena, tessy, this);

		try {
			worker.execute();
			//worker.doInBackground();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}

	public void releaseProject() {
		String center = centerText.getText();
		String acc = projaccText.getText();
		String login = loginText.getText();
		boolean prod = prodButton.isSelected();
		char[] passwdchars = passwdText.getPassword();
		String pass = new String(passwdchars);
		if (acc.equals("")) {
			JOptionPane.showMessageDialog(this, "You must enter an accession number for project to release");
			return;
		}
		if (center.equals("")) {
			JOptionPane.showMessageDialog(this, "You must enter an institution");
			return;
		}
		String randomUUIDString = UUID.randomUUID().toString();
		Submission s = new Submission(center, randomUUIDString);

		s.setCurlPath(curlPath);
		s.setTmpPath(tmpPath);
		if (prod) {
			s.useProductionServer(true);
		}

		Project p = new Project("","",s);
		p.setAccession(acc);
		p.setReleaseAction(true);
		s.setlogArea(logArea);
		s.setLogin(login,pass);
		s.addEntry(p);
		s.uploadFiles();
		s.submit();
	}


	public void submitProject() {
		logArea.append("Study submission started...\n");
		String alias = projectText.getText();
		String center = centerText.getText();
		String login = loginText.getText();
		String hold = holdText.getText();
		boolean prod = prodButton.isSelected();

		char[] passwdchars = passwdText.getPassword();
		String pass = new String(passwdchars);



		if (center.equals("")) {
			JOptionPane.showMessageDialog(this, "You must enter an institution");
			return;
		}
		if (alias.equals("")) {
			JOptionPane.showMessageDialog(this, "You must enter a study alias");
			return;
		}
		String randomUUIDString = UUID.randomUUID().toString();
		Submission s = new Submission(center, randomUUIDString);

		s.setCurlPath(curlPath);
		s.setTmpPath(tmpPath);
		if (prod) {
			s.useProductionServer(true);
		}

		Project p = new Project(center,alias,s);
		p.setReleaseDate(hold);

		s.setlogArea(logArea);
		s.setLogin(login,pass);
		s.addEntry(p);
		s.uploadFiles();
		s.submit();
		String acc = p.getAccession(); 
		if (acc.equals("")) {
			logArea.append("Study submission failed.\n");
			JOptionPane.showMessageDialog(this, "Study submission failed.");
			return;
		}
		logArea.append("Study submission complete. Aquired accession: " + acc +"\n");
		projaccText.setText(acc);
	}

	public void loadState(File f) {
		String line;
		int row = 0;
		try {
			BufferedReader br = new BufferedReader(new FileReader(f));
			while ((line = br.readLine())!=null) {
				if (!line.equals("")) {
					String[] fields = line.split("=",-1);
					String key = fields[0];
					String value = fields[1];
					if (key.equals("TESSYCOUNTRY")) {
						tessyCountryText.setText(value);
					} else if (key.equals("TESSYLOGIN")) {
						tessyLoginText.setText(value);
					} else if (key.equals("TESSYURL")) {
						tessyHostText.setText(value);
					} else if (key.equals("TESSYDOMAIN")) {
						tessyDomainText.setText(value);
					} else if (key.equals("TESSYTARGET")) {
						tessyTargetText.setText(value);
					} else if (key.equals("TESSYCONTACT")) {
						tessyContactText.setText(value);
					} else if (key.equals("TESSYPROVIDER")) {
						tessyProviderText.setText(value);
					} else if (key.equals("TESSYSUBJECT")) {
						tessySubjectText.setText(value);
					} else if (key.equals("TESSYMETA")) {
						tessyMetaText.setText(value);
					} else if (key.equals("PROJECT_ALIAS")) {
						projectText.setText(value);
					} else if (key.equals("PROJECT_ACC")) {
						projaccText.setText(value);
					} else if (key.equals("CENTRE")) {
						centerText.setText(value);
					} else if (key.equals("DATA_DIR")) {
						datadirText.setText(value);
					} else if (key.equals("DELIMITER")) {
						delimiterText.setText(value);
					} else if (key.equals("CHECKLIST")) {
						checklistText.setText(value);
					} else if (key.equals("LOGIN")) {
						loginText.setText(value);
					} else if (key.equals("FTPLOGIN")) {
						ftpLoginText.setText(value);
					} else if (key.equals("FTPHOST")) {
						ftpHostText.setText(value);
					} else if (key.equals("ANONYMIZE")) {
						anonButton.setSelected(Boolean.parseBoolean(value));
					} else if (key.equals("PRODUCTION")) {
						prodButton.setSelected(Boolean.parseBoolean(value));
					} else if (key.equals("USEFTP")) {
						ftpCheckBox.setSelected(Boolean.parseBoolean(value));
					} else if (key.equals("USEENA")) {
						enaCheckBox.setSelected(Boolean.parseBoolean(value));
					} else if (key.equals("USETESSY")) {
						tessyCheckBox.setSelected(Boolean.parseBoolean(value));
					} else if (key.equals("FTP_EXISTS")) {
						ftpButton.setSelected(Boolean.parseBoolean(value));
					}  else if (key.equals("HALTWARN")) {
						tessyWarnCheckBox.setSelected(Boolean.parseBoolean(value));
					} else if (key.equals("HALTREMARK")) {
						tessyRemarkCheckBox.setSelected(Boolean.parseBoolean(value));
					} else if (key.equals("METADATA")) {

						String[] header = value.split(",",-1);
						if (header.length==1) {
							return;
						}
						String[][] data = new String[MAXROWS][header.length];
						dataTableModel = new DefaultTableModel(data,header);

						dataTable.setModel(dataTableModel);
					} else if (key.equals("DATAROW")) {
						String[] csvFields = value.split(",",-1);
						for (int i = 0;i<csvFields.length;i++) {
							String cellValue = csvFields[i];
							if (cellValue.equals("null")) {
								cellValue = "";
							}


							dataTable.setValueAt(cellValue, row, i);
						}
						row++;
					}
				}
			}
			br.close();



			addTableListener();

			setTitle(BASE_TITLE + " - "+ f.toString());
			saveFile = f;
		} catch (IOException e) {

			e.printStackTrace();
		}

	}

	private void addTableListener() {
		dataTableModel.addTableModelListener(new TableModelListener() {

			@Override
			public void tableChanged(TableModelEvent e) {
				int col = e.getColumn();
				if (col==1 || col > dataTable.getColumnCount()-7) {
					return;
				}
				
				int row1 = e.getFirstRow();
				int row2 = e.getLastRow();
				for (int i = row1;i<=row2;i++) {
					if (dataTableModel.getValueAt(i,0)!=null) {
						dataTableModel.setValueAt(timestamp.format(new Date()), i, 1);
					}
				}

				if (saveFile!=null) {
					save();
				}
			}


		});


	}


	public void saveState(File f) {




		String ftpHost = ftpHostText.getText();
		String ftpLogin = ftpLoginText.getText();

		boolean useFtp = ftpCheckBox.isSelected();
		boolean useEna = enaCheckBox.isSelected();
		boolean useTessy = tessyCheckBox.isSelected();

		boolean haltWarn = tessyWarnCheckBox.isSelected();
		boolean haltRemark = tessyRemarkCheckBox.isSelected();

		String tessyLogin = tessyLoginText.getText();
		String tessyUrl = tessyHostText.getText();
		String tessyDomain = tessyDomainText.getText();
		String tessyTarget = tessyTargetText.getText();
		String tessyContact = tessyContactText.getText();
		String tessyProvider = tessyProviderText.getText();
		String tessySubject = tessySubjectText.getText();
		String tessyMeta = tessyMetaText.getText();
		
		String tessyCountry = tessyCountryText.getText();


		String projectId = projaccText.getText();
		String center = centerText.getText();
		String alias = projectText.getText();
		String dataDir = datadirText.getText();

		boolean anonymize = anonButton.isSelected();
		boolean prod = prodButton.isSelected();
		boolean ftpExist = ftpButton.isSelected();



		String enaChecklist = checklistText.getText();

		String delimiter = delimiterText.getText();
		String login = loginText.getText();
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(f));
			bw.write("PROJECT_ALIAS="+alias+"\n");
			bw.write("PROJECT_ACC="+projectId+"\n");
			bw.write("CENTRE="+center+"\n");
			bw.write("DATA_DIR="+dataDir+"\n");
			bw.write("ANONYMIZE="+Boolean.toString(anonymize)+"\n");
			bw.write("PRODUCTION="+Boolean.toString(prod)+"\n");
			bw.write("FTP_EXISTS="+Boolean.toString(ftpExist)+"\n");
			bw.write("DELIMITER="+delimiter+"\n");
			bw.write("LOGIN="+login+"\n");
			bw.write("CHECKLIST="+enaChecklist+"\n");
			bw.write("TESSYLOGIN="+tessyLogin+"\n");
			bw.write("TESSYURL="+tessyUrl+"\n");
			bw.write("TESSYDOMAIN="+tessyDomain+"\n");
			bw.write("TESSYTARGET="+tessyTarget+"\n");
			bw.write("TESSYCONTACT="+tessyContact+"\n");
			bw.write("TESSYPROVIDER="+tessyProvider+"\n");
			bw.write("TESSYSUBJECT="+tessySubject+"\n");
			bw.write("TESSYMETA="+tessyMeta+"\n");
			bw.write("TESSYCOUNTRY="+tessyCountry+"\n");
			bw.write("FTPHOST="+ftpHost+"\n");
			bw.write("FTPLOGIN="+ftpLogin+"\n");
			bw.write("USEFTP="+Boolean.toString(useFtp)+"\n");
			bw.write("USEENA="+Boolean.toString(useEna)+"\n");
			bw.write("USETESSY="+Boolean.toString(useTessy)+"\n");
			bw.write("HALTWARN="+Boolean.toString(haltWarn)+"\n");
			bw.write("HALTREMARK="+Boolean.toString(haltRemark)+"\n");

			String headRow = "";
			for (int j = 0;j<dataTable.getColumnCount();j++) {
				if (!headRow.equals("")) {
					headRow = headRow + ",";
				}
				headRow = headRow + dataTable.getColumnName(j);
			}
			bw.write("METADATA="+headRow+"\n");

			for (int i = 0;i<dataTable.getRowCount();i++) {
				String id = (String)dataTable.getValueAt(i,0);
				if (id== null || id.equals("")) {
					break;
				}
				String csvRow = id;
				for (int j = 1;j<dataTable.getColumnCount();j++) {
					csvRow = csvRow + ","+dataTable.getValueAt(i,j);
				}
				bw.write("DATAROW="+csvRow+"\n");
			}


			bw.close();
			saveFile = f;
		} catch (IOException e) {

			e.printStackTrace();
		}

		try {
			BufferedWriter bw2 = new BufferedWriter(new FileWriter("lastopen.txt"));
			bw2.write(f.toString());
			bw2.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void loadMetaData(File infile) {
		ArrayList<String> headFields = new ArrayList<String>();
		headFields.add("ID");
		headFields.add("Last modified");

		String line;
		try {
			BufferedReader br = new BufferedReader(new FileReader(infile));
			while ((line = br.readLine())!=null) {
				if (!line.equals("")) {
					headFields.add(line.trim());
				}
			}
			br.close();
		} catch (IOException e) {

			e.printStackTrace();
		}

		headFields.add("Instrument model");
		headFields.add("species");
		headFields.add("File base name");
		headFields.add("ENA Anonymized id*");
		headFields.add("ENA Sample acc*");
		headFields.add("ENA Experiment acc*");
		headFields.add("ENA Run acc*");
		headFields.add("ENA Uploaded files*");
		headFields.add("TESSy batch*");
		headFields.add("TESSy validation*");
		headFields.add("TESSy date last uploaded*");
		headFields.add("TESSy date last approved*");
		headFields.add("TESSy id*");
		headFields.add("SFTP uploaded date*");



		String [] header = (String[]) headFields.toArray(new String[0]);
		String[][] data = new String[MAXROWS][header.length];
		dataTableModel = new DefaultTableModel(data,header);
		dataTable.setModel(dataTableModel);
		addTableListener();

	}



}
