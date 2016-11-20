/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test.jms.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ProcessStreamReader  
{
	private static final String CLASS_NAME = ProcessStreamReader.class.getName();
	
	private String streamType;
	private InputStream stream;
	private StringBuffer buffer = new StringBuffer();
	
	public ProcessStreamReader()
	{
		
	}
	
	public ProcessStreamReader(String streamType, InputStream stream)
	{
		this.streamType = streamType;
		this.stream = stream;
	}
	
	public void setStreamType(String streamType)
	{
		this.streamType = streamType;
	}
	
	public String getStreamType()
	{
		return this.streamType;
	}
	
	public void setInputStream(InputStream stream)
	{
		this.stream = stream;
	}
	
	public InputStream getInputStream()
	{
		return this.stream;
	}
	
	public String getStreamContents()
	{
		return buffer.toString();
	}
	
	public boolean startProcessing()
	{
		Thread t = new Thread(new StreamReader());
		t.start();
		return true;
	}

	/*
	 * Inner class to read process input stream...
	 */
	class StreamReader implements Runnable
	{
		private final String CLASS_NAME = StreamReader.class.getName();
		
		public void run()
		{
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
			String line = null;
		
			try 
			{
				while( (line = reader.readLine()) != null)
				{
					//logger.logp(Level.INFO, CLASS_NAME, "run", "Stream Type=" + streamType + ", line=" + line);
					buffer.append(line);
					buffer.append("\r\n");
					System.out.println(line);
					line = null;
				}
			} 
			catch (Exception e) 
			{
				throw new RuntimeException(e);
			}
			finally
			{
				try 
				{
					if(reader != null)
						reader.close();
				} catch (IOException e) { e.printStackTrace(); }
			}
		}// end of run.
	}// end of StreamReader inner class...
}
 