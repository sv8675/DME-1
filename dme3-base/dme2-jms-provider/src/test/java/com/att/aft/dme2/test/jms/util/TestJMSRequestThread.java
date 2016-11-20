/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test.jms.util;

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

public class TestJMSRequestThread implements Runnable {
  private static final Logger logger = LoggerFactory.getLogger( TestJMSRequestThread.class );
	/** The id. */
	String clientId = null;

	/** The dme2 resolve url. */
	String resUrl = null;
	InitialContext context;

	public TestJMSRequestThread(String id, String url, InitialContext context) {
		this.clientId = id;
		this.resUrl = url;
		this.context = context;
	}

	public void run() {
		try {
			/*Properties props = RegistryFsSetup.init();
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
			*/
			QueueConnectionFactory factory = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
			QueueConnection connection = factory.createQueueConnection();
			QueueSession session = connection.createQueueSession(true, 0);
			Queue remoteQueue = (Queue)context.lookup(TestConstants.dme2FailSvcResolveStr);
			QueueSender sender = session.createSender(remoteQueue);

			TextMessage msg = session.createTextMessage();
			msg.setText("TEST");
			msg.setStringProperty("com.att.aft.dme2.jms.dataContext", TestConstants.dataContext);
			//msg.setStringProperty("com.att.aft.dme2.jms.partner", TestConstants.partner);
			msg.setStringProperty("AFT_DME2_REQ_TRACE_ON", "true");
			msg.setStringProperty("com.att.aft.dme2.jms.perEndpointTimeoutMs", "120000" );
			Queue replyToQueue = (Queue) context.lookup("http://DME2LOCAL/clientResponseQueue");
			msg.setJMSReplyTo(replyToQueue);

      logger.debug( null, "run", "clientId: {} resUrl: {} context: {} sending msg {}", clientId, resUrl, context, msg );
			sender.send(msg);
			 //Thread.sleep(2000);
			QueueReceiver replyReceiver = session.createReceiver(replyToQueue);
			try {
				Thread.sleep(1000);
			} catch (Exception ex) {
			}
      logger.debug( null, "run", "clientId: {} resUrl: {} context: {} receiving msg", clientId, resUrl, context );
      //Thread.sleep(60000);
//			TextMessage rcvMsg = (TextMessage) replyReceiver.receive(14000);
			TextMessage rcvMsg = (TextMessage) replyReceiver.receive(14000);
      logger.debug( null, "run", "clientId: {} resUrl: {} context: {} received msg {}", clientId, resUrl, context, rcvMsg );
			if (rcvMsg != null && rcvMsg.getText() != null) {
				String traceInfo = rcvMsg.getStringProperty("AFT_DME2_REQ_TRACE_INFO");
				System.out.println("trace info is: "+traceInfo);
        logger.debug( null, "run", "clientId: {} resUrl: {} context: {} received traceInfo {}", clientId, resUrl, context, traceInfo );
				if (traceInfo != null) {
					System.out.println("TraceInfo = " + traceInfo);
					if (traceInfo.contains(
							":9595/service=com.att.test.FastFailService/version=1.1.0/envContext=DEV/routeOffer=BAU_SE:onResponseCompleteStatus=503")) {
						FastFailoverResultCounter.set9595True();
					}
					if (traceInfo.contains(
							":9596/service=com.att.test.FastFailService/version=1.1.0/envContext=DEV/routeOffer=BAU_SE:onResponseCompleteStatus=503")) {
						FastFailoverResultCounter.set9596True();
					}
				}
				if (rcvMsg.getText().contains("ServerPort=9595")) {
					FastFailoverResultCounter.fastFail9595++;
				} else if (rcvMsg.getText().contains("ServerPort=9596")) {
					FastFailoverResultCounter.fastFail9596++;
				} else {
					System.out.println("TestJMSRequestThread, Received message :"+rcvMsg.getText());
				}
			} else {
				FastFailoverResultCounter.failed++;
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.warn( null, "run", "Error: ", e );
			FastFailoverResultCounter.failed++;
		}
	}
}
