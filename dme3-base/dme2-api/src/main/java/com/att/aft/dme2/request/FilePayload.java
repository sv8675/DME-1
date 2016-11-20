package com.att.aft.dme2.request;

import java.util.ArrayList;
import java.util.List;

/**
 * DME2 Payload object that holds file input
 */
public class FilePayload extends DME2Payload {	
	private List<String> multipartFileNames;
	private boolean multipart;
	private boolean binary;
	private String fileName;
	private String multiPartFileName = "upload_file";
	
	public FilePayload(String fileName, boolean multipart, boolean binary) {
		
		this.multipartFileNames = new ArrayList<String>();
		this.multipartFileNames.add(fileName);
		this.fileName = fileName;		
		this.multipart = multipart;
		this.binary = binary;
	}
	
	public FilePayload(String fileName, boolean multipart, boolean binary, String multiPartFileName) {
		this.multipartFileNames = new ArrayList<String>();
		this.multipartFileNames.add(fileName);
		this.fileName = fileName;		
		this.multipart = multipart;
		this.binary = binary;
		this.multiPartFileName = multiPartFileName;
	}
	
	public FilePayload(String fileName, List<String> multipartFileNamesWithPaths, boolean multipart, boolean binary) {
		this.multipartFileNames = new ArrayList<String>();
		this.multipartFileNames.addAll(multipartFileNamesWithPaths);
		this.multipart = multipart;
		this.binary = binary;
		this.fileName = fileName;
	}
	
	public FilePayload(List<String> multipartFileNamesWithPaths, boolean multipart, boolean binary, String multiPartFileName) {
		this.multipartFileNames = new ArrayList<String>();
		this.multipartFileNames.addAll(multipartFileNamesWithPaths);
		this.multipart = multipart;
		this.binary = binary;
		this.fileName = multipartFileNamesWithPaths.get(0);
		this.multiPartFileName = multiPartFileName;
	}
	
	
	public FilePayload(List<String> multipartFileNamesWithPaths, boolean multipart, boolean binary) {
		this.multipartFileNames = new ArrayList<String>();
		this.multipartFileNames.addAll(multipartFileNamesWithPaths);
		this.multipart = multipart;
		this.binary = binary;
		this.fileName = multipartFileNamesWithPaths.get(0);
	}

	public List<String> getMultipartFileNamesWithPaths() {
		return this.multipartFileNames;
	}
	
	public boolean isBinaryFile(){
		return this.binary;
	}

	public boolean isMultipartPayload(){
		return this.multipart;
	}

	public String getFileName() {
		return this.fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public boolean isMultipart() {
		return this.multipart;
	}

	public void setMultipart(boolean multipart) {
		this.multipart = multipart;
	}

	public String getMultiPartFileName() {
		return multiPartFileName;
	}

	public void setMultiPartFileName(String multiPartFileName) {
		this.multiPartFileName = multiPartFileName;
	}
}

