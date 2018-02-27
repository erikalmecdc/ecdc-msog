package eu.europa.ecdc.enauploader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

public class SubmissionFactory {
	

	private static final String[] MANDATORY_FIELDS = {"collected_by","collection_date","country","taxon","instrument"};


	public static Submission createSubmissionFromCSV(String center, String projectId, File csvFile, File dataDir,String login,String pass, String prod) {
		return createSubmissionFromCSV(center, projectId, csvFile, dataDir,login, pass, prod, ",");
	}


	public static Submission createSubmissionFromCSV(String center, String projectId, File csvFile, File dataDir,String login,String pass, String prod, String separator) {

		File csvFileOut = new File(csvFile.toPath()+".out.csv");
		ENAUtils.init();
		UUID uuid = UUID.randomUUID();
		String randomUUIDString = uuid.toString();
		String sname = center+"_"+randomUUIDString;

		Project p = null;
		//Create new submission
		Submission s = new Submission(center,"sub_"+sname);

		if (prod.equals("yes")) {
			s.useProductionServer(true);
		}
		if (!login.equals("") && !pass.equals("")) {
			s.SetLogin(login, pass);
		}


		if (projectId == null || projectId.equals("")) {
			projectId = "proj_"+sname;
			p = new Project(center,projectId);
			s.addEntry(p);
		}

		HashMap<String,String> csvRows= new HashMap<String,String>();

		HashMap<String,String> uuIds = new HashMap<String,String>();
		HashMap<String,SampleWrapper> wrappers = new HashMap<String,SampleWrapper>();

		//Open csv and check that the bare minimum columns exist, add additional columns to metadata-array
		try {


			BufferedReader br = new BufferedReader(new FileReader(csvFile));

			File[] dataFiles = dataDir.listFiles();

			String line = br.readLine();
			String[] fields = line.split(separator);
			HashMap<String,Integer> columnIndex = new HashMap<String,Integer>();
			String csvHeader = line;

			for (int i = 1;i<fields.length;i++) {
				String field = fields[i];
				columnIndex.put(field,i);
			}
			if (!check(columnIndex)) {
				return null;
			}
			int taxonColumn = columnIndex.get("taxon");
			columnIndex.remove("taxon");
			int instrumentColumn = columnIndex.get("instrument");
			columnIndex.remove("instrument");
			int fileIndex = -1;

			if (columnIndex.containsKey("files")) {
				fileIndex = columnIndex.get("files");
			}

			while ((line=br.readLine())!=null) {
				String[] lfields = line.split(separator);
				if (lfields.length<fields.length) {
					continue;
				}
				String id = lfields[0];
				csvRows.put(id, line);
				String rUUIDString = UUID.randomUUID().toString();
				uuIds.put(id,rUUIDString);
				String instrument = lfields[instrumentColumn];
				String taxon = lfields[taxonColumn];
				SampleWrapper wrap = new SampleWrapper(center,projectId, rUUIDString);
				wrap.experiment.setInstrument(instrument);
				wrap.sample.setTaxon(taxon);
				wrap.sample.setAttribute("host_associated","yes");
				wrap.sample.setAttribute("specific_host","human");
				wrap.sample.setAttribute("host_disease_status","diseased");
				wrap.sample.setAttribute("serovar","unknown");
				for (String k : columnIndex.keySet()) {
					wrap.sample.setAttribute(k,lfields[columnIndex.get(k)]);
				}

				int foundFiles = 0;
				for (File f : dataFiles) {
					if (!f.getName().toLowerCase().matches(".*fastq.*")) {
						continue;
					}
					boolean foundFile = false;
					if (fileIndex==-1) {
						if (f.getName().toLowerCase().startsWith(id.toLowerCase())) {
							foundFiles++;
						}
					} else {
						if (f.getName().toLowerCase().startsWith(lfields[fileIndex].toLowerCase())) {
							foundFile = true;
						}
					}
					if (foundFile) {
						System.out.println(id+"\tAdding file: "+f.getName());
						wrap.run.addFile(f);
						foundFiles++;
					}
				}
				if (foundFiles==0) {
					System.out.println("NO FILES FOUND FOR: "+id);

					continue;
				}
				wrappers.put(id,wrap);
				s.addEntry(wrap);
			}

			br.close();

			s.uploadFiles();
			if (s.submit()) {
				System.out.println("Submission complete.");
			} else {
				System.out.println("ERROR: Submission failed. Aborting.");
				return null;
			}



			BufferedWriter bw = new BufferedWriter(new FileWriter(csvFileOut));
			if (p!=null) {
				bw.write("Created project"+separator+p.getAccession()+"\n");
			} else {
				bw.write("Existing project"+separator+projectId+"\n");
			}

			csvHeader = csvHeader+separator+"submission UUID"+separator+"Sample acc"+separator+"Experiment acc"+separator+"Run acc"+separator+"Files uploaded"; 
			bw.write(csvHeader+"\n");
			for (String id : csvRows.keySet()) {
				String uui = uuIds.get(id);
				String csvRow = csvRows.get(id);
				if (wrappers.containsKey(id)) {
					SampleWrapper wrap = wrappers.get(id);
					String sampleAcc = wrap.getSample().getAccession();
					String experimentAcc = wrap.getExperiment().getAccession();
					String runAcc = wrap.getRun().getAccession();
					csvRow = csvRow + separator + uui + separator + sampleAcc + separator + experimentAcc + separator + runAcc;
					ArrayList<File> origFiles = wrap.getRun().getOriginalFiles();
					for (File f : origFiles) {
						csvRow = csvRow +separator+ f.getName();
					}
				} else {
					csvRow = csvRow + separator + "Fail" + separator + "Fail" + separator + "Fail" + separator + "Fail"+separator+"Fail";
				}
				bw.write(csvRow+"\n");
			}

			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}







		return s;
	}


	private static boolean check(HashMap<String, Integer> columnIndex) {


		for (String k : MANDATORY_FIELDS) {
			if (!columnIndex.containsKey(k)) {
				System.out.println("In CSV-file, missing field "+k+", aborting submission.");
				return false;
			}
		}

		return true;
	}


}
