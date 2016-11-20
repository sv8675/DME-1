/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.server.test;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;


public class EchoFileServlet extends HttpServlet
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private boolean isMultipart;
	private String filePath = "c:\\temp/fileupload/";
	private int maxFileSize = 1500 * 1024;
	private int maxMemSize = 1500 * 1024;
	private File file;

	
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
			java.io.IOException
	{
		
		// Check that we have a file upload request
		isMultipart = ServletFileUpload.isMultipartContent(request);
		System.out.println("isMultiPart=" + isMultipart);
		// response.setContentType("text/html");
		
		String returnFile = request.getHeader("testReturnFile");
		if (returnFile != null)
		{
			this.writeFile(response.getOutputStream());
			return;
		}
		
		java.io.PrintWriter out = response.getWriter();
		if (!isMultipart)
		{
			out.println("No file uploaded");
			out.flush();
			return;
		}
		
		
		
		DiskFileItemFactory factory = new DiskFileItemFactory();
		// maximum size that will be stored in memory
		factory.setSizeThreshold(maxMemSize);
		// Location to save data that is larger than maxMemSize.
		filePath = System.getProperty("java.io.tmpdir") + File.separator;
		factory.setRepository(new File(filePath));
		// Create a new file upload handler
		ServletFileUpload upload = new ServletFileUpload(factory);
		// maximum file size to be uploaded.
		upload.setSizeMax(maxFileSize);
		try
		{
			// Parse the request to get file items.
			List fileItems = upload.parseRequest(request);
			// Process the uploaded file items
			Iterator i = fileItems.iterator();

			// out.println("<html>");
			// out.println("<head>");
			// out.println("<title>Servlet upload</title>");
			// out.println("</head>");
			// out.println("<body>");

			while (i.hasNext())
			{
				FileItem fi = (FileItem) i.next();
				if (!fi.isFormField())
				{

					// Get the uploaded file parameters
					String fieldName = fi.getFieldName();
					String fileName = fi.getName();
					String contentType = fi.getContentType();
					boolean isInMemory = fi.isInMemory();
					long sizeInBytes = fi.getSize();
					// Write the file
					if (fileName.lastIndexOf("\\") >= 0)
					{
						file = new File(filePath + fileName.substring(fileName.lastIndexOf("\\")));
					}
					else
					{
						file = new File(filePath + fileName.substring(fileName.lastIndexOf("\\") + 1));
					}
					fi.write(file);
					System.out.println("File location" + filePath);
					out.println("Uploaded Filename: " + fileName + "<br>"+" and FieldName:"+fieldName);
				}
			}
			// out.println("</body>");
			// out.println("</html>");
			out.flush();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			System.out.println(ex);
		}
	}

	private void writeFile(java.io.OutputStream out) throws IOException
	{
		FileInputStream input = new FileInputStream("src/test/etc/CSPConfigureWebApp.jar");
		FileChannel channel = input.getChannel();
		byte[] buffer = new byte[256 * 1024];
		ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);

		try
		{
			for (int length = 0; (length = channel.read(byteBuffer)) != -1;)
			{
				out.write(buffer, 0, length);
				byteBuffer.clear();
			}
			out.flush();
		}
		finally
		{
			input.close();
		}
	}
	
	public String bytesToString(byte[] _bytes)
	{
	    String file_string = "";

	    for(int i = 0; i < _bytes.length; i++)
	    {
	        file_string += (char)_bytes[i];
	    }

	    return file_string;    
	}
}
