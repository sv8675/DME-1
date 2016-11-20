/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test.jms.util;

import java.util.List;

import javax.jms.QueueConnection;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TemporaryQueue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import com.att.aft.dme2.DME2UnitTestUtil;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.manager.registry.DME2EndpointRegistryGRM;
import com.att.aft.dme2.registry.accessor.BaseAccessor;
import com.att.aft.dme2.registry.dto.ServiceEndpoint;

public abstract class JMSBaseTestCase {
  private static Logger logger = LoggerFactory.getLogger( JMSBaseTestCase.class );

  @BeforeClass
	public static void setUpBeforeClass() throws Exception {
	//	System.setProperty(DME2Constants.DME2_GRM_USER, DME2Constants.getGRMUserName());
//		System.setProperty(DME2Constants.DME2_GRM_PASS, DME2Constants.getGRMUserPass());
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
//		System.clearProperty(DME2Constants.DME2_GRM_USER);
//		System.clearProperty(DME2Constants.DME2_GRM_PASS);
	}
  
	@Before
	public void setUp() throws Exception{
		System.out.println("\n\n=========================================================");
		System.out.println("******************** PERFORMING SETUP *******************");
		System.out.println("=========================================================");
		
		Locations.BHAM.set();
		System.setProperty("platform", "SANDBOX-DEV");
	}
	
	@After
	public void tearDown() throws Exception{
		System.out.println("\n\n=========================================================");
		System.out.println("******************** PERFORMING TEARDOWN *******************");
		System.out.println("=========================================================");
		
		System.clearProperty("AFT_ENVIRONMENT");
		System.clearProperty("AFT_LATITUDE");
		System.clearProperty("AFT_LONGITUDE");
	}
	
	public static void closeJMSResources(QueueConnection conn, QueueSession session, TemporaryQueue tempQ, QueueSender sender, QueueReceiver receiver)
	{
		if(conn != null)
		{
			try{conn.close();}
			catch(Exception e) {}
		}
		
		if(session != null)
		{
			try{session.close();}
			catch(Exception e) {}
		}
		
		if(receiver != null)
		{
			try{receiver.close();}
			catch(Exception e) {}
		}
		
		if(tempQ != null)
		{
			try{tempQ.delete();}
			catch(Exception e) {}
		}
		
		if(sender != null)
		{
			try{sender.close();}
			catch(Exception e) {}
		}

	}

  public void cleanPreviousEndpoints( String serviceName, String serviceVersion, String envContext )
      throws Exception {
    System.setProperty("AFT_ENVIRONMENT", "AFTUAT"); // Stolen from ServerLauncher
    logger.debug( null, "cleanPreviousEndpoints", LogMessage.METHOD_ENTER );
    DME2Configuration config = new DME2Configuration( serviceName );
    DME2Manager manager = new DME2Manager( serviceName, config);
    BaseAccessor grm = (BaseAccessor) DME2UnitTestUtil.getPrivate( DME2EndpointRegistryGRM.class.getDeclaredField( "grm" ), ((DME2EndpointRegistryGRM) manager.getEndpointRegistry()));
    ServiceEndpoint serviceEndpoint = new ServiceEndpoint();//DME2EndpointUtil.convertToServiceEndpoint( endpoint );
    serviceEndpoint.setName( serviceName );
    serviceEndpoint.setVersion( serviceVersion );
    serviceEndpoint.setEnv( envContext );
    List<ServiceEndpoint> serviceEndpointList = grm.findRunningServiceEndPoint( serviceEndpoint );
    if ( serviceEndpointList != null ) {
      for ( ServiceEndpoint sep : serviceEndpointList ) {
        logger.debug( null, "cleanPreviousEndpoints", "Removing old endpoint {} {} {}", sep.getName(),sep.getHostAddress(), sep.getPort());
        manager.getEndpointRegistry().unpublish( "/service=" + sep.getName() + "/envContext=" + envContext + "/version=" + sep.getVersion(),sep.getHostAddress(), Integer.valueOf( sep.getPort() ));
      }
    }

    logger.debug( null, "cleanPreviousEndpoints", LogMessage.METHOD_EXIT );
  }
}
