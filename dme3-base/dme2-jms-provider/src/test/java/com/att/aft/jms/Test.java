/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.jms;

public class Test {

    public static void main(String[] args) throws Exception {
        String jndiClass = null;
        String jndiUrl = null;
        String clientConn = null;
        String clientDest = null;
        String clientReplyTo = null;
        String serverConn = null;
        String serverDest = null;
        String serverThreadsStr = "0";
        String clientThreadsStr = "0";
        String testLengthStr = "60000";
        
        System.out.println("Starting AFTJMS Tester");
        
        String usage = "Tester -jndiClass <jndiClass> -jndiUrl <jndiUrl> -clientConn <url> -clientDest <url> -clientReplyTo <url> -clientThreads <n> -serverConn <url> -serverDest <url> -serverThreads <n>";

        for (int i = 0; i < args.length; i++) {
            if ("-jndiClass".equals(args[i])) {
                jndiClass = args[i+1];
            } else if ("-jndiUrl".equals(args[i])) {
                jndiUrl = args[i+1];
            } else if ("-clientConn".equals(args[i])) {
                clientConn = args[i+1];
            } else if ("-clientDest".equals(args[i])) {
                clientDest = args[i+1];
            } else if ("-clientReplyTo".equals(args[i])) {
                clientReplyTo = args[i+1];
            } else if ("-serverConn".equals(args[i])) {
                serverConn = args[i+1];
            } else if ("-serverDest".equals(args[i])) {
                serverDest = args[i+1];
            } else if ("-clientThreads".equals(args[i])) {
                clientThreadsStr = args[i+1];
            } else if ("-serverThreads".equals(args[i])) {
                serverThreadsStr = args[i+1];
            } else if ("-testLength".equals(args[i])) {
                testLengthStr = args[i+1];
            } else if ("-?".equals(args[i])) {
                System.out.println(usage);
                System.exit(0);
            }
        }
        
        
        
        
        int serverThreads = Integer.parseInt(serverThreadsStr);
        int clientThreads = Integer.parseInt(clientThreadsStr);
        int testLength = Integer.parseInt(testLengthStr);

        System.out.println("Running with following arguments:");
        System.out.println("    JNDI Provider Class: " + jndiClass);
        System.out.println("    JNDI Provider URL: " + jndiUrl);
        System.out.println("    Server Connection: " + serverConn);
        System.out.println("    Server Destination: " + serverDest);
        System.out.println("    Server Threads: " + serverThreads);
        System.out.println("    Client Connection: " + clientConn);
        System.out.println("    Client Destination: " + clientDest);
        System.out.println("    Client Threads: " + clientThreads);
        System.out.println("    Test Length: " + testLength);
        
        
        /**TestServer server = null;
        if (serverThreads > 0) {
            System.out.println("Starting servers...");
            server = new TestServer(jndiClass, jndiUrl, serverConn, serverDest, serverThreads);
            server.start();
        } */
        
        TestClient client = null;
        if (clientThreads > 0) {
            System.out.println("Starting clients...");
            client = new TestClient(jndiClass, jndiUrl, clientConn, clientDest, clientReplyTo, clientThreads);
            client.start();
        }
        
        Thread.sleep(testLength);

        if (client != null) {
            client.stop();
            TestClientSender.dumpCounters();
        }
        /**
        if (server != null) {
            server.stop();
        }*/
    }
}
