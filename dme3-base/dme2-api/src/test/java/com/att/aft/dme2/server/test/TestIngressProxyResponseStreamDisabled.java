/*******************************************************************************
* Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.server.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.att.aft.dme2.test.DME2BaseTestCase;
import com.att.aft.dme2.util.DME2Constants;

// This was moved here (outside of TestIngressProxy) because of the way DME2IngressProxyServlet utilizes the config.
// Since it is set up statically and by name, it is complicated to reload it.
@Ignore
public class TestIngressProxyResponseStreamDisabled extends DME2BaseTestCase {
  @Before
  public void setUp()
  {
    super.setUp();
    System.setProperty("AFT_DME2_PROXY_SKIPEXIT", "true");
    //This test class requires a different platform than others. Im not clear why.
    System.setProperty("platform", "SANDBOX-DEV" );
  }


  @After
  public void tearDown()
  {
    System.clearProperty("AFT_DME2_GRM_URLS");
    System.clearProperty("platform");
    System.clearProperty("AFT_DME2_PROXY_SKIPEXIT");
  }

  @Ignore
  @Test
  public void testIngressProxy_WithResponseStreamDisabled() throws Exception
  {
    try
    {
      System.setProperty( DME2Constants.AFT_DME2_DISABLE_INGRESS_REPLY_STREAM, "true");


      String[] args = { "-p", "5617" };
      IngressProxyThread pt = new IngressProxyThread(args);

      Thread t = new Thread(pt);
      t.setDaemon(true);
      t.start();

      Thread.sleep(10000);

      int timeout = 120000;
      String payload = "Sending ECHOTest";

      InputStream istream = null;

      URL url = null;
      //com.att.aft.DME2CREchoService service is not running in SANDBOX-DEV/UAT that's why changed the envContext from UAT to TEST
      url = new URL("http://localhost:5617/service=com.att.aft.DME2CREchoService/version=1/envContext=TEST/routeOffer=BAU/partner=xyz");

      HttpURLConnection conn = null;
      conn = (HttpURLConnection) url.openConnection();
      conn.setConnectTimeout(timeout);
      conn.setReadTimeout(timeout);
      conn.setDoInput(true);
      conn.setDoOutput(true);

      OutputStream out = conn.getOutputStream();
      out.write(payload.getBytes());
      out.flush();
      out.close();

      Thread.sleep(3000);

      int respCode = conn.getResponseCode();
      assertEquals(200, respCode);

      istream = conn.getInputStream();
      String streamheader = conn.getHeaderField("X-DME2_PROXY_STREAM");
      String proxyStreamMode = conn.getHeaderField("X-DME2_PROXY_RESPONSE_STREAM");

      InputStreamReader input = new InputStreamReader(istream);
      final char[] buffer = new char[8096];
      StringBuilder output = new StringBuilder(8096);

      try
      {
        for (int read = input.read(buffer, 0, buffer.length); read != -1; read = input.read(buffer, 0,
            buffer.length))
        {
          output.append(buffer, 0, read);
        }
      }
      catch (IOException e){}

      System.out.println("OUTPUT: " + output);

			/*check for some known data from the echo service
			Sending ECHOTest; Receiver: PID@HOST: 19299@hltd216.hydc.sbc.com*/
      assertTrue(output.toString().contains("PID@HOST"));
      assertTrue(streamheader == null);
      assertTrue(proxyStreamMode == null);
    }
    finally
    {
      System.clearProperty(DME2Constants.AFT_DME2_DISABLE_INGRESS_REPLY_STREAM);
    }

  }
}
