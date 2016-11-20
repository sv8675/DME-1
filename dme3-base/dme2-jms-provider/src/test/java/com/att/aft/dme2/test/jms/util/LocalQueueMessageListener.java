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

public class LocalQueueMessageListener implements javax.jms.MessageListener
{
	private QueueConnection connection = null;
	private Queue dest = null;
	private QueueReceiver receiver = null;
	private QueueSession session = null;
	
	public LocalQueueMessageListener(QueueConnection connection, QueueSession session, Queue dest)
	{
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
        try  {
	        if(connection != null)
	        	connection.close();
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
	        Queue replyTo = (Queue)msg.getJMSReplyTo();
	        System.out.println("Reply msg in the lqueuelistener: [" + replyTo + "]");
	        if(replyTo != null)
	        {
	        	String requestCharSet = msg.getStringProperty("com.att.aft.dme2.jms.charset");
	        	
	        	String useReplyCharset = msg.getStringProperty("com.att.aft.dme2.jms.test.useForReplyCharSet");
	        	
	        	String echoRequestText = msg.getStringProperty("com.att.aft.dme2.jms.test.echoRequestText");
	        	
	        	TextMessage tm = (TextMessage)msg;
	        	
	        	System.out.println("Test server got: [" + tm.getText() + "]");
	        	
	        	TextMessage replyMsg = session.createTextMessage();
	        	if (echoRequestText != null && echoRequestText.equals("true")) {
	        		replyMsg.setText(((TextMessage)msg).getText());
	        	} else {
	        		replyMsg.setText("LocalQueueMessageListener:::" + ((TextMessage)msg).getText());
	        	}
	        	replyMsg.setJMSCorrelationID(msg.getJMSMessageID());
	        	replyMsg.setJMSExpiration(System.currentTimeMillis() + 60000);
	        	
	        	if (requestCharSet != null) {
	        		replyMsg.setStringProperty("com.att.aft.dme2.jms.test.charset", requestCharSet);
	        	}
	        	
	        	if (useReplyCharset != null) {
	        		replyMsg.setStringProperty("com.att.aft.dme2.jms.charset", useReplyCharset);
	        	}
	        	
	        	MessageProducer producer = session.createProducer(replyTo);
	        	producer.send(replyMsg);
	        	producer.close();
	        	System.out.println("LocalQueueMessageListener thread=" + Thread.currentThread().getId() + " responded to " + replyTo.getQueueName());
	        }
	        else
	        {
	        	
	        }
        }
        catch (JMSException e)
        {
        	e.printStackTrace();
        	throw new RuntimeException(e);
        }
	    
    }

}
