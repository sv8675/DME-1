package com.att.aft.dme2.util;

import java.io.IOException;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

public class DME2CountingServletOutputStream extends ServletOutputStream {
	private ServletOutputStream delegate;
	private int byteCounter = 0;
	private boolean closeRequested = false;
	public DME2CountingServletOutputStream(ServletOutputStream delegate) {
		this.delegate = delegate;
		
	}
	public void write(int b) throws IOException {
		delegate.write(b);
	}
	public int hashCode() {
		return delegate.hashCode();
	}
	public void write(byte[] b) throws IOException {
		byteCounter++;
		delegate.write(b);
	}
	public void print(String s) throws IOException {
		delegate.print(s);
	}
	public void write(byte[] b, int off, int len) throws IOException {
		delegate.write(b, off, len);
	}
	public void print(boolean b) throws IOException {
		delegate.print(b);
	}
	public void print(char c) throws IOException {
		delegate.print(c);
	}
	public boolean equals(Object obj) {
		return delegate.equals(obj);
	}
	public void print(int i) throws IOException {
		delegate.print(i);
	}
	public void flush() throws IOException {
		delegate.flush();
	}
	public void print(long l) throws IOException {
		delegate.print(l);
	}
	public void print(float f) throws IOException {
		delegate.print(f);
	}
	public void close() throws IOException {
		closeRequested = true;
	}
	public void print(double d) throws IOException {
		delegate.print(d);
	}
	public void println() throws IOException {
		delegate.println();
	}
	public void println(String s) throws IOException {
		delegate.println(s);
	}
	public void println(boolean b) throws IOException {
		delegate.println(b);
	}
	public void println(char c) throws IOException {
		delegate.println(c);
	}
	public void println(int i) throws IOException {
		delegate.println(i);
	}
	public void println(long l) throws IOException {
		delegate.println(l);
	}
	public void println(float f) throws IOException {
		delegate.println(f);
	}
	public void println(double d) throws IOException {
		delegate.println(d);
	}
	public String toString() {
		return delegate.toString();
	}

	public int getCurrentByteCount() {
		return byteCounter;
	}
	
	public boolean isCloseRequested() {
		return closeRequested;
	}
	
	public void closeForReal() throws IOException {
		delegate.close();
	}
	@Override
	public boolean isReady() {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public void setWriteListener(WriteListener writeListener) {
		// TODO Auto-generated method stub
		
	}
};