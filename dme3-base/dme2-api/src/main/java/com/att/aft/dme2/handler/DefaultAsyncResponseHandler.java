package com.att.aft.dme2.handler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.ErrorContext;

public class DefaultAsyncResponseHandler implements AsyncResponseHandlerIntf {

  private static Logger logger = LoggerFactory.getLogger(DefaultAsyncResponseHandler.class.getName());
  private DME2Configuration config;
  private Throwable e = null;

  private String response = null;

  private final byte[] waiter = new byte[0];

  private final String service;

  private boolean allowAllHttpReturnCodes = false;

  public DefaultAsyncResponseHandler(String service) {
    this.service = service;
  }

  public DefaultAsyncResponseHandler(DME2Configuration config, String service, boolean allowAllHttpReturnCodes) {
	this.config = config;
    this.service = service;
    this.allowAllHttpReturnCodes = allowAllHttpReturnCodes;
  }

  /**
   *
   * @param timeoutMs
   * @return
   * @throws Exception
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
        logger.debug(null, "getResponse", "DefaultAsyncResponseHandler interruptedException. ElapsedTime={}; timeoutMs={}", elapsed, timeoutMs);
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
    String charset = parameterMap.get("Content-Type");
    if (charset == null) {
      charset = parameterMap.get("content-type");
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
  public void handleException(Map<String, String> requestHeaders, Throwable e) {
    this.e = e;
    synchronized (waiter) {
      waiter.notify();
    }
  }

  @Override
  public void handleReply(int responseCode, String responseMessage, InputStream in, Map<String, String> requestHeaders, Map<String, String> responseHeaders) {
		GZIPInputStream gis = null;
		if (allowAllHttpReturnCodes || responseCode == 200) {
			if (responseHeaders != null && responseHeaders.get(config.getProperty(DME2Constants.AFT_DME2_CONTENT_ENCODING_KEY)) != null && config.getBoolean(DME2Constants.AFT_DME2_ALLOW_COMPRESS_ENCODING)) {
				if (responseHeaders.get(config.getProperty(DME2Constants.AFT_DME2_CONTENT_ENCODING_KEY)).equalsIgnoreCase(config.getProperty(DME2Constants.AFT_DME2_COMPRESS_ENCODING))) {
					try {
						gis = new GZIPInputStream(in);
					} catch (Exception e) {
						this.e = new Exception(DME2Constants.EXP_CORE_AFT_CONTENT_ENCODING);
						this.e.initCause(e);
						return;
					}
				}
			}
			BufferedReader reader = null;
			try {
				String charset = findCharSet(responseHeaders);
				if (charset != null) {
					if(gis == null){
						reader = new BufferedReader(new InputStreamReader(in, charset));
					}
					else{
						reader = new BufferedReader(new InputStreamReader(gis, charset));
					}
				} else {
					if(gis == null){
						reader = new BufferedReader(new InputStreamReader(in));
					}
					else{
						reader = new BufferedReader(new InputStreamReader(gis));
					}
				}
				final char[] buffer = new char[8096];
				StringBuilder inputText = new StringBuilder(8096);
				int n = -1;
				while ((n = reader.read(buffer)) != -1) {
					inputText.append(buffer, 0, n);
				}
				response = inputText.toString();
			} catch(IOException e) {
				this.e = new Exception(DME2Constants.EXP_CORE_AFT_UNABLE_READ_RES);
				this.e.initCause(e);
				return;
			} finally {
				try {
					if (reader != null) {
						reader.close();
					}
				} catch (IOException e) {
					logger.warn( null, "handleReply", "IOException: ", e);
				}
			}
			
		} else {
			if(responseCode == 500 && responseMessage != null ) {
				response = responseMessage;
			}
			else {
				e = new Exception("Call Failed, RC=" + responseCode + " - "	+ responseMessage);
			}
		}
		synchronized (waiter) {
			waiter.notify();
		}		
	}
  
}