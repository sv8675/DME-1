/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.server.test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;
import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.att.aft.dme2.api.DME2Client;
import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.handler.DME2SimpleReplyHandler;
import com.att.aft.dme2.request.DME2TextPayload;
import com.att.aft.dme2.request.HttpRequest.RequestBuilder;
import com.att.aft.dme2.request.Request;
import com.att.aft.dme2.test.DME2BaseTestCase;
import com.att.aft.dme2.util.DME2Utils;

public class TestDME2ManagerSSL extends DME2BaseTestCase{
	@BeforeClass
	public static void init(){

		System.setProperty("java.util.logging.config.file", "src/test/resources/console.logging.properties");
		
	}
    
	@Test
	  @Ignore
	public void testDME2ManagerSSL() throws Exception {
		System.err.println("Starting testDME2ManagerSSL()");
		Properties props = RegistryFsSetup.init();
		//client specific properties
        //the key property
		props.setProperty("AFT_DME2_CLIENT_IGNORE_SSL_CONFIG", "false");
		props.setProperty("AFT_DME2_CLIENT_KEYSTORE","m2e.jks");
		props.setProperty("AFT_DME2_CLIENT_KEYSTORE_PASSWORD", "password");
		props.setProperty("AFT_DME2_CLIENT_SSL_CERT_ALIAS","m2e-nonprod");
		props.setProperty("AFT_DME2_CLIENT_TRUSTSTORE","m2e.jks");
		props.setProperty("AFT_DME2_CLIENT_TRUSTSTORE_PASSWORD", "password");
		props.setProperty("AFT_DME2_CLIENT_KEY_PASSWORD", "password");
		
		
		//server properties
		props.setProperty("AFT_DME2_KEYSTORE", "m2e.jks");
		props.setProperty("AFT_DME2_KEY_PASSWORD", "password");
		props.setProperty("AFT_DME2_KEYSTORE_PASSWORD", "password");
		props.setProperty("AFT_DME2_SSL_NEED_CLIENT_AUTH","true");
		props.setProperty("AFT_DME2_SSL_ENABLE","true");
		props.put("AFT_DME2_PORT","6101");
		
		DME2Manager sslMgr =  null;
		String serviceName = "com.att.aft.dme2.test.DME2ManagerSSL";
		String version = "1.0.0";
		String envContext="LAB";
		String routeOffer="DEFAULT";
		String serviceUri = DME2Utils.buildServiceURIString(serviceName, version, envContext, routeOffer);
		
		try{
			// Construct DME2Manager to use the ssl props. 
			sslMgr = new DME2Manager(serviceName, new DME2Configuration(serviceName,props));
			sslMgr.bindServiceListener(serviceUri, new EchoServlet(serviceUri,serviceName));
			
			Thread.sleep(1000);
			
			String uriStr = getSearchUriString(serviceName, version, envContext, "FOO");
			//System.out.println("SearchUri:" + uriStr);
			Request request = new RequestBuilder(new URI(uriStr)).withHttpMethod("POST").withReadTimeout(10000).withReturnResponseAsBytes(false).withLookupURL(uriStr).build();

			
			DME2Client sender = new DME2Client(sslMgr, request);
			//sender.setPayload("this is a test");
			DME2SimpleReplyHandler replyHandler = new DME2SimpleReplyHandler(sslMgr.getConfig(), "DME2ManagerSSL", false);
			sender.setResponseHandlers(replyHandler);
			sender.send(new DME2TextPayload("this is a test"));
		
			String reply = replyHandler.getResponse(1000);
			assertTrue(String.format("Did not find the expected string %s in the reply. [%s]", serviceUri, reply),reply.contains(serviceUri));
			

		} catch (DME2Exception e){
			e.printStackTrace();
			fail(String.format("Caught unexpected DME2Exception %s",e.getMessage()));
		} catch(Exception e){
			e.printStackTrace();
			fail(String.format("Caught unexpected Exception %s",e.getMessage()));
		}
		finally {
			sslMgr.unbindServiceListener(serviceUri);
			System.err.println("Completed testDME2ManagerSSL()");
		}
	}
    
	@Test
	  @Ignore
	public void testDME2ManagerWithoutKeystorePasswordSSL() throws Exception {
		System.err.println("Starting testDME2ManagerWithoutKeystorePasswordSSL()");
		Properties props = RegistryFsSetup.init();
		
		//client specific properties
        //the key property
		props.setProperty("AFT_DME2_CLIENT_IGNORE_SSL_CONFIG", "false");
		props.setProperty("AFT_DME2_CLIENT_KEYSTORE","m2e.jks");
		//intentionally unset
		props.setProperty("AFT_DME2_CLIENT_KEYSTORE_PASSWORD", "");
		props.setProperty("AFT_DME2_CLIENT_SSL_CERT_ALIAS","m2e-nonprod");
		props.setProperty("AFT_DME2_CLIENT_TRUSTSTORE","m2e.jks");
		props.setProperty("AFT_DME2_CLIENT_TRUSTSTORE_PASSWORD", "password");
		props.setProperty("AFT_DME2_CLIENT_KEY_PASSWORD", "password");
		
		
		//server properties
		props.setProperty("AFT_DME2_KEYSTORE", "m2e.jks");
		props.setProperty("AFT_DME2_KEY_PASSWORD", "password");
		props.setProperty("AFT_DME2_KEYSTORE_PASSWORD", "password");
		props.setProperty("AFT_DME2_SSL_NEED_CLIENT_AUTH","true");
		props.setProperty("AFT_DME2_SSL_ENABLE","true");
		props.put("AFT_DME2_PORT","6102");

		
		DME2Manager sslMgr =  null;
		String serviceName = "com.att.aft.dme2.test.DME2ManagerWithoutKeystorePasswordSSL";
		String version = "1.0.0";
		String envContext="LAB";
		String routeOffer="DEFAULT";
		String serviceUri = DME2Utils.buildServiceURIString(serviceName, version, envContext, routeOffer);
		
		try{
			// Construct DME2Manager to use the ssl props. 
			sslMgr = new DME2Manager(serviceName, new DME2Configuration(serviceName,props));
			sslMgr.bindServiceListener(serviceUri, new EchoServlet(serviceUri,serviceName));
			Thread.sleep(1000);
			
			String uriStr = getSearchUriString(serviceName, version, envContext, "FOO");
			
			Request request = new RequestBuilder(new URI(uriStr)).withHttpMethod("POST").withReadTimeout(10000).withReturnResponseAsBytes(false).withLookupURL(uriStr).build();
			
			//System.out.println("SearchUri:" + uriStr);
			DME2Client sender = new DME2Client(sslMgr, request);
			//sender.setPayload("this is a test");
			DME2SimpleReplyHandler replyHandler = new DME2SimpleReplyHandler(sslMgr.getConfig(), "DME2ManagerWithoutKeystorePasswordSSL", false);
			sender.setResponseHandlers(replyHandler);
			sender.send(new DME2TextPayload("this is a test"));
			
			String reply = replyHandler.getResponse(1000);
			
			//shouldnt happen. we should have thrown an exception.
			fail(String.format("Did not throw expected exception, and instead got a reply [%s]", reply));
			
		} catch (DME2Exception e){
			e.printStackTrace();
			assertTrue(String.format("Caught unexpected DME2Exception %s",e.getMessage()),e.getMessage().contains("AFT-DME2-0918"));
		} catch(Exception e){
			e.printStackTrace();
			fail(String.format("Caught unexpected Exception %s",e.getMessage()));
		}
		finally {
			sslMgr.unbindServiceListener(serviceUri);
			System.err.println("Completed testDME2ManagerWithoutKeystorePasswordSSL()");
		}
	}
    
	@Test
	  @Ignore
	public void testDME2ManagerWithoutClientTruststorePasswordSSL() throws Exception {
		System.err.println("Starting testDME2ManagerWithoutClientTruststorePasswordSSL()");
		Properties props = RegistryFsSetup.init();
		//client specific properties
        //the key property
		props.setProperty("AFT_DME2_CLIENT_IGNORE_SSL_CONFIG", "false");
		props.setProperty("AFT_DME2_CLIENT_KEYSTORE","m2e.jks");
		props.setProperty("AFT_DME2_CLIENT_KEYSTORE_PASSWORD", "password");
		props.setProperty("AFT_DME2_CLIENT_SSL_CERT_ALIAS","m2e-nonprod");
		props.setProperty("AFT_DME2_CLIENT_TRUSTSTORE","m2e.jks");
		props.setProperty("AFT_DME2_CLIENT_KEY_PASSWORD", "password");
		
		//intentionally unset
		props.setProperty("AFT_DME2_CLIENT_TRUSTSTORE_PASSWORD", "");
		
		//server properties
		props.setProperty("AFT_DME2_KEYSTORE", "m2e.jks");
		props.setProperty("AFT_DME2_KEY_PASSWORD", "password");
		props.setProperty("AFT_DME2_KEYSTORE_PASSWORD", "password");
		props.setProperty("AFT_DME2_SSL_NEED_CLIENT_AUTH","true");
		props.setProperty("AFT_DME2_SSL_ENABLE","true");
		props.put("AFT_DME2_PORT","6103");
		
		
		DME2Manager sslMgr =  null;
		String serviceName = "com.att.aft.dme2.test.DME2ManagerWithoutClientTruststorePasswordSSL";
		String version = "1.0.0";
		String envContext="LAB";
		String routeOffer="DEFAULT";
		String serviceUri = DME2Utils.buildServiceURIString(serviceName, version, envContext, routeOffer);
		
		try{
			// Construct DME2Manager to use the ssl props. 
			sslMgr = new DME2Manager(serviceName, new DME2Configuration(serviceName,props));
			sslMgr.bindServiceListener(serviceUri, new EchoServlet(serviceUri,serviceName));
			Thread.sleep(1000);
			
			
			String uriStr = getSearchUriString(serviceName, version, envContext, "FOO");
			Request request = new RequestBuilder(new URI(uriStr)).withHttpMethod("POST").withReadTimeout(10000).withReturnResponseAsBytes(false).withLookupURL(uriStr).build();

			
			//System.out.println("SearchUri:" + uriStr);
			DME2Client sender = new DME2Client(sslMgr, request);
			//sender.setPayload("this is a test");
			DME2SimpleReplyHandler replyHandler = new DME2SimpleReplyHandler(sslMgr.getConfig(), "DME2ManagerWithoutClientTruststorePasswordSSL", false);
			sender.setResponseHandlers(replyHandler);
			sender.send(new DME2TextPayload("this is a test"));
			
			String reply = replyHandler.getResponse(1000);
			
			//shouldnt happen. we should have thrown an exception.
			fail(String.format("Did not throw expected exception, and instead got a reply [%s]", reply));
			
		} catch (DME2Exception e){
			e.printStackTrace();
			assertTrue(String.format("Caught unexpected DME2Exception %s",e.getMessage()),e.getMessage().contains("AFT-DME2-0919"));
		} catch(Exception e){
			e.printStackTrace();
			fail(String.format("Caught unexpected Exception %s",e.getMessage()));
		}
		finally {
			sslMgr.unbindServiceListener(serviceUri);
			System.err.println("Completed testDME2ManagerWithoutClientTruststorePasswordSSL()");
		}
	}	
	
	private static String getSearchUriString(String serviceName, String version, String envContext, String partner){
		return String.format("http://DME2SEARCH/service=%s/version=%s/envContext=%s/partner=%s", serviceName, version, envContext, partner);
	}
	
}
