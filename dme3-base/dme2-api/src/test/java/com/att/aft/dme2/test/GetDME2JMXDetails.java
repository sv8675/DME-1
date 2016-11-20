package com.att.aft.dme2.test;


import java.io.IOException;
import java.util.Hashtable;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.naming.Context;
import javax.naming.InitialContext;
public class GetDME2JMXDetails {

   private MBeanServerConnection server; 

   protected InitialContext initialContext;
   protected Hashtable jndiEnv;

   public GetDME2JMXDetails(){
    	try {
			setUp();
		} catch (Exception e) {
			e.printStackTrace();
		}   
   }
   
   /**
    * The JUnit setup method
    */
   public void setUp() throws Exception
   {
	   System.out.println("TestDME2JMXBeans.setUp()");
	   String host = "localhost";  // or some A.B.C.D
	   int port = 5000;
	   String url = "service:jmx:rmi:///jndi/rmi://" + host + ":" + port + "/jmxrmi";
	   JMXServiceURL serviceUrl = new JMXServiceURL(url);
	   JMXConnector jmxConnector = JMXConnectorFactory.connect(serviceUrl, null);
	   server = jmxConnector.getMBeanServerConnection();
       System.out.println("TestDME2JMXBeans.setUp()");
   }

   public Set<ObjectName> getObjectSet() {
	    try {
	    	return server.queryNames(null, null);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    return null;
	}


   /**
    * Gets the InitialContext attribute of the dme2serverCase object
    *
    * @return   The InitialContext value
    */
   public InitialContext getInitialContext() throws Exception
   {
      return initialContext;
   }

   /**
    * Gets the Server attribute of the dme2serverCase object
    *
    * @return   The Server value
    */
   public MBeanServerConnection getServer() throws Exception
   {
      if (server == null)
      {
         String adaptorName = System.getProperty("dme2server.server.name", "jmx/invoker/RMIAdaptor");
         server = (MBeanServerConnection)initialContext.lookup(adaptorName);
      }
      return server;
   }




   String getJndiURL()
   {
      String url = (String)jndiEnv.get(Context.PROVIDER_URL);
      return url;
   }

   String getJndiInitFactory()
   {
      String factory = (String)jndiEnv.get(Context.INITIAL_CONTEXT_FACTORY);
      return factory;
   }


   public void init() throws Exception
   {
      setUp();
   }
   

   public String getServerHost()
   {
      String hostName = System.getProperty("dme2server.server.host", "localhost");
      return hostName;
   }
}