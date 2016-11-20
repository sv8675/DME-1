/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test.jms;

import static org.junit.Assert.assertEquals;

import java.util.Hashtable;
import java.util.Properties;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TextMessage;
import javax.naming.InitialContext;

import org.junit.Test;

import com.att.aft.dme2.test.jms.util.JMSBaseTestCase;
import com.att.aft.dme2.test.jms.util.Locations;
import com.att.aft.dme2.test.jms.util.RegistryFsSetup;


public class ReplyToTest extends JMSBaseTestCase
{
    @Test
    public void testReplyToLocalQueue() throws Exception
	{
		System.setProperty("AFT_LATITUDE", String.valueOf(Locations.BHAM.getLatitude()));
		System.setProperty("AFT_LONGITUDE", String.valueOf(Locations.BHAM.getLongitude()));

		Hashtable<String,Object> table = new Hashtable<String,Object>();
        table.put("java.naming.factory.initial", "com.att.aft.dme2.jms.DME2JMSInitialContextFactory");
        table.put("java.naming.provider.url", "qcf://dme2");
		Properties props = RegistryFsSetup.init();
        for (Object key: props.keySet()) {
        	table.put((String)key, props.get(key));
        }
        
        InitialContext context = new InitialContext(table);
        QueueConnectionFactory qcf = (QueueConnectionFactory) context.lookup("qcf://dme2");
        QueueConnection qConn = qcf.createQueueConnection();
        QueueSession session = qConn.createQueueSession(true, QueueSession.AUTO_ACKNOWLEDGE);
        
        String dest = "http://DME2LOCAL/service=LocalQueueService/version=1.0/envContext=DEV/partner=BAU_SE";
        String replyTo = "http://DME2LOCAL/service=ReplyToLocalQueueService/version=1.0/envContext=DEV/partner=BAU_SE";
        Queue requestQueue = (Queue)context.lookup(dest);
        Queue replyToQueue = (Queue)context.lookup(replyTo);
        
        String msg = "TEST";
        TextMessage requestMsg = session.createTextMessage();
        requestMsg.setText(msg);
        requestMsg.setJMSReplyTo(replyToQueue);
        requestMsg.setJMSMessageID(String.valueOf(System.currentTimeMillis()));
        
        QueueSender sender = session.createSender(requestQueue);
        QueueReceiver requestReceiver = session.createReceiver(requestQueue);

        requestReceiver.setMessageListener(new RequestMessageProcessor(qConn,session));

        sender.send(requestMsg);

        Thread.sleep(1000);

        QueueReceiver receiver = session.createReceiver(replyToQueue);
        Message m = receiver.receiveNoWait();
        assertEquals("RequestMessageProcessor:::" + msg , ((TextMessage)m).getText());
	}

    @Test
    public void testReplyToTemporaryQueue() throws Exception
	{
		System.setProperty("AFT_LATITUDE", String.valueOf(Locations.BHAM.getLatitude()));
		System.setProperty("AFT_LONGITUDE", String.valueOf(Locations.BHAM.getLongitude()));

		Properties props = RegistryFsSetup.init();
		Hashtable<String,Object> table = new Hashtable<String,Object>();
        for (Object key: props.keySet()) {
        	table.put((String)key, props.get(key));
        }
        table.put("java.naming.factory.initial", "com.att.aft.dme2.jms.DME2JMSInitialContextFactory");
        table.put("java.naming.provider.url", "qcf://dme2");
        InitialContext context = new InitialContext(table);
        QueueConnectionFactory qcf = (QueueConnectionFactory) context.lookup("qcf://dme2");
        QueueConnection qConn = qcf.createQueueConnection();
        QueueSession session = qConn.createQueueSession(true, QueueSession.AUTO_ACKNOWLEDGE);
        
        String dest = "http://DME2LOCAL/service=LocalQueueService/version=1.0/envContext=DEV/partner=BAU_SE";
        Queue requestQueue = (Queue)context.lookup(dest);
        Queue replyToQueue = session.createTemporaryQueue();
        
        String msg = "TEST";
        TextMessage requestMsg = session.createTextMessage();
        requestMsg.setText(msg);
        requestMsg.setJMSReplyTo(replyToQueue);
        requestMsg.setJMSMessageID(String.valueOf(System.currentTimeMillis()));
        
        QueueSender sender = session.createSender(requestQueue);
        QueueReceiver requestReceiver = session.createReceiver(requestQueue);

        requestReceiver.setMessageListener(new RequestMessageProcessor(qConn,session));

        sender.send(requestMsg);

        Thread.sleep(1000);

        QueueReceiver receiver = session.createReceiver(replyToQueue);
        Message m = receiver.receiveNoWait();
        assertEquals("RequestMessageProcessor:::"+msg, ((TextMessage)m).getText());
	}

	class RequestMessageProcessor implements MessageListener
	{
		QueueConnection connection;
		QueueSession session;
		public RequestMessageProcessor(QueueConnection connection, QueueSession session)
		{
			this.connection = connection;
			this.session = session;
		}
		public void onMessage(Message m)
		{
			if(m instanceof TextMessage)
			{
				try
                {
                    TextMessage incomingMsg = (TextMessage)m;
                    TextMessage outgoingMsg = session.createTextMessage();
                    outgoingMsg.setText("RequestMessageProcessor:::" + incomingMsg.getText());
                    outgoingMsg.setJMSExpiration(System.currentTimeMillis() + 60000000);
                    //outgoingMsg.setBooleanProperty("com.att.aft.dme2.jms.isReceiveToService", true);
                    
                    Queue sendResponseTo = (Queue)incomingMsg.getJMSReplyTo();
                    if(sendResponseTo != null)
                    {
                    	QueueSender sender = session.createSender(sendResponseTo);
                    	sender.send(outgoingMsg);
                    	System.out.println("Listener message to replyTo queue=" + sendResponseTo.getQueueName() + "," + outgoingMsg.getText());
                    	sender.close();
                    }
                }
                catch (JMSException e)
                {
                    e.printStackTrace();
                }
			}
		}
	}		
}

