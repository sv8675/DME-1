/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jaxws.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.ProtocolException;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import com.att.aft.dme2.api.DME2Client;
import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;

/**
 * This handler provides a JAX-WS bridge to DME2 without dealing with the 
 * complexities of each JAX-WS implementation's JMS/SOAP bindings (if even
 * available).
 */
public class DME2SOAPHandler implements SOAPHandler<SOAPMessageContext> {
    private String handlerName=null;
    
    private DME2Manager manager = null;

    /**
     * Set the handler's name
     * @param name
     */
    public void setHandlerName(String name) {
        handlerName=name;
    }
    
    /**
     * Get the handler's name
     * @return
     */
    public String getHandlerName() {
        return handlerName;
    }

    /**
     * Initialize the handler
     */
    @PostConstruct
    public synchronized void init() {
    	try {
    		if (manager == null) {
    			manager = DME2Manager.getDefaultInstance();
    		}
		} catch (DME2Exception e) {
			throw new RuntimeException(DME2SOAPHandler.class.getName() + " init() failed with extended exception message: " + e.toString(), e);
		}
    }

    /**
     * Destroy the handler
     */
    @PreDestroy
    public void destroy() {
        
    }

    /*
     * Return the handler's headers
     * @see javax.xml.ws.handler.soap.SOAPHandler#getHeaders()
     */
    public Set<QName> getHeaders() {
        return new TreeSet<QName>();
    }
    
    /**
     * Handle an outbound or enbound message
     */
	@Override
	public boolean handleMessage(SOAPMessageContext context) {
		if (manager == null) {
			init();
		}
		
		boolean outbound = ((Boolean)context.get(SOAPMessageContext.MESSAGE_OUTBOUND_PROPERTY)).booleanValue();
        
		String uriString = (String)context.get("com.att.aft.dme2.jaxws.client.uri");
		
		if (uriString == null) {
			uriString = (String)context.get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY);
			
		}
		
		if (uriString == null) {
			throw new RuntimeException("DME2SOAPHandler required context property com.att.aft.dme2.jaxws.client.uri to be set");
		}
		
		Long connTimeoutMs = (Long)context.get("com.att.aft.dme2.jaxws.client.connTimeoutMs");
		
		if (connTimeoutMs == null) {
			connTimeoutMs = 4000L;
		}

		Long readTimeoutMs = (Long)context.get("com.att.aft.dme2.jaxws.client.readTimeoutMs");
		
		if (readTimeoutMs == null) {
			readTimeoutMs = 30000L;
		}
		
		URI uri;
		try {
			uri = new URI(uriString);
		} catch (URISyntaxException e1) {
			throw new RuntimeException("DME2SOAPHandler retrieved uri [" + uriString + "] from com.att.aft.dme2.jaxws.client.uri, but it is not in a valid format: " + e1.toString(), e1);
		}
		
        if (outbound) {
            SOAPMessage soapMessage =  context.getMessage();
    		try {
    			DME2Client client = manager.newClient(uri, connTimeoutMs.longValue());
    			///client.setHeaders(headers);
    			client.setMethod("POST");
    			client.setPayload(convertSOAPMessageToString(soapMessage));
    			String response = client.sendAndWait(readTimeoutMs);
    			context.setMessage(convertStringToSOAPMessage(response));
    			
    		} catch (SOAPException e) {
    			throw new ProtocolException("Failure during call to remote service: " + e.toString(), e);
    		} catch (Exception e) {
    			throw new ProtocolException(e.toString(), e);
    		}
    			
    		return false;
        } else {
        	return true;
        }
	}

	/**
	 * Handle a fault message
	 */
	@Override
	public boolean handleFault(SOAPMessageContext context) {
		if (manager == null) {
			init();
		}

		return true;
	}    

	/**
	 * Close the exchange for the associated context
	 */
    public void close(MessageContext context)
    {

    }

    /** 
     * Helper to stringify the message
     * @param message
     * @return
     */
    private String convertSOAPMessageToString(SOAPMessage message) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            message.writeTo(baos);
            return baos.toString();
        } catch (Exception e) {
        	throw new RuntimeException("Failed to convert SOAPMessage to payload stream because: " + e.toString(), e);
        }
    }
    
    /** 
     * Helper to stringify the message
     * @param message
     * @return
     */
    private SOAPMessage convertStringToSOAPMessage(String soapText) {
        try {
            MessageFactory msgFactory     = MessageFactory.newInstance();  
            SOAPMessage message           = msgFactory.createMessage();  
            SOAPPart soapPart             = message.getSOAPPart();  
            byte[] buffer                 = soapText.getBytes();  
            ByteArrayInputStream stream   = new ByteArrayInputStream(buffer);  
            StreamSource source           = new StreamSource(stream);  
            soapPart.setContent(source);
            return message;
        } catch (Exception e) {
        	throw new ProtocolException("Failed to convert incoming payload to a SOAPMessage because: " + e.toString(), e);
        }
    }
    

}