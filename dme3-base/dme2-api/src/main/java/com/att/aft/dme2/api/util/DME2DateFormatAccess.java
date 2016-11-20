/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.DME2Constants;

public class DME2DateFormatAccess {
	final String ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS zzz";
	final String GMT = "GMT";
	/** The logger. */
	private static Logger logger = LoggerFactory.getLogger( DME2DateFormatAccess.class );
	
	private DME2Configuration config = null;
	
	
	public DME2DateFormatAccess(DME2Configuration config) {
		this.config = config;
	}
	
	private ThreadLocal<DateFormat> df = new ThreadLocal<DateFormat>() {

		@Override
		public DateFormat get() {
			return super.get();
		}

		@Override
		protected DateFormat initialValue() {
			SimpleDateFormat sdf = new SimpleDateFormat(ISO_FORMAT);
			if(config.getBoolean(DME2Constants.AFT_DME2_ALLOW_CLIENT_SEND_TZ_OVERRIDE, false)) {
				sdf.setTimeZone(TimeZone.getTimeZone(config.getProperty(DME2Constants.AFT_DME2_CLIENT_SEND_TIMESTAMP_TZ_KEY, GMT)));
			}
			return sdf;
		}

		@Override
		public void remove() {
			super.remove();
		}

		@Override
		public void set(DateFormat value) {
			super.set(value);
		}

	};

	public Date convertStringToDate(String dateString) {
		try {
			return df.get().parse(dateString);
		} catch (Exception e) {
			logger.warn( null, "convertStringToDate",  "Error in formatting string to date", e);
			// Ignore any parse exception and return null
			return null;
		}
	}

	public String convertDateToString(Date date) {
		try {
			return df.get().format(date);
		} catch (Exception e) {
			logger.warn( null, "convertDateToString",  "Error in formatting date to string", e);
			// Ignore any parse exception and return null
			return null;
		}
	}

}
