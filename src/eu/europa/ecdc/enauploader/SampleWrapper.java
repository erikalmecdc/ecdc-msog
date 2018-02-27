package eu.europa.ecdc.enauploader;
import java.util.ArrayList;

public class SampleWrapper {

	
	
	public Sample sample;
	public Experiment experiment;
	public Run run;
	
	
	public Sample getSample() {
		return sample;
	}
	public Run getRun() {
		return run;
	}
	public Experiment getExperiment() {
		return experiment;
	}
	
	
	SampleWrapper(String center, String project, String basename) {
		String expName = "exp_"+basename;
		String sampleName = "sam_"+basename;
		String runName = "run_"+basename;
		
		sample = new Sample(center,sampleName);
		experiment = new Experiment(center,expName,project,sampleName);
		run = new Run(center,runName,expName);
	}
	

}