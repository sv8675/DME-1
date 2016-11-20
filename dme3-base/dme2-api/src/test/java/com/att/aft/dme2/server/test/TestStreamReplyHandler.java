/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.server.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.att.aft.dme2.api.DME2StreamReplyHandler;

public class TestStreamReplyHandler extends DME2StreamReplyHandler
{

	ByteArrayOutputStream bos = new ByteArrayOutputStream();
	
	@Override
	public void handleContent(byte[] bytes)
	{
		try
		{
			bos.write(bytes);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
	}

	public ByteArrayOutputStream getByteStream()
	{
		return bos;
	}

}
