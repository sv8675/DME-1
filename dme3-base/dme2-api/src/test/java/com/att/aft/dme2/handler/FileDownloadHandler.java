/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.handler;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Map;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.ErrorContext;




public class FileDownloadHandler implements AsyncResponseHandlerIntf {
	/** The waiter. */
	private byte[] waiter = new byte[0];
	private String fileName;
	private String service;
	private String response = null;
	 private static Logger logger = LoggerFactory.getLogger(FileDownloadHandler.class.getName());
	 
	/** The e. */
	private Throwable e = null;
	
	public FileDownloadHandler(String fileName, String service) {
		this.fileName= fileName;
		this.service = service;
	}
	
	 @Override
	  public void handleException(Map<String, String> requestHeaders, Throwable e) {
	    this.e = e;
	    synchronized (waiter) {
	      waiter.notify();
	    }
	  }
	
	/**
	 * Gets the response.
	 * 
	 * @param timeoutMs
	 *            the timeout ms
	 * @return the response
	 * @throws Exception
	 *             the exception
	 */
	public String getResponse(long timeoutMs) throws Exception {
	    long start = System.currentTimeMillis();
	    synchronized (waiter) {
	      if (response != null) {
	        return response;
	      } else if (e != null) {
	        if (!(e instanceof Exception)) {
	          throw new DME2Exception(DME2Constants.EXP_CORE_AFT_DME2_0998, new ErrorContext().add("service",service), e);
	        } else if (e instanceof DME2Exception) {
	          throw new DME2Exception(((DME2Exception) e).getErrorCode(), ((DME2Exception) e).getErrorMessage(), e);
	        } else {
	          throw new DME2Exception(DME2Constants.EXP_CORE_AFT_DME2_0998, new ErrorContext().add("service",service), e);
	        }
	      }
	      try {
	        waiter.wait(timeoutMs);
	      } catch(InterruptedException ie) {
	        long elapsed = System.currentTimeMillis() - start;
	        logger.debug( null, "getResponse", "DME2FileDownloadHandler interruptedException. ElapsedTime={}; timeoutMs={}", elapsed, timeoutMs);
	        if(elapsed < timeoutMs) {
	          waiter.wait(timeoutMs-elapsed);
	        }
	      }
	      if (response != null) {
	        return response;
	      } else if (e != null) {
	        if (!(e instanceof Exception)) {
	          throw new RuntimeException(e);
	        } else {
	          throw (Exception) e;
	        }
	      }
	      throw new DME2Exception(DME2Constants.EXP_CORE_AFT_DME2_0998, new ErrorContext()
	          .add("service",service).add("timeoutMs",timeoutMs+""), new Exception(DME2Constants.EXP_CORE_AFT_SERVICE_CALL_TIMEDOUT));
	    }
	  }

	private String findCharSet(Map<String,String> parameterMap) {
		String charset = (String)parameterMap.get("Content-Type");
		if (charset == null) {
			charset = (String)parameterMap.get("content-type");
		} 
		if (charset != null) {
			String[] toks = charset.split(";");
			if (toks.length > 1) {
				charset = toks[1];
				String[] toks2 = toks[1].split("=");
				if (toks2.length > 1) {
					charset = toks2[1];
				} else {
					charset = null;
				}
			} else {
				charset = null;
			}
		}
		return charset;
	}
	
	@Override
	public void handleReply(int responseCode, String responseMessage,
			InputStream in, Map<String, String> requestHeaders,
			Map<String, String> responseHeaders) {
		InputStreamReader reader = null;
		try {
			String charset = findCharSet(responseHeaders);
			final byte[] buffer = new byte[8096];
			StringBuilder inputText = new StringBuilder(8096);
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			int n = -1;
			while ((n = in.read(buffer,0,buffer.length)) != -1) {
				//inputText.append(buffer, 0, n);
				bos.write(buffer);
			}
			OutputStream os = new FileOutputStream(this.fileName);
			bos.writeTo(os);
			bos.flush();
			bos.close();
			response = "true";
		} catch (IOException e) {
			e.printStackTrace();
			this.e = new Exception("UNABLE TO READ RESPONSE MESSAGE");
			this.e.initCause(e);
			return;
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException e) {

			}
		}
		synchronized (waiter) {
			waiter.notify();
		}
	}

	
}
