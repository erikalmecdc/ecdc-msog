package eu.europa.ecdc.enauploader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Experiment extends DatabaseEntity {

	private String studyAccession;
	private String sampleAccession;

	private String pairedDistance;
	private String pairedDistanceSdev;
	
	private String instrument;

	Experiment(String c, String a, String studyAcc, String sampleAcc, Submission s) {
		super(c, a, s);
		
		pairedDistance = "500";
		pairedDistanceSdev = "200.0";
		
		
		type = "EXPERIMENT";
		studyAccession = studyAcc;
		sampleAccession = sampleAcc;
	}

	public void setInstrument(String instr) {
		instrument = instr;
	}
	
	@Override
	public void writeXml(File f) {
		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new FileWriter(f));
			bw.write("<?xml version = '1.0' encoding = 'UTF-8'?>\n");
			bw.write("<EXPERIMENT_SET xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"ftp://ftp.sra.ebi.ac.uk/meta/xsd/sra_1_5/SRA.experiment.xsd\">\n");
			bw.write("<EXPERIMENT alias=\""+alias+"\" center_name=\""+centerName+"\">\n");
			bw.write("<TITLE>"+title+"</TITLE>\n");
			if (studyAccession.startsWith("PRJ")) {
				bw.write("<STUDY_REF accession=\""+studyAccession+"\"/>\n");
			} else {
				bw.write("<STUDY_REF refname=\""+studyAccession+"\"/>\n");
			}
			
			bw.write("<DESIGN>\n");
			bw.write("<DESIGN_DESCRIPTION>Whole genome sequencing of pathogens</DESIGN_DESCRIPTION>\n");
			bw.write("<SAMPLE_DESCRIPTOR refname=\""+sampleAccession+"\"/>\n");
			bw.write("<LIBRARY_DESCRIPTOR>\n");
			bw.write("<LIBRARY_NAME>"+alias+"</LIBRARY_NAME>\n");
			bw.write("<LIBRARY_STRATEGY>WGS</LIBRARY_STRATEGY>\n");
			bw.write("<LIBRARY_SOURCE>GENOMIC</LIBRARY_SOURCE>\n");
			bw.write("<LIBRARY_SELECTION>RANDOM</LIBRARY_SELECTION>\n");
			bw.write("<LIBRARY_LAYOUT>\n");
			if (instrument.toLowerCase().matches(".*illumina.*")) {
				bw.write("<PAIRED NOMINAL_LENGTH=\""+pairedDistance+"\" NOMINAL_SDEV=\""+pairedDistanceSdev+"\"/>\n");
			} else {
				bw.write("<SINGLE/>\n");
			}
			bw.write("</LIBRARY_LAYOUT>\n");
			bw.write("<LIBRARY_CONSTRUCTION_PROTOCOL>unknown</LIBRARY_CONSTRUCTION_PROTOCOL>\n");
			bw.write("</LIBRARY_DESCRIPTOR>\n");
			bw.write("</DESIGN>\n");
			bw.write("<PLATFORM>\n");
			if (instrument.toLowerCase().matches(".*illumina.*")||instrument.toLowerCase().matches(".*nextseq.*")||instrument.toLowerCase().matches(".*hiseq.*")) {
				bw.write("<ILLUMINA>\n");
				bw.write("<INSTRUMENT_MODEL>"+instrument+"</INSTRUMENT_MODEL>\n");
				bw.write("</ILLUMINA>\n");	
			} else if(instrument.toLowerCase().matches(".*ion torrent.*")) {
				bw.write("<ION_TORRENT>\n");
				bw.write("<INSTRUMENT_MODEL>"+instrument+"</INSTRUMENT_MODEL>\n");
				bw.write("</ION_TORRENT>\n");	
			} else if(instrument.toLowerCase().matches(".*pac bio.*") || instrument.toLowerCase().matches(".*sequel.*")) {
				bw.write("<PACBIO_SMRT>\n");
				bw.write("<INSTRUMENT_MODEL>"+instrument+"</INSTRUMENT_MODEL>\n");
				bw.write("</PACBIO_SMRT>\n");	
			}
			bw.write("</PLATFORM>\n");
			bw.write("</EXPERIMENT>\n");
			bw.write("</EXPERIMENT_SET>\n");
			bw.close();
		} catch (IOException e) {
		
			e.printStackTrace();
		}
	}

	
	@Override
	public String getSubmitRow() {
		return "<ADD schema=\"experiment\" source=\""+xmlFile.toString()+"\"/>";
	}

}
