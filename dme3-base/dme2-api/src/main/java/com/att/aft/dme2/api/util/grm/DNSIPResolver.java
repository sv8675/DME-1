/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api.util.grm;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * This is a utility class that can be used to get all IP numbers associated with one DNS name
 * 
 * @see com.att.aft.dme2.registry.accessor.GRMEndPointsDiscoveryDNS
 * @author ar671m
 * 
 */
public class DNSIPResolver {

    private DNSIPResolver() {
    }

    /**
     * connect to DNS server and get the initial server list
     * 
     * @throws UnknownHostException
     */
    public static List<String> getListIPForName(String dnsName) throws UnknownHostException {
        // connect to DNS and load the cache content
        List<String> newList = new ArrayList<String>();
        InetAddress[] addresses = InetAddress.getAllByName(dnsName); // UnknownHostException
        for (int i = 0; i < addresses.length; i++) {
            newList.add(ipToString(addresses[i].getAddress()));
        }
        return newList;
    }

    static String ipToString(byte[] ip) {
        StringBuilder builder = new StringBuilder(23); // IP6 size
        for (int part = 0; part < ip.length; part++) {
            if (builder.length() > 0) {
                builder.append('.');
            }
            int code = ip[part];
            if (code < 0) {
                code += 256; // numbers above 127 is interpreted as negative as byte is signed in java
            }
            builder.append(code);
        }
        return builder.toString();
    }
}
