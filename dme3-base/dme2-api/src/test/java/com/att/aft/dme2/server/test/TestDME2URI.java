/*
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 * Developed and maintained by the Common Services System Architecture (CSSA) Group
 */
package com.att.aft.dme2.server.test;

import java.net.URI;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.util.DME2URI;

import junit.framework.TestCase;

/**
 * The Class TestDME2URI.
 */
public class TestDME2URI extends TestCase {

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		// set required system properties...
		System.setProperty("AFT_DME2_SVCCONFIG_DIR", RegistryFsSetup.getSrcConfigDir());
		TestDME2URI.class.getClassLoader().setClassAssertionStatus(
				DME2URI.class.getName(), true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#tearDown()
	 */
	@Override
	protected void tearDown() throws Exception {
		DME2URI.class.getClassLoader().clearAssertionStatus();
	}

	/**
	 * Test direct uri.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	public void testDirectURI() throws Exception {
		String directStr = "http://brcbsp01:4600/service=MyService/version=1.0.0/envContext=UAT/routeOffer=APPLE_SE";
		DME2URI directUri = new DME2URI(new URI(directStr));
		directUri.assertValid();
	}
	
	/**
	 * 
	 * @throws Exception
	 */
	public void testNaturalURI() throws Exception {
		String directStr = "http://CustomerSupport.cust.att.com/subContext/?version=1.0.0&envContext=UAT&routeOffer=APPLE_SE";
		DME2URI directUri = new DME2URI(new URI(directStr));
		directUri.assertValid();
		assertEquals(directUri.getService(),"com.att.cust.CustomerSupport/subContext/");
		assertEquals(directUri.getVersion(),"1.0.0");
		assertEquals(directUri.getEnvContext(),"UAT");
		assertEquals(directUri.getRouteOffer(),"APPLE_SE");
	}
	
	/**
	 * 
	 * @throws Exception
	 */
	public void testNaturalURIWithSearch() throws Exception {
		String directStr = "http://CustomerSupport.cust.att.com/subContext/?version=1.0.0&envContext=UAT&partner=TEST";
		DME2URI directUri = new DME2URI(new URI(directStr));
		directUri.assertValid();
		assertEquals(directUri.getService(),"com.att.cust.CustomerSupport/subContext/");
		assertEquals(directUri.getVersion(),"1.0.0");
		assertEquals(directUri.getEnvContext(),"UAT");
		assertNull(directUri.getRouteOffer());
		assertEquals(directUri.getPartner(),"TEST");
	}

	/**
	 * Test invalid resolve uri.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	public void testInvalidResolveURI_MissingRouteOfferValue() throws Exception {
		String resolveStr = "http://DME2RESOLVE/service=MyService/version=1.0.0/envContext=PROD/routeOffer=";
		DME2URI resolveUri = new DME2URI(new URI(resolveStr));
		try {
			resolveUri.assertValid();
			fail("Should have failed. null routeOffer.");
		} catch (DME2Exception e) {
		}
		
	} 
	
	public void testInvalidResolveURI_MissingEnvContext() throws Exception {
		String resolveStr = "http://DME2RESOLVE/service=MyService/version=1.0.0/routeOffer=APPLE_SE";
		DME2URI resolveUri = new DME2URI(new URI(resolveStr));
		try {
			resolveUri.assertValid();
			fail("Should have failed. null envContext.");
		} catch (DME2Exception e) {
		}
	}
	
	public void testInvalidResolveURI_MissingVersion() throws Exception {
		String resolveStr = "http://DME2RESOLVE/service=MyService/version=/envContext=PROD/routeOffer=";
		DME2URI resolveUri = new DME2URI(new URI(resolveStr));
		try {
			resolveUri.assertValid();
			fail("Should have failed. null version.");
		} catch (DME2Exception e) {
		}
	}
	
	public void testInvalidResolveURI_MissingService() throws Exception {
		String resolveStr = "http://DME2RESOLVE/version=1.0.0/envContext=PROD/routeOffer=APPLE_SE";
		DME2URI resolveUri = new DME2URI(new URI(resolveStr));
		try {
			resolveUri.assertValid();
			fail("Should have failed. null service.");
		} catch (DME2Exception e) {
		}
	} 
	
	public void testInvalidResolveURI_MissingHostname() throws Exception {
		String resolveStr = "http://service=MyService/version=1.0.0/envContext=PROD/routeOffer=";
		DME2URI resolveUri = new DME2URI(new URI(resolveStr));
		try {
			resolveUri.assertValid();
			fail("Should have failed. No type provided.");
		} catch (DME2Exception e) {
		}
	}

	/**
	 * Test invalid search uri.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	public void testInvalidSearchURI_MissingPartner() throws Exception {
		String searchStr = "http://DME2SEARCH/service=MyService/version=1.0.0/envContext=PROD/dataContext=205977";
		DME2URI searchUri = new DME2URI(new URI(searchStr));
		try {
			searchUri.assertValid();
			fail("Should have failed. null partner.");
		} catch (DME2Exception e) {
		}
	}
	
	public void testInvalidSearchURI_MissingService() throws Exception {
		String searchStr = "http://DME2SEARCH/service=/version=1.0.0/envContext=PROD/dataContext=205977/partner=APPLE";
		DME2URI searchUri = new DME2URI(new URI(searchStr));
		try {
			searchUri.assertValid();
			fail("Should have failed. null service");
		} catch (DME2Exception e) {
		}
	}
	
	public void testInvalidSearchURI_MissingVersion() throws Exception {
		String searchStr = "http://DME2SEARCH/service=MyService/envContext=PROD/dataContext=205977/partner=APPLE";
		DME2URI searchUri = new DME2URI(new URI(searchStr));
		try {
			searchUri.assertValid();
			fail("Should have failed. null version");
		} catch (DME2Exception e) {
		}
	}
	
	public void testInvalidSearchURI_MissingEnvContext() throws Exception {
		String searchStr = "http://DME2SEARCH/service=MyService/version=1.0.0/dataContext=205977/partner=APPLE";
		DME2URI searchUri = new DME2URI(new URI(searchStr));
		try {
			searchUri.assertValid();
			fail("Should have failed. null envContext");
		} catch (DME2Exception e) {
		}
		
	}
	
	public void testInvalidSearchURI_MissingDataContextOK() throws Exception {
		String searchStr = "http://DME2SEARCH/service=MyService/version=1.0.0/envContext=PROD/partner=APPLE";
		DME2URI searchUri = new DME2URI(new URI(searchStr));
		try {
			searchUri.assertValid();
		} catch (DME2Exception e) {
			fail("Should NOT have failed. null dataContext");
		}
	}

	/**
	 * Test invalid uri.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	public void testInvalidURI() throws Exception {
		String uriStr = "ftp://cbs.it.att.com/service=MyService/version=1.0/envContext=PROD,routeOffer=APPLE";
		DME2URI uri = new DME2URI(new URI(uriStr));
		try {
			uri.assertValid();
			fail("Should have failed. Unsupported protocol and type.");
		} catch (DME2Exception e) {
		}
	}

	/**
	 * Test resolve uri.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	public void testResolveURI() throws Exception {
		String resolveStr = "http://DME2RESOLVE/service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE";
		DME2URI resolveUri = new DME2URI(new URI(resolveStr));
		resolveUri.assertValid();
	}

	/**
	 * Test search uri.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	public void testSearchURI() throws Exception {
		String searchStr = "http://DME2SEARCH/service=MyService/version=1.0.0/envContext=PROD/dataContext=205977/partner=APPLE";
		DME2URI searchUri = new DME2URI(new URI(searchStr));
		searchUri.assertValid();
	}

}
