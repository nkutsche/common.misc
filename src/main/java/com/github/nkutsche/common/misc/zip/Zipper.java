package com.github.nkutsche.common.misc.zip;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class Zipper {
	private static byte[] readFile(File file) {
		BufferedInputStream bis = null;
		byte[] buffer = null;
		try {
			bis = new BufferedInputStream(new FileInputStream(file));
			int avail = bis.available();
			buffer = new byte[avail];
			if (avail > 0) {
				bis.read(buffer, 0, avail);
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			try {
				if (bis != null)
					bis.close();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		return buffer;
	}

	private static void writeZipEntry(RelativeFile file, ZipOutputStream zipOut)
			throws IOException {
		byte[] buffer = readFile(file.absFile.getAbsoluteFile());
		
		if(file.relFile == null){
			throw new IOException("Relative Path is not available for " + file.absFile.getAbsolutePath());
		} 
		ZipEntry ze = new ZipEntry(file.relFile.getPath());
		zipOut.putNextEntry(ze);
		zipOut.write(buffer, 0, buffer.length);
		zipOut.closeEntry();
	}

	private static ZipOutputStream writeEmptyZip(File zipfile) {
		ZipOutputStream zipOut = null;
		zipfile.getParentFile().mkdirs();
		try {
			zipOut = new ZipOutputStream(new FileOutputStream(zipfile));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return zipOut;
	}
	
	protected static ArrayList<RelativeFile> getAllChilds(File parent) {
		String[] childNames = new File(parent.getPath()).list();
		return getAllFiles(childNames, parent, "");
	}
	
	protected static ArrayList<RelativeFile> getAllFiles(File parent) {
		String[] childNames = new File(parent.getPath()).list();
		return getAllFiles(childNames, parent, parent.getName() + "/");
	}

	protected static ArrayList<RelativeFile> getAllFiles(String[] childNames,
			File parent, String path) {

		ArrayList<RelativeFile> files = new ArrayList<RelativeFile>();

		if (childNames != null) {
			for (int i = 0; i < childNames.length; i++) {
				File absChild = new File(parent.getAbsolutePath() + "/"
						+ childNames[i]);
				RelativeFile child = new RelativeFile(absChild, new File(path
						+ childNames[i] + "/"));
				files.add(child);
				ArrayList<RelativeFile> grandChild = getAllFiles(
						absChild.list(), absChild, path + childNames[i] + "/");
				files.addAll(grandChild);
			}
		}
		return files;
	}

	public static class RelativeFile {
		public File absFile;
		public File relFile;

		public RelativeFile(File absFile, File relFile) {
			this.absFile = absFile;
			this.relFile = relFile;
		}
	}
	
	private static ArrayList<RelativeFile> getAllFiles(File baseFolder, String[] files){
		ArrayList<RelativeFile> list = new ArrayList<Zipper.RelativeFile>();
		for (int i = 0; i < files.length; i++) {
			
			String path = files[i];
			File file = new File(baseFolder, path);
			if(file.isDirectory()){
				String[] childNames = new File(file.getPath()).list();
				list.addAll(getAllFiles(childNames, file, path + "/"));
			} else {
				list.add(new RelativeFile(file.getAbsoluteFile(), new File(path)));
			}
			
			
		}
		return list;
	}
	
	public static void zipFiles(String folderPath, String[] subfiles, String zipPath){
		
		File folder;
		ZipOutputStream zip = null;
		
		try {
			folder = new File(new URI(folderPath));
			ArrayList<RelativeFile> files = getAllFiles(folder, subfiles);
			
			zip = writeEmptyZip(new File(new URI(zipPath)));
			for (Iterator<RelativeFile> iterator = files.iterator(); iterator
					.hasNext();) {
				RelativeFile file = iterator.next();
				if (!file.absFile.getAbsoluteFile().isDirectory())
					writeZipEntry(file, zip);
			}
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (zip != null)
					zip.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void zipFolder(String folderPath, String zipPath) {
		zipFolder(folderPath, zipPath, true);
	}
	
	
	
	public static void zipFolder(String folderPath, String zipPath, boolean rootInclude) {
		File folder;
		ZipOutputStream zip = null;
		try {
			folder = new File(new URI(folderPath));
			ArrayList<RelativeFile> files = rootInclude ? getAllFiles(folder) : getAllChilds(folder);
			zip = writeEmptyZip(new File(new URI(zipPath)));
			for (Iterator<RelativeFile> iterator = files.iterator(); iterator
					.hasNext();) {
				RelativeFile file = iterator.next();
				if (!file.absFile.getAbsoluteFile().isDirectory())
					writeZipEntry(file, zip);
			}
		} catch (URISyntaxException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				if (zip != null)
					zip.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static void unzip(String zipPath, String entry, File file) throws URISyntaxException, IOException {
		byte[] buffer = new byte[1024];
		ZipInputStream zis = null;
		File zipFile = new File(new URI(zipPath));
		zis = new ZipInputStream(new FileInputStream(zipFile));

		ZipEntry ze = zis.getNextEntry();

		while (ze != null) {

			String fileName = ze.getName();
			System.out.println(entry.replace('/', File.separatorChar));
			if(fileName.equals(entry.replace('/', File.separatorChar))){
				System.out.println(fileName);
				// create all non exists folders
				// else you will hit FileNotFoundException for compressed folder
				new File(file.getParent()).mkdirs();
				
				FileOutputStream fos = new FileOutputStream(file);
				
				int len;
				while ((len = zis.read(buffer)) > 0) {
					fos.write(buffer, 0, len);
				}
				
				fos.close();
				break;
			}
			ze = zis.getNextEntry();
		}

		zis.closeEntry();
		zis.close();
	}

	public static void unzip(String zipPath, File folder) throws URISyntaxException, IOException {
		unzip(new File(zipPath), folder);
	}
	
	public static void unzip(File zipPath, File folder) throws URISyntaxException, IOException {
		byte[] buffer = new byte[1024];
		ZipInputStream zis = null;
		File zipFile = zipPath.getAbsoluteFile();
		zis = new ZipInputStream(new FileInputStream(zipFile));

		ZipEntry ze = zis.getNextEntry();
		
//		System.out.println("in:  " + zipPath.getAbsolutePath());
//		System.out.println("out: " + folder.getAbsolutePath());
		while (ze != null) {

			String fileName = ze.getName();
			File newFile = new File(folder + File.separator + fileName);
//			System.out.println(newFile.getAbsolutePath());
			// create all non exists folders
			// else you will hit FileNotFoundException for compressed folder
			new File(newFile.getParent()).mkdirs();

			FileOutputStream fos = new FileOutputStream(newFile);

			int len;
			while ((len = zis.read(buffer)) > 0) {
				fos.write(buffer, 0, len);
			}

			fos.close();
			ze = zis.getNextEntry();
		}

		zis.closeEntry();
		zis.close();
	}

	public static void unzip(String zipPath, String output) {
		File outputFile;
		try {
			outputFile = new File(new URI(output));
			if(zipPath.contains("!")){
				String[] paths = zipPath.split("!");
				unzip(paths[0], paths[1], outputFile);
			} else {
				unzip(zipPath, outputFile);
			}
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	
	
}
