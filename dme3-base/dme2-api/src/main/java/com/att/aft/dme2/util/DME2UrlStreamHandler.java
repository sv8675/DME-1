package com.att.aft.dme2.util;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public class DME2UrlStreamHandler extends URLStreamHandler
{

	@Override
	protected URLConnection openConnection(URL u) throws IOException
	{
		return new DME2UrlConnection(u);
	}

}
