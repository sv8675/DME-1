/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.server.test;

import java.util.HashSet;
import java.util.Set;

import com.att.aft.dme2.logging.LogMessage;

import junit.framework.TestCase;

public class TestLogMessages 
extends TestCase
{
	/** guarantee that all codes are distinct */
	public void testDistinctCodes()
	{
		Set<String> codes = new HashSet<String>();
		Set<LogMessage> duplicates = new HashSet<LogMessage>();
		for(LogMessage msg : LogMessage.values())
		{
			if(codes.contains(msg.getCode())) duplicates.add(msg);
			else codes.add(msg.getCode());
		}
		
		assertTrue("these messages have duplicate codes: " + duplicates, duplicates.isEmpty());
	}
	
	public void testArgCount()
	{
		assertEquals(1, LogMessage.DEBUG_MESSAGE.getArgCount());   			// only %s
		assertEquals(1, LogMessage.SERVLET_PARAM_MISSING.getArgCount());		// beginning
		assertEquals(1, LogMessage.SERVLET_RECV.getArgCount());				// middle
		assertEquals(1, LogMessage.SERVER_CALLBACK.getArgCount());			// end
		
		assertEquals(12, LogMessage.SERVER_PARAMS.getArgCount());			// lots
		assertEquals(0, LogMessage.METHOD_ENTER.getArgCount());				// none
	}
	
	public void testPreventDuplicateCode()
	{
		String code = LogMessage.DEBUG_MESSAGE.getCode();
		assertTrue(LogMessage.values().contains(LogMessage.DEBUG_MESSAGE));
		try
		{
			new DupLogMessage(code, "diff template");
			fail("should have thrown exception");
		}
		catch(IllegalArgumentException e)
		{
			
		}
	}
	
	private static class DupLogMessage
	extends LogMessage
	{
		public DupLogMessage(String code, String template) { super(code, template); }
	}
}
