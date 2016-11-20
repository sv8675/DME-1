/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.iterator.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;

import org.junit.Ignore;
import org.junit.Test;

import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.iterator.DME2EndpointIterator;
import com.att.aft.dme2.iterator.domain.DME2EndpointReference;
import com.att.aft.dme2.iterator.factory.DME2EndpointIteratorFactory;
import com.att.aft.dme2.iterator.test.integration.TestUtils;
import com.att.aft.dme2.iterator.test.servlet.DME2SimpleServlet;
import com.att.aft.dme2.manager.registry.DME2Endpoint;
import com.att.aft.dme2.test.DME2BaseTestCase;
import com.att.aft.dme2.util.DME2Utils;



public class TestDME2EndpointIterator extends DME2BaseTestCase {
	@Ignore   
    @Test
    public void testDME2EndpointIterator_ResolveEndpoints_1()
	{
		//This test case uses DME2SEARCH for the lookup.
		
		DME2Manager mgr_1 = null;
		DME2Manager mgr_2 = null;
		DME2Manager mgr_3 = null;
		DME2Manager mgr_4 = null;
		
		String service_1 = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints", "1.0.0", "LAB", "A1");
		String service_2 = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints", "1.0.0", "LAB", "A2");
		String service_3 = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints", "1.0.0", "LAB", "B1");
		String service_4 = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints", "1.0.0", "LAB", "B2");
		
		try
		{
			mgr_1 = new DME2Manager("testDME2EndpointIterator_ResolveEndpoints_1.1", TestUtils.initAFTProperties());
			mgr_2 = new DME2Manager("testDME2EndpointIterator_ResolveEndpoints_1.2", TestUtils.initAFTProperties());
			mgr_3 = new DME2Manager("testDME2EndpointIterator_ResolveEndpoints_1.3", TestUtils.initAFTProperties());
			mgr_4 = new DME2Manager("testDME2EndpointIterator_ResolveEndpoints_1.4", TestUtils.initAFTProperties());
			
			mgr_1.bindServiceListener(service_1, new DME2SimpleServlet());
			mgr_2.bindServiceListener(service_2, new DME2SimpleServlet());
			mgr_3.bindServiceListener(service_3, new DME2SimpleServlet());
			mgr_4.bindServiceListener(service_4, new DME2SimpleServlet());
			
			Thread.sleep(3000);
			
			String clientURI = "http://DME2SEARCH/service=com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints/version=1.0.0/envContext=LAB/partner=DME2_TEST";
			
			DME2EndpointIteratorFactory factory = DME2EndpointIteratorFactory.getInstance();
			DME2EndpointIterator iter = (DME2EndpointIterator) factory.getIterator(clientURI, null, null, mgr_1);
			
			List<String> servicesToValidate = new ArrayList<String>();
			
			while(iter.hasNext())
			{
				DME2EndpointReference ref = iter.next();
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
	
    @Test
    public void testDME2EndpointIterator_ResolveEndpoints_2()
	{
		//This test case uses DME2RESOLVE for the lookup.
		
		DME2Manager mgr_1 = null;
		DME2Manager mgr_2 = null;
		
		String service_1 = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints_2", "1.0.0", "LAB", "D1");
		String service_2 = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints_2", "1.0.0", "LAB", "D1");

		
		try
		{
			mgr_1 = new com.att.aft.dme2.api.DME2Manager("testDME2EndpointIterator_ResolveEndpoints_2.1", TestUtils.initAFTProperties());
			mgr_2 = new DME2Manager("testDME2EndpointIterator_ResolveEndpoints_2.2", TestUtils.initAFTProperties());
			
			mgr_1.bindServiceListener(service_1, new DME2SimpleServlet());
			mgr_2.bindServiceListener(service_2, new DME2SimpleServlet());
			
			Thread.sleep(3000);
			
			String clientURI = "http://DME2RESOLVE/service=com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints_2/version=1.0.0/envContext=LAB/routeOffer=D1";
			
			DME2EndpointIteratorFactory factory = DME2EndpointIteratorFactory.getInstance();
			DME2EndpointIterator iter = (DME2EndpointIterator) factory.getIterator(clientURI, null, null, mgr_1);
			
			List<String> servicesToValidate = new ArrayList<String>();
			
			while(iter.hasNext())
			{
				DME2EndpointReference ref = iter.next();
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
	
	
    @Test
    public void testDME2EndpointIterator_AllEndpointsExhausted_1()
	{	
		DME2Manager mgr_1 = null;
		DME2Manager mgr_2 = null;
		
		String service_1 = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_AllEndpointsExhausted_1", "1.0.0", "LAB", "D1");
		String service_2 = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_AllEndpointsExhausted_1", "1.0.0", "LAB", "D1");

		
		try
		{
			mgr_1 = new DME2Manager("testDME2EndpointIterator_AllEndpointsExhausted_1.1", TestUtils.initAFTProperties());
			mgr_2 = new DME2Manager("testDME2EndpointIterator_AllEndpointsExhausted_1.2", TestUtils.initAFTProperties());
			
			mgr_1.bindServiceListener(service_1, new DME2SimpleServlet());
			mgr_2.bindServiceListener(service_2, new DME2SimpleServlet());
			
			Thread.sleep(3000);
			
			String clientURI = "http://DME2RESOLVE/service=com.att.aft.dme2.test.TestDME2EndpointIterator_AllEndpointsExhausted_1/version=1.0.0/envContext=LAB/routeOffer=D1";
			
			DME2EndpointIteratorFactory factory = DME2EndpointIteratorFactory.getInstance();
			DME2EndpointIterator iter = (DME2EndpointIterator) factory.getIterator(clientURI, null, null, mgr_1);
			
			while(iter.hasNext())
			{
				iter.next();
			}
			
			System.out.println("Iterator has next: " + iter.hasNext());
			System.out.println("Number of active elements: " + iter.getNumberOfActiveElements());
			
			assertFalse(iter.hasNext());
			assertEquals(2, iter.getNumberOfActiveElements());
			
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
	
	
 @Test
    public void testDME2EndpointIterator_AllEndpointsExhausted_2()
	{	
		DME2Manager mgr_1 = null;
		DME2Manager mgr_2 = null;
		
		String service_1 = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_AllEndpointsExhausted_2", "1.0.0", "LAB", "D1");
		String service_2 = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_AllEndpointsExhausted_2", "1.0.0", "LAB", "D1");

		
		try
		{
			mgr_1 = new DME2Manager("testDME2EndpointIterator_AllEndpointsExhausted_2.1", TestUtils.initAFTProperties());
			mgr_2 = new DME2Manager("testDME2EndpointIterator_AllEndpointsExhausted_2.2", TestUtils.initAFTProperties());
			
			mgr_1.bindServiceListener(service_1, new DME2SimpleServlet());
			mgr_2.bindServiceListener(service_2, new DME2SimpleServlet());
			
			Thread.sleep(3000);
			
			String clientURI = "http://DME2RESOLVE/service=com.att.aft.dme2.test.TestDME2EndpointIterator_AllEndpointsExhausted_2/version=1.0.0/envContext=LAB/routeOffer=D1";
			
			DME2EndpointIteratorFactory factory = DME2EndpointIteratorFactory.getInstance();
			DME2EndpointIterator iter = (DME2EndpointIterator) factory.getIterator(clientURI, null, null, mgr_1);
			
			//Set the first element stale.
			while(iter.hasNext())
			{
				DME2EndpointReference ref = iter.next();
					ref.setStale();
				break;		
			}
			
			//Reset the Iterator
			iter.resetIterator();
			
			while(iter.hasNext())
			{
				iter.next();
			}
			
			//One element should have been skipped due to being stale and one other element should be active.
			System.out.println("Iterator has next: " + iter.hasNext());
			System.out.println("Number of active elements: " + iter.getNumberOfActiveElements());
			System.out.println("Number of stale elements: " + iter.getNumberOfStaleElements());
			
			assertFalse(iter.hasNext());
			assertEquals(1, iter.getNumberOfActiveElements());
			assertEquals(1, iter.getNumberOfStaleElements());
			
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
	
	
 @Ignore     
 @Test
    public void testDME2EndpointIterator_IllegallyRemoveElement_1()
	{
		DME2Manager mgr_1 = null;
		DME2Manager mgr_2 = null;
		DME2Manager mgr_3 = null;
		DME2Manager mgr_4 = null;
		
		String service_1 = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.testDME2EndpointIterator_IllegallyRemoveElement_1", "1.0.0", "LAB", "A1");
		String service_2 = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.testDME2EndpointIterator_IllegallyRemoveElement_1", "1.0.0", "LAB", "A2");
		String service_3 = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.testDME2EndpointIterator_IllegallyRemoveElement_1", "1.0.0", "LAB", "B1");
		String service_4 = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.testDME2EndpointIterator_IllegallyRemoveElement_1", "1.0.0", "LAB", "B2");
		
		try
		{
			mgr_1 = new DME2Manager("testDME2EndpointIterator_IllegallyRemoveElement_1.1", TestUtils.initAFTProperties());
			mgr_2 = new DME2Manager("testDME2EndpointIterator_IllegallyRemoveElement_1.2", TestUtils.initAFTProperties());
			mgr_3 = new DME2Manager("testDME2EndpointIterator_IllegallyRemoveElement_1.3", TestUtils.initAFTProperties());
			mgr_4 = new DME2Manager("testDME2EndpointIterator_IllegallyRemoveElement_1.4", TestUtils.initAFTProperties());
			
			mgr_1.bindServiceListener(service_1, new DME2SimpleServlet());
			mgr_2.bindServiceListener(service_2, new DME2SimpleServlet());
			mgr_3.bindServiceListener(service_3, new DME2SimpleServlet());
			mgr_4.bindServiceListener(service_4, new DME2SimpleServlet());
			
			Thread.sleep(3000);
			
			String clientURI = "http://DME2SEARCH/service=com.att.aft.dme2.test.testDME2EndpointIterator_IllegallyRemoveElement_1/version=1.0.0/envContext=LAB/partner=DME2_TEST";
			
			DME2EndpointIteratorFactory factory = DME2EndpointIteratorFactory.getInstance();
			DME2EndpointIterator iter = (DME2EndpointIterator) factory.getIterator(clientURI, null, null, mgr_1);

			while(iter.hasNext())
			{
				iter.remove();
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
	
 @Ignore   
    @Test
    public void testDME2EndpointIterator_IllegallyRemoveElement_2()
	{
		DME2Manager mgr_1 = null;
		DME2Manager mgr_2 = null;
		DME2Manager mgr_3 = null;
		DME2Manager mgr_4 = null;
		
		String service_1 = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.testDME2EndpointIterator_IllegallyRemoveElement_2", "1.0.0", "LAB", "A1");
		String service_2 = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.testDME2EndpointIterator_IllegallyRemoveElement_2", "1.0.0", "LAB", "A2");
		String service_3 = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.testDME2EndpointIterator_IllegallyRemoveElement_2", "1.0.0", "LAB", "B1");
		String service_4 = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.testDME2EndpointIterator_IllegallyRemoveElement_2", "1.0.0", "LAB", "B2");
		
		try
		{
			mgr_1 = new DME2Manager("testDME2EndpointIterator_IllegallyRemoveElement_2.1", TestUtils.initAFTProperties());
			mgr_2 = new DME2Manager("testDME2EndpointIterator_IllegallyRemoveElement_2.2", TestUtils.initAFTProperties());
			mgr_3 = new DME2Manager("testDME2EndpointIterator_IllegallyRemoveElement_2.3", TestUtils.initAFTProperties());
			mgr_4 = new DME2Manager("testDME2EndpointIterator_IllegallyRemoveElement_2.4", TestUtils.initAFTProperties());
			
			mgr_1.bindServiceListener(service_1, new DME2SimpleServlet());
			mgr_2.bindServiceListener(service_2, new DME2SimpleServlet());
			mgr_3.bindServiceListener(service_3, new DME2SimpleServlet());
			mgr_4.bindServiceListener(service_4, new DME2SimpleServlet());
			
			Thread.sleep(3000);
			
			String clientURI = "http://DME2SEARCH/service=com.att.aft.dme2.test.testDME2EndpointIterator_IllegallyRemoveElement_2/version=1.0.0/envContext=LAB/partner=DME2_TEST";
			
			DME2EndpointIteratorFactory factory = DME2EndpointIteratorFactory.getInstance();
			DME2EndpointIterator iter = (DME2EndpointIterator) factory.getIterator(clientURI, null, null, mgr_1);

			while(iter.hasNext())
			{
				iter.next();
				iter.remove();
				iter.remove();
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
	
    @Ignore   
    @Test
    public void testDME2EndpointIterator_FailOnNoSuchElementException()
	{
		DME2Manager mgr_1 = null;
		DME2Manager mgr_2 = null;
		DME2Manager mgr_3 = null;
		DME2Manager mgr_4 = null;
		
		String service_1 = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.testDME2EndpointIterator_FailOnNoSuchElementException", "1.0.0", "LAB", "A1");
		String service_2 = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.testDME2EndpointIterator_FailOnNoSuchElementException", "1.0.0", "LAB", "A2");
		String service_3 = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.testDME2EndpointIterator_FailOnNoSuchElementException", "1.0.0", "LAB", "B1");
		String service_4 = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.testDME2EndpointIterator_FailOnNoSuchElementException", "1.0.0", "LAB", "B2");
		
		try
		{
			mgr_1 = new DME2Manager("testDME2EndpointIterator_FailOnNoSuchElementException.1", TestUtils.initAFTProperties());
			mgr_2 = new DME2Manager("testDME2EndpointIterator_FailOnNoSuchElementException.2", TestUtils.initAFTProperties());
			mgr_3 = new DME2Manager("testDME2EndpointIterator_FailOnNoSuchElementException.3", TestUtils.initAFTProperties());
			mgr_4 = new DME2Manager("testDME2EndpointIterator_FailOnNoSuchElementException.4", TestUtils.initAFTProperties());
			
			mgr_1.bindServiceListener(service_1, new DME2SimpleServlet());
			mgr_2.bindServiceListener(service_2, new DME2SimpleServlet());
			mgr_3.bindServiceListener(service_3, new DME2SimpleServlet());
			mgr_4.bindServiceListener(service_4, new DME2SimpleServlet());
			
			Thread.sleep(3000);
			
			String clientURI = "http://DME2SEARCH/service=com.att.aft.dme2.test.testDME2EndpointIterator_FailOnNoSuchElementException/version=1.0.0/envContext=LAB/partner=DME2_TEST";
			
			DME2EndpointIteratorFactory factory = DME2EndpointIteratorFactory.getInstance();
			DME2EndpointIterator iter = (DME2EndpointIterator) factory.getIterator(clientURI, null, null, mgr_1);
			
			/*Exhaust all elements in the iterator until NoSuchElementException is thrown*/
			iter.next();
			iter.next();
			iter.next();
			iter.next();
			iter.next();

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
	
	
    @Ignore   
    @Test
    public void testDME2EndpointIterator_RemoveElements()
	{
		DME2Manager mgr_1 = null;
		DME2Manager mgr_2 = null;
		DME2Manager mgr_3 = null;
		DME2Manager mgr_4 = null;
		
		String service_1 = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_RemoveElements", "1.0.0", "LAB", "A1");
		String service_2 = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_RemoveElements", "1.0.0", "LAB", "A2");
		String service_3 = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_RemoveElements", "1.0.0", "LAB", "B1");
		String service_4 = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_RemoveElements", "1.0.0", "LAB", "B2");
		
		try
		{
			mgr_1 = new DME2Manager("TestDME2EndpointIterator_RemoveElements_1", TestUtils.initAFTProperties());
			mgr_2 = new DME2Manager("TestDME2EndpointIterator_RemoveElements_2", TestUtils.initAFTProperties());
			mgr_3 = new DME2Manager("TestDME2EndpointIterator_RemoveElements_3", TestUtils.initAFTProperties());
			mgr_4 = new DME2Manager("TestDME2EndpointIterator_RemoveElements_4", TestUtils.initAFTProperties());
			
			mgr_1.bindServiceListener(service_1, new DME2SimpleServlet());
			mgr_2.bindServiceListener(service_2, new DME2SimpleServlet());
			mgr_3.bindServiceListener(service_3, new DME2SimpleServlet());
			mgr_4.bindServiceListener(service_4, new DME2SimpleServlet());
			
			Thread.sleep(3000);
			
			String clientURI = "http://DME2SEARCH/service=com.att.aft.dme2.test.TestDME2EndpointIterator_RemoveElements/version=1.0.0/envContext=LAB/partner=DME2_TEST";
			
			DME2EndpointIteratorFactory factory = DME2EndpointIteratorFactory.getInstance();
			DME2EndpointIterator iter = (DME2EndpointIterator) factory.getIterator(clientURI, null, null, mgr_1);
			System.out.println("Starting number of Active Elements: " + iter);
			
			System.out.println("Starting number of Active Elements: " + iter.getNumberOfActiveElements());
			assertEquals(4, iter.getNumberOfActiveElements());
			
			while(iter.hasNext())
			{
				iter.next();
				iter.remove();
			}
			
			System.out.println("Number of Removed Elements: " + iter.getNumberOfRemovedElements());
			assertEquals(4, iter.getNumberOfRemovedElements());
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
	
	
    @Test
    public void testDME2EndpointIterator_ReturnNoEndpoints()
	{
		try
		{	
			String clientURI = "http://DME2RESOLVE/service=com.att.aft.dme2.test.TestDME2EndpointIterator_ReturnNoEndpoints/version=1.0.0/envContext=LAB/routeOffer=DME2_TEST";
			
			DME2EndpointIteratorFactory factory = DME2EndpointIteratorFactory.getInstance();
			DME2EndpointIterator iter = (DME2EndpointIterator) factory.getIterator(clientURI, null, null);
			assertFalse(iter.hasNext());
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
	
	
//    @Test
//    public void testDME2EndpointIterator_SetEndpointsStale_1()
//	{
//		DME2Manager mgr_1 = null;
//		DME2Manager mgr_2 = null;
//		DME2Manager mgr_3 = null;
//		DME2Manager mgr_4 = null;
//		
//		String service_1 = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_SetElementStale", "1.0.0", "LAB", "A1");
//		String service_2 = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_SetElementStale", "1.0.0", "LAB", "A2");
//		String service_3 = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_SetElementStale", "1.0.0", "LAB", "B1");
//		String service_4 = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_SetElementStale", "1.0.0", "LAB", "B2");
//		
//		try
//		{
//			mgr_1 = new DME2Manager("testDME2EndpointIterator_ResolveEndpoints_1", TestUtils.initAFTProperties());
//			mgr_2 = new DME2Manager("testDME2EndpointIterator_ResolveEndpoints_2", TestUtils.initAFTProperties());
//			mgr_3 = new DME2Manager("testDME2EndpointIterator_ResolveEndpoints_3", TestUtils.initAFTProperties());
//			mgr_4 = new DME2Manager("testDME2EndpointIterator_ResolveEndpoints_4", TestUtils.initAFTProperties());
//			
//			mgr_1.bindServiceListener(service_1, new DME2SimpleServlet());
//			mgr_2.bindServiceListener(service_2, new DME2SimpleServlet());
//			mgr_3.bindServiceListener(service_3, new DME2SimpleServlet());
//			mgr_4.bindServiceListener(service_4, new DME2SimpleServlet());
//			
//			Thread.sleep(3000);
//			
//			String clientURI = "dme2://DME2SEARCH/service=com.att.aft.dme2.test.TestDME2EndpointIterator_SetElementStale/version=1.0.0/envContext=LAB/partner=DME2_TEST";
//			
//			DME2EndpointIteratorFactory factory = DME2EndpointIteratorFactory.getInstance();
//			DME2EndpointIterator iter = (DME2EndpointIterator) factory.getIterator(clientURI, null, null, mgr_1);
//			
//			List<String> staleEndpoints = new ArrayList<String>();
//			
//			while(iter.hasNext())
//			{
//				DME2EndpointReference ref = iter.next();
//				ref.setStale();
//				staleEndpoints.add(ref.getEndpoint().toURLString());
//			}
//			
//			Map<String, Long> staleEndpointCache = iter.getManager().getStaleEndpointCache().getCache();
//			System.out.println("Size of stale cache (should be 4): " + staleEndpointCache.size());
//			System.out.println("Contents of stale cache: " + staleEndpointCache.keySet());
//			assertEquals(4, staleEndpointCache.size());
//		}
//		catch(Exception e)
//		{
//			e.printStackTrace();
//			fail(e.getMessage());
//		}
//		finally
//		{
//			try{mgr_1.unbindServiceListener(service_1);}
//			catch(Exception e){}
//			
//			try{mgr_2.unbindServiceListener(service_2);}
//			catch(Exception e){}
//			
//			try{mgr_3.unbindServiceListener(service_3);}
//			catch(Exception e){}
//			
//			try{mgr_4.unbindServiceListener(service_4);}
//			catch(Exception e){}
//		}
//	}
//	
//	
//    @Test
//    public void testDME2EndpointIterator_SetEndpointsStale_2()
//	{
//		DME2EndpointIterator iter = null;
//		
//		DME2Manager mgr_1 = null;
//		DME2Manager mgr_2 = null;
//		DME2Manager mgr_3 = null;
//		DME2Manager mgr_4 = null;
//		
//		String service_1 = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_SetElementStale_2", "1.0.0", "LAB", "A1");
//		String service_2 = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_SetElementStale_2", "1.0.0", "LAB", "A2");
//		String service_3 = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_SetElementStale_2", "1.0.0", "LAB", "B1");
//		String service_4 = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_SetElementStale_2", "1.0.0", "LAB", "B2");
//		
//		try
//		{
//			mgr_1 = new DME2Manager("testDME2EndpointIterator_SetEndpointsStale_1", TestUtils.initAFTProperties());
//			mgr_2 = new DME2Manager("testDME2EndpointIterator_SetEndpointsStale_2", TestUtils.initAFTProperties());
//			mgr_3 = new DME2Manager("testDME2EndpointIterator_SetEndpointsStale_3", TestUtils.initAFTProperties());
//			mgr_4 = new DME2Manager("testDME2EndpointIterator_SetEndpointsStale_4", TestUtils.initAFTProperties());
//			
//			mgr_1.bindServiceListener(service_1, new DME2SimpleServlet());
//			mgr_2.bindServiceListener(service_2, new DME2SimpleServlet());
//			mgr_3.bindServiceListener(service_3, new DME2SimpleServlet());
//			mgr_4.bindServiceListener(service_4, new DME2SimpleServlet());
//			
//			Thread.sleep(3000);
//			
//			String clientURI = "http://DME2SEARCH/service=com.att.aft.dme2.test.TestDME2EndpointIterator_SetElementStale_2/version=1.0.0/envContext=LAB/partner=DME2_TEST";
//			
//			DME2EndpointIteratorFactory factory = DME2EndpointIteratorFactory.getInstance();
//			iter = (DME2EndpointIterator) factory.getIterator(clientURI, null, null, mgr_1);
//			
//			List<String> staleEndpoints = new ArrayList<String>();
//			iter.getManager().getStaleEndpointCache().getCache().clear();
//			
//			//Marking all elements in the Iterator stale.
//			while(iter.hasNext())
//			{
//				DME2EndpointReference ref = iter.next();
//				ref.setStale();
//				staleEndpoints.add(ref.getEndpoint().toURLString());
//			}
//			
//			Map<String, Long> staleEndpointCache = iter.getManager().getStaleEndpointCache().getCache();
//			System.out.println("Contents of stale cache: " + staleEndpointCache.keySet());
//			System.out.println("Size of stale cache: " + staleEndpointCache.size());
//			assertEquals(4, staleEndpointCache.size());
//			
//			
//			/* Reseting the iterator to restore all value and reset the positioning. 
//			 * This does not remove elements from stale cache, so the next attempt to 
//			 * Iterate over the elements should identify them all as stale */
//			iter.resetIterator();
//			
//			while(iter.hasNext())
//			{
//				iter.next();
//			}
//			
//			System.out.println("Number of stale elements (Should be 4): " + iter.getNumberOfStaleElements());
//			assertEquals(4, iter.getNumberOfStaleElements());
//			
//			System.out.println("Number of active elements (Should be 0): " + iter.getNumberOfActiveElements());
//			assertEquals(0, iter.getNumberOfActiveElements());
//					
//		}
//		catch(Exception e)
//		{
//			e.printStackTrace();
//			fail(e.getMessage());
//		}
//		finally
//		{
//			try{mgr_1.unbindServiceListener(service_1);}
//			catch(Exception e){}
//			
//			try{mgr_2.unbindServiceListener(service_2);}
//			catch(Exception e){}
//			
//			try{mgr_3.unbindServiceListener(service_3);}
//			catch(Exception e){}
//			
//			try{mgr_4.unbindServiceListener(service_4);}
//			catch(Exception e){}
//			
//			try{iter.removeAllStaleIteratorElements();}
//			catch(Exception e){}
//		}
//	}
//	
//	
//    @Test
//    public void testDME2EndpointIterator_SetEndpointsStale_3()
//	{
//		DME2EndpointIterator iter = null;
//		
//		DME2Manager mgr_1 = null;
//		DME2Manager mgr_2 = null;
//		
//		String service_1 = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_SetElementStale_3", "1.1.0", "LAB", "D1");
//		String service_2 = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_SetElementStale_3", "1.2.0", "LAB", "D1");
//		
//		try
//		{
//			mgr_1 = new DME2Manager("testDME2EndpointIterator_SetEndpointsStale_1", TestUtils.initAFTProperties());
//			mgr_2 = new DME2Manager("testDME2EndpointIterator_SetEndpointsStale_2", TestUtils.initAFTProperties());
//			
//			mgr_1.bindServiceListener(service_1, new DME2SimpleServlet());
//			mgr_2.bindServiceListener(service_2, new DME2SimpleServlet());
//			
//			Thread.sleep(3000);
//			
//			String clientURI = "http://DME2RESOLVE/service=com.att.aft.dme2.test.TestDME2EndpointIterator_SetElementStale_3/version=1/envContext=LAB/routeOffer=D1";
//			
//			DME2EndpointIteratorFactory factory = DME2EndpointIteratorFactory.getInstance();
//			iter = (DME2EndpointIterator) factory.getIterator(clientURI, null, null, mgr_1);
//			
//			List<String> staleEndpoints = new ArrayList<String>();
//			iter.getManager().getStaleEndpointCache().getCache().clear();
//			
//			//Marking all elements in the Iterator stale.
//			while(iter.hasNext())
//			{
//				DME2EndpointReference ref = iter.next();
//				if(ref.getEndpoint().getServiceEndpointID().contains("1.2.0"))
//				{
//					System.out.println("Setting Endpoint Stale: " + ref.getEndpoint().getServiceEndpointID());
//					iter.setStale();
//					staleEndpoints.add(ref.getEndpoint().getServiceEndpointID());
//				}
//			}
//			
//			Map<String, Long> staleEndpointCache = iter.getManager().getStaleEndpointCache().getCache();
//			System.out.println("Contents of stale cache: " + staleEndpointCache.keySet());
//			System.out.println("Size of stale cache: " + staleEndpointCache.size());
//			assertEquals(1, staleEndpointCache.size());
//			
//			
//			/* Reseting the iterator to restore all value and reset the positioning. 
//			 * This does not remove elements from stale cache, so the next attempt to 
//			 * Iterate over the elements should identify them all as stale */
//			iter.resetIterator();
//			
//			while(iter.hasNext())
//			{
//				iter.next();
//			}
//			
//			System.out.println("Number of stale elements (Should be 1): " + iter.getNumberOfStaleElements());
//			assertEquals(1, iter.getNumberOfStaleElements());
//			
//			System.out.println("Number of active elements (Should be 1): " + iter.getNumberOfActiveElements());
//			assertEquals(1, iter.getNumberOfActiveElements());
//					
//		}
//		catch(Exception e)
//		{
//			e.printStackTrace();
//			fail(e.getMessage());
//		}
//		finally
//		{
//			try{mgr_1.unbindServiceListener(service_1);}
//			catch(Exception e){}
//			
//			try{mgr_2.unbindServiceListener(service_2);}
//			catch(Exception e){}
//			
//		}
//	}
//	
//    @Test
//    public void testDME2EndpointIterator_SetEndpointsStale_4()
//	{
//		System.setProperty(DME2Constants.DME2_ENDPOINT_STALENESS_PERIOD, "54000");
//		
//		try
//		{
//            DME2Manager mgr = new DME2Manager("IntTest", new Properties());
//            String clientURI = "http://DME2SEARCH/service=com.att.aft.DME2CREchoService/version=1/envContext=LAB/partner=BAU/dataContext=404988";
//       
//            DME2EndpointIteratorFactory factory = DME2EndpointIteratorFactory.getInstance();
//            DME2EndpointIterator iter = (DME2EndpointIterator) factory.getIterator(clientURI, null, null, mgr);
//
//            while(iter.hasNext())
//            {
//                    DME2EndpointReference ref = iter.next();
//                    DME2Endpoint endpoint = ref.getEndpoint();
//
//                    System.out.println("Marking endpoint stale: " + endpoint.getServiceEndpointID());
//                    iter.setStale();
//                    break;
//            }
//
//
//            iter = (DME2EndpointIterator) factory.getIterator(clientURI, null, null);
//
//            while(iter.hasNext())
//            {
//                    DME2EndpointReference ref = iter.next();
//                    DME2Endpoint endpoint = ref.getEndpoint();
//                    System.out.println("Is  endpoint stale: " + endpoint.getServiceEndpointID() + "--------" + ref.isStale() );
//
//            }
//
//
//            Thread.sleep(15000);
//
//             iter = (DME2EndpointIterator) factory.getIterator(clientURI, null, null);
//
//            while(iter.hasNext())
//            {
//                     DME2EndpointReference ref = iter.next();
//                    DME2Endpoint endpoint = ref.getEndpoint();
//                    System.out.println("Is  endpoint stale: " + endpoint.getServiceEndpointID() + "--------" + ref.isStale() );
//
//            }
//
//            
//            Thread.sleep(60000);
//
//            iter = (DME2EndpointIterator) factory.getIterator(clientURI, null, null);
//
//           while(iter.hasNext())
//           {
//                   DME2EndpointReference ref = iter.next();
//                   DME2Endpoint endpoint = ref.getEndpoint();
//                   System.out.println("Is  endpoint stale: " + endpoint.getServiceEndpointID() + "--------" + ref.isStale() );
//                   assertTrue(!ref.isStale());
//
//           }
//
//           System.out.println(mgr.getStaleEndpointCache().getCache());
//           assert(mgr.getStaleEndpointCache().getCache().isEmpty());
//
//
//		}
//		catch(Exception e)
//		{
//			e.printStackTrace();
//		}
//		finally
//		{
//			System.clearProperty(DME2Constants.DME2_ENDPOINT_STALENESS_PERIOD);
//		}
//	}
//	
//    @Test
//    public void testDME2EndpointIterator_UsePreferredRouteOffer()
//	{
//		DME2EndpointIterator iter = null;
//		
//		DME2Manager mgr_1 = null;
//		DME2Manager mgr_2 = null;
//		DME2Manager mgr_3 = null;
//		DME2Manager mgr_4 = null;
//		
//		String service_1 = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_UsePreferredRouteOffer", "1.0.0", "LAB", "A1");
//		String service_2 = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_UsePreferredRouteOffer", "1.0.0", "LAB", "A2");
//		String service_3 = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_UsePreferredRouteOffer", "1.0.0", "LAB", "B1");
//		String service_4 = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_UsePreferredRouteOffer", "1.0.0", "LAB", "B2");
//		
//		try
//		{
//			mgr_1 = new DME2Manager("testDME2EndpointIterator_UsePreferredRouteOffer_1", TestUtils.initAFTProperties());
//			mgr_2 = new DME2Manager("testDME2EndpointIterator_UsePreferredRouteOffer_2", TestUtils.initAFTProperties());
//			mgr_3 = new DME2Manager("testDME2EndpointIterator_UsePreferredRouteOffer_3", TestUtils.initAFTProperties());
//			mgr_4 = new DME2Manager("testDME2EndpointIterator_UsePreferredRouteOffer_4", TestUtils.initAFTProperties());
//			
//			mgr_1.bindServiceListener(service_1, new DME2SimpleServlet());
//			mgr_2.bindServiceListener(service_2, new DME2SimpleServlet());
//			mgr_3.bindServiceListener(service_3, new DME2SimpleServlet());
//			mgr_4.bindServiceListener(service_4, new DME2SimpleServlet());
//			
//			Thread.sleep(3000);
//			
//			String clientURI = "http://DME2SEARCH/service=com.att.aft.dme2.test.TestDME2EndpointIterator_UsePreferredRouteOffer/version=1.0.0/envContext=LAB/partner=DME2_TEST";
//			
//			Properties props = new Properties();
//			props.put("AFT_DME2_PREFERRED_ROUTEOFFER", "B2");
//			
//			DME2EndpointIteratorFactory factory = DME2EndpointIteratorFactory.getInstance();
//			iter = (DME2EndpointIterator) factory.getIterator(clientURI, props, null, mgr_1);
//			
//
//			//First Iterator element should contain routeOffer B2 since we assigned it as our preferred routeOffer
//			while(iter.hasNext())
//			{
//				DME2EndpointReference ref = iter.next();
//				System.out.println("RouteOffer: " + ref.getEndpoint().getRouteOffer());
//				assertEquals("B2", ref.getEndpoint().getRouteOffer());
//				break;
//			}
//				
//		}
//		catch(Exception e)
//		{
//			e.printStackTrace();
//			fail(e.getMessage());
//		}
//		finally
//		{
//			try{mgr_1.unbindServiceListener(service_1);}
//			catch(Exception e){}
//			
//			try{mgr_2.unbindServiceListener(service_2);}
//			catch(Exception e){}
//			
//			try{mgr_3.unbindServiceListener(service_3);}
//			catch(Exception e){}
//			
//			try{mgr_4.unbindServiceListener(service_4);}
//			catch(Exception e){}
//			
//			try{iter.removeAllStaleIteratorElements();}
//			catch(Exception e){}
//		}
//	}
    @Ignore   
    @Test
    public void testDME2EndpointIterator_UsePreferredURL()
	{
		DME2EndpointIterator iter = null;

		DME2Manager mgr_1 = null;
		DME2Manager mgr_2 = null;
		DME2Manager mgr_3 = null;
		DME2Manager mgr_4 = null;

		String service_1 = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_UsePreferredURL", "1.0.0", "LAB", "A1");
		String service_2 = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_UsePreferredURL", "1.0.0", "LAB", "A2");
		String service_3 = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_UsePreferredURL", "1.0.0", "LAB", "B1");
		String service_4 = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_UsePreferredURL", "1.0.0", "LAB", "B2");

		try
		{
			mgr_1 = new DME2Manager("testDME2EndpointIterator_UsePreferredURL_1", TestUtils.initAFTProperties());
			mgr_2 = new DME2Manager("testDME2EndpointIterator_UsePreferredURL_2", TestUtils.initAFTProperties());
			mgr_3 = new DME2Manager("testDME2EndpointIterator_UsePreferredURL_3", TestUtils.initAFTProperties());
			mgr_4 = new DME2Manager("testDME2EndpointIterator_UsePreferredURL_4", TestUtils.initAFTProperties());

			mgr_1.bindServiceListener(service_1, new DME2SimpleServlet());
			mgr_2.bindServiceListener(service_2, new DME2SimpleServlet());
			mgr_3.bindServiceListener(service_3, new DME2SimpleServlet());
			mgr_4.bindServiceListener(service_4, new DME2SimpleServlet());

			Thread.sleep(3000);

			String clientURI = "http://DME2SEARCH/service=com.att.aft.dme2.test.TestDME2EndpointIterator_UsePreferredURL/version=1.0.0/envContext=LAB/partner=DME2_TEST";

			Properties props = new Properties();

			DME2EndpointIteratorFactory factory = DME2EndpointIteratorFactory.getInstance();
			iter = (DME2EndpointIterator) factory.getIterator(clientURI, props, null, mgr_1);

			String perferredURL = iter.next().getEndpoint().toURLString();
			perferredURL = iter.next().getEndpoint().toURLString();
			perferredURL = iter.next().getEndpoint().toURLString();
			perferredURL = iter.next().getEndpoint().toURLString();

			props.put("AFT_DME2_PREFERRED_URL", perferredURL);

			System.out.println("Preferred URL: " + perferredURL);

			factory = DME2EndpointIteratorFactory.getInstance();
			iter = (DME2EndpointIterator) factory.getIterator(clientURI, props, null, mgr_1);

			while(iter.hasNext())
			{
				DME2EndpointReference ref = iter.next();
				System.out.println("First URL in Iterator: " + ref.getEndpoint().toURLString());
				assertEquals(perferredURL, ref.getEndpoint().toURLString());
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

