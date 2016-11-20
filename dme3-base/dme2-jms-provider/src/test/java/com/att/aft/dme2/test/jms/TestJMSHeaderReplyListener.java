/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test.jms;

import java.util.Enumeration;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;
import javax.jms.TextMessage;

public class TestJMSHeaderReplyListener implements MessageListener {
	private QueueConnection connection = null;
	private Queue dest = null;
	private QueueReceiver receiver = null;
	private QueueSession session = null;

	public TestJMSHeaderReplyListener(QueueConnection connection,
			QueueSession session, Queue dest) {
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

	public void onMessage(Message msg)
    {
	    try
        {
	        System.out.println("TestJMSHeaderReplyListener thread=" + Thread.currentThread().getId() + " received request");
	        Queue replyTo = (Queue)msg.getJMSReplyTo();
	        System.out.println("TestJMSHeaderReplyListener thread=" + Thread.currentThread().getId() + " will respond to " + replyTo.getQueueName());
	        // Sleep before replying to simulate long run transaction
	        if(replyTo != null)
	        {
	        	TextMessage replyMsg = session.createTextMessage();
	        	// Not sending any text on purpose

						if ( msg != null ) {
							for ( Enumeration<String> propertyNames = msg.getPropertyNames(); propertyNames.hasMoreElements() ;) {
								String propertyName = propertyNames.nextElement();
								System.out.println( "receive Property: " + propertyName + " Value: " + msg.getStringProperty( propertyName ) );
							}
						}
	        	replyMsg.setText("TestJMSHeaderReplyListener:::" + ((TextMessage)msg).getText());
	        	replyMsg.setJMSCorrelationID(msg.getJMSMessageID());
	        	replyMsg.setJMSExpiration(System.currentTimeMillis() + 60000);
	        	replyMsg.setStringProperty("CLIENT_SENT_TIME", msg.getStringProperty("AFT_DME2_CLIENT_REQ_SEND_TIMESTAMP"));
	        	replyMsg.setStringProperty("CLIENT_EP_READ_TIMEOUT", msg.getStringProperty("AFT_DME2_EP_READ_TIMEOUT_MS"));
						System.out.println( "Text: " + replyMsg.getText() + " AFT_DME2_EP_READ_TIMEOUT_MS: " + msg.getStringProperty("AFT_DME2_EP_READ_TIMEOUT_MS") );
	        	MessageProducer producer = session.createProducer(replyTo);
	        	producer.send(replyMsg);
	        	producer.close();
	        	System.out.println("TestJMSHeaderReplyListener thread=" + Thread.currentThread().getId() + " responded to " + replyTo.getQueueName());
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
        catch(Exception ie){
        	//ignore thread sleep failure
        }

    }


}