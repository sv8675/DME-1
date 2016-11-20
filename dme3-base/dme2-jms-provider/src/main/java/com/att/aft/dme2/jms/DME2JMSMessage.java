/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms;

import java.util.Enumeration;
import java.util.Properties;
import java.util.UUID;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;

import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.ErrorContext;

public abstract class DME2JMSMessage implements Message {
	private static final Logger logger = LoggerFactory.getLogger(DME2JMSMessage.class.getName());

	private final Properties props = new Properties();
	private Destination destination;
	private Destination replyTo;

	// default expiration for any message lacking an expiration.

	public DME2JMSMessage() {
	}

	protected void genID() {
		String id = "ID:" + UUID.randomUUID();
		props.setProperty("JMSMessageID", id);
	}

	public Properties getProperties() {
		return props;
	}

	@Override
	public void acknowledge() throws JMSException {

	}

	@Override
	public void clearProperties() throws JMSException {
		props.clear();
	}

	@Override
	public boolean getBooleanProperty(String key) throws JMSException {
		return Boolean.parseBoolean(props.getProperty(key));
	}

	@Override
	public byte getByteProperty(String key) throws JMSException {
		return Byte.parseByte(props.getProperty(key));
	}

	@Override
	public double getDoubleProperty(String key) throws JMSException {
		return Double.parseDouble(props.getProperty(key));
	}

	@Override
	public float getFloatProperty(String key) throws JMSException {
		return Float.parseFloat(props.getProperty(key));
	}

	@Override
	public int getIntProperty(String key) throws JMSException {
		return Integer.parseInt(props.getProperty(key));
	}

	@Override
	public String getJMSCorrelationID() throws JMSException {
		return props.getProperty("JMSCorrelationID");
	}

	@Override
	public byte[] getJMSCorrelationIDAsBytes() throws JMSException {
		String id = props.getProperty("JMSCorrelationID");
		if (id != null) {
			return id.getBytes();
		} else {
			return null;
		}
	}

	@Override
	public int getJMSDeliveryMode() throws JMSException {
		String s = props.getProperty("JMSDeliverMode");
		if (s != null) {
			return Integer.parseInt(s);
		} else {
			return DeliveryMode.NON_PERSISTENT;
		}
	}

	@Override
	public Destination getJMSDestination() throws JMSException {
		return destination;
	}

	@Override
	public long getJMSExpiration() throws JMSException {
		String s = props.getProperty("JMSExpiration");
		if (s != null) {
			return Long.parseLong(s);
		} else {
			return -1;
		}
	}

	@Override
	public String getJMSMessageID() throws JMSException {
		return props.getProperty("JMSMessageID");
	}

	@Override
	public int getJMSPriority() throws JMSException {
		String s = props.getProperty("JMSPriority");
		if (s != null) {
			return Integer.parseInt(s);
		} else {
			return -1;
		}
	}

	@Override
	public boolean getJMSRedelivered() throws JMSException {
		return Boolean.parseBoolean(props.getProperty("JMSRedelivered"));
	}

	@Override
	public Destination getJMSReplyTo() throws JMSException {
		return replyTo;
	}

	@Override
	public long getJMSTimestamp() throws JMSException {
		String s = props.getProperty("JMSTimestamp");
		if (s != null) {
			return Long.parseLong(s);
		} else {
			return 0L;
		}
	}

	@Override
	public String getJMSType() throws JMSException {
		return props.getProperty("JMSType");
	}

	@Override
	public long getLongProperty(String key) throws JMSException {
		String s = props.getProperty(key);
		if (s != null) {
			return Long.parseLong(s);
		} else {
			return 0;
		}
	}

	@Override
	public Object getObjectProperty(String key) throws JMSException {
		return props.get(key);
	}

	@Override
	public Enumeration<?> getPropertyNames() throws JMSException {
		return props.keys();
	}

	@Override
	public short getShortProperty(String key) throws JMSException {
		String s = props.getProperty(key);
		if (s != null) {
			return Short.parseShort(s);
		} else {
			return 0;
		}
	}

	@Override
	public String getStringProperty(String key) throws JMSException {
		return props.getProperty(key);
	}

	@Override
	public boolean propertyExists(String key) throws JMSException {
		if (props.contains(key)) {
			return true;
		}
		return false;
	}

	@Override
	public void setBooleanProperty(String key, boolean value) throws JMSException {
		props.setProperty(key, Boolean.toString(value));
	}

	@Override
	public void setByteProperty(String key, byte value) throws JMSException {
		props.setProperty(key, Byte.toString(value));
	}

	@Override
	public void setDoubleProperty(String arg0, double arg1) throws JMSException {
		props.setProperty(arg0, Double.toString(arg1));

	}

	@Override
	public void setFloatProperty(String arg0, float arg1) throws JMSException {
		props.setProperty(arg0, Float.toString(arg1));

	}

	@Override
	public void setIntProperty(String arg0, int arg1) throws JMSException {
		props.setProperty(arg0, Integer.toString(arg1));

	}

	@Override
	public void setJMSCorrelationID(String arg0) throws JMSException {
		if (arg0 != null) { // some clients (Spring, CXF) appear to sometimes
							// call this method with an null CID
			props.setProperty("JMSCorrelationID", arg0);
		}
	}

	@Override
	public void setJMSCorrelationIDAsBytes(byte[] arg0) throws JMSException {
		if (arg0 != null) {
			props.setProperty("JMSCorrelationID", new String(arg0));
		}
	}

	@Override
	public void setJMSDeliveryMode(int arg0) throws JMSException {
		props.setProperty("JMSDeliveryMode", Integer.toString(arg0));
	}

	@Override
	public void setJMSDestination(Destination arg0) throws JMSException {
		destination = arg0;
	}

	@Override
	public void setJMSExpiration(long arg0) throws JMSException {
		props.setProperty("JMSExpiration", Long.toString(arg0));
	}

	@Override
	public void setJMSMessageID(String arg0) throws JMSException {
		if (this.getJMSMessageID() == null && arg0 != null) {
			props.setProperty("JMSMessageID", arg0);
		}
	}

	@Override
	public void setJMSPriority(int arg0) throws JMSException {
		props.setProperty("JMSPriority", Integer.toString(arg0));
	}

	@Override
	public void setJMSRedelivered(boolean arg0) throws JMSException {
		props.setProperty("JMSRedelivered", Boolean.toString(arg0));
	}

	@Override
	public void setJMSReplyTo(Destination arg0) throws JMSException {
		replyTo = arg0;
	}

	@Override
	public void setJMSTimestamp(long arg0) throws JMSException {
		props.setProperty("JMSTimestamp", Long.toString(arg0));
	}

	@Override
	public void setJMSType(String arg0) throws JMSException {
		// CSSA-11007 - adding null check
		if (arg0 != null) {
			props.setProperty("JMSType", arg0);
		}
	}

	@Override
	public void setLongProperty(String arg0, long arg1) throws JMSException {
		props.setProperty(arg0, Long.toString(arg1));
	}

	@Override
	public void setObjectProperty(String key, Object obj) throws JMSException {
		checkNullAndEmpty(key);
		if (!(obj instanceof Integer) && !(obj instanceof String) && !(obj instanceof Long) && !(obj instanceof Double)
				&& !(obj instanceof Boolean) && !(obj instanceof Short) && !(obj instanceof Byte)
				&& !(obj instanceof Float) && !(obj instanceof Character) && obj != null) {
			throw new DME2JMSException("AFT-DME2-5600",
					new ErrorContext().add("JMSProperty-Name", key).add("JMSProperty-Value", obj.toString()));
		}
		if (obj != null) {
			props.put(key, obj);
		}
	}

	@Override
	public void setShortProperty(String arg0, short arg1) throws JMSException {
		props.setProperty(arg0, Short.toString(arg1));
	}

	@Override
	public void setStringProperty(String arg0, String arg1) throws JMSException {
		props.setProperty(arg0, arg1);
	}

	/**
	 * 
	 * @param filter
	 * @return
	 * @throws JMSException
	 */
	public boolean matches(String filter) throws JMSException {
		logger.debug(null, "matches", "DME2JMSMessage matches filter={};messageFilter={}", filter,
				this.getJMSCorrelationID());
		if ((filter == null) || (filter != null && filter.trim().equals(""))) {
			logger.debug(null, "matches", "DME2JMSMessage matches filter={};messageFilter={};matches=true", filter,
					this.getJMSCorrelationID());
			return true;
		}
		String[] expressions = filter.split("AND|OR");
		// TODO: further flesh out filter compare logic to be JMS compliant
		// (where possible/necessary)
		for (String expression : expressions) {
			String[] toks = expression.split("=");
			if (toks.length < 2) {
				continue;
			}
			String key = toks[0];
			String value = toks[1];
			if (key.trim().equals("JMSCorrelationID")) {
				if (this.getJMSCorrelationID() != null) {
					if (this.getJMSCorrelationID().trim().equals(value.trim().replaceAll("'", ""))) {
						return true;
					}
				}
			}
		}

		return false;
	}

	/**
	 * 
	 * @return
	 * @throws JMSException
	 */
	public boolean isExpired() throws JMSException {
		long expiration = this.getJMSExpiration();
		if (expiration > 0 && expiration < System.currentTimeMillis()) {
			logger.debug(null, "isExpired", "message has expired: ", this.getJMSMessageID());
			return true;
		}
		return false;
	}

	protected void checkNullAndEmpty(String key) throws JMSException {
		if (key == null) {
			throw new DME2JMSException("AFT-DME2-5601", new ErrorContext().add("JMSProperty-Name", key));
		}
		if (key == "") {
			throw new DME2JMSException("AFT-DME2-5601", new ErrorContext().add("JMSProperty-Name", key));
		} else {
			return;
		}
	}

}
