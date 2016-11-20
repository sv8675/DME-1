/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api.quick;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import com.att.aft.dme2.api.DME2Client;
import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.handler.DME2SimpleReplyHandler;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.request.DME2TextPayload;
import com.att.aft.dme2.request.HttpRequest.RequestBuilder;
import com.att.aft.dme2.request.Request;

public class QuickClient {
	private static final String USAGE = "usage: java com.att.aft.dme2.quick.QuickClient -t <timeout to wait for response> -s <service> { -m <message> | -f <inputfile> } [ -o <outputfile> ]";
  private static final Logger logger = LoggerFactory.getLogger( QuickClient.class );
	
	public static void main(String[] args) throws DME2Exception, InterruptedException {
		String service = null;
		String message = null;
		String file = null;
		String outFile = null;
		String timeoutStr = null;
		int loops = 1;
		boolean verbose = false;
		Long sleep = null;
		
		if (args.length == 0) {
			System.err.println(USAGE);
			System.exit(1);
		}
		
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (arg.equals("-s")) {
				service = args[i+1];
			} else if (arg.equals("-m")) {
				message = args[i+1];
			} else if (arg.equals("-f")) {
				file = args[i+1];
			} else if (arg.equals("-o")) {
				outFile = args[i+1];
			} else if (arg.equals("-t")) {
				timeoutStr = args[i+1];
			} else if (arg.equals("-l")) {
				loops = Integer.parseInt(args[i+1]);
			} else if (arg.equals("-v")) {
				verbose = true;
			} else if (arg.equals("-sleep")) {
				sleep = Long.parseLong(args[i+1]);
			} else if (arg.equals("-?")) {
				System.err.println(USAGE);
				System.exit(0);
			}
		}
		
		if (service == null) {
			fatal("-s <service> required", 1);
		}
		
		if (message == null && file == null) {
			fatal("-m <message> OR -f <inputfile> required", 2);
		}
		
		if (message != null && file != null) {
			fatal("Either -m <message> OR -f <inputfile> required, but both are not allowed", 3);
		}
		
		if (file != null) {
			File infile = new File(file);
			if (!infile.exists()) {
				fatal("Input file [" + file + "] does not exist", 4);
			}
			
			if (!infile.canRead()) {
				fatal("Input file [" + file + "] is not readable", 5);
			}
			
			message = new String(read(file));
		}

		logger.debug( null, "main", "sleep={}", sleep);
		long timeout = Long.parseLong(timeoutStr);
		long start = System.currentTimeMillis();
		Long firstRequest =null; 
		int successes = 0;
		int timeouts = 0;
		int fails = 0;
		for (int i = 0; i < loops; i++) {
			try {
				long start2 = System.currentTimeMillis();
				DME2Configuration config = new DME2Configuration("QuickClient");
				DME2Manager manager = new DME2Manager("QuickClient", config);

				Request request = new RequestBuilder(new URI(service)).withHttpMethod("POST").withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(service).build();
				
				DME2Client client = new DME2Client(manager, request);
				//client.setCredentials("test", "test");
				//client.setPayload(message);
				DME2SimpleReplyHandler replyHandler = new DME2SimpleReplyHandler(manager.getConfig(), "QuickClient", false);
				client.setResponseHandlers(replyHandler);
				client.send(new DME2TextPayload(message));
				String response = replyHandler.getResponse(timeout);
				long elapsed = System.currentTimeMillis() - start2;
				if (firstRequest == null) {
					firstRequest = System.currentTimeMillis() - start;
				}
				if (outFile != null) {
					write(new File(outFile), response);
				} 
				if (verbose) System.out.println("[" + i + "] - "+ elapsed + " - Response=" + response);
				
				successes++;
			} catch (DME2Exception e) {
				if (e.getMessage().toLowerCase().indexOf("timeout") > -1) {
					timeouts++;
				} else {
					fails++;
				}
				fatal("[" + i + "] - Error during call", e, 50, false);
			} catch (URISyntaxException e) {
				fails++;
				fatal("[" + i + "] - URI invalid: ", e, 100, false);
			} catch (Exception e) {
				fails++;
				fatal("[" + i + "] - Failure during call: ", e, 200, false);
			}
			
			if (sleep != null) {
				Thread.sleep(sleep);
			}
		}
		
		long end = System.currentTimeMillis();
		long total = end - start;
		System.out.println("Fails: " + fails);
		System.out.println("Successes: " + successes);
		System.out.println("Timeouts: " + timeouts);
		System.out.println("Total Time Ms: " + total);
		System.out.println("Time per request: " + total/loops);
		System.out.println("First request: " + firstRequest);
		System.out.println("TPS: " + loops/(total/1000));

		Thread.sleep(2000);
		System.exit(0);
	}
	
	public static final void fatal(String error, int rc) {
		System.err.println(USAGE);
		System.err.println("ERROR: " + error);
		System.exit(rc);
	}
	
	public static final void fatal(String error, Throwable e, int rc, boolean exit) {
		System.err.println(USAGE);
		System.err.println("ERROR: " + error + ": " + e.toString());
		e.printStackTrace();
		if (exit) 
			System.exit(rc);
	}	
	
	public static byte[] read(String fileName) {
        InputStream is = null;
        try {
            File file = new File(fileName);
            is = new FileInputStream(file);
            long length = file.length();
            byte[] bytes = new byte[(int) length];
            int offset = 0;
            int numRead = 0;
            while (offset < bytes.length
                    && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }
            if (offset < bytes.length) {
                throw new RuntimeException("Could not completely read file " + file.getName());
            }
            return bytes;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if(is != null) {
                try {
                    is.close();
                } catch(Exception e) {
                	logger.debug(null, "read", LogMessage.DEBUG_MESSAGE, "Exception",e);
                    //     
                }
            }
        }
    }
	
	public static void write(File outfile, String message) {
		FileWriter writer = null;
		try {
			writer = new FileWriter(outfile);
			writer.write(message);
		} catch (IOException e) {
			throw new RuntimeException(e);
        } finally {
            if(writer != null) {
                try {
                	writer.close();
                } catch(Exception e) {
                	logger.debug(null, "write", LogMessage.DEBUG_MESSAGE, "Exception",e);
                }
            }
        }

	}

}
