/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.HashMap;
import java.util.Map;

import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;

public class PortFileLockManager {

  private static final Logger logger = LoggerFactory.getLogger( PortFileLockManager.class );
	/** Stores locks for various log files */
	private static Map<String,PortFileLockManager> map = new HashMap<String,PortFileLockManager>();
	
	/** wait time before giving up on a lock */
	private final long MAXWAITTIME = 25; // 25 ms
	private long sleepInterval = 5; // 5 ms
	private long waitIterations = 5;

	/** The lock file to use */
	private static String lockDir = null;

	/** The FileChannel object which we are locking */
	private FileChannel channel = null;

	/** The FileLock object which is currently held */
	private FileLock lock = null;

	/** The name of the GlobalLock object */
	private String name = null;

	private String portFileToLock = null;
	private String LOCKFILE = null;

	/**
	 * /** Private constructor to create new PortFileLockManager object
	 */
	private PortFileLockManager(String fileName, long waitIterations,
			long sleepInterval) throws IOException {
		name = fileName;
		this.waitIterations = waitIterations;
		this.sleepInterval = sleepInterval;

		File portFile = new File(fileName);
		if (portFile.exists()) {
			lockDir = portFile.getParent();
			portFileToLock = portFile.getName();
		} else {
			lockDir = fileName.substring(0,
					fileName.lastIndexOf(File.separator));
			portFileToLock = fileName.substring(fileName
					.lastIndexOf(File.separator) + 1);
		}
		LOCKFILE = lockDir + File.separator + "." + portFileToLock + ".lock";
		FileOutputStream stream = getNewFileOutputStream(LOCKFILE);
		channel = stream.getChannel();
	}

	/**
	 * Returns the PortFileLockManager object for the lock file identified
	 */
	public static synchronized PortFileLockManager getInstance(String fileName,
			long waitIterations, long sleepInterval) throws IOException {
		PortFileLockManager lock = map.get(fileName);
		if (lock == null) {
			lock = new PortFileLockManager(fileName, waitIterations,
					sleepInterval);
			map.put(fileName, lock);
		}
		return lock;
	}

	/**
	 * Returns a new FileOutputStream object
	 */
	private static FileOutputStream getNewFileOutputStream(String filename)
			throws IOException {
		File file = new File(filename);
		file.createNewFile();
		FileOutputStream stream = new FileOutputStream(file);
		return stream;
	}

	/**
	 * Call this to acquire a lock with default timeout (15 mins)
	 */
	public synchronized void acquire() throws LockFailedException {
		acquire(MAXWAITTIME);
	}

	/**
	 * Call this to acquire a lock with specified timeout, in milliseconds
	 */
	public synchronized void acquire(long timeout) throws LockFailedException {

		// if its not null then another thread has already locked
		if (lock != null) {
			return;
		}

		try {
			long startTime = System.currentTimeMillis();
			long elapsedTime = 0;
			for (int i = 0; i < waitIterations; i++) {

				lock = channel.tryLock();

				if (lock != null) {
					return;
				}

				sleep(sleepInterval);
				elapsedTime = System.currentTimeMillis() - startTime;

				logger.debug( null, "acquire", "PortFileLockManager waiting to acquire lock after waiting [{}] ms", elapsedTime);
			}

      logger.debug( null, "acquire", "PortFileLockManager could not acquire after waiting [{}] ms", elapsedTime);

			throw new LockFailedException(name,
					"Could not acquire after waiting [" + elapsedTime + "] ms");

		} catch (ClosedChannelException e) {
			throw new LockFailedException(name, e);
		} catch (OverlappingFileLockException e) {
			logger.debug( null, "acquire", "WARNING: Lock is already held by another thread");
		} catch (IOException e) {
			throw new LockFailedException(name, e);
		}
	}

	/**
	 * Call this to acquire a lock on the PortFileLockManager object instance
	 */
	public synchronized void release() {
		if (lock != null) {
			try {
				lock.release();
				lock = null;
			} catch (IOException e) {
				logger.debug( null, "release", "WARNING: While releasing lock caught [{}]", e.toString());
			}
		}
	}

	/**
	 * Helper sleep method
	 */
	private void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {

		}

		return;
	}

}

/*
 * This exception will be thrown when a lock cannot be acquired
 */
class LockFailedException extends RuntimeException {

	public LockFailedException(String name, Exception e) {
		super("Lock [" + name + "] could not be acquired with exception ["
				+ e.toString() + "]", e);

	}

	public LockFailedException(String name, String msg) {
		super("Lock [" + name + "] could not be acquired with reason [" + msg
				+ "]");
	}
}