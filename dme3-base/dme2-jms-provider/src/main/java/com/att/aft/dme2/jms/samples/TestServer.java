/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms.samples;

import java.util.Hashtable;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.naming.InitialContext;

@SuppressWarnings("PMD.SystemPrintln")
public class TestServer {

    private String jndiClass = null;
    private String jndiUrl = null;
    private String serverConn = null;
    private String serverDest = null;
    private int threads = 0;
    
    private QueueConnection connection = null;
    private static final int CONSTANT_5000=5000;

    public TestServer(String jndiClass, String jndiUrl, String serverConn, String serverDest, int threads) throws Exception {
        this.jndiClass = jndiClass;
        this.jndiUrl = jndiUrl;
        this.serverConn = serverConn;
        this.serverDest = serverDest;
        this.threads = threads;
    }
    
    public void start() throws JMSException, javax.naming.NamingException {
        Hashtable<String,Object> table = new Hashtable<String,Object>();
        table.put("java.naming.factory.initial",
               jndiClass);
        table.put("java.naming.provider.url", jndiUrl);

        System.out.println("Getting InitialContext");
        InitialContext context = new InitialContext(table);

        System.out.println("Looking up QueueConnectionFactory");
        QueueConnectionFactory qcf = (QueueConnectionFactory) context
                .lookup(serverConn);

        System.out.println("Looking up request Queue");
        Queue requestQueue = (Queue) context
                .lookup(serverDest);

        System.out.println("Creating QueueConnection");
        connection = qcf.createQueueConnection();

        listeners = new TestServerListener[threads];
        for (int i = 0; i < threads; i++) {
            listeners[i] = new TestServerListener(connection, requestQueue);
            listeners[i].start();
        }        
    }
    
    private TestServerListener[] listeners = null;
    
    public void stop() throws JMSException {
        if (listeners != null) {
            for (int i = 0; i < threads; i++) {
                listeners[i].stop();
            }
        } 
        listeners = null;
        connection.close();
        connection = null;
    }
    
    public static void main(String[] args) throws Exception {
        String jndiClass = null;
        String jndiUrl = null;
        String serverConn = null;
        String serverDest = null;
        String serverThreadsStr = "0";
        
        System.out.println("Starting HttpJMS TestServer");
        
        String usage = "TestServer -jndiClass <jndiClass> -jndiUrl <jndiUrl> -serverConn <url> -serverDest <url> -serverThreads <n>";
        
        for (int i = 0; i < args.length; i++) {
            if ("-jndiClass".equals(args[i])) {
                jndiClass = args[i+1];
            } else if ("-jndiUrl".equals(args[i])) {
                jndiUrl = args[i+1];
            } else if ("-serverConn".equals(args[i])) {
                serverConn = args[i+1];
            } else if ("-serverDest".equals(args[i])) {
                serverDest = args[i+1];
            } else if ("-serverThreads".equals(args[i])) {
                serverThreadsStr = args[i+1];
            } else if ("-?".equals(args[i])) {
                System.out.println(usage);
                System.exit(0);
            }
        }
        
        int serverThreads = Integer.parseInt(serverThreadsStr);

        System.out.println("Running with following arguments:");
        System.out.println("    JNDI Provider Class: " + jndiClass);
        System.out.println("    JNDI Provider URL: " + jndiUrl);
        System.out.println("    Server Connection: " + serverConn);
        System.out.println("    Server Destination: " + serverDest);
        System.out.println("    Server Threads: " + serverThreads);
              
        TestServer server = null;
        if (serverThreads > 0) {
            System.out.println("Starting listeners...");
            server = new TestServer(jndiClass, jndiUrl, serverConn, serverDest, serverThreads);
            server.start();
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


