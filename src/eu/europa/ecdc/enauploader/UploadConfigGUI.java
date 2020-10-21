package eu.europa.ecdc.enauploader;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class UploadConfigGUI extends JFrame implements ActionListener {

	
	private static final long serialVersionUID = 5532960884551659103L;
	private boolean isMain = false;
	private JTextField contactField;
	private JTextField tessyProviderField;
	private JTextField subjectField;
	private JTextField metaversionField;
	private JTextField countryField;
	private JTextField loginField;
	private JPasswordField passwordField;
	private JTextField domainField;
	private JTextField tessyurlField;
	private JTextField tessytargetField;
	private JCheckBox haltwarnBox;
	private JCheckBox haltremarkBox;
	private JButton saveButton;
	private JButton revertButton;
	private JComboBox<String> confignameField;
	private JTextField ftphostField;
	private JTextField ftpdirField;
	private JTextField ftploginField;
	private JPasswordField ftppasswordField;
	private JTextField enacenterField;
	private JCheckBox submitTessyBox;
	private JCheckBox submitFtpBox;
	private JCheckBox submitEnaBox;
	private JCheckBox submitTessyAssemblyBox;
	private JTextField enastudyField;
	private JTextField enachecklistField;
	private JTextField enaloginField;
	private JPasswordField enapasswordField;
	private JCheckBox enaanonymizeBox;
	private JCheckBox enaprodBox;
	private JTextField rawdirField;
	private JTextField assemblydirField;
	private JComboBox<String> nameformatField;
	private JComboBox<String> assemblynameformatField;
	private JCheckBox shareFtpBox;
	private JCheckBox enaForwardBox;


	private HashMap<String,UploadConfig> configs;
	private JButton newButton;
	private JButton renameButton;
	private String oldSelection;
	private JTextArea descriptionArea;
	private JButton deleteButton;
	private JTextField tmpdirField;
	private JTextField curlpathField;
	private JTextField enaftpField;
	private JTextField enaorganismField;
	private JCheckBox anonymizeFtpBox;
	private JCheckBox shareYearFtpBox;

	public void updateUi(UploadConfig cfg) {
		if (cfg==null) {
			return;
		}

		//confignameField.setText(cfg.getName());
		descriptionArea.setText(cfg.getDescription());

		//Filesystem
		tmpdirField.setText(cfg.getTmpPath());
		rawdirField.setText(cfg.getRawdataDir());
		assemblydirField.setText(cfg.getAssemblyDir());
		String rawDelim = cfg.getRawdataDelimiter();
		if (rawDelim.equals("_")) {
			nameformatField.setSelectedIndex(0);
		} else if (rawDelim.equals(".")) {
			nameformatField.setSelectedIndex(1);
		}

		String assemblyDelim = cfg.getAssemblyDelimiter();
		if (assemblyDelim.equals("")) {
			assemblynameformatField.setSelectedIndex(0);
		} else if (assemblyDelim.equals("_")) {
			assemblynameformatField.setSelectedIndex(1);
		} else if (assemblyDelim.equals(".")) {
			assemblynameformatField.setSelectedIndex(2);
		}

		//TESSy
		submitTessyBox.setSelected(cfg.isSubmitTessy());
		submitTessyAssemblyBox.setSelected(cfg.isSubmitTessyAssembly());
		TessyCredentials tessyCred = cfg.getTessyCredentials();

		enaftpField.setText(cfg.getEnaFtpHost());
		curlpathField.setText(cfg.getCurlPath());
		
		loginField.setText(tessyCred.getUsername());
		passwordField.setText(new String(tessyCred.getPassword()));
		tessyurlField.setText(tessyCred.getHostname());
		domainField.setText(tessyCred.getDomain());
		tessytargetField.setText(tessyCred.getTarget());

		contactField.setText(cfg.getTessyContact());
		countryField.setText(cfg.getTessyCountry());
		subjectField.setText(cfg.getTessySubject());
		metaversionField.setText(cfg.getTessyMeta());
		tessyProviderField.setText(cfg.getTessyProvider());
		haltwarnBox.setSelected(cfg.getTessyHaltWarn());
		haltremarkBox.setSelected(cfg.getTessyHaltRemark());

		//SFTP

		submitFtpBox.setSelected(cfg.isSubmitFtp());
		shareFtpBox.setSelected(cfg.isShareFtp());
		shareYearFtpBox.setSelected(cfg.isShowYearFtp());
		anonymizeFtpBox.setSelected(cfg.isAnonymizeFtp());
		ftphostField.setText(cfg.getSftpHost());
		ftpdirField.setText(cfg.getSftpPath());
		ftploginField.setText(cfg.getSftpLogin());
		ftppasswordField.setText(new String(cfg.getSftpPass()));

	
		//ENA
		submitEnaBox.setSelected(cfg.isSubmitEna());
		enacenterField.setText(cfg.getEnaCenter());
		enaorganismField.setText(cfg.getOrganism());
		enastudyField.setText(cfg.getEnaProjectAcc());
		enachecklistField.setText(cfg.getEnaChecklist());
		enaloginField.setText(cfg.getEnaLogin());
		enapasswordField.setText(new String(cfg.getEnaPassword()));
		enaanonymizeBox.setSelected(cfg.getEnaAnonymize());
		enaprodBox.setSelected(cfg.getEnaProd());





	}

	public UploadConfig createConfig() {
		UploadConfig cfg = new UploadConfig();


		cfg.setName((String)confignameField.getSelectedItem());

		cfg.setDescription(descriptionArea.getText());

		// Filesystem
		cfg.setTmpPath(tmpdirField.getText());
		cfg.setRawdataDir(rawdirField.getText());
		cfg.setAssemblyDir(assemblydirField.getText());
		String rawDelim = "_";
		if (nameformatField.getSelectedIndex()==1) {
			rawDelim = ".";
		}
		cfg.setRawdataDelimiter(rawDelim);

		String assemblyDelim = "";
		if (assemblynameformatField.getSelectedIndex()==1) {
			rawDelim = "_";
		} else if (assemblynameformatField.getSelectedIndex()==2) {
			rawDelim = ".";
		}
		cfg.setAssemblyDelimiter(assemblyDelim);

		//TESSy
		cfg.setSubmitTessyAssembly(submitTessyAssemblyBox.isSelected());
		cfg.setSubmitTessy(submitTessyBox.isSelected());
		TessyCredentials tessyCred = new TessyCredentials();
		tessyCred.setUsername(loginField.getText());
		tessyCred.setPassword(passwordField.getPassword());
		tessyCred.setHostname(tessyurlField.getText());
		tessyCred.setDomain(domainField.getText());
		tessyCred.setTarget(tessytargetField.getText());
		cfg.setTessyCredentials(tessyCred);

		cfg.setCurlPath(curlpathField.getText());
		cfg.setEnaFtpHost(enaftpField.getText());
		
		cfg.setTessyContact(contactField.getText());
		cfg.setTessyCountry(countryField.getText());
		cfg.setTessySubject(subjectField.getText());
		cfg.setTessyMeta(metaversionField.getText());
		cfg.setTessyProvider(tessyProviderField.getText());
		cfg.setTessyHaltWarn(haltwarnBox.isSelected());
		cfg.setTessyHaltRemark(haltremarkBox.isSelected());

		//SFTP
		cfg.setSubmitFtp(submitFtpBox.isSelected());
		cfg.setShareFtp(shareFtpBox.isSelected());
		cfg.setShowYearFtp(shareYearFtpBox.isSelected());
		cfg.setAnonymizeFtp(anonymizeFtpBox.isSelected());
		cfg.setSftpHost(ftphostField.getText());
		cfg.setSftpPath(ftpdirField.getText());
		cfg.setSftpLogin(ftploginField.getText());
		cfg.setSftpPass(ftppasswordField.getPassword());

		//ENA
		cfg.setOrganism(enaorganismField.getText());
		cfg.setSubmitEna(submitEnaBox.isSelected());
		cfg.setEnaCenter(enacenterField.getText());
		cfg.setEnaProjectAcc(enastudyField.getText());
		cfg.setEnaChecklist(enachecklistField.getText());
		cfg.setEnaLogin(enaloginField.getText());
		cfg.setEnaPassword(enapasswordField.getPassword());
		cfg.setEnaAnonymize(enaanonymizeBox.isSelected());
		cfg.setEnaProd(enaprodBox.isSelected());
		cfg.setEnaFtpExist(false);


		/*

		 */


		return cfg;
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
		UploadConfigGUI gui = new UploadConfigGUI(true);
		gui.init();
	}

	UploadConfigGUI(boolean m) {
		isMain = m;
	}

	UploadConfigGUI() {

	}

	public void init() {


		configs = new HashMap<String,UploadConfig>();
		File dir = new File(".");
		File[] files = dir.listFiles();
		for (File f : files) {
			if (f.getName().endsWith(".cfg")) {
				try {
					UploadConfig cfg = UploadConfigHandler.loadConfig(f);
					configs.put(f.getName(),cfg);
				} catch (ClassNotFoundException | IOException e) {
					e.printStackTrace();
					JOptionPane.showMessageDialog(null,
							"Could not open config file "+f.toString()+". If it is open in another program, please close it and try again.");
				}

			}
		}


		try {
			String imagePath = "media/ECDC2.png";
			InputStream imgStream = UploadConfigGUI.class.getResourceAsStream(imagePath );
			BufferedImage myImg;
			myImg = ImageIO.read(imgStream);
			setIconImage(myImg);
		} catch (Exception e) {
			e.printStackTrace();
		}


		setTitle("ECDC WGS upload configuration");
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
		saveButton.setActionCommand("saveAndClose");

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
		JTabbedPane tabs = new JTabbedPane();
		mainPanel.add(tabs,BorderLayout.CENTER);
		mainPanel.add(topPanel,BorderLayout.NORTH);

		JPanel fileTab = new JPanel();
		FlowLayout mgrFile = new FlowLayout(FlowLayout.RIGHT);
		fileTab.setLayout(mgrFile);
		fileTab.setBorder(BorderFactory.createEtchedBorder());
		populateFile(fileTab);

		JPanel tessyTab = new JPanel();
		FlowLayout mgrTessy = new FlowLayout(FlowLayout.RIGHT);
		tessyTab.setLayout(mgrTessy);
		tessyTab.setBorder(BorderFactory.createEtchedBorder());
		populateTessy(tessyTab);

		JPanel ftpTab = new JPanel();
		FlowLayout mgrFtp = new FlowLayout(FlowLayout.RIGHT);
		ftpTab.setBorder(BorderFactory.createEtchedBorder());
		ftpTab.setLayout(mgrFtp);
		//ftpTab.setLayout(new BoxLayout(ftpTab,BoxLayout.PAGE_AXIS));
		populateFtp(ftpTab);


		JPanel enaTab = new JPanel();
		FlowLayout mgrEna = new FlowLayout(FlowLayout.RIGHT);
		enaTab.setLayout(mgrEna);
		enaTab.setBorder(BorderFactory.createEtchedBorder());
		populateEna(enaTab);

		JPanel bigsDbTab = new JPanel();
		FlowLayout mgrBigsDb = new FlowLayout(FlowLayout.RIGHT);
		bigsDbTab.setLayout(mgrBigsDb);
		bigsDbTab.setBorder(BorderFactory.createEtchedBorder());
		bigsDbTab.setLayout(new BoxLayout(bigsDbTab,BoxLayout.PAGE_AXIS));

		tabs.add("Local filesystem config", fileTab);
		tabs.add("TESSy config", tessyTab);
		tabs.add("ECDC SFTP config", ftpTab);
		tabs.add("ENA config", enaTab);
		tabs.add("BIGSdb config", bigsDbTab);


		String item = (String)confignameField.getSelectedItem();
		UploadConfig cfg = configs.get(item);
		updateUi(cfg);
		oldSelection = item;

		this.add(mainPanel);
		if (isMain) {
			this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		}
		this.setSize(540,740);
		this.setVisible(true);
	}


	private void populateFile(JPanel fileTab) {
		JLabel rawdirLabel = new JLabel("Raw reads base directory");
		rawdirField = new JTextField("");
		rawdirField.setPreferredSize(new Dimension(240,20));
		JPanel rawdirPanel = new JPanel ();
		JButton rawdirBrowseButton = new JButton("Browse...");
		rawdirBrowseButton.setActionCommand("browseRaw");
		rawdirBrowseButton.addActionListener(this);
		rawdirPanel.add(rawdirLabel);
		rawdirPanel.add(rawdirField);
		rawdirPanel.add(rawdirBrowseButton);
		fileTab.add(rawdirPanel);



		JLabel nameformatLabel = new JLabel("Raw reads filename format");
		nameformatField = new JComboBox<String>();
		nameformatField.addItem("<sample>_*.fastq.gz");
		nameformatField.addItem("<sample>.*.fastq.gz");
		nameformatField.addItem("<sample>.*.fastq");
		nameformatField.setPreferredSize(new Dimension(240,20));
		JPanel nameformatPanel = new JPanel ();
		nameformatPanel.add(nameformatLabel);
		nameformatPanel.add(nameformatField);
		fileTab.add(nameformatPanel);

		JLabel assemblydirLabel = new JLabel("Assemblies base directory");
		assemblydirField = new JTextField("");
		assemblydirField.setPreferredSize(new Dimension(240,20));
		JPanel assemblydirPanel = new JPanel ();
		JButton assemblydirBrowseButton = new JButton("Browse...");
		assemblydirBrowseButton.setActionCommand("browseAssembly");
		assemblydirBrowseButton.addActionListener(this);
		assemblydirPanel.add(assemblydirLabel);
		assemblydirPanel.add(assemblydirField);
		assemblydirPanel.add(assemblydirBrowseButton);
		fileTab.add(assemblydirPanel);

		JLabel assemblynameformatLabel = new JLabel("Assembly filename format");
		assemblynameformatField = new JComboBox<String>();
		assemblynameformatField.addItem("<sample>.fasta");
		assemblynameformatField.addItem("<sample>_*.fasta");
		assemblynameformatField.addItem("<sample>.*.fasta");
		assemblynameformatField.setPreferredSize(new Dimension(240,20));
		JPanel assemblynameformatPanel = new JPanel ();
		assemblynameformatPanel.add(assemblynameformatLabel);
		assemblynameformatPanel.add(assemblynameformatField);
		fileTab.add(assemblynameformatPanel);
		
		
		JLabel tmpdirLabel = new JLabel("TMP directory");
		tmpdirField = new JTextField("");
		tmpdirField.setPreferredSize(new Dimension(240,20));
		JPanel tmpdirPanel = new JPanel ();
		JButton tmpdirBrowseButton = new JButton("Browse...");
		tmpdirBrowseButton.setActionCommand("browseTmp");
		tmpdirBrowseButton.addActionListener(this);
		tmpdirPanel.add(tmpdirLabel);
		tmpdirPanel.add(tmpdirField);
		tmpdirPanel.add(tmpdirBrowseButton);
		fileTab.add(tmpdirPanel);
		
		
		
	}

	private void populateEna(JPanel enaTab) {


		
		
		
		submitEnaBox = new JCheckBox("Submit raw reads (if available) to ENA");
		JPanel submitEnaPanel = new JPanel ();
		submitEnaPanel.add(submitEnaBox);
		enaTab.add(submitEnaPanel);

		JLabel curlpathLabel = new JLabel("Path to CURL program");
		curlpathField = new JTextField("");
		curlpathField.setPreferredSize(new Dimension(240,20));
		JPanel curlpathPanel = new JPanel ();
		JButton curlpathBrowseButton = new JButton("Browse...");
		curlpathBrowseButton.setActionCommand("browseCurl");
		curlpathBrowseButton.addActionListener(this);
		curlpathPanel.add(curlpathLabel);
		curlpathPanel.add(curlpathField);
		curlpathPanel.add(curlpathBrowseButton);
		enaTab.add(curlpathPanel);

		
		JLabel enaftpLabel = new JLabel("ENA FTP server");
		enaftpField = new JTextField("");
		enaftpField.setPreferredSize(new Dimension(240,20));
		JPanel enaftpPanel = new JPanel ();
		enaftpPanel.add(enaftpLabel);
		enaftpPanel.add(enaftpField);
		enaTab.add(enaftpPanel);
		
		JLabel enaorganismLabel = new JLabel("Organism name");
		enaorganismField = new JTextField("");
		enaorganismField.setPreferredSize(new Dimension(240,20));
		JPanel enaorganismPanel = new JPanel ();
		enaorganismPanel.add(enaorganismLabel);
		enaorganismPanel.add(enaorganismField);
		enaTab.add(enaorganismPanel);
		
		JLabel enacenterLabel = new JLabel("Submitting Centre");
		enacenterField = new JTextField("");
		enacenterField.setPreferredSize(new Dimension(240,20));
		JPanel enacenterPanel = new JPanel ();
		enacenterPanel.add(enacenterLabel);
		enacenterPanel.add(enacenterField);
		enaTab.add(enacenterPanel);

		JLabel enastudyLabel = new JLabel("ENA study accession");
		enastudyField = new JTextField("");
		enastudyField.setPreferredSize(new Dimension(240,20));
		JPanel enastudyPanel = new JPanel ();
		enastudyPanel.add(enastudyLabel);
		enastudyPanel.add(enastudyField);
		enaTab.add(enastudyPanel);

		JLabel enachecklistLabel = new JLabel("ENA checklist (ERCXXXXXX)");
		enachecklistField = new JTextField("");
		enachecklistField.setPreferredSize(new Dimension(240,20));
		JPanel enachecklistPanel = new JPanel ();
		enachecklistPanel.add(enachecklistLabel);
		enachecklistPanel.add(enachecklistField);
		enaTab.add(enachecklistPanel);

		JLabel enaloginLabel = new JLabel("ENA Webin account");
		enaloginField = new JTextField("");
		enaloginField.setPreferredSize(new Dimension(240,20));
		JPanel enaloginPanel = new JPanel ();
		enaloginPanel.add(enaloginLabel);
		enaloginPanel.add(enaloginField);
		enaTab.add(enaloginPanel);

		JLabel enapasswordLabel = new JLabel("ENA Webin password");
		enapasswordField = new JPasswordField("");
		enapasswordField.setPreferredSize(new Dimension(240,20));
		JPanel enapasswordPanel = new JPanel ();
		enapasswordPanel.add(enapasswordLabel);
		enapasswordPanel.add(enapasswordField);
		enaTab.add(enapasswordPanel);

		enaanonymizeBox = new JCheckBox("Anonymize Record ID in ENA submission");
		enaanonymizeBox.setSelected(true);
		JPanel enaanonymizePanel = new JPanel ();
		enaanonymizePanel.add(enaanonymizeBox);
		enaTab.add(enaanonymizePanel);

		enaprodBox = new JCheckBox("Submit to ENA production service");
		enaprodBox.setSelected(true);
		JPanel enaprodPanel = new JPanel ();
		enaprodPanel.add(enaprodBox);
		enaTab.add(enaprodPanel);

		JLabel infoEnaLabel = new JLabel("<html><body>Note: The submission to ENA will include a minimal metadata set<br>including: year, country, and isolation source (human/nonhuman)</body></html>");
		JPanel infoEnaPanel = new JPanel ();
		infoEnaPanel.add(infoEnaLabel);
		enaTab.add(infoEnaPanel);
	}

	private void populateFtp(JPanel ftpTab) {


		submitFtpBox = new JCheckBox("Submit raw reads (if available) to ECDC SFTP");
		JPanel submitFtpPanel = new JPanel ();
		submitFtpPanel.add(submitFtpBox);
		ftpTab.add(submitFtpPanel);

		shareFtpBox = new JCheckBox("Share reads with other ECDC SFTP users");
		shareFtpBox.setSelected(true);
		JPanel shareFtpPanel = new JPanel ();
		shareFtpPanel.add(shareFtpBox);
		ftpTab.add(shareFtpPanel);
		
		anonymizeFtpBox = new JCheckBox("Anonymize filenames");
		anonymizeFtpBox.setSelected(true);
		JPanel anonymizeFtpPanel = new JPanel ();
		anonymizeFtpPanel.add(anonymizeFtpBox);
		ftpTab.add(anonymizeFtpPanel);
		
		shareYearFtpBox = new JCheckBox("Share year with other users");
		shareYearFtpBox.setSelected(true);
		JPanel shareYearFtpPanel = new JPanel ();
		shareYearFtpPanel.add(shareYearFtpBox);
		ftpTab.add(shareYearFtpPanel);

		JLabel ftphostLabel = new JLabel("SFTP host");
		ftphostField = new JTextField("");
		ftphostField.setPreferredSize(new Dimension(240,20));
		JPanel ftphostPanel = new JPanel ();
		ftphostPanel.add(ftphostLabel);
		ftphostPanel.add(ftphostField);
		ftpTab.add(ftphostPanel);

		JLabel ftpdirLabel = new JLabel("SFTP home directory");
		ftpdirField = new JTextField("");
		ftpdirField.setPreferredSize(new Dimension(240,20));
		JPanel ftpdirPanel = new JPanel ();
		ftpdirPanel.add(ftpdirLabel);
		ftpdirPanel.add(ftpdirField);
		ftpTab.add(ftpdirPanel);

		JLabel ftploginLabel = new JLabel("SFTP Login");
		ftploginField = new JTextField("");
		ftploginField.setPreferredSize(new Dimension(240,20));
		JPanel ftploginPanel = new JPanel ();
		ftploginPanel.add(ftploginLabel);
		ftploginPanel.add(ftploginField);
		ftpTab.add(ftploginPanel);

		JLabel ftppasswordLabel = new JLabel("SFTP Password");
		ftppasswordField = new JPasswordField("");
		ftppasswordField.setPreferredSize(new Dimension(240,20));
		JPanel ftppasswordPanel = new JPanel ();
		ftppasswordPanel.add(ftppasswordLabel);
		ftppasswordPanel.add(ftppasswordField);
		ftpTab.add(ftppasswordPanel);
	}

	private void populateTessy(JPanel tessyTab) {

		submitTessyBox = new JCheckBox("Submit epidemiological data to TESSy");
		submitTessyBox.setSelected(true);
		submitTessyBox.setEnabled(false);
		JPanel submitTessyPanel = new JPanel ();
		submitTessyPanel.add(submitTessyBox);
		tessyTab.add(submitTessyPanel);

		submitTessyAssemblyBox = new JCheckBox("Submit assembly (if available) to TESSy");
		JPanel submitTessyAssemblyPanel = new JPanel ();
		submitTessyAssemblyPanel.add(submitTessyAssemblyBox);
		tessyTab.add(submitTessyAssemblyPanel);

		JLabel contactLabel = new JLabel("Contact person (name)");
		contactField = new JTextField("");
		contactField.setPreferredSize(new Dimension(240,20));
		JPanel contactPanel = new JPanel ();
		contactPanel.add(contactLabel);
		contactPanel.add(contactField);
		tessyTab.add(contactPanel);

		JLabel tessySourceLabel = new JLabel("Data Provider");
		tessyProviderField = new JTextField("");
		tessyProviderField.setPreferredSize(new Dimension(240,20));
		JPanel tessyProviderPanel = new JPanel ();
		tessyProviderPanel.add(tessySourceLabel);
		tessyProviderPanel.add(tessyProviderField);
		tessyTab.add(tessyProviderPanel);

		JLabel subjectLabel = new JLabel("Subject (XXXXISO)");
		subjectField = new JTextField("");
		subjectField.setPreferredSize(new Dimension(240,20));
		JPanel subjectPanel = new JPanel ();
		subjectPanel.add(subjectLabel);
		subjectPanel.add(subjectField);
		tessyTab.add(subjectPanel);

		JLabel metaversionLabel = new JLabel("TESSy metadata version (X)");
		metaversionField = new JTextField("");
		metaversionField.setPreferredSize(new Dimension(240,20));
		JPanel metaversionPanel = new JPanel ();
		metaversionPanel.add(metaversionLabel);
		metaversionPanel.add(metaversionField);
		tessyTab.add(metaversionPanel);

		JLabel countryLabel = new JLabel("Country code (XX)");
		countryField = new JTextField("");
		countryField.setPreferredSize(new Dimension(240,20));
		JPanel countryPanel = new JPanel ();
		countryPanel.add(countryLabel);
		countryPanel.add(countryField);
		tessyTab.add(countryPanel);

		JLabel loginLabel = new JLabel("TESSy username");
		loginField = new JTextField("");
		loginField.setPreferredSize(new Dimension(240,20));
		JPanel loginPanel = new JPanel ();
		loginPanel.add(loginLabel);
		loginPanel.add(loginField);
		tessyTab.add(loginPanel);

		JLabel passwordLabel = new JLabel("TESSy password");
		passwordField = new JPasswordField("");
		passwordField.setPreferredSize(new Dimension(240,20));
		JPanel passwordPanel = new JPanel ();
		passwordPanel.add(passwordLabel);
		passwordPanel.add(passwordField);
		tessyTab.add(passwordPanel);

		JLabel tessyurlLabel = new JLabel("TESSy URL");
		tessyurlField = new JTextField("");
		tessyurlField.setPreferredSize(new Dimension(240,20));
		JPanel tessyurlPanel = new JPanel ();
		tessyurlPanel.add(tessyurlLabel);
		tessyurlPanel.add(tessyurlField);
		tessyTab.add(tessyurlPanel);

		JLabel domainLabel = new JLabel("TESSy domain");
		domainField = new JTextField("");
		domainField.setPreferredSize(new Dimension(240,20));
		JPanel domainPanel = new JPanel ();
		domainPanel.add(domainLabel);
		domainPanel.add(domainField);
		tessyTab.add(domainPanel);

		JLabel tessytargetLabel = new JLabel("TESSy API target");
		tessytargetField = new JTextField("");
		tessytargetField.setPreferredSize(new Dimension(240,20));
		JPanel tessytargetPanel = new JPanel ();
		tessytargetPanel.add(tessytargetLabel);
		tessytargetPanel.add(tessytargetField);
		tessyTab.add(tessytargetPanel);

		haltwarnBox = new JCheckBox("Halt submission on warnings");
		JPanel haltwarnPanel = new JPanel ();
		haltwarnPanel.add(haltwarnBox);
		tessyTab.add(haltwarnPanel);

		haltremarkBox = new JCheckBox("Halt submission on remarks");
		JPanel haltremarkPanel = new JPanel ();
		haltremarkPanel.add(haltremarkBox);
		tessyTab.add(haltremarkPanel);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if (command.equals("close")) {
			this.dispose();
		} else if (command.equals("saveAndClose")) {

			UploadConfig cfg = createConfig();
			String selected = (String)confignameField.getSelectedItem();
			if (selected!=null) {
				configs.put(selected,cfg);
			}

			for (String k : configs.keySet()) {
				System.out.println(k);
				UploadConfig cfg2 = configs.get(k);
				File f = new File(k);
				if (cfg2!=null && f!=null) {
					try {
						UploadConfigHandler.saveConfig(f, cfg2);
					} catch (IOException e1) {
						e1.printStackTrace();
						JOptionPane.showMessageDialog(null,
								"Could not save to config file "+f.toString()+". If it is open in another program, please close it and try again.");
					}
				}
			}


		} else if (command.equals("revert")) {
			for (String name : configs.keySet()) {
				File f = new File(name);
				if (f.exists()) {
					try {
						UploadConfig cfg = UploadConfigHandler.loadConfig(f);
						configs.put(name,cfg);
					} catch (ClassNotFoundException | IOException e1) {

						e1.printStackTrace();
					}
				} else {
					configs.put(name,new UploadConfig());
				}
			}
			UploadConfig cfg = configs.get((String)confignameField.getSelectedItem());
			updateUi(cfg);
		} else if (command.equals("browseTmp")) {


			JFileChooser chooser = new JFileChooser(); 
			chooser.setCurrentDirectory(new java.io.File("."));
			chooser.setDialogTitle("Choose TMP directory");
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

			chooser.setAcceptAllFileFilterUsed(false);

			if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) { 		   
				tmpdirField.setText(chooser.getSelectedFile().toString());	  	
			}

		} else if (command.equals("browseCurl")) {


			JFileChooser chooser = new JFileChooser(); 
			chooser.setCurrentDirectory(new java.io.File("."));
			chooser.setDialogTitle("Select the curl executable");
			chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

			chooser.setAcceptAllFileFilterUsed(false);

			if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) { 		   
				curlpathField.setText(chooser.getSelectedFile().toString());	  	
			}

		} else if (command.equals("browseRaw")) {


			JFileChooser chooser = new JFileChooser(); 
			chooser.setCurrentDirectory(new java.io.File("."));
			chooser.setDialogTitle("Choose directory containing raw data (.fastq.gz)");
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

			chooser.setAcceptAllFileFilterUsed(false);

			if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) { 		   
				rawdirField.setText(chooser.getSelectedFile().toString());	  	
			}

		} else if (command.equals("browseAssembly")) {
			JFileChooser chooser = new JFileChooser(); 
			chooser.setCurrentDirectory(new java.io.File("."));
			chooser.setDialogTitle("Choose directory containing assemblies (.fasta)");
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

			chooser.setAcceptAllFileFilterUsed(false);

			if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) { 		   
				assemblydirField.setText(chooser.getSelectedFile().toString());	  	
			}
		} else if (command.equals("selection")) {
			UploadConfig cfgOld = createConfig();
			if (oldSelection!=null) {
				configs.put(oldSelection,cfgOld);
			}

			String item = (String)confignameField.getSelectedItem();
			UploadConfig cfg = configs.get(item);
			updateUi(cfg);
			oldSelection = item;
		} else if (command.equals("new")) {
			String s = (String)JOptionPane.showInputDialog(
					this,
					"Choose a name for the config:",
					"Create new config",
					JOptionPane.PLAIN_MESSAGE
					);


			if ((s != null) && (s.length() > 0)) {
				if (!s.endsWith(".cfg")) {
					s = s + ".cfg";
				}
				for (String k : configs.keySet()) {
					if (s.equals(k)) {
						JOptionPane.showMessageDialog(this,
								"This config name already exists.");
						return;
					}
				}
				System.out.println(s);
				UploadConfig cfg = new UploadConfig();
				configs.put(s,cfg);
				confignameField.addItem(s);
				confignameField.setSelectedItem(s);
				updateUi(cfg);
			}



		} else if (command.equals("rename")) {
			String name = (String)confignameField.getSelectedItem();
			if (name==null) {
				return;
			}
			String s = (String)JOptionPane.showInputDialog(
					this,
					"Choose a new name for config "+name,
					"Rename config",
					JOptionPane.PLAIN_MESSAGE
					);


			if ((s != null) && (s.length() > 0)) {
				if (!s.endsWith(".cfg")) {
					s = s + ".cfg";
				}
				for (String k : configs.keySet()) {
					if (s.equals(k)) {
						JOptionPane.showMessageDialog(this,
								"This config name already exists, choose another name.");
						return;
					}
				}
				System.out.println(s);


				File oldFile = new File(name);
				File newFile = new File(s);
				UploadConfig cfg = createConfig();
				if (oldFile.exists()) {
					try {
						UploadConfigHandler.saveConfig(newFile, cfg);
						oldFile.delete();
					} catch (IOException e1) {
						e1.printStackTrace();
						JOptionPane.showMessageDialog(this,
								"Writing to file "+s+" failed. Aborting rename.");
						return;

					}

				}


				configs.remove(name);
				configs.put(s,cfg);
				System.out.println("OLD NAME: "+name);



				confignameField.removeItem(name);
				confignameField.addItem(s);
				confignameField.setSelectedItem(s);
				updateUi(cfg);
			} 



		} else if (command.equals("delete")) {
			String name = (String)confignameField.getSelectedItem();
			if (name==null) {
				return;
			}
			int n = JOptionPane.showConfirmDialog(
					this,
					"Are you sure? This will rename the config file to "+name+".bak\n and it will no longer be visible.",
					"Delete config?",
					JOptionPane.YES_NO_OPTION);
			if (n==JOptionPane.YES_OPTION) {

				File oldFile = new File(name);
				File newFile = new File(name+".bak");
				UploadConfig cfg = createConfig();
				if (oldFile.exists()) {
					try {
						UploadConfigHandler.saveConfig(newFile, cfg);
						oldFile.delete();
					} catch (IOException e1) {
						e1.printStackTrace();
						JOptionPane.showMessageDialog(this,
								"Deletion failed.");
						return;

					}

				}
				System.out.println("Previous configs:");
				for (String k : configs.keySet()) {
					System.out.println(k);	
				}
				System.out.println("====");
				System.out.println("Removing "+name);
				System.out.println(configs.remove(name));
				System.out.println("Remaining configs:");
				for (String k : configs.keySet()) {
					System.out.println(k);	
				}
				oldSelection = null;
				confignameField.removeItem(name);
				try {
					confignameField.setSelectedIndex(0);
					UploadConfig cfg2 = configs.get((String)confignameField.getSelectedItem());
					updateUi(cfg2);
				} catch (Exception e2) {
					UploadConfig cfg2 = new UploadConfig();
					updateUi(cfg2);
				}
				System.out.println("Remaining configs:");
				for (String k : configs.keySet()) {
					System.out.println(k);	
				}
				System.out.println("====");
			}

		}
	}





}
