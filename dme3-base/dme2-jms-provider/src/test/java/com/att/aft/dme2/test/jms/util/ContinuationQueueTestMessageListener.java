/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test.jms.util;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;
import javax.jms.TextMessage;
import javax.naming.InitialContext;

public class ContinuationQueueTestMessageListener implements javax.jms.MessageListener
{
	private QueueConnection connection = null;
	private Queue dest = null;
	private QueueReceiver receiver = null;
	private QueueSession session = null;
	private InitialContext context = null;
	
	public ContinuationQueueTestMessageListener(InitialContext context,QueueConnection connection, QueueSession session, Queue dest)
	{
		this.context = context;
		this.connection = connection;
		this.session = session;
		this.dest = dest;
	}
	
    public void start() throws JMSException 
    {
        session = connection.createQueueSession(true, QueueSession.AUTO_ACKNOWLEDGE);
        receiver = session.createReceiver(dest);
        receiver.setMessageListener(this);
    }
    
    public void stop() throws JMSException 
    {
        //receiver.setMessageListener(null);
        try
        {
        	if(receiver != null)
        		receiver.close();
        }
        catch (Exception e)
        {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }
        try
        {
	        if(session != null)
	        	session.close();
        }
        catch (Exception e)
        {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }
    }

    public void setReceiver(QueueReceiver receiver)
    {
    	this.receiver = receiver;
    }
    
	@Override
    public void onMessage(Message msg)
    {
	    try
        {
	    	System.out.println("ContinuationQueueTestMessageListener thread invoked");

	        Queue replyTo = (Queue)msg.getJMSReplyTo();
	        if(replyTo != null)
	        {
	        	String m = msg.getJMSCorrelationID();
	        	if(m== null) {
	        		m = msg.getJMSMessageID();
	        	}
	        	Queue	repTo = (Queue)context.lookup("http://DME2REPLY/"+m);
	        	System.out.println("ContinuationQueueTestMessageListener thread=" + Thread.currentThread().getId() + " corr id " +m);
	        		
	        	TextMessage replyMsg = session.createTextMessage();
	        	replyMsg.setText("ContinuationQueueTestMessageListener:::" + ((TextMessage)msg).getText());
	        	replyMsg.setJMSCorrelationID(msg.getJMSMessageID());
	        	replyMsg.setJMSExpiration(System.currentTimeMillis() + 60000);
	        	System.out.println("ContinuationQueueTestMessageListener thread=" + Thread.currentThread().getId() + " responding to " + repTo.getQueueName());
	        	MessageProducer producer = session.createProducer(repTo);
		    	Thread.sleep(121000);
	        	producer.send(replyMsg);
	        	producer.close();
	        	System.out.println("ContinuationQueueTestMessageListener thread=" + Thread.currentThread().getId() + " responded to " + replyTo.getQueueName());
	        }
	        else
	        {
	        	
	        }
        }
        catch (Exception e)
        {
        	e.printStackTrace();
        	throw new RuntimeException(e);
        }
    }

}
