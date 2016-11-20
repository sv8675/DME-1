package com.att.aft.dme2.handler;

import java.util.Map;

public interface FailoverHandlerIntf {
	public boolean isFailoverRequired( String replyMessage, int respCode, Map<String, String> respHeaders );
	
}
