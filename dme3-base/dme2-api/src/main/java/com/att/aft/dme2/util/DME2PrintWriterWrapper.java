package com.att.aft.dme2.util;

import java.io.OutputStream;
import java.io.PrintWriter;

public class DME2PrintWriterWrapper extends PrintWriter {

	private boolean closeRequested = false;
	
	public DME2PrintWriterWrapper(OutputStream out) {
		super(out);
	}
	
	@Override
	public void close() {
		closeRequested = true;
	}
	
	public boolean isCloseRequested() {
		return closeRequested;
	}
	
	public void closeForReal() {
		super.close();
	}
}
