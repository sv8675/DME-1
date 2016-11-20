/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test.server;

import java.io.File;
import java.util.Hashtable;
import java.util.Properties;

import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.server.test.EchoServlet;
import com.att.aft.dme2.server.test.RegistryFsSetup;





public class SSLServer1{

		private String city = null;
		private String killFile = null;
		private String port = null;
		public boolean servletThreadFailure = false;
		public boolean socketAcceptorFailure = false;
		
		public SSLServer1() throws Exception
		{
		}

		public void setCity(String city)
		{
			this.city = city;
		}
		public String getCity()
		{
			return this.city;
		}
		public void setKillFile(String killFile)
		{
			this.killFile = killFile;
		}
		public String getKillFile()
		{
			return this.killFile;
		}
		
		public void setPort(String port) {
			this.port = port;
		}
		
		public void init() throws Exception
		{
		
			int sPort = 0;
			if(port != null) {
				try {
					sPort = Integer.parseInt(port);
				}catch(Exception e) {
					
				}
			}
			Properties props = RegistryFsSetup.init();
			if(sPort > 0) {
				props.setProperty("AFT_DME2_PORT", ""+sPort);
			}
			else {
				sPort = 9595;
				props.setProperty("AFT_DME2_PORT", ""+sPort);
			}
			if(this.servletThreadFailure) {
				props.setProperty("AFT_DME2_CORE_POOL_SIZE","9");
				props.setProperty("AFT_DME2_MAX_POOL_SIZE", "10");
				props.setProperty("AFT_DME2_SOCKET_ACCEPTOR_THREADS","4");
			}
			if(this.socketAcceptorFailure) {
				props.setProperty("AFT_DME2_CORE_POOL_SIZE","3");
				props.setProperty("AFT_DME2_MAX_POOL_SIZE", "6");
				props.setProperty("AFT_DME2_SOCKET_ACCEPTOR_THREADS","1");
			}
			Hashtable<String,Object> table = new Hashtable<String,Object>();
	        for (Object key: props.keySet()) {
	        	table.put((String)key, props.get(key));
	        }
	        String uriStr = "/service=com.att.aft.TestSSLEndpointsRenegotiateOff/version=1.0.0/envContext=DEV/routeOffer=BAU";
	        Properties props1 =  new Properties();
			props1.setProperty("AFT_DME2_KEYSTORE", SSLServer1.class.getResource( "/m2e.jks" ).getFile());
			props1.setProperty("AFT_DME2_KEY_PASSWORD", "password");
			props1.setProperty("AFT_DME2_PORT", "46890");
			props1.setProperty("AFT_DME2_KEYSTORE_PASSWORD", "password");
			props1.setProperty("AFT_DME2_QUICKSTART_SERVICE", uriStr);

			props1.setProperty("AFT_DME2_SSL_ENABLE", "true");
			props1.setProperty("AFT_DME2_ALLOW_RENEGOTIATE", "true");
			//System.setProperty("javax.net.debug", "all");
			//System.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
			
			DME2Configuration config = new DME2Configuration("testSSLEnabled", props1);			
			
			// create a new DME2 manager with above SSL properties
			DME2Manager serverManager = new DME2Manager("testSSLEnabled", config);
			serverManager.getServer().getServerProperties().setPort(sPort);
			serverManager.bindServiceListener(uriStr,  new EchoServlet(
					"service=com.att.aft.TestSSLEndpointsRenegotiateOff/version=1.0.0/envContext=DEV/routeOffer=BAU",
					"1"));
			serverManager.start();
			
			System.out.println("SSLServerLauncher started successfully...");
			
			File f = new File(killFile);
			while(!f.exists())
			{
				try{Thread.sleep(5000);}catch(Exception ex){}
				System.out.println("Sleeping for 5000 and waiting for kill file " + getKillFile());
			}
			
			f.delete();
			serverManager.shutdown();
//			Thread.sleep(20000);
			System.out.println("SSLServerLauncher destroyed.");
			System.exit(0);
		}
		
		public static void main(String[] args) throws Exception
		{
			SSLServer1 server = new SSLServer1();

			//String city = null;
			if(args.length == 0)
				server.setCity("BHAM");
			else
			{
				for(int i=0; i<args.length; i++)
				{
					if("-city".equals(args[i]))
						server.setCity(args[i + 1]);
					else if("-killfile".equals(args[i]))
						server.setKillFile(args[i + 1]);
					else if("-port".equals(args[i])) 
						server.setPort(args[i+1]);
					else if ("-servlet".equals(args[i])) 
						server.servletThreadFailure=true;
					else if("-socket".equals(args[i]))
						server.socketAcceptorFailure=true;
				}
			}
			
			server.init();
		}

}



