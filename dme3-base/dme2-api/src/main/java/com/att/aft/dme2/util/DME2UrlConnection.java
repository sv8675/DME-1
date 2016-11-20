package com.att.aft.dme2.util;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

public class DME2UrlConnection extends URLConnection
{

	protected DME2UrlConnection(URL url)
	{
		super(url);
	}

	@Override
	public void connect() throws IOException
	{
		throw new UnsupportedOperationException("The connect() method is not supported");
	}

}
