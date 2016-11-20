/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.jms;

import javax.jms.JMSException;

public class TestClient {

    public static final String SCM_RELEASE = "@(#) Harvest Environment [environment] Baseline [baseline]";
    public static final String SCM_VERSION = "@(#) Harvest [viewpath]/[item] Version [version] [crtime]";

    private String jndiClass;
    private String jndiUrl;
    private String clientConn;
    private String clientDest;
    private String clientReplyTo;
    private int threadCount;

    private TestClientSender[] senders = null;

    public TestClient(String jndiClass, String jndiUrl, String clientConn,
            String clientDest, String clientReplyTo, int threadCount) {
        this.jndiClass = jndiClass;
        this.jndiUrl = jndiUrl;
        this.clientConn = clientConn;
        this.clientDest = clientDest;
        this.clientReplyTo = clientReplyTo;
        this.threadCount = threadCount;
    }

    public void start() throws JMSException, javax.naming.NamingException {
        senders = new TestClientSender[threadCount];
        for (int i = 0; i < threadCount; i++) {
            senders[i] = new TestClientSender("" + i, jndiClass, jndiUrl,
                    clientConn, clientDest, clientReplyTo);
            senders[i].start();
            Thread thread = new Thread(senders[i]);
            thread.setDaemon(false);
            thread.start();
        }
    }

    public void stop() throws JMSException {
        if (senders != null) {
            for (int i = 0; i < senders.length; i++) {
                try {
                    senders[i].stop();
                } catch (JMSException e) {
                    System.err.println(senders[i].getID()
                            + " failed to stop properly");
                    e.printStackTrace();
                }
            }
        }
    }
}
