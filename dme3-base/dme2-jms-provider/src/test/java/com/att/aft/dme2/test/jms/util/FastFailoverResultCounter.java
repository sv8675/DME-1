/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test.jms.util;

public class FastFailoverResultCounter {

	public static int fastFail9595 = 0;
	public static int fastFail9596 = 0;
	public static int passed = 0;
	public static int failed = 0;
	
	public static boolean fastFailover9595 = false;
	public static boolean fastFailover9596 = false;
	
	public static void reset() {
		fastFail9595 = 0;
		fastFail9596 = 0;
		passed = 0;
		failed = 0;
		
		fastFailover9595 = false;
		fastFailover9596 = false;
		
	}
	
	public  synchronized static void set9595True() {
		fastFailover9595 = true;
	}
	
	public  synchronized static void set9596True() {
		fastFailover9596 = true;
	}
}
