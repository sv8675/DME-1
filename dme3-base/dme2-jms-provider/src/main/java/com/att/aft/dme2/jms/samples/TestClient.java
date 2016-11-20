/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms.samples;

import javax.jms.JMSException;
import javax.naming.NamingException;
@SuppressWarnings("PMD.SystemPrintln")
public class TestClient {
    private String jndiClass;
    private String jndiUrl;
    private String clientConn;
    private String clientDest;
    private String clientReplyTo;
    private int threadCount;

    private TestClientSender[] senders = null;
    private static final int CONSTANT_5000=5000;
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
            thread.setName("dme2JMS::TestClient::SenderThread-" + i);
            thread.setDaemon(true);
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
    
    public static void main(String[] args) throws JMSException, NamingException, InterruptedException {
        String jndiClass = null;
        String jndiUrl = null;
        String conn = null;
        String dest = null;
        String replyTo = null;
        String threadsStr = "0";
        
        System.out.println("Starting HttpJMS TestClient");
        
        String usage = "TestClient -jndiClass <jndiClass> -jndiUrl <jndiUrl> -conn <url> -dest <url> -replyTo <name> -threads <n>";
        
        for (int i = 0; i < args.length; i++) {
            if ("-jndiClass".equals(args[i])) {
                jndiClass = args[i+1];
            } else if ("-jndiUrl".equals(args[i])) {
                jndiUrl = args[i+1];
            } else if ("-conn".equals(args[i])) {
                conn = args[i+1];
            } else if ("-dest".equals(args[i])) {
                dest = args[i+1];
            } else if ("-replyTo".equals(args[i])) {
            	replyTo = args[i+1];
            } else if ("-replyTo".equals(args[i])) {
            	replyTo = args[i+1];
            } 
            else if ("-threads".equals(args[i])) {
                threadsStr = args[i+1];
            } else if ("-?".equals(args[i])) {
                System.out.println(usage);
                System.exit(0);
            }
        }
        
        int threads = Integer.parseInt(threadsStr);

        System.out.println("CLIENT Running with following arguments:");
        System.out.println("    JNDI Provider Class: " + jndiClass);
        System.out.println("    JNDI Provider URL: " + jndiUrl);
        System.out.println("    Connection: " + conn);
        System.out.println("    Destination: " + dest);
        System.out.println("    ReplyTo: " + replyTo);
        System.out.println("    Threads: " + threads);
              
        TestClient client = null;
        if (threads > 0) {
            System.out.println("Starting listeners...");
            client = new TestClient(jndiClass, jndiUrl, conn, dest, replyTo, threads);
            client.start();
        } else {
        	System.out.flush();
        	System.err.println("No thread count specified, cannot start server");
        	System.exit(1);
        }
        
        while (true) {
        	Thread.sleep(CONSTANT_5000);
        }
    }
}
