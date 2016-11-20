/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.jms;

import java.util.Hashtable;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.att.aft.dme2.jms.util.JMSLogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;

public class TestClientSender implements Runnable {

    private static int sentCounter = 0;
    private static int successCounter = 0;
    private static int failCounter = 0;
    private static int timeoutCounter = 0;
    private static int mismatchCounter = 0;
    private static final Logger logger = LoggerFactory.getLogger(TestClientSender.class.getName());
	
    public static final void dumpCounters() {
        System.err.println("Sent=" + sentCounter + ", Success=" + successCounter + ", Timeout=" + timeoutCounter + ", MisMatch=" + mismatchCounter + ", Fail=" + failCounter);
    }
    
    private String ID = null;
    private long pausetime = 100;
    private Queue replyToQueue = null;
    private boolean running = false;

    private QueueSender sender = null;
    private QueueConnection conn = null;

    private QueueSession session = null;

    private String jndiClass;
    private String jndiUrl;
    private String clientConn;
    private String clientDest;
    private String clientReplyTo;

    public String getID() {
        return ID;
    }
    
    public TestClientSender(String ID, String jndiClass, String jndiUrl,
            String clientConn, String clientDest, String clientReplyTo) {
        this.ID = ID;
        this.jndiClass = jndiClass;
        this.jndiUrl = jndiUrl;
        this.clientConn = clientConn;
        this.clientDest = clientDest;
        this.clientReplyTo = clientReplyTo;
    }

    public void start() throws JMSException, NamingException {
        if (conn != null) {
            return;
        }
        Hashtable<String,Object> table = new Hashtable<String,Object>();
        table.put("java.naming.factory.initial", jndiClass);
        table.put("java.naming.provider.url", jndiUrl);
        table.put("jmsc", jndiUrl);

        System.out.println("Getting InitialContext");
        InitialContext context = new InitialContext(table);

        System.out.println("Looking up QueueConnectionFactory");
        QueueConnectionFactory qcf = (QueueConnectionFactory) context
                .lookup(clientConn);

        System.out.println("Looking up requeust Queue");
        Queue requestQueue = (Queue) context.lookup(clientDest);

        System.out.println("Looking up reply Queue");
        replyToQueue = (Queue) context.lookup(clientReplyTo);

        System.out.println("Creating QueueConnection");
        conn = qcf.createQueueConnection();

        System.out.println("Creating Session");
        session = conn.createQueueSession(true, 0);

        System.out.println("Creating MessageProducer");
        sender = session.createSender(requestQueue);
    }

    public void run() {
        running = true;
        while (running) {
            try {
                TextMessage message = session.createTextMessage();
                String sentID = ID + "::" + System.currentTimeMillis();
                message.setText(sentID);
                long start = System.currentTimeMillis();
                sentCounter++;
                TextMessage response = send(message, 20000);
                long elapsed = System.currentTimeMillis() - start;
                if (response == null) {
                	logger.info(null, "run", JMSLogMessage.CLIENT_TIMEOUT, ID, elapsed, message.getJMSMessageID(), clientReplyTo);
					timeoutCounter++;
                } else {
                	logger.debug(null, "run", JMSLogMessage.CLIENT_DATA, sentID, response.getText());
					
                    if (response.getText().equals(sentID)) {
                    	logger.info(null, "run", JMSLogMessage.CLIENT_SUCCESS, 
								ID, response.getText(), elapsed, response.getJMSMessageID(), response.getJMSCorrelationID(), clientReplyTo);
				    	successCounter++;
                    } else {
                       logger.warn(null, "run", JMSLogMessage.CLIENT_MISMATCH, 
								ID, elapsed, response.getJMSMessageID(), response.getJMSCorrelationID(), clientReplyTo);
						
                    	mismatchCounter++;
                    }
                }
                Thread.sleep(pausetime);
            } catch (Exception e) {
            	logger.error(null, "run", JMSLogMessage.CLIENT_FATAL, ID, e);
		        failCounter++;
                return;
            }
        }
    }

    private TextMessage send(TextMessage message, long timeout)
            throws JMSException {

        // System.out.println("Setting replyQueue");
        message.setJMSReplyTo(replyToQueue);

        sender.send(message);

        // System.out.println("Sent MessageID="+message.getJMSMessageID());

        // System.out.println("Getting replyTo MessageConsumer");
        QueueReceiver consumer = session.createReceiver(replyToQueue,
                "JMSCorrelationID = '" + message.getJMSMessageID() + "'");

        // System.out.println("Waiting for reply");
        return (TextMessage) consumer.receive(timeout);

    }

    public void stop() throws JMSException {
        running = false;
        if (conn != null) {
            sender.close();
            session.close();
            conn.close();
            sender = null;
            session = null;
        }
        conn = null;

    }
}
