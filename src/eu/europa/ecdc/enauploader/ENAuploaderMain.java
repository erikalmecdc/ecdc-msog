package eu.europa.ecdc.enauploader;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

public class ENAuploaderMain {

	// This is a command line version
	public static void main2(String[] args) {
		//args: 
		//Center
		//Project id ("" means create new project) "PRJEB25143"
		//CSV file path
		//Data directory path
		// Login (Webin-NNNNN)
		// Password
		// yes/no whether to use the production database at EBI
		
		if (args.length < 4) {
			return;
		}
		
		String center = args[0];
		String project = args[1];
		File csvFile = new File(args[2]);
		File dataDir = new File(args[3]);
		
		String login = "";
		String pass = "";
		String prod = "no";
		if (args.length > 5) {
			login = args[4];
			pass = args[5];
		}
		
		if (args.length > 6) {
			prod = args[6];
		}
		
		SubmissionFactory.createSubmissionFromCSV(center,project, csvFile,dataDir, login, pass, prod);
	}
	
	
	//Example code for use of the library, rename to main to run it
	public static void main(String[] args) {
		
		//Init tmp folder
		ENAUtils.init();
		
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
		SampleWrapper wrap = new SampleWrapper("ECDC","PRJEB25143", sname);
		
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
