
package com.att.aft.dme2.util;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.server.UID;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;


public class GuidGen {
	private static final Logger logger = LoggerFactory.getLogger(GuidGen.class.getCanonicalName());
	
	// private fields
	private static Random random = new Random(System.currentTimeMillis());

	private static StringBuffer IPAddressHex;

	private static char[] hexTable = { '0', '1', '2', '3', '4', '5', '6', '7',
			'8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	// private constants
	private final static String DIGEST_ALGO = "MD5";

	// Static initialization block to retrieve IP Address of host
	// and add JVM ID
	static {
		StringBuffer ipBuffer = new StringBuffer(15);
		try {
			InetAddress localAddr = InetAddress.getLocalHost();
			byte[] IPAddressBytes = localAddr.getAddress();
			int ipAddressLength = IPAddressBytes.length;

			for (int i = 0; i < ipAddressLength; i++) {
				ipBuffer.append(IPAddressBytes[i]);
				if (i < (ipAddressLength - 1)) {
					ipBuffer.append('.');
				}
			}
			GuidGen.IPAddressHex = GuidGen.convertBytesToHexSB(IPAddressBytes);
		} // end of try catch
		catch (UnknownHostException ex) {
			logger.error(null, "static GuidGen", "AFT-DME2-6501", new ErrorContext().add("exception", ex.getMessage()));
		} // end of catch block


	} // end of static init block

	private static StringBuffer convertBytesToHexSB(byte[] bytes) {
		StringBuffer sb = new StringBuffer(bytes.length * 2);
		int byteLen = bytes.length;
		for (int index = 0; index < byteLen; index++) {
			char tempChar;
			/*
			 * Get the first 4 bits (high) Do bitwise logical AND to get rid of
			 * low nibble. Shift results to right by 4 and get char
			 * representation
			 */
			tempChar = GuidGen.hexTable[((bytes[index] & 0xf0) >>> 4)];
			sb.append(tempChar);

			/*
			 * Get the last 4 bits (low) Do bitwise logical AND to get rid of
			 * high nibble. Get char representation
			 */
			tempChar = GuidGen.hexTable[(bytes[index] & 0x0f)];
			sb.append(tempChar);
		} // end of for block

		return sb;
	}

	public static String getGUID(String objDesc) {
		StringBuffer sb = new StringBuffer(35);

		sb.append(GuidGen.getTimeMillisHex());
		// sb.append(UUID_TOKEN);
		sb.append(GuidGen.getIPAddressHex());
		// sb.append(UUID_TOKEN);
		sb.append(GuidGen.getSimpleHash(objDesc, null));
		// sb.append(UUID_TOKEN);
		sb.append(GuidGen.getRandomNumber());

		return sb.toString();
	}

	/**
	 * Generates unique identifier.
	 * 
	 * Algorithm used to generate ID is defined in the Architecture
	 * Configuration with the element: uniqueIdAlgorithm = GUID|UID
	 * 
	 * @return String representing unique identifier
	 */
	public static String getId() {
		String id = null;
		id = GuidGen.getGUID(GuidGen.getRandomNumber().toString());
		return id;
	}

	private static StringBuffer getIPAddressHex() {
		return GuidGen.IPAddressHex;
	}

	public static StringBuffer getRandomNumber() {
		StringBuffer sb = null;
		byte bytes[] = new byte[4];

		GuidGen.random.nextBytes(bytes);
		sb = GuidGen.convertBytesToHexSB(bytes);

		if (sb == null) {
			new DME2Exception("AFT-DME2-6502", new ErrorContext());
		} // end of if block

		return sb;
	}

	private static StringBuffer getSimpleHash(String targetStr, byte[] key) {
		StringBuffer sb = new StringBuffer(8);
		byte[] digest;

		byte[] bytes = targetStr.getBytes();

		try {
			String digestStr;
			MessageDigest md = MessageDigest.getInstance(GuidGen.DIGEST_ALGO);

			md.update(bytes);
			if (key == null) {
				digest = md.digest();
			} // end of if blank
			else {
				digest = md.digest(key);
			} // end of else block

			digestStr = new BigInteger(1, digest).toString(16);
			sb.append(digestStr.substring(0, 8));
		} catch (NoSuchAlgorithmException ex) {
			new DME2Exception("AFT-DME2-6500", new ErrorContext().add("exception", ex.getMessage()));
		}

		return sb;
	}

	private static StringBuffer getTimeMillisHex() {
		StringBuffer sb = new StringBuffer(8);
		long time = System.currentTimeMillis();
		String timeHex = Long.toHexString(time);
		int strLength = timeHex.length();

		if (strLength == 8) {
			sb.append(timeHex);
		} // end of if block
		else if (strLength > 8) {
			sb.append(timeHex.substring(strLength - 8, strLength));
		} // end of else-if block
		else {
			// time hex string length is less than 8
			int remainder = 8 - strLength;
			for (int i = 0; i < remainder; i++) {
				sb.append('0');
			}
			sb.append(timeHex);

		} // end of else block

		return sb;
	}

	public static String getUID() {
		String UIDString = null;
		;
		StringBuffer sb = null;
		UID uid = new UID();

		UIDString = uid.toString();
		sb = new StringBuffer(UIDString.length() + 9);
		sb.append(UIDString);
		sb.append(':');
		sb.append(GuidGen.getIPAddressHex());

		return sb.toString();
	}

}
