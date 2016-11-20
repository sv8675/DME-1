/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.server.test;

import junit.framework.TestCase;

public class TestLogUtil extends TestCase {

	public void testToFromString()
	{
//		MessageHandlingConfig orig = new MessageHandlingConfig(true, false, false, LogMessageConfig.fromString("COUNT"), DME2ErrorCodeOption.BOTH, StackHandlingOption.IMMEDIATE, StackHandlingOption.NONE);
//		String repr = orig.toString();
//		MessageHandlingConfig recreate = MessageHandlingConfig.fromString(repr);
//		assertTrue(recreate.useStdout);
//		assertFalse(recreate.useStderr);
//		assertFalse(recreate.useLogger);
//		assertEquals("COUNT", recreate.collectorConfig.toString());
//		assertEquals(StackHandlingOption.IMMEDIATE, recreate.exceptionStack);
//		assertEquals(StackHandlingOption.NONE, recreate.callerStack);
	}
	
	/** make sure getCaller() shows the method that called the method that created the Throwable.
	 * 
	 *  the Throwable is created in LogUtil.report() so it should report the method that called LogUtil.report().
	 */
	public void testGetCaller()
	throws Exception
	{
		Throwable t = createException();
//		Method m = LogUtil.class.getDeclaredMethod("getCaller", Throwable.class);
//		m.setAccessible(true);
//		String caller = (String)m.invoke(LogUtil.INSTANCE, t);
	//	assertTrue(caller.contains("testGetCaller"));
	}
	
	private Throwable createException()
	{
		return new Throwable();
	}
}
