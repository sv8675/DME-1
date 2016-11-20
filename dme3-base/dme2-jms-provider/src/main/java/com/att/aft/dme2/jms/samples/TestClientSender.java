/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms.samples;

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

@SuppressWarnings("PMD.SystemPrintln")
public class TestClientSender implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(TestClientSender.class.getName());

	private static int sentCounter = 0;
	private static int successCounter = 0;
	private static int failCounter = 0;
	private static int timeoutCounter = 0;
	private static int mismatchCounter = 0;

	public static final void dumpCounters() {
		System.err.println("Sent=" + sentCounter + ", Success=" + successCounter + ", Timeout=" + timeoutCounter
				+ ", MisMatch=" + mismatchCounter + ", Fail=" + failCounter);
	}

	private String ID = null;
	private static final int CONSTANT_PAUSETIME = 100;
	private static final int CONSTANT_60000 = 60000;
	private static final int CONSTANT_100 = 100;

	private final long pausetime = CONSTANT_PAUSETIME;

	private Queue replyToQueue = null;
	private boolean running = false;

	private QueueSender sender = null;
	private QueueConnection conn = null;

	private QueueSession session = null;

	private final String jndiClass;
	private final String jndiUrl;
	private final String clientConn;
	private final String clientDest;
	private final String clientReplyTo;

	public String getID() {
		return ID;
	}

	public TestClientSender(String ID, String jndiClass, String jndiUrl, String clientConn, String clientDest,
			String clientReplyTo) {
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
		Hashtable<String, Object> table = new Hashtable<String, Object>();
		table.put("java.naming.factory.initial", jndiClass);
		table.put("java.naming.provider.url", jndiUrl);

		System.out.println("Getting InitialContext");
		InitialContext context = new InitialContext(table);

		System.out.println("Looking up QueueConnectionFactory");
		QueueConnectionFactory qcf = (QueueConnectionFactory) context.lookup(clientConn);

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

	@Override
	public void run() {
		running = true;
		while (running) {
			try {
				TextMessage message = session.createTextMessage();
				String dataContext = System.getProperty("dataContext");
				if (dataContext != null) {
					message.setObjectProperty("com.att.aft.dme2.jms.dataContext", dataContext);
				} else {
					message.setObjectProperty("com.att.aft.dme2.jms.dataContext", "205977");
				}
				String partner = System.getProperty("partner");
				if (partner != null) {
					message.setStringProperty("com.att.aft.dme2.jms.partner", partner);
				} else {
					message.setStringProperty("com.att.aft.dme2.jms.partner", "1C");
				}
				String selKey = System.getProperty("selKey");
				if (selKey != null) {
					message.setStringProperty("com.att.aft.dme2.jms.stickySelectorKey", selKey);
				}

				String sentID = ID + "::" + System.currentTimeMillis();

				String xmlMsg = "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:SOAP-ENC=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">"
						+ "<SOAP-ENV:Header>"
						+ "<m:MessageHeader xmlns:m=\"http://csi.cingular.com/CSI/Namespaces/Types/Public/MessageHeader.xsd\" xmlns:m0=\"http://csi.cingular.com/CSI/Namespaces/Types/Public/CingularDataModel.xsd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
						+ "<m:TrackingMessageHeader>" + "<m0:version>v44</m0:version>"
						+ "<m0:messageId>Vasanthi_Thu4_Mar_26_09-19-55_PDT_2009</m0:messageId>"
						+ "<m0:originatorId>String</m0:originatorId>" + "<m0:responseTo>String</m0:responseTo>"
						+ "<m0:returnURL>String</m0:returnURL>" + "<m0:timeToLive>300000</m0:timeToLive>"
						+ "<m0:conversationId>String</m0:conversationId>"
						+ "<m0:dateTimeStamp>2001-12-17T09:30:47.0Z</m0:dateTimeStamp>" + "</m:TrackingMessageHeader>"
						+ "<m:SecurityMessageHeader>" + "<m0:userName>csitest</m0:userName>"
						+ "<m0:userPassword>testcsi</m0:userPassword>" + "</m:SecurityMessageHeader>"
						+ "<m:SequenceMessageHeader>" + "<m0:sequenceNumber>1</m0:sequenceNumber>"
						+ "<m0:totalInSequence>1</m0:totalInSequence>" + "</m:SequenceMessageHeader>"
						+ "</m:MessageHeader>" + "</SOAP-ENV:Header>" + "<SOAP-ENV:Body>"
						+ "<EchoRequest xmlns=\"http://csi.cingular.com/CSI/Namespaces/Container/Public/EchoRequest.xsd\">"
						+ "<data>M2E"
						+ "M2E is an Infrastructure Framework.  The primary goal of M2E is to provide a high performance runtime platform for execution of services that are modeled based on MDA.  The short term vision is to provide an alternate/redundant execution environment for the existing XPDL process models.  Long term, M2E may emerge as the primary execution environment."
						+

				"M2E-CSI"
						+ "M2E-CSI is an Application Framework.  M2E-CSI takes the Model Drive Architecture (MDA) developed at Cingular and replaces the runtime implementation for faster performance and better, more cost-effective resource usage, resulting in great Return on Investment in MDA technology.  M2E-CSI components are JMS-Listening services that play the role of \"SPM\" layer within the overall CSI Application Model.  They must listen on CSI EMS Brokers on specified Request Queues and must respond on CSI EMS Brokers on specified Response Queues.  The protocol is a CSI specific XML Document protocol, engineered to interact with \"The Gateway\" (see CSI Documents). </data>"
						+ "</EchoRequest>" + "</SOAP-ENV:Body>" + "</SOAP-ENV:Envelope>";

				/**
				 * String ifsqMsg =
				 * "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:SOAP-ENC=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:m0=\"http://csi.cingular.com/CSI/Namespaces/Types/Public/CingularDataModel.xsd\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
				 * + "<SOAP-ENV:Header>"+
				 * "<m:MessageHeader xmlns:m=\"http://csi.cingular.com/CSI/Namespaces/Types/Public/MessageHeader.xsd\">"
				 * + "<m:TrackingMessageHeader>"+ "<m0:version>v46</m0:version>"
				 * +
				 * "<m0:messageId>37223_IFSQ_addressID_boundaryLength_validation_1104</m0:messageId>"
				 * +
				 * "<m0:dateTimeStamp>2008-04-25T09:30:47.0Z</m0:dateTimeStamp>"
				 * + "</m:TrackingMessageHeader>"+ "<m:SecurityMessageHeader>"+
				 * "<m0:userName>csitest</m0:userName>"+
				 * "<m0:userPassword>testingcsi</m0:userPassword>"+
				 * "</m:SecurityMessageHeader>"+ "<m:SequenceMessageHeader>"+
				 * "<m0:sequenceNumber>1</m0:sequenceNumber>"+
				 * "<m0:totalInSequence>1</m0:totalInSequence>"+
				 * "</m:SequenceMessageHeader>"+ "</m:MessageHeader>"+
				 * "</SOAP-ENV:Header>"+ "<SOAP-ENV:Body>"+
				 * "<m:InquireFiberServiceQualificationRequest xmlns:m=\"http://csi.cingular.com/CSI/Namespaces/Container/Public/InquireFiberServiceQualificationRequest.xsd\">"
				 * + "<m:ServiceAddressSelection>"+
				 * "<m:addressId>P28d10e4d5asdfasdferwe35654wer</m:addressId>"+
				 * "</m:ServiceAddressSelection>"+
				 * "<m:networkInfoLevel>2</m:networkInfoLevel>"+
				 * "</m:InquireFiberServiceQualificationRequest>"+
				 * "</SOAP-ENV:Body>"+ "</SOAP-ENV:Envelope>";
				 */

				message.setText(sentID + xmlMsg);
				long start = System.currentTimeMillis();
				sentCounter++;
				TextMessage response = send(message, CONSTANT_60000);
				long elapsed = System.currentTimeMillis() - start;
				if (response == null) {
					logger.info(null, "run", JMSLogMessage.CLIENT_TIMEOUT, ID, elapsed, message.getJMSMessageID(),
							clientReplyTo);
					timeoutCounter++;
				} else {
					logger.info(null, "run", JMSLogMessage.CLIENT_DATA, sentID, response.getText());
					if (response.getText().startsWith(sentID)) {
						logger.info(null, "run", JMSLogMessage.CLIENT_SUCCESS, ID, response.getText(), elapsed,
								response.getJMSMessageID(), response.getJMSCorrelationID(), clientReplyTo);
						successCounter++;
					} else {
						logger.info(null, "run", JMSLogMessage.CLIENT_MISMATCH, ID, ID, elapsed,
								response.getJMSMessageID(), response.getJMSCorrelationID(), clientReplyTo);
						mismatchCounter++;
					}
				}
				if (mismatchCounter == CONSTANT_100) {
					running = false;
				}
				Thread.sleep(pausetime);
			} catch (JMSException e) {
				logger.warn(null, "run", JMSLogMessage.CLIENT_JMSEX_RCV, ID, e);

				try {
					Thread.sleep(pausetime);
				} catch (InterruptedException ie) {
				}
			} catch (Exception e) {
				logger.warn(null, "run", JMSLogMessage.CLIENT_FATAL, ID, e);
				failCounter++;
				return;
			}
		}
	}

	private TextMessage send(TextMessage message, long timeout) throws JMSException {

		message.setJMSReplyTo(replyToQueue);

		sender.send(message);

		QueueReceiver consumer = session.createReceiver(replyToQueue,
				"JMSCorrelationID = '" + message.getJMSMessageID() + "'");

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
