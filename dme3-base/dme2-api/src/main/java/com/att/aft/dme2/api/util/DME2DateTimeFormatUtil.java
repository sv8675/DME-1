/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api.util;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.TimeZone;

import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.DME2Constants;

public final class DME2DateTimeFormatUtil {
	private static final Logger LOGGER = LoggerFactory.getLogger(DME2DateTimeFormatUtil.class);
	private static final String ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS zzz";
	private static final String UTC = "UTC";

	private DME2DateTimeFormatUtil() {
	}

	public static final DateTimeFormatter DEFAULT_DATE_TIME_FORMAT_WITH_UTC_ZONE = DateTimeFormatter
			.ofPattern(ISO_FORMAT).withZone(ZoneOffset.UTC.normalized());
	public static final DateTimeFormatter DEFAULT_DATE_TIME_FORMAT_WITH_SYSTEM_ZONE = DateTimeFormatter
			.ofPattern(ISO_FORMAT);

	public static ZonedDateTime convertStringToDateTime(String dateString, DME2Configuration config) {
		try {
			return Objects.nonNull(config)
					&& config.getBoolean(DME2Constants.AFT_DME2_ALLOW_CLIENT_SEND_TZ_OVERRIDE, false)
							? ZonedDateTime.parse(dateString,
									DEFAULT_DATE_TIME_FORMAT_WITH_SYSTEM_ZONE.withZone(TimeZone
											.getTimeZone(config.getProperty(
													DME2Constants.AFT_DME2_CLIENT_SEND_TIMESTAMP_TZ_KEY, UTC))
											.toZoneId()))
							: ZonedDateTime.parse(dateString, DEFAULT_DATE_TIME_FORMAT_WITH_SYSTEM_ZONE);
		} catch (Exception ex) {
			LOGGER.warn(null, "convertStringToDateTime", "Error in formatting string to date", ex);
			// Ignore any parse exception and return null
			return null;
		}
	}

	public static String convertDateTimeToString(ZonedDateTime dateTime, DME2Configuration config) {
		try {
			return Objects.nonNull(config)
					&& config
							.getBoolean(DME2Constants.AFT_DME2_ALLOW_CLIENT_SEND_TZ_OVERRIDE, false)
									? dateTime.withZoneSameInstant(TimeZone
											.getTimeZone(config.getProperty(
													DME2Constants.AFT_DME2_CLIENT_SEND_TIMESTAMP_TZ_KEY, UTC))
											.toZoneId()).format(DEFAULT_DATE_TIME_FORMAT_WITH_SYSTEM_ZONE)
									: dateTime.format(DEFAULT_DATE_TIME_FORMAT_WITH_SYSTEM_ZONE);
		} catch (Exception ex) {
			LOGGER.warn(null, "convertDateTimeToString", "Error in formatting string to date", ex);
			// Ignore any parse exception and return null
			return null;
		}

	}
}