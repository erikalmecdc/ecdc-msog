package eu.europa.ecdc.enauploader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import javax.swing.JOptionPane;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class ENAuploaderMain {

	private static String version="0.22";


	// This is a command line version
	public static void main(String[] args) {
		//args: 
		//Center C
		//Project id ("" means create new project) "PRJEB25143"
		//CSV file path
		//Data directory path
		// Login (Webin-NNNNN)
		// Password
		// yes/no whether to use the production database at EBI
		// yes/no whether to anonymizehttp://marketplace.eclipse.org/marketplace-client-intro?mpc_install=156
		// yes/no whether files are already on FTP
		//delimiter
		//ENA checklist
		
		
		String ftpHost = "webin.ebi.ac.uk";
		
		String curlPath = "curl.exe";
		String tmpPath = ".";
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
						if (!(new File(curlPath)).exists()) {
							System.out.println("Path to the curl program: "+curlPath+" is incorrect. Check paths.txt");
							return;
						}
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
		
		
		
		
		
		
		CommandLine commandLine;
	    Option option_center = Option.builder("C").longOpt("center").argName("center").hasArg().desc("Submitting center (MANDATORY)").build();
	    Option option_proj = Option.builder("p").argName("project").argName("project").hasArg().desc("Project alias or accession. If the argument is an accession (PRJEB*****), data will be submitted to that project. If the argument is an alias, a new project will be created. (MANDATORY)").build();
	    Option option_csv = Option.builder("c").longOpt("csv").argName("csv").hasArg().desc("CSV-infile. First four columns must be (in order): ID, File base name, Instrument, species. An output csv file will be created. Accessions will be written in the last five columns in this order: ID used for upload, Sample acc, Experiment acc, Run acc, uploaded files. These column heading must exist (MANDATORY unless only project submission/release is involved)").build();
	    Option option_data = Option.builder("d").longOpt("data-dir").argName("dir").hasArg().desc("Directory for data files (MANDATORY unless ftp option is set to yes or if only study submission/release is involved)").build();
	    Option option_login = Option.builder("l").longOpt("login").argName("login").hasArg().desc("Webin login (MANDATORY)").build();
	    Option option_passwd = Option.builder().longOpt("pass").argName("passwd").hasArg().desc("Webin password (Will be promped for if not entered)").build();
	    Option option_anon = Option.builder("a").longOpt("anonymize").argName("yes/no").hasArg().desc("Anonymize (yes/no) (Default: yes)").build();
	    Option option_prod = Option.builder().longOpt("production").argName("yes/no").hasArg().desc("Use production server (yes/no) (Default: no)").build();
	    Option option_ftp = Option.builder().longOpt("ftp").argName("yes/no").hasArg().desc("Files already on FTP? (yes/no) (Default: no)").build();
	    Option option_delim = Option.builder().longOpt("delimiter").hasArg().desc("Delimiter for file, usually _ (Default: any delimiter)").build();
	    Option option_checklist = Option.builder().longOpt("checklist").argName("checklist name").hasArg().desc("ENA checklist (Default: ERC000044)").build();
	    Option option_out = Option.builder("o").longOpt("out").argName("outfile").hasArg().desc("Output csv file name. (Default: <filename>.out.csv)").build();
	    Option option_hold = Option.builder("h").longOpt("hold").argName("yyyy-mm-dd").hasArg().desc("Hold study release until specified date. (only relevant with -p <alias> option.)").build();
	    Option option_release = Option.builder("r").longOpt("release").argName("study accession").hasArg().desc("Release study with the specified accession immediately, then quit. (Requires only -l and -C)").build();    
	    
	    Options options = new Options();
	    CommandLineParser parser = new DefaultParser();

	    options.addOption(option_center);
	    options.addOption(option_proj);
	    options.addOption(option_csv);
	    options.addOption(option_data);
	    options.addOption(option_login);
	    options.addOption(option_passwd);
	    options.addOption(option_anon); 
	    options.addOption(option_prod);
	    options.addOption(option_ftp);
	    options.addOption(option_delim);
	    options.addOption(option_checklist);
	    options.addOption(option_out);
	    options.addOption(option_hold);
	    options.addOption(option_release);
	    
	    String header1 = "Options, flags and arguments may be in any order";
	    String footer1 = "ENA uploader version "+version;
	    HelpFormatter formatter = new HelpFormatter();
	    formatter.printHelp("ENAuploader", header1, options, footer1, true);    

	  
		try
	    {
	        commandLine = parser.parse(options, args);
	        
	        String center;
	        if (commandLine.hasOption("C")) {
	            center = commandLine.getOptionValue("C");
	        } else {
	        	System.out.println("-C or --center option mandatory");
		        return;
	        }
	        
	        String login;
	        if (commandLine.hasOption("l")) {
	            login = commandLine.getOptionValue("l");
	        } else {
	        	System.out.println("-l or --logjn option mandatory");
		        return;
	        }
	        
	        String passwd;
	        if (commandLine.hasOption("pass")) {
	            passwd = commandLine.getOptionValue("pass");
	        } else {
	        	System.out.print("Enter Webin password for " + login+": ");
	        	passwd = new String(System.console().readPassword());
	        }
	        
	        boolean prod=false;
	        if (commandLine.hasOption("production")) {
	            String anonStr = commandLine.getOptionValue("production");
	            if (anonStr.equals("yes")) {
	            	prod = true;
	            } else if (anonStr.equals("no")) {
	            	prod = false;
	            } else {
	            	System.out.println("--production must be yes or no.");
			        return;
	            }
	        } 
	        
	        
	        String release;
	        if (commandLine.hasOption("r")) {
	            release = commandLine.getOptionValue("r");
	            releaseStudy(center,release,curlPath,tmpPath,login,passwd,prod);
	            return;
	        } else {
	        	
	        }
	       
	        String project;
	        if (commandLine.hasOption("p")) {
	            project = commandLine.getOptionValue("p");
	        } else {
	        	project = "";
	        }
	        
	        String delimiter;
	        if (commandLine.hasOption("delimiter")) {
	        	delimiter = commandLine.getOptionValue("delimiter");
	        } else {
	        	delimiter = "";
	        }
	        
	        String holdDate = "";
	        if (commandLine.hasOption("h")) {
	        	holdDate = commandLine.getOptionValue("h");
	        } else {
	        	holdDate = "";
	        }

	        String checklist;
	        if (commandLine.hasOption("checklist")) {
	        	checklist = commandLine.getOptionValue("checklist");
	        } else {
	        	checklist = "ERC000044";
	        	System.out.println("No checklist, defaulting to " + checklist);
	        }
	        
	       

	        boolean csv = true;
	        File csvFile=null;
	        if (commandLine.hasOption("c")) {
	            csvFile = new File(commandLine.getOptionValue("c"));
	        } else {
	        	csv=false;
	        }

	        File outCsv;
	        if (commandLine.hasOption("o")) {
	        	outCsv = new File(commandLine.getOptionValue("o"));
	        } else {
	        	if (csv) {
	        		outCsv = new File(csvFile.toString()+".out.csv");
	        	} else {
	        		System.out.println("Output CSV is required if there is no input CSV.");
	        		return;
	        	}
	        	
	        	
	        }
	        

	       
	        
	        boolean anon=true;
	        if (commandLine.hasOption("a")) {
	            String anonStr = commandLine.getOptionValue("a");
	            if (anonStr.equals("yes")) {
	            	anon = true;
	            } else if (anonStr.equals("no")) {
	            	anon = false;
	            } else {
	            	System.out.println("-a and --anonymize must be yes or no.");
			        return;
	            }
	        } 
	      
	        boolean ftpExist=false;
	        if (commandLine.hasOption("ftp")) {
	            String anonStr = commandLine.getOptionValue("ftp");
	            if (anonStr.equals("yes")) {
	            	System.out.println("Setting ftpExist to true");
	            	ftpExist = true;
	            } else if (anonStr.equals("no")) {
	            	ftpExist = false;
	            } else {
	            	System.out.println("--ftp must be yes or no.");
			        return;
	            }
	        } 
	        
	        File dataDir=null;
	        if (commandLine.hasOption("d")) {
	            dataDir = new File(commandLine.getOptionValue("d"));
	        } else {
	        	if (ftpExist) {
	        		dataDir = new File(".");
	        	} else if (csv) {
	        		System.out.println("-d or --data-dir option mandatory if ftp option is not set.");
	        		return;
	        	}
	        }
	        

			
			
			
			
			if (!project.startsWith("PRJEB")) {
				String randomUUIDString = UUID.randomUUID().toString();
				Submission s = new Submission(center, randomUUIDString);

				s.setCurlPath(curlPath);
				s.setTmpPath(tmpPath);
				if (prod) {
					s.useProductionServer(true);
				} else {
					s.useProductionServer(false);
				}

				Project p = new Project(center,project,s);
				p.setReleaseDate(holdDate);
				
				s.setLogin(login,passwd);
				s.addEntry(p);
				s.uploadFiles();
				s.submit();
				String acc = p.getAccession(); 
				if (acc.equals("")) {
					System.out.println("Study submission failed.");
					return;
				}
				System.out.println("Study submission complete. Aquired accession: " + acc );
				project = acc;
				
				if (!csv) {
					System.out.println("No CSV input file, creating project and exiting...");
					String[][] data = new String[0][0];
					String[] header = new String[0];
					ENAUtils.writeCsv(outCsv, project, data, header);
					return;
				}
				
			}
			
			
			if (csv) {
				String[][] data = ENAUtils.readCsv(csvFile,true);
				String[] header = ENAUtils.readCsvHeader(csvFile);
				CsvOutputHandler outHandler = new CsvOutputHandler(csvFile,outCsv,project);
				
				SubmissionWorker worker = new SubmissionWorker(center, project, data, header, dataDir,login,passwd, prod, anon, ftpExist, delimiter, checklist, null, tmpPath, curlPath, ftpHost, outHandler, null,"","","", false, true, false, null);
				worker.doInBackground();
			}
	        
	    }
	    catch (ParseException exception)
	    {
	        System.out.print("Parse error: ");
	        System.out.println(exception.getMessage());
	        return;
	    } catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		
		
		
		
		
		
		
		
		//SubmissionFactory.createSubmissionFromCSV(center,project, csvFile,dataDir, login, pass, prod);
	}
	
	
	private static void releaseStudy(String center, String acc, String curlPath, String tmpPath, String login, String passwd, boolean prod) {
		String randomUUIDString = UUID.randomUUID().toString();
		Submission s = new Submission(center, randomUUIDString);

		s.setCurlPath(curlPath);
		s.setTmpPath(tmpPath);
		if (prod) {
			s.useProductionServer(true);
		}
		
		Project p = new Project(center,"",s);
		p.setAccession(acc);
		p.setReleaseAction(true);
		
		s.setLogin(login,passwd);
		s.addEntry(p);
		s.uploadFiles();
		s.submit();
		
	
		
	}


	//Example code for use of the library, rename to main to run it
	public static void main2(String[] args) {
		
		
		
		//base name for testing
		String randomUUIDString = UUID.randomUUID().toString();
		String sname = "ECDCtest_"+randomUUIDString;
		
		//Create new submission
		Submission s = new Submission("ECDC","sub_"+sname);
		//s.SetLogin("login", "pwd"); //Use this to change credentials from ECDC test account
		//s.useProductionServer(false); //Use this to change between test and production on EBI 
		//(Test submissions are deleted within 24 hours)
		
		//New project
		//Project p = new Project("ECDC","proj_"+sname);
		//s.addEntry(p);
		
		
		//New sample wrapper (includes sample, experiment and run)
		SampleWrapper wrap = new SampleWrapper("ECDC","PRJEB25143", sname,s);
		
		//Set minimal metadata
		wrap.sample.setAttribute("host_associated","yes");
		wrap.sample.setAttribute("specific_host","human");
		wrap.sample.setAttribute("host_disease_status","diseased");
		wrap.sample.setAttribute("collected_by","ECDC");
		wrap.sample.setAttribute("collection_date","2018");
		wrap.sample.setAttribute("country","Sweden");
		wrap.sample.setAttribute("serovar","unknown");
		wrap.sample.setTaxon("Listeria monocytogenes");
		wrap.experiment.setInstrument("Ion Torrent S5 XL");
		
		//Add a sequence file
		wrap.run.addFile(new File("C:/ENAtmp/raw/MI-90_18b.fastq.gz"));
		
		//Add entries in wrapper to submission
		s.addEntry(wrap);
		
		//Upload files and submit
		s.uploadFiles();
		s.submit();
		
		
		//Test output
		//System.out.println(p.getAccession());
		System.out.println(wrap.run.getAccession());
		System.out.println(wrap.sample.getAccession());
		System.out.println(wrap.experiment.getAccession());
		for (File f : wrap.run.getOriginalFiles()) {
			System.out.println(f.toString());
		}
			
	}

}
