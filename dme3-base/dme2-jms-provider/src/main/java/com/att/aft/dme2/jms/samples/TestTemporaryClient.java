/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms.samples;

import javax.jms.JMSException;
import javax.naming.NamingException;

@SuppressWarnings("PMD.SystemPrintln")
public class TestTemporaryClient {
    private String jndiClass;
    private String jndiUrl;
    private String clientConn;
    private String clientDest;
    private int threadCount;

    private TestTemporaryClientSender[] senders = null;
    private static final int CONSTANT_5000=5000;
    public TestTemporaryClient(String jndiClass, String jndiUrl, String clientConn,
            String clientDest, int threadCount) {
        this.jndiClass = jndiClass;
        this.jndiUrl = jndiUrl;
        this.clientConn = clientConn;
        this.clientDest = clientDest;

        this.threadCount = threadCount;
    }

    public void start() throws JMSException, javax.naming.NamingException {
        senders = new TestTemporaryClientSender[threadCount];
        for (int i = 0; i < threadCount; i++) {
            senders[i] = new TestTemporaryClientSender("" + i, jndiClass, jndiUrl,
                    clientConn, clientDest);
            senders[i].start();
            Thread thread = new Thread(senders[i]);
            thread.setName("dme2JMS::TestTemporaryClient::SenderThread-" + i);
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
        String threadsStr = "0";
        
        System.out.println("Starting HttpJMS TestTemporaryClient");
        
        String usage = "TestTemporaryClient -jndiClass <jndiClass> -jndiUrl <jndiUrl> -conn <url> -dest <url> -threads <n>";
        
        for (int i = 0; i < args.length; i++) {
            if ("-jndiClass".equals(args[i])) {
                jndiClass = args[i+1];
            } else if ("-jndiUrl".equals(args[i])) {
                jndiUrl = args[i+1];
            } else if ("-conn".equals(args[i])) {
                conn = args[i+1];
            } else if ("-dest".equals(args[i])) {
                dest = args[i+1];
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
        System.out.println("    Threads: " + threads);
              
        TestTemporaryClient client = null;
        if (threads > 0) {
            System.out.println("Starting listeners...");
            client = new TestTemporaryClient(jndiClass, jndiUrl, conn, dest, threads);
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
