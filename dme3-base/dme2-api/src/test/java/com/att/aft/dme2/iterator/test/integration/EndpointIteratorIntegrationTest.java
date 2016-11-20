/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.iterator.test.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;

import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.iterator.domain.DME2EndpointReference;
import com.att.aft.dme2.iterator.factory.EndpointIteratorFactory;
import com.att.aft.dme2.iterator.helper.StaleProcessor;
import com.att.aft.dme2.iterator.service.DME2BaseEndpointIterator;
import com.att.aft.dme2.iterator.test.servlet.DME2SimpleServlet;
import com.att.aft.dme2.manager.registry.DME2Endpoint;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.DME2URIUtils;


public class EndpointIteratorIntegrationTest  {
	private static final String DEFAULT_VERSION = "1.0";
	  private static final String DEFAULT_HOST = "TestHost";
	  private static final String DEFAULT_PATH = "/service=com.att.test.TestService-2/version=1.0.0/envContext=LAB/routeOffer=DEFAULT";
	  private static final int DEFAULT_PORT = 12345;
	   private static final String DEFAULT_ROUTE_OFFER = "DEFAULT";
	  private static final double DEFAULT_LATITUDE = 1.11;
	  private static final double DEFAULT_LONGITUDE = -2.22;
	  private static final String DEFAULT_PROTOCOL = "http";
	  private static final String DEFAULT_ENV_CONTEXT = "LAB";

	@Before
	public void setUp() throws Exception
	{
		TestUtils.initAFTProperties();
	}
	
	@After
	public void tearDown() throws Exception{
	}
	
	
    //@Test
//    @Ignore
    public void testDME2EndpointIterator_ResolveEndpoints_1()
	{
		//This test case uses DME2SEARCH for the lookup.
    	
    	String DEFAULT_SERVICE_NAME = "com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints_1";
    	String DME2_MANAGER_NAME_1 = "testDME2EndpointIterator_ResolveEndpoints_1";
    	String DME2_MANAGER_NAME_2 = "testDME2EndpointIterator_ResolveEndpoints_2";
    	String DME2_MANAGER_NAME_3 = "testDME2EndpointIterator_ResolveEndpoints_3";
    	String DME2_MANAGER_NAME_4 = "testDME2EndpointIterator_ResolveEndpoints_4";
		
		DME2Manager mgr_1 = null;
		DME2Manager mgr_2 = null;
		DME2Manager mgr_3 = null;
		DME2Manager mgr_4 = null;
		
		String service_1 = DME2URIUtils.buildServiceURIString(DEFAULT_SERVICE_NAME, DEFAULT_VERSION, DEFAULT_ENV_CONTEXT, "A1");
		String service_2 = DME2URIUtils.buildServiceURIString(DEFAULT_SERVICE_NAME, DEFAULT_VERSION, DEFAULT_ENV_CONTEXT, "A2");
		String service_3 = DME2URIUtils.buildServiceURIString(DEFAULT_SERVICE_NAME, DEFAULT_VERSION, DEFAULT_ENV_CONTEXT, "B1");
		String service_4 = DME2URIUtils.buildServiceURIString(DEFAULT_SERVICE_NAME, DEFAULT_VERSION, DEFAULT_ENV_CONTEXT, "B2");
		
		String clientURI = "http://DME2RESOLVE/service="+DEFAULT_SERVICE_NAME+"/version="+DEFAULT_VERSION+"/envContext="+DEFAULT_ENV_CONTEXT+"/partner=DME2_TEST";
		
		try
		
		{
			Properties props = TestUtils.initAFTProperties();
			
			mgr_1 = new DME2Manager(DME2_MANAGER_NAME_1, new DME2Configuration(DME2_MANAGER_NAME_1, props));
			mgr_2 = new DME2Manager(DME2_MANAGER_NAME_2,  new DME2Configuration(DME2_MANAGER_NAME_2, props));
			mgr_3 = new DME2Manager(DME2_MANAGER_NAME_3, new DME2Configuration(DME2_MANAGER_NAME_3, props));
			mgr_4 = new DME2Manager(DME2_MANAGER_NAME_4, new DME2Configuration(DME2_MANAGER_NAME_4, props));
			
			mgr_1.bindServiceListener(service_1, new DME2SimpleServlet());
			mgr_2.bindServiceListener(service_2, new DME2SimpleServlet());
			mgr_3.bindServiceListener(service_3, new DME2SimpleServlet());
			mgr_4.bindServiceListener(service_4, new DME2SimpleServlet());
			
			Thread.sleep(3000);
			
			
			DME2BaseEndpointIterator endpointIterator = EndpointIteratorFactory.getDefaultEndpointIteratorBuilder(new DME2Configuration(DME2_MANAGER_NAME_1, props))
																						.setServiceURI(clientURI)
																						.setManager(mgr_1)
																						.build();
			
			List<String> servicesToValidate = new ArrayList<String>();
			
			while(endpointIterator.hasNext())
			{
				DME2EndpointReference ref = endpointIterator.next();
				DME2Endpoint endpoint = ref.getEndpoint();
				
				System.out.println(endpoint.getServiceName());
				servicesToValidate.add(endpoint.getServiceName());
			}
			
			assertEquals(4, servicesToValidate.size());
			assertTrue(servicesToValidate.contains(service_1));
			assertTrue(servicesToValidate.contains(service_2));
			assertTrue(servicesToValidate.contains(service_3));
			assertTrue(servicesToValidate.contains(service_4));
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{
			try{mgr_1.unbindServiceListener(service_1);}
			catch(Exception e){}
			
			try{mgr_2.unbindServiceListener(service_2);}
			catch(Exception e){}
			
			try{mgr_3.unbindServiceListener(service_3);}
			catch(Exception e){}
			
			try{mgr_4.unbindServiceListener(service_4);}
			catch(Exception e){}
		}
	}
	
    //@Test
    public void testDME2EndpointIterator_ResolveEndpoints_2()
	{
		//This test case uses DME2RESOLVE for the lookup.
		
		DME2Manager mgr_1 = null;
		DME2Manager mgr_2 = null;
		
		String service_1 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints_2", "1.0.0", "LAB", "D1");
		String service_2 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints_2", "1.0.0", "LAB", "D1");
		
		try
		{
			Properties props = TestUtils.initAFTProperties();
			
			String mgr_1_name = "testDME2EndpointIterator_ResolveEndpoints_1";
			String mgr_2_name = "testDME2EndpointIterator_ResolveEndpoints_2";
			mgr_1 = new DME2Manager(mgr_1_name, new DME2Configuration(mgr_1_name, props));
			mgr_2 = new DME2Manager(mgr_2_name, new DME2Configuration(mgr_2_name, props));
			
			//mgr_1.bindServiceListener(service_1, new DME2SimpleServlet());
			//mgr_2.bindServiceListener(service_2, new DME2SimpleServlet());
			
			Thread.sleep(3000);
			
			String clientURI = "http://DME2RESOLVE/service=com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints_2/version=1.0.0/envContext=LAB/routeOffer=D1";
			
			DME2BaseEndpointIterator endpointIterator = EndpointIteratorFactory
															.getDefaultEndpointIteratorBuilder(new DME2Configuration(mgr_1_name, props))
															.setServiceURI(clientURI)
															.setManager(mgr_1)
															.build();

			List<String> servicesToValidate = new ArrayList<String>();
			
			while(endpointIterator.hasNext())
			{
				DME2EndpointReference ref = endpointIterator.next();
				DME2Endpoint endpoint = ref.getEndpoint();
				
				System.out.println(endpoint.getServiceName());
				servicesToValidate.add(endpoint.getServiceName());
			}
			
			assertEquals(2, servicesToValidate.size());
			assertTrue(servicesToValidate.contains(service_1));
			assertTrue(servicesToValidate.contains(service_2));
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{
			try{mgr_1.unbindServiceListener(service_1);}
			catch(Exception e){}
			
			try{mgr_2.unbindServiceListener(service_2);}
			catch(Exception e){}
			
		}
	}
	
	
    //@Test
    public void testDME2EndpointIterator_AllEndpointsExhausted_1()
	{	
		DME2Manager mgr_1 = null;
		DME2Manager mgr_2 = null;
		
		String service_1 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_AllEndpointsExhausted_1", "1.0.0", "LAB", "D1");
		String service_2 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_AllEndpointsExhausted_1", "1.0.0", "LAB", "D1");

		
		try
		{
			Properties props = TestUtils.initAFTProperties();

			String mgr_1_name = "testDME2EndpointIterator_AllEndpointsExhausted_1";
			String mgr_2_name = "testDME2EndpointIterator_AllEndpointsExhausted_2";
			
			mgr_1 = new DME2Manager(mgr_1_name, new DME2Configuration(mgr_1_name, props));
			mgr_2 = new DME2Manager(mgr_2_name, new DME2Configuration(mgr_2_name, props));
			
			//mgr_1.bindServiceListener(service_1, new DME2SimpleServlet());
			//mgr_2.bindServiceListener(service_2, new DME2SimpleServlet());
			
			Thread.sleep(3000);
			
			String clientURI = "http://DME2RESOLVE/service=com.att.aft.dme2.test.TestDME2EndpointIterator_AllEndpointsExhausted_1/version=1.0.0/envContext=LAB/routeOffer=D1";
			
			DME2BaseEndpointIterator endpointIterator = EndpointIteratorFactory.getDefaultEndpointIteratorBuilder(new DME2Configuration(mgr_1_name, props))
														.setServiceURI(clientURI)
														.setManager(mgr_1)
														.build();
			
			while(endpointIterator.hasNext())
			{
				endpointIterator.next();
			}
			
			System.out.println("Iterator has next: " + endpointIterator.hasNext());
			System.out.println("Number of active elements: " + endpointIterator.getNumberOfActiveElements());
			
			assertFalse(endpointIterator.hasNext());
			assertEquals(2, endpointIterator.getNumberOfActiveElements());
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{
			try{mgr_1.unbindServiceListener(service_1);}
			catch(Exception e){}
			
			try{mgr_2.unbindServiceListener(service_2);}
			catch(Exception e){}
			
		}
	}
	
	
    //@Test
    public void testDME2EndpointIterator_AllEndpointsExhausted_2()
	{	
		DME2Manager mgr_1 = null;
		DME2Manager mgr_2 = null;
		
		String service_1 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_AllEndpointsExhausted_2", "1.0.0", "LAB", "D1");
		String service_2 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_AllEndpointsExhausted_2", "1.0.0", "LAB", "D1");

		
		try
		{
			Properties props = TestUtils.initAFTProperties();

			String mgr_1_name = "testDME2EndpointIterator_AllEndpointsExhausted_1";
			String mgr_2_name = "testDME2EndpointIterator_AllEndpointsExhausted_2";
			
			mgr_1 = new DME2Manager(mgr_1_name, new DME2Configuration(mgr_1_name, props));
			mgr_2 = new DME2Manager(mgr_2_name, new DME2Configuration(mgr_2_name, props));			

			
			//mgr_1.bindServiceListener(service_1, new DME2SimpleServlet());
			//mgr_2.bindServiceListener(service_2, new DME2SimpleServlet());
			
			Thread.sleep(3000);
			
			String clientURI = "http://DME2RESOLVE/service=com.att.aft.dme2.test.TestDME2EndpointIterator_AllEndpointsExhausted_2/version=1.0.0/envContext=LAB/routeOffer=D1";
	
			DME2BaseEndpointIterator endpointIterator = EndpointIteratorFactory
					.getDefaultEndpointIteratorBuilder(new DME2Configuration(mgr_1_name, props))
					.setServiceURI(clientURI)
					.setManager(mgr_1)
					.build();
			
			//Set the first element stale.
			while(endpointIterator.hasNext())
			{
				DME2EndpointReference ref = endpointIterator.next();
				StaleProcessor.setStale(ref.getRouteOffer(), mgr_1, ref.getEndpoint());
				break;		
			}
			
			//Reset the Iterator
			endpointIterator.resetIterator();
			
			while(endpointIterator.hasNext())
			{
				endpointIterator.next();
			}
			
			//One element should have been skipped due to being stale and one other element should be active.
			System.out.println("Iterator has next: " + endpointIterator.hasNext());
			System.out.println("Number of active elements: " + endpointIterator.getNumberOfActiveElements());
			System.out.println("Number of stale elements: " + endpointIterator.getNumberOfStaleElements());
			
			assertFalse(endpointIterator.hasNext());
			assertEquals(1, endpointIterator.getNumberOfActiveElements());
			assertEquals(1, endpointIterator.getNumberOfStaleElements());
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{
			try{mgr_1.unbindServiceListener(service_1);}
			catch(Exception e){}
			
			try{mgr_2.unbindServiceListener(service_2);}
			catch(Exception e){}
			
		}
	}
	
	
    private void assertFalse(boolean hasNext) {
		// TODO Auto-generated method stub
		
	}

	//@Test
    public void testDME2EndpointIterator_IllegallyRemoveElement_1()
	{
		DME2Manager mgr_1 = null;
		DME2Manager mgr_2 = null;
		DME2Manager mgr_3 = null;
		DME2Manager mgr_4 = null;
		
		String service_1 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints", "1.0.0", "LAB", "A1");
		String service_2 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints", "1.0.0", "LAB", "A2");
		String service_3 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints", "1.0.0", "LAB", "B1");
		String service_4 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints", "1.0.0", "LAB", "B2");
		
		try
		{
			Properties props = TestUtils.initAFTProperties();

			String mgr_1_name = "testDME2EndpointIterator_ResolveEndpoints_1";
			String mgr_2_name = "testDME2EndpointIterator_ResolveEndpoints_2";
			String mgr_3_name = "testDME2EndpointIterator_ResolveEndpoints_3";
			String mgr_4_name = "testDME2EndpointIterator_ResolveEndpoints_4";

			mgr_1 = new DME2Manager(mgr_1_name, new DME2Configuration(mgr_1_name, props));
			mgr_2 = new DME2Manager(mgr_2_name, new DME2Configuration(mgr_2_name, props));	
			mgr_3 = new DME2Manager(mgr_3_name, new DME2Configuration(mgr_3_name, props));	
			mgr_4 = new DME2Manager(mgr_4_name, new DME2Configuration(mgr_4_name, props));	

			
			//mgr_1.bindServiceListener(service_1, new DME2SimpleServlet());
			//mgr_2.bindServiceListener(service_2, new DME2SimpleServlet());
			//mgr_3.bindServiceListener(service_3, new DME2SimpleServlet());
			//mgr_4.bindServiceListener(service_4, new DME2SimpleServlet());
			
			Thread.sleep(3000);
			
			String clientURI = "http://DME2SEARCH/service=com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints/version=1.0.0/envContext=LAB/partner=DME2_TEST";
			
			DME2BaseEndpointIterator endpointIterator = EndpointIteratorFactory
					.getDefaultEndpointIteratorBuilder(new DME2Configuration(mgr_1_name, props))
					.setServiceURI(clientURI)
					.setManager(mgr_1)
					.build();

			while(endpointIterator.hasNext())
			{
				endpointIterator.remove();
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			assertTrue(e instanceof IllegalStateException);
			assertTrue(e.getMessage().contains("Error occured - removed() cannot be called before called next(). Additionally, the remove() method can be called only once per call to next()."));
		}
		finally
		{
			try{mgr_1.unbindServiceListener(service_1);}
			catch(Exception e){}
			
			try{mgr_2.unbindServiceListener(service_2);}
			catch(Exception e){}
			
			try{mgr_3.unbindServiceListener(service_3);}
			catch(Exception e){}
			
			try{mgr_4.unbindServiceListener(service_4);}
			catch(Exception e){}
		}
	}
	
	
    //@Test
    public void testDME2EndpointIterator_IllegallyRemoveElement_2()
	{
		DME2Manager mgr_1 = null;
		DME2Manager mgr_2 = null;
		DME2Manager mgr_3 = null;
		DME2Manager mgr_4 = null;
		
		String service_1 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints", "1.0.0", "LAB", "A1");
		String service_2 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints", "1.0.0", "LAB", "A2");
		String service_3 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints", "1.0.0", "LAB", "B1");
		String service_4 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints", "1.0.0", "LAB", "B2");
		
		try
		{
			Properties props = TestUtils.initAFTProperties();

			String mgr_1_name = "testDME2EndpointIterator_ResolveEndpoints_1";
			String mgr_2_name = "testDME2EndpointIterator_ResolveEndpoints_2";
			String mgr_3_name = "testDME2EndpointIterator_ResolveEndpoints_3";
			String mgr_4_name = "testDME2EndpointIterator_ResolveEndpoints_4";

			mgr_1 = new DME2Manager(mgr_1_name, new DME2Configuration(mgr_1_name, props));
			mgr_2 = new DME2Manager(mgr_2_name, new DME2Configuration(mgr_2_name, props));	
			mgr_3 = new DME2Manager(mgr_3_name, new DME2Configuration(mgr_3_name, props));	
			mgr_4 = new DME2Manager(mgr_4_name, new DME2Configuration(mgr_4_name, props));	
			
			//mgr_1.bindServiceListener(service_1, new DME2SimpleServlet());
			//mgr_2.bindServiceListener(service_2, new DME2SimpleServlet());
			//mgr_3.bindServiceListener(service_3, new DME2SimpleServlet());
			//mgr_4.bindServiceListener(service_4, new DME2SimpleServlet());
			
			Thread.sleep(3000);
			
			String clientURI = "http://DME2SEARCH/service=com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints/version=1.0.0/envContext=LAB/partner=DME2_TEST";
			
			DME2BaseEndpointIterator endpointIterator = EndpointIteratorFactory
					.getDefaultEndpointIteratorBuilder(new DME2Configuration(mgr_1_name, props))
					.setServiceURI(clientURI)
					.setManager(mgr_1)
					.build();

			while(endpointIterator.hasNext())
			{
				endpointIterator.next();
				endpointIterator.remove();
				endpointIterator.remove();
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			assertTrue(e instanceof IllegalStateException);
			assertTrue(e.getMessage().contains("Error occured - removed() cannot be called before called next(). Additionally, the remove() method can be called only once per call to next()."));
		}
		finally
		{
			try{mgr_1.unbindServiceListener(service_1);}
			catch(Exception e){}
			
			try{mgr_2.unbindServiceListener(service_2);}
			catch(Exception e){}
			
			try{mgr_3.unbindServiceListener(service_3);}
			catch(Exception e){}
			
			try{mgr_4.unbindServiceListener(service_4);}
			catch(Exception e){}
		}
	}
	
	
    //@Test
    public void testDME2EndpointIterator_FailOnNoSuchElementException()
	{
		DME2Manager mgr_1 = null;
		DME2Manager mgr_2 = null;
		DME2Manager mgr_3 = null;
		DME2Manager mgr_4 = null;
		
		String service_1 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints", "1.0.0", "LAB", "A1");
		String service_2 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints", "1.0.0", "LAB", "A2");
		String service_3 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints", "1.0.0", "LAB", "B1");
		String service_4 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints", "1.0.0", "LAB", "B2");
		
		try
		{
			Properties props = TestUtils.initAFTProperties();

			String mgr_1_name = "testDME2EndpointIterator_ResolveEndpoints_1";
			String mgr_2_name = "testDME2EndpointIterator_ResolveEndpoints_2";
			String mgr_3_name = "testDME2EndpointIterator_ResolveEndpoints_3";
			String mgr_4_name = "testDME2EndpointIterator_ResolveEndpoints_4";

			mgr_1 = new DME2Manager(mgr_1_name, new DME2Configuration(mgr_1_name, props));
			mgr_2 = new DME2Manager(mgr_2_name, new DME2Configuration(mgr_2_name, props));	
			mgr_3 = new DME2Manager(mgr_3_name, new DME2Configuration(mgr_3_name, props));	
			mgr_4 = new DME2Manager(mgr_4_name, new DME2Configuration(mgr_4_name, props));	
			
			//mgr_1.bindServiceListener(service_1, new DME2SimpleServlet());
			//mgr_2.bindServiceListener(service_2, new DME2SimpleServlet());
			//mgr_3.bindServiceListener(service_3, new DME2SimpleServlet());
			//mgr_4.bindServiceListener(service_4, new DME2SimpleServlet());
			
			Thread.sleep(3000);
			
			String clientURI = "http://DME2SEARCH/service=com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints/version=1.0.0/envContext=LAB/partner=DME2_TEST";
			
			DME2BaseEndpointIterator endpointIterator = EndpointIteratorFactory
					.getDefaultEndpointIteratorBuilder(new DME2Configuration(mgr_1_name, props))
					.setServiceURI(clientURI)
					.setManager(mgr_1)
					.build();
			
			/*Exhaust all elements in the iterator until NoSuchElementException is thrown*/
			endpointIterator.next();
			endpointIterator.next();
			endpointIterator.next();
			endpointIterator.next();
			endpointIterator.next();

		}
		catch(Exception e)
		{
			e.printStackTrace();
			assertTrue(e instanceof NoSuchElementException);
			assertTrue(e.getMessage().contains("No more Endpoint References are available in the Iterator"));
		}
		finally
		{
			try{mgr_1.unbindServiceListener(service_1);}
			catch(Exception e){}
			
			try{mgr_2.unbindServiceListener(service_2);}
			catch(Exception e){}
			
			try{mgr_3.unbindServiceListener(service_3);}
			catch(Exception e){}
			
			try{mgr_4.unbindServiceListener(service_4);}
			catch(Exception e){}
		}
	}
	
	
    //@Test
    public void testDME2EndpointIterator_RemoveElements()
	{
		DME2Manager mgr_1 = null;
		DME2Manager mgr_2 = null;
		DME2Manager mgr_3 = null;
		DME2Manager mgr_4 = null;
		
		String service_1 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_RemoveElements", "1.0.0", "LAB", "A1");
		String service_2 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_RemoveElements", "1.0.0", "LAB", "A2");
		String service_3 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_RemoveElements", "1.0.0", "LAB", "B1");
		String service_4 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_RemoveElements", "1.0.0", "LAB", "B2");
		
		try
		{
			Properties props = TestUtils.initAFTProperties();

			String mgr_1_name = "TestDME2EndpointIterator_RemoveElements_1";
			String mgr_2_name = "TestDME2EndpointIterator_RemoveElements_2";
			String mgr_3_name = "TestDME2EndpointIterator_RemoveElements_3";
			String mgr_4_name = "TestDME2EndpointIterator_RemoveElements_4";

			mgr_1 = new DME2Manager(mgr_1_name, new DME2Configuration(mgr_1_name, props));
			mgr_2 = new DME2Manager(mgr_2_name, new DME2Configuration(mgr_2_name, props));	
			mgr_3 = new DME2Manager(mgr_3_name, new DME2Configuration(mgr_3_name, props));	
			mgr_4 = new DME2Manager(mgr_4_name, new DME2Configuration(mgr_4_name, props));	
			
			//mgr_1.bindServiceListener(service_1, new DME2SimpleServlet());
			//mgr_2.bindServiceListener(service_2, new DME2SimpleServlet());
			//mgr_3.bindServiceListener(service_3, new DME2SimpleServlet());
			//mgr_4.bindServiceListener(service_4, new DME2SimpleServlet());
			
			Thread.sleep(3000);
			
			String clientURI = "http://DME2SEARCH/service=com.att.aft.dme2.test.TestDME2EndpointIterator_RemoveElements/version=1.0.0/envContext=LAB/partner=DME2_TEST";
			
			DME2BaseEndpointIterator endpointIterator = EndpointIteratorFactory
					.getDefaultEndpointIteratorBuilder(new DME2Configuration(mgr_1_name, props))
					.setServiceURI(clientURI)
					.setManager(mgr_1)
					.build();
			
			System.out.println("Starting number of Active Elements: " + endpointIterator.getNumberOfActiveElements());
			assertEquals(4, endpointIterator.getNumberOfActiveElements());
			
			while(endpointIterator.hasNext())
			{
				endpointIterator.next();
				endpointIterator.remove();
			}
			
			System.out.println("Number of Removed Elements: " + endpointIterator.getNumberOfRemovedElements());
			assertEquals(4, endpointIterator.getNumberOfRemovedElements());
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{
			try{mgr_1.unbindServiceListener(service_1);}
			catch(Exception e){}
			
			try{mgr_2.unbindServiceListener(service_2);}
			catch(Exception e){}
			
			try{mgr_3.unbindServiceListener(service_3);}
			catch(Exception e){}
			
			try{mgr_4.unbindServiceListener(service_4);}
			catch(Exception e){}
		}
	}
	
	
    //@Test
    public void testDME2EndpointIterator_ReturnNoEndpoints()
	{
		try
		{	
			String clientURI = "http://DME2RESOLVE/service=com.att.aft.dme2.test.TestDME2EndpointIterator_ReturnNoEndpoints/version=1.0.0/envContext=LAB/routeOffer=DME2_TEST";
			
			DME2BaseEndpointIterator endpointIterator = EndpointIteratorFactory
					.getDefaultEndpointIteratorBuilder(new DME2Configuration())
					.setServiceURI(clientURI)
					.build();

			assertFalse(endpointIterator.hasNext());
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{

		}
	}
	
	
    //@Test
    public void testDME2EndpointIterator_SetEndpointsStale_1()
	{
		DME2Manager mgr_1 = null;
		DME2Manager mgr_2 = null;
		DME2Manager mgr_3 = null;
		DME2Manager mgr_4 = null;
		
		String service_1 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_SetElementStale", "1.0.0", "LAB", "A1");
		String service_2 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_SetElementStale", "1.0.0", "LAB", "A2");
		String service_3 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_SetElementStale", "1.0.0", "LAB", "B1");
		String service_4 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_SetElementStale", "1.0.0", "LAB", "B2");
		
		try
		{
			Properties props = TestUtils.initAFTProperties();

			String mgr_1_name = "testDME2EndpointIterator_ResolveEndpoints_1";
			String mgr_2_name = "testDME2EndpointIterator_ResolveEndpoints_2";
			String mgr_3_name = "testDME2EndpointIterator_ResolveEndpoints_3";
			String mgr_4_name = "testDME2EndpointIterator_ResolveEndpoints_4";

			mgr_1 = new DME2Manager(mgr_1_name, new DME2Configuration(mgr_1_name, props));
			mgr_2 = new DME2Manager(mgr_2_name, new DME2Configuration(mgr_2_name, props));	
			mgr_3 = new DME2Manager(mgr_3_name, new DME2Configuration(mgr_3_name, props));	
			mgr_4 = new DME2Manager(mgr_4_name, new DME2Configuration(mgr_4_name, props));	
			
			//mgr_1.bindServiceListener(service_1, new DME2SimpleServlet());
			//mgr_2.bindServiceListener(service_2, new DME2SimpleServlet());
			//mgr_3.bindServiceListener(service_3, new DME2SimpleServlet());
			//mgr_4.bindServiceListener(service_4, new DME2SimpleServlet());
			
			Thread.sleep(3000);
			
			String clientURI = "dme2://DME2SEARCH/service=com.att.aft.dme2.test.TestDME2EndpointIterator_SetElementStale/version=1.0.0/envContext=LAB/partner=DME2_TEST";
			
			DME2BaseEndpointIterator endpointIterator = EndpointIteratorFactory
					.getDefaultEndpointIteratorBuilder(new DME2Configuration(mgr_1_name, props))
					.setServiceURI(clientURI)
					.setManager(mgr_1)
					.build();
			
			List<String> staleEndpoints = new ArrayList<String>();
			
			while(endpointIterator.hasNext())
			{
				DME2EndpointReference ref = endpointIterator.next();
				StaleProcessor.setStale(ref.getRouteOffer(), mgr_1, ref.getEndpoint());
				staleEndpoints.add(ref.getEndpoint().toURLString());
			}
			/*
			DME2Cache staleEndpointCache = mgr_1.getStaleCache().
			System.out.println("Size of stale cache (should be 4): " + (((AbstractCache)staleEndpointCache).getCurrentSize()));
			System.out.println("Contents of stale cache: " + (((AbstractCache)staleEndpointCache).getKeys()));
			assertEquals(4, (((AbstractCache)staleEndpointCache).getCurrentSize()));*/
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{
			try{mgr_1.unbindServiceListener(service_1);}
			catch(Exception e){}
			
			try{mgr_2.unbindServiceListener(service_2);}
			catch(Exception e){}
			
			try{mgr_3.unbindServiceListener(service_3);}
			catch(Exception e){}
			
			try{mgr_4.unbindServiceListener(service_4);}
			catch(Exception e){}
		}
	}
	
	
    //@Test
    public void testDME2EndpointIterator_SetEndpointsStale_2()
	{
		DME2BaseEndpointIterator endpointIterator = null;
		
		DME2Manager mgr_1 = null;
		DME2Manager mgr_2 = null;
		DME2Manager mgr_3 = null;
		DME2Manager mgr_4 = null;
		
		String service_1 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_SetElementStale_2", "1.0.0", "LAB", "A1");
		String service_2 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_SetElementStale_2", "1.0.0", "LAB", "A2");
		String service_3 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_SetElementStale_2", "1.0.0", "LAB", "B1");
		String service_4 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_SetElementStale_2", "1.0.0", "LAB", "B2");
		
		try
		{
			Properties props = TestUtils.initAFTProperties();

			String mgr_1_name = "testDME2EndpointIterator_SetEndpointsStale_1";
			String mgr_2_name = "testDME2EndpointIterator_SetEndpointsStale_2";
			String mgr_3_name = "testDME2EndpointIterator_SetEndpointsStale_3";
			String mgr_4_name = "testDME2EndpointIterator_SetEndpointsStale_4";

			mgr_1 = new DME2Manager(mgr_1_name, new DME2Configuration(mgr_1_name, props));
			mgr_2 = new DME2Manager(mgr_2_name, new DME2Configuration(mgr_2_name, props));	
			mgr_3 = new DME2Manager(mgr_3_name, new DME2Configuration(mgr_3_name, props));	
			mgr_4 = new DME2Manager(mgr_4_name, new DME2Configuration(mgr_4_name, props));	
			
			//mgr_1.bindServiceListener(service_1, new DME2SimpleServlet());
			//mgr_2.bindServiceListener(service_2, new DME2SimpleServlet());
			//mgr_3.bindServiceListener(service_3, new DME2SimpleServlet());
			//mgr_4.bindServiceListener(service_4, new DME2SimpleServlet());
			
			Thread.sleep(3000);
			
			String clientURI = "http://DME2SEARCH/service=com.att.aft.dme2.test.TestDME2EndpointIterator_SetElementStale_2/version=1.0.0/envContext=LAB/partner=DME2_TEST";
			
			endpointIterator = EndpointIteratorFactory
					.getDefaultEndpointIteratorBuilder(new DME2Configuration(mgr_1_name, props))
					.setServiceURI(clientURI)
					.setManager(mgr_1)
					.build();
			
			List<String> staleEndpoints = new ArrayList<String>();
			//((AbstractCache)(mgr_1.getStaleEndpointCache().getCache())).clear();
      mgr_1.getStaleCache().clearStaleEndpoints();
			
			//Marking all elements in the Iterator stale.
			while(endpointIterator.hasNext())
			{
				DME2EndpointReference ref = endpointIterator.next();
				StaleProcessor.setStale(ref.getRouteOffer(),mgr_1,ref.getEndpoint());
				staleEndpoints.add(ref.getEndpoint().toURLString());
			}
			

			/*DME2Cache staleEndpointCache = mgr_1.getStaleEndpointCache().getCache();
			System.out.println("Size of stale cache (should be 4): " + (((AbstractCache)staleEndpointCache).getCurrentSize()));
			System.out.println("Contents of stale cache: " + (((AbstractCache)staleEndpointCache).getKeys()));
			assertEquals(4, (((AbstractCache)staleEndpointCache).getCurrentSize()));*/
			
			/* Reseting the iterator to restore all value and reset the positioning. 
			 * This does not remove elements from stale cache, so the next attempt to 
			 * Iterate over the elements should identify them all as stale */
			endpointIterator.resetIterator();
			
			while(endpointIterator.hasNext())
			{
				endpointIterator.next();
			}
			
			System.out.println("Number of stale elements (Should be 4): " + endpointIterator.getNumberOfStaleElements());
			assertEquals(4, endpointIterator.getNumberOfStaleElements());
			
			System.out.println("Number of active elements (Should be 0): " + endpointIterator.getNumberOfActiveElements());
			assertEquals(0, endpointIterator.getNumberOfActiveElements());
					
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{
			try{mgr_1.unbindServiceListener(service_1);}
			catch(Exception e){}
			
			try{mgr_2.unbindServiceListener(service_2);}
			catch(Exception e){}
			
			try{mgr_3.unbindServiceListener(service_3);}
			catch(Exception e){}
			
			try{mgr_4.unbindServiceListener(service_4);}
			catch(Exception e){}
			
			try{endpointIterator.removeAllStaleIteratorElements();}
			catch(Exception e){}
		}
	}
	
	
    //@Test
    public void testDME2EndpointIterator_SetEndpointsStale_3()
	{
		DME2BaseEndpointIterator endpointIterator = null;
		
		DME2Manager mgr_1 = null;
		DME2Manager mgr_2 = null;
		
		String service_1 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_SetElementStale_3", "1.1.0", "LAB", "D1");
		String service_2 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_SetElementStale_3", "1.2.0", "LAB", "D1");
		
		try
		{
			Properties props = TestUtils.initAFTProperties();

			String mgr_1_name = "testDME2EndpointIterator_SetEndpointsStale_1";
			String mgr_2_name = "testDME2EndpointIterator_SetEndpointsStale_2";

			mgr_1 = new DME2Manager(mgr_1_name, new DME2Configuration(mgr_1_name, props));
			mgr_2 = new DME2Manager(mgr_2_name, new DME2Configuration(mgr_2_name, props));	
			
			//mgr_1.bindServiceListener(service_1, new DME2SimpleServlet());
			//mgr_2.bindServiceListener(service_2, new DME2SimpleServlet());
			
			Thread.sleep(3000);
			
			String clientURI = "http://DME2RESOLVE/service=com.att.aft.dme2.test.TestDME2EndpointIterator_SetElementStale_3/version=1/envContext=LAB/routeOffer=D1";
			
			endpointIterator = EndpointIteratorFactory
					.getDefaultEndpointIteratorBuilder(new DME2Configuration(mgr_1_name, props))
					.setServiceURI(clientURI)
					.setManager(mgr_1)
					.build();

			List<String> staleEndpoints = new ArrayList<String>();
			//mgr_1.getStaleEndpointCache().getCache().clear();
      mgr_1.getStaleCache().clearStaleEndpoints();
			
			//Marking all elements in the Iterator stale.
			while(endpointIterator.hasNext())
			{
				DME2EndpointReference ref = endpointIterator.next();
				if(ref.getEndpoint().getServiceEndpointID().contains("1.2.0"))
				{
					System.out.println("Setting Endpoint Stale: " + ref.getEndpoint().getServiceEndpointID());
					StaleProcessor.setStale(ref.getRouteOffer(), mgr_1, ref.getEndpoint());
					staleEndpoints.add(ref.getEndpoint().getServiceEndpointID());
				}
			}
			
			/*DME2Cache staleEndpointCache = mgr_1.getStaleEndpointCache().getCache();
			System.out.println("Size of stale cache (should be 4): " + (((AbstractCache)staleEndpointCache).getCurrentSize()));
			System.out.println("Contents of stale cache: " + (((AbstractCache)staleEndpointCache).getKeys()));
			assertEquals(1, (((AbstractCache)staleEndpointCache).getCurrentSize()));*/
			
			/* Reseting the iterator to restore all value and reset the positioning. 
			 * This does not remove elements from stale cache, so the next attempt to 
			 * Iterate over the elements should identify them all as stale */
			endpointIterator.resetIterator();
			
			while(endpointIterator.hasNext())
			{
				endpointIterator.next();
			}
			
			System.out.println("Number of stale elements (Should be 1): " + endpointIterator.getNumberOfStaleElements());
			assertEquals(1, endpointIterator.getNumberOfStaleElements());
			
			System.out.println("Number of active elements (Should be 1): " + endpointIterator.getNumberOfActiveElements());
			assertEquals(1, endpointIterator.getNumberOfActiveElements());
					
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{
			try{mgr_1.unbindServiceListener(service_1);}
			catch(Exception e){}
			
			try{mgr_2.unbindServiceListener(service_2);}
			catch(Exception e){}
			
		}
	}
	
    //@Test
    public void testDME2EndpointIterator_SetEndpointsStale_4()
	{
		System.setProperty(DME2Constants.DME2_ENDPOINT_STALENESS_PERIOD, "54000");
		
		try
		{
			String mgr_1_name = "IntTest";
            DME2Manager mgr = new DME2Manager(mgr_1_name, new DME2Configuration(mgr_1_name, new Properties()));
			
            String clientURI = "http://DME2SEARCH/service=com.att.aft.DME2CREchoService/version=1/envContext=LAB/partner=BAU/dataContext=404988";
       
			DME2BaseEndpointIterator endpointIterator = EndpointIteratorFactory
					.getDefaultEndpointIteratorBuilder(new DME2Configuration())
					.setServiceURI(clientURI)
					.setManager(mgr)
					.build();

            while(endpointIterator.hasNext())
            {
                    DME2EndpointReference ref = endpointIterator.next();
                    DME2Endpoint endpoint = ref.getEndpoint();

                    System.out.println("Marking endpoint stale: " + endpoint.getServiceEndpointID());
                    StaleProcessor.setStale(ref.getRouteOffer(), mgr, ref.getEndpoint());
                    break;
            }

            endpointIterator = EndpointIteratorFactory
					.getDefaultEndpointIteratorBuilder(new DME2Configuration())
					.setServiceURI(clientURI)
					.build();

            while(endpointIterator.hasNext())
            {
                    DME2EndpointReference ref = endpointIterator.next();
                    DME2Endpoint endpoint = ref.getEndpoint();
                    System.out.println("Is  endpoint stale: " + endpoint.getServiceEndpointID() + "--------" +   endpointIterator.isStale() );

            }

            Thread.sleep(15000);

            endpointIterator = EndpointIteratorFactory
					.getDefaultEndpointIteratorBuilder(new DME2Configuration())
					.setServiceURI(clientURI)
					.build();

            while(endpointIterator.hasNext())
            {
                DME2EndpointReference ref = endpointIterator.next();
                DME2Endpoint endpoint = ref.getEndpoint();
                System.out.println("Is  endpoint stale: " + endpoint.getServiceEndpointID() + "--------" + endpointIterator.isStale() );

            }

            Thread.sleep(60000);

            endpointIterator = EndpointIteratorFactory
					.getDefaultEndpointIteratorBuilder(new DME2Configuration())
					.setServiceURI(clientURI)
					.build();

           while(endpointIterator.hasNext())
           {
                   DME2EndpointReference ref = endpointIterator.next();
                   DME2Endpoint endpoint = ref.getEndpoint();
                   System.out.println("Is  endpoint stale: " + endpoint.getServiceEndpointID() + "--------" + endpointIterator.isStale() );
                   assertTrue(!endpointIterator.isStale());

           }

           //System.out.println(mgr.getStaleEndpointCache().getCache());
           //assert(mgr.getStaleEndpointCache().getCache().getCurrentSize()==0);


		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			System.clearProperty(DME2Constants.DME2_ENDPOINT_STALENESS_PERIOD);
		}
	}
	
    //@Test
    public void testDME2EndpointIterator_UsePreferredRouteOffer()
	{
		DME2BaseEndpointIterator iter = null;
		
		DME2Manager mgr_1 = null;
		DME2Manager mgr_2 = null;
		DME2Manager mgr_3 = null;
		DME2Manager mgr_4 = null;
		
		String service_1 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_UsePreferredRouteOffer", "1.0.0", "LAB", "A1");
		String service_2 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_UsePreferredRouteOffer", "1.0.0", "LAB", "A2");
		String service_3 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_UsePreferredRouteOffer", "1.0.0", "LAB", "B1");
		String service_4 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_UsePreferredRouteOffer", "1.0.0", "LAB", "B2");
		
		try
		{
			Properties props = TestUtils.initAFTProperties();

			String mgr_1_name = "testDME2EndpointIterator_UsePreferredRouteOffer_1";
			String mgr_2_name = "testDME2EndpointIterator_UsePreferredRouteOffer_2";
			String mgr_3_name = "testDME2EndpointIterator_UsePreferredRouteOffer_3";
			String mgr_4_name = "testDME2EndpointIterator_UsePreferredRouteOffer_4";

			mgr_1 = new DME2Manager(mgr_1_name, new DME2Configuration(mgr_1_name, props));
			mgr_2 = new DME2Manager(mgr_2_name, new DME2Configuration(mgr_2_name, props));	
			mgr_3 = new DME2Manager(mgr_3_name, new DME2Configuration(mgr_3_name, props));	
			mgr_4 = new DME2Manager(mgr_4_name, new DME2Configuration(mgr_4_name, props));	
			
			//mgr_1.bindServiceListener(service_1, new DME2SimpleServlet());
			//mgr_2.bindServiceListener(service_2, new DME2SimpleServlet());
			//mgr_3.bindServiceListener(service_3, new DME2SimpleServlet());
			//mgr_4.bindServiceListener(service_4, new DME2SimpleServlet());
			
			Thread.sleep(3000);
			
			String clientURI = "http://DME2SEARCH/service=com.att.aft.dme2.test.TestDME2EndpointIterator_UsePreferredRouteOffer/version=1.0.0/envContext=LAB/partner=DME2_TEST";
			
			props = new Properties();
			props.put("AFT_DME2_PREFERRED_ROUTEOFFER", "B2");
			
            DME2BaseEndpointIterator endpointIterator = EndpointIteratorFactory
					.getDefaultEndpointIteratorBuilder(new DME2Configuration(mgr_1_name, props))
					.setServiceURI(clientURI)
					.setManager(mgr_1)
					.setProps(props)
					.build();

 			//First Iterator element should contain routeOffer B2 since we assigned it as our preferred routeOffer
			while(endpointIterator.hasNext())
			{
				DME2EndpointReference ref = endpointIterator.next();
				System.out.println("RouteOffer: " + ref.getEndpoint().getRouteOffer());
				assertEquals("B2", ref.getEndpoint().getRouteOffer());
				break;
			}
				
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{
			try{mgr_1.unbindServiceListener(service_1);}
			catch(Exception e){}
			
			try{mgr_2.unbindServiceListener(service_2);}
			catch(Exception e){}
			
			try{mgr_3.unbindServiceListener(service_3);}
			catch(Exception e){}
			
			try{mgr_4.unbindServiceListener(service_4);}
			catch(Exception e){}
			
			try{iter.removeAllStaleIteratorElements();}
			catch(Exception e){}
		}
	}
}