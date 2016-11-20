package com.att.aft.dme2.server.test;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.att.aft.dme2.api.DME2Manager;

public class GRMFindRunningResponseServlet extends HttpServlet{

  String expireTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format( new Date(System.currentTimeMillis()+1000000));
	String badFindRunningResponse = "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">"+
			"<SOAP-ENV:Header></SOAP-ENV:Header><SOAP-ENV:Body><ns3:findRunningServi"+
			"ceEndPointResponse xmlns:ns2=\"http://scld.att.com/grm/types/v1\" xmlns:ns3=\"http://scld.att.com/grm/v1\" xmlns:ns4=\"http://scld.att.com/grm/policy/v1\" xmlns:ns5=\""+
			"http://scld.att.com/grm/route/types/v1\" xmlns:ns6=\"http://scld.att.com/grm/tms/v1\">"+
			"<ns3:ServiceEndPointList><ns2:name>com.att.aft.DME2CREchoService</ns2:name><ns2:version major=\"69\" minor=\"5\" patch=\"0\"></ns2:version><"+
			"ns2:hostAddress>zld01845.vci.att.com</ns2:hostAddress><ns2:listenPort>45372</ns2:listenPort><ns2:latitude>37.66</ns2:latitude><ns2:longitude>-122.09683"+
			"9</ns2:longitude><ns2:registrationTime>"+
			"2014-05-21T05:44:38.237-07:00</ns2:registrationTime><ns2:expirationTime>2099-05-21T05:44:38.237-07:00</ns2:expirationTime>"+
			"<ns2:contextPath>/service=com.att.aft.DME2CREchoService/version="+
			"1.5.0/envContext=LAB/routeOffer=BAU</ns2:contextPath><ns2:routeOffer>BAU</ns2:routeOffer>"+
			"<ns2:protocol>http</ns2:protocol><ns2:DME2Version>3.2.0</ns2:DME2Version></ns3:ServiceEndPointList>"+
			"<ns3:ServiceEndPointList><ns2:name>com.att.aft.DME2CREchoService</ns2:name><ns2:version major=\"69\" minor=\"5\" patch=\"0\"></ns2:version><"+
			"ns2:hostAddress>zld01854.vci.att.com</ns2:hostAddress><ns2:listenPort>45371</ns2:listenPort><ns2:latitude>abc</ns2:latitude>"+
			"<ns2:longitude>abc</ns2:longitude><ns2:registrationTime>2014-05-21T05:44:38.237-07:00</ns2:registrationTime>"+
			"<ns2:expirationTime>" + expireTime  + "</ns2:expirationTime>"+
			"<ns2:contextPath>/service=com.att.aft.DME2CREchoService/version=1.5.0/envContext=LAB/routeOffer=BAU</ns2:contextPath>"+
			"<ns2:routeOffer>BAU</ns2:routeOffer><ns2:protocol>http</ns2:protocol><ns2:DME2Version>2.5.13</ns2:DME2Version></ns3:ServiceEndPointList>"+
			"<ns3:ServiceEndPointList><ns2:name>com.att.aft.DME2CREchoService</ns2:name><ns2:version major=\"69\" minor=\"5\" patch=\"0\"></ns2:version><"+
			"ns2:hostAddress>zldv0433.vci.att.com</ns2:hostAddress><ns2:listenPort>45373</ns2:listenPort><ns2:latitude></ns2:latitude>"+
			"<ns2:longitude></ns2:longitude><ns2:registrationTime>2014-05-21T05:44:38.237-07:00</ns2:registrationTime>"+
			"<ns2:expirationTime>" + expireTime  + "</ns2:expirationTime>"+
			"<ns2:contextPath>/service=com.att.aft.DME2CREchoService/version=1.5.0/envContext=LAB/routeOffer=BAU</ns2:contextPath>"+
			"<ns2:routeOffer>BAU</ns2:routeOffer><ns2:protocol>http</ns2:protocol><ns2:DME2Version>2.5.13</ns2:DME2Version></ns3:ServiceEndPointList>"+
			"</ns3:findRunningServiceEndPointResponse></SOAP-ENV:Body></SOAP-ENV:Envelope>";
	
	public void service(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		resp.getWriter().print(badFindRunningResponse);
		return;
	}
	
	
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, java.io.IOException {
		response.getWriter().print(badFindRunningResponse);
		return;
	}
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, java.io.IOException {
		response.getWriter().print(badFindRunningResponse);
		return;
	}
	
	
	public static void main(String a[]) throws Exception {
		System.setProperty("AFT_LATITUDE", "1.0");
		System.setProperty("AFT_LONGITUDE", "1.0");
		System.setProperty("AFT_ENVIRONMENT", "AFTUAT");
		System.setProperty("platform", "SCLD-DEV");
		System.setProperty("DME2_GRM_DNS_BOOTSTRAP", "");
		String serviceURIStr = "/service=com.att.aft.dme2.test.GRMSimulator/version=1.0.0/envContext=LAB/routeOffer=BAU/";
		DME2Manager mgr = new DME2Manager();
		mgr.bindServiceListener(serviceURIStr, new GRMFindRunningResponseServlet());
		while(true) {
			Thread.sleep(20000);
		}
	}
}
