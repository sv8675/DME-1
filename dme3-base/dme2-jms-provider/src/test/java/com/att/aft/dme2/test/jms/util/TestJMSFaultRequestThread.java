/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test.jms.util;

import java.util.Hashtable;
import java.util.Properties;

import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TextMessage;
import javax.naming.InitialContext;

import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;

public class TestJMSFaultRequestThread implements Runnable {
	/** The id. */
	String clientId = null;

	/** The dme2 resolve url. */
	String resUrl = null;
	  private static final Logger logger = LoggerFactory.getLogger( TestJMSRequestThread.class );
	public TestJMSFaultRequestThread(String id, String url) {
		this.clientId = id;
		this.resUrl = url;
	}

	public void run() {
		try {
			Locations.BHAM.set();
		    System.setProperty("AFT_DME2_DISABLE_THROTTLE_FILTER","true");	
//			Properties props = RegistryFsSetup.init();		    
			Properties props = RegistryFsSetup.init();
			props.setProperty("AFT_DME2_CLIENT_QDEPTH", "30");	
			
			Hashtable<String, Object> table = new Hashtable<String, Object>();
			for (Object key : props.keySet()) {
				table.put((String) key, props.get(key));
			}
			table.put("java.naming.factory.initial", TestConstants.jndiClass);
			table.put("java.naming.provider.url", TestConstants.jndiUrl);

			InitialContext context = new InitialContext(table);
			QueueConnectionFactory factory = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
			QueueConnection connection = factory.createQueueConnection();
			QueueSession session = connection.createQueueSession(true, 0);
			Queue remoteQueue = (Queue) context.lookup(TestConstants.dme2FailSvcResolveStr);
			QueueSender sender = session.createSender(remoteQueue);

			TextMessage msg = session.createTextMessage();
			msg.setText("sendFault");
			msg.setStringProperty("com.att.aft.dme2.jms.dataContext", TestConstants.dataContext);
			msg.setStringProperty("com.att.aft.dme2.jms.partner", TestConstants.partner);
			msg.setStringProperty("AFT_DME2_REQ_TRACE_ON", "true");
			Queue replyToQueue = (Queue) context.lookup("http://DME2LOCAL/clientResponseQueue");
			msg.setJMSReplyTo(replyToQueue);

			sender.send(msg);
			QueueReceiver replyReceiver = session.createReceiver(replyToQueue);
			try {
				Thread.sleep(30000);
			} catch (Exception ex) {
			}
			TextMessage rcvMsg = (TextMessage) replyReceiver.receive(90000);
		     logger.debug( null, "run", "clientId: {} resUrl: {} context: {} received msg {}", clientId, resUrl, context, rcvMsg );			
			if (rcvMsg != null && rcvMsg.getText() != null) {
				String traceInfo = rcvMsg.getStringProperty("AFT_DME2_REQ_TRACE_INFO");
				if (traceInfo != null) {
					System.out.println("TraceInfo = " + traceInfo);
					if (traceInfo.contains(
							":9595/service=com.att.test.FastFailService/version=1.1.0/envContext=DEV/routeOffer=BAU_SE:onResponseCompleteStatus=503")) {
						FastFailoverResultCounter.fastFailover9595 = true;
					}
					if (traceInfo.contains(
							":9596/service=com.att.test.FastFailService/version=1.1.0/envContext=DEV/routeOffer=BAU_SE:onResponseCompleteStatus=503")) {
						FastFailoverResultCounter.fastFailover9596 = true;
					}
				}
				if (rcvMsg.getText().contains("ServerPort=9595")) {
					FastFailoverResultCounter.fastFail9595++;
				} else if (rcvMsg.getText().contains("ServerPort=9596")) {
					FastFailoverResultCounter.fastFail9596++;
				} else {
					System.out.println(rcvMsg.getText());
				}
			} else {
				FastFailoverResultCounter.failed++;
			}
		} catch (Exception e) {
			e.printStackTrace();
			FastFailoverResultCounter.failed++;
		}
	}
}
