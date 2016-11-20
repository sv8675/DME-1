/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.scld.config.strategy;

import java.util.Map;
import java.util.Random;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.att.aft.scld.config.dto.Config;
import com.att.aft.scld.config.exception.ConfigException;
import com.att.aft.scld.config.strategy.FileConfigurationStrategy;
import com.google.common.collect.Maps;
import com.mycila.junit.concurrent.Concurrency;
import com.mycila.junit.concurrent.ConcurrentJunitRunner;

@RunWith(ConcurrentJunitRunner.class)
@Concurrency(value = 5)
public class ConcurrencyFileConfigurationStrategyTest {

	private static FileConfigurationStrategy dynamicFilestrategy;
	
	@BeforeClass
	public static void setUp() throws ConfigException {
		Map<String, Map<String, String>> configs = Maps.newConcurrentMap();
		Map<String, Config> defaultConfigs = Maps.newHashMap();
		
		dynamicFilestrategy = new FileConfigurationStrategy("TestConfigs_LargeFile.properties");
		dynamicFilestrategy.registerForRefresh(configs, defaultConfigs);
	}
	
	public void testFileConfigurationStrategy() throws InterruptedException {
		int w = new Random().nextInt(1000);
        //System.out.println(String.format("[%s] %s %s %s", Thread.currentThread().getName(), getClass().getName(), new Throwable().getStackTrace()[1].getMethodName(), w));
        for(int i = 1; i <= 5000 ; i++) {
			Assert.assertEquals("localhost" + i, dynamicFilestrategy.getPropertiesConfiguration().getString("dbName" + i));
		}
        Thread.sleep(w);
	}
	
	@Test public void test0() throws Throwable { testFileConfigurationStrategy(); }
    @Test public void test1() throws Throwable { testFileConfigurationStrategy(); }
    @Test public void test2() throws Throwable { testFileConfigurationStrategy(); }
    @Test public void test3() throws Throwable { testFileConfigurationStrategy(); }
    @Test public void test4() throws Throwable { testFileConfigurationStrategy(); }
    @Test public void test5() throws Throwable { testFileConfigurationStrategy(); }
    @Test public void test6() throws Throwable { testFileConfigurationStrategy(); }
    @Test public void test7() throws Throwable { testFileConfigurationStrategy(); }
    @Test public void test8() throws Throwable { testFileConfigurationStrategy(); }
    @Test public void test9() throws Throwable { testFileConfigurationStrategy(); }

}
