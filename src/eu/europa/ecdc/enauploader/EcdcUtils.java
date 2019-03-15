package eu.europa.ecdc.enauploader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.zip.GZIPOutputStream;

import javax.swing.JTable;
import javax.swing.filechooser.FileNameExtensionFilter;

public class EcdcUtils {

	// This utility function returns a gzipped byte array of the uncompressed byte array input
	// Intended to be used for compressing FASTA before upload
	public static byte[] gzip(byte[] dataToCompress) {
		try
		{
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream(dataToCompress.length);
			try
			{
				GZIPOutputStream zipStream = new GZIPOutputStream(byteStream);
				try
				{
					zipStream.write(dataToCompress);
				} finally {
					zipStream.close();
				}
			} finally {
				byteStream.close();
			}

			byte[] compressedData = byteStream.toByteArray();
			return compressedData;
		} catch (Exception e) {
			return null;
		}
	}

	// This utility function compresses a file using GZIP
	// Intended to be used for compressing FASTQ before upload
	public static void compressGzipFile(File file, File gzipFile) {
		try {
			FileInputStream fis = new FileInputStream(file);
			FileOutputStream fos = new FileOutputStream(gzipFile);
			GZIPOutputStream gzipOS = new GZIPOutputStream(fos);
			byte[] buffer = new byte[1024];
			int len;
			while((len=fis.read(buffer)) != -1){
				gzipOS.write(buffer, 0, len);
			}
			gzipOS.close();
			fos.close();
			fis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	//This utility function calculates the data from a table containing the three different dates used by TESSy to calculate
	// DateUsedForStatistics
	public static String calcDate(JTable table, int row, String format) {
		String dateStr = "";
		int sampleDateCol = -1;
		int origlabDateCol = -1;
		int referenceLabCol = -1;
		for (int i = 0;i<table.getColumnCount();i++) {
			String name = table.getColumnName(i);
			if (name.equals("DateOfSampling")) {
				sampleDateCol = i;
			} else if (name.equals("DateOfReceiptSourceLab")) {
				origlabDateCol = i;
			} else if (name.equals("DateOfReceiptReferenceLab")) {
				referenceLabCol = i;
			}
		}
		SimpleDateFormat sourceFormat = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat targetFormat = new SimpleDateFormat(format);
		if (sampleDateCol!=-1 && table.getModel().getValueAt(row, sampleDateCol)!=null && ((String)table.getValueAt(row, sampleDateCol)).matches("[0-9][0-9][0-9][0-9][-][0-9][0-9][-][0-9][0-9]")) {
			try {
				Date d = sourceFormat.parse(((String)table.getModel().getValueAt(row, sampleDateCol)));
				dateStr = targetFormat.format(d);
				return dateStr;
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		if (origlabDateCol!=-1 && table.getModel().getValueAt(row, origlabDateCol)!=null && ((String)table.getModel().getValueAt(row, origlabDateCol)).matches("[0-9][0-9][0-9][0-9][-][0-9][0-9][-][0-9][0-9]")) {
			try {
				Date d = sourceFormat.parse(((String)table.getModel().getValueAt(row, origlabDateCol)));
				dateStr = targetFormat.format(d);
				return dateStr;
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		if (referenceLabCol!=-1 && table.getValueAt(row, referenceLabCol)!=null && ((String)table.getModel().getValueAt(row, referenceLabCol)).matches("[0-9][0-9][0-9][0-9][-][0-9][0-9][-][0-9][0-9]")) {
			try {
				Date d = sourceFormat.parse(((String)table.getModel().getValueAt(row, referenceLabCol)));
				dateStr = targetFormat.format(d);
				return dateStr;
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		return dateStr;
	}
	
	// This utility function recursively searches a file system for sequence files
		public static ArrayList<File> searchDev(File file, ArrayList<File> foundFiles) throws IOException {
			System.out.println("Reading directory: "+file.getName());

			/*FileFilter filter = new FileFilter() {
				@Override public boolean accept(File f)
				{
					String name = f.getName();
					return (name.endsWith(".fastq.gz") || name.endsWith(".fastq") || name.endsWith(".fasta") || name.endsWith(".fasta.gz") || f.isDirectory());
				}
			};*/

		
			System.out.println("Reading files");
			//File[] files = file.listFiles(filter);
		//	System.out.println("Iterating over "+Integer.toString(files.length)+" files");

			DirectoryStream.Filter<Path> filter = new DirectoryStream.Filter<Path>() {
                @Override
                public boolean accept(Path entry) throws IOException {
                String name = entry.getFileName().toString();
                return (name.endsWith(".fastq.gz") || name.endsWith(".fastq") || name.endsWith(".fasta") || name.endsWith(".fasta.gz") || Files.isDirectory(entry));
                  
                }
            }; 
			
		      Path dir = file.toPath();
		      DirectoryStream<Path> stream = Files.newDirectoryStream( dir , filter);
		      for (Path path : stream) {
		    	  File f = path.toFile();
		    	  if (f.isDirectory()) {
						 foundFiles = searchDev(f,foundFiles);
					 } else {
						 foundFiles.add(f);
					 }
		      }
		      stream.close();

			return foundFiles;
		}

	// This utility function recursively searches a file system for sequence files
	public static ArrayList<File> search(File file, ArrayList<File> foundFiles) throws IOException {
		System.out.println("Reading directory: "+file.getName());

		FileFilter filter = new FileFilter() {
			@Override public boolean accept(File f)
			{
				String name = f.getName();
				return (name.endsWith(".fastq.gz") || name.endsWith(".fastq") || name.endsWith(".fasta") || name.endsWith(".fasta.gz") || f.isDirectory());
			}
		};

	
		System.out.println("Reading files");
		File[] files = file.listFiles(filter);
		System.out.println("Iterating over "+Integer.toString(files.length)+" files");

		

		for (File f : files) {
			 if (f.isDirectory()) {
				 foundFiles = search(f,foundFiles);
			 } else {
				 foundFiles.add(f);
			 }
		}

		return foundFiles;
	}

}
