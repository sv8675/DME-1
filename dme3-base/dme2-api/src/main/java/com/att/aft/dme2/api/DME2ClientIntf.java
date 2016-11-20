/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api;

import com.att.aft.dme2.request.DME2Payload;

public interface DME2ClientIntf {
  public void send( DME2Payload payload ) throws DME2Exception ;
  public Object sendAndWait( DME2Payload payload ) throws DME2Exception ;
  public void stop() throws DME2Exception ;
}