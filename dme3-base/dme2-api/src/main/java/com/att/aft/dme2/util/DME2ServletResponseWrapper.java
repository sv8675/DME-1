package com.att.aft.dme2.util;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class DME2ServletResponseWrapper  extends
    HttpServletResponseWrapper {
	private DME2CountingServletOutputStream countingOutputStream;
	private DME2PrintWriterWrapper writer;

	public DME2ServletResponseWrapper(HttpServletResponse response) throws IOException{
        super(response);
        this.countingOutputStream = new DME2CountingServletOutputStream(response.getOutputStream());
        this.writer = new DME2PrintWriterWrapper(this.countingOutputStream);
    }
    
    public DME2CountingServletOutputStream getOutputStream() throws IOException {
    	return this.countingOutputStream;
    }
    
    public int getCurrentResponseByteSize() {
    	return this.countingOutputStream.getCurrentByteCount();
    }
    
    public DME2PrintWriterWrapper getWriter() {
    	return writer;
    }
}

