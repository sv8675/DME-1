package com.att.aft.dme2.event;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.URI;
import java.util.Date;

import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.request.DmeUniformResource;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.DME2Utils;
import com.att.aft.dme2.util.ErrorContext;

/**
 * The DME2ServiceStats class provides a class which holds different service stats during application runtime.
 * The DME2ServiceStatManager holds an instance of DME2ServiceStats per service.
 * @author po704t
 *
 */

public class DME2ServiceStats {

	private byte[] statsObjLock = new byte[0];

	private Logger logger = LoggerFactory.getLogger(DME2ServiceStats.class.getName());
	private long createTime = 0;
	long lastTouchedTime = 0;

	long requestCount = 0;
	long currentHourRequestCount = 0;
	long replyCount = 0;

	public String getServiceVersion() {
		return serviceVersion;
	}

	public void setServiceVersion(String serviceVersion) {
		this.serviceVersion = serviceVersion;
	}

	public String getServiceEnv() {
		return serviceEnv;
	}

	public void setServiceEnv(String serviceEnv) {
		this.serviceEnv = serviceEnv;
	}

	public String getContainerName() {
		return containerName;
	}

	public void setContainerName(String containerName) {
		this.containerName = containerName;
	}

	public String getContainerVersion() {
		return containerVersion;
	}

	public void setContainerVersion(String containerVersion) {
		this.containerVersion = containerVersion;
	}

	public String getContainerEnv() {
		return containerEnv;
	}

	public void setContainerEnv(String containerEnv) {
		this.containerEnv = containerEnv;
	}

	public String getContainerPlat() {
		return containerPlat;
	}

	public void setContainerPlat(String containerPlat) {
		this.containerPlat = containerPlat;
	}

	public String getContainerHost() {
		return containerHost;
	}

	public void setContainerHost(String containerHost) {
		this.containerHost = containerHost;
	}

	public String getContainerPid() {
		return containerPid;
	}

	public void setContainerPid(String containerPid) {
		this.containerPid = containerPid;
	}

	public String getContainerRO() {
		return containerRO;
	}

	public void setContainerRO(String containerRO) {
		this.containerRO = containerRO;
	}

	long failoverCount = 0;

	long maxElapsed = -1;
	long minElapsed = -1;
	private long lastRequestElapsed = -1;

	long lastRequestMsgSize = -1;
	private long lastReplyMsgSize = -1;
	private String queueName;

	long totalElapsed = -1;
	private long expiredCount = 0;

	private DME2Configuration config;

	String service;
	String serviceVersion;
	String serviceEnv;

	String containerName;
	String containerVersion;
	String containerEnv;
	String containerPlat;
	String containerHost;
	String containerPid;
	String containerRO;

	/**
	 * Constructor for DME2ServiceStats
	 * @param queueName
	 */
	public DME2ServiceStats(DME2Configuration config, String queueName) {
		this.config = config;
		this.queueName = queueName;
		this.createTime = System.currentTimeMillis();
		// Initiate TIMER for reloading the cached endpoints

		// Set metrics publish interval
		System.setProperty("metrics.publish.interval", "" + config.getLong(DME2Constants.AFT_DME2_PUBLISH_METRICS_INTERVAL));
		String host = null;
		try {
			host = InetAddress.getLocalHost().getHostName();
		} catch (Exception e) {
			logger.debug(null, "DME2ServiceStats", LogMessage.DEBUG_MESSAGE, "Exception",e);
			// ignore any exception
		}
		containerName = config.getProperty(config.getProperty(DME2Constants.AFT_DME2_CONTAINER_NAME_KEY), System.getProperty(config.getProperty(DME2Constants.AFT_DME2_CONTAINER_NAME_KEY)));
		if (containerName == null) {
			containerName = DME2Constants.DEFAULT_NA_VALUE;
		}
		containerVersion = config.getProperty(config.getProperty(DME2Constants.AFT_DME2_CONTAINER_VERSION_KEY), System.getProperty(config.getProperty(DME2Constants.AFT_DME2_CONTAINER_VERSION_KEY)));
		if (containerVersion == null) {
			containerVersion = DME2Constants.DEFAULT_NA_VALUE;
		}
		containerRO = config.getProperty(config.getProperty(DME2Constants.AFT_DME2_CONTAINER_ROUTEOFFER_KEY),System.getProperty(config.getProperty(DME2Constants.AFT_DME2_CONTAINER_ROUTEOFFER_KEY)));
		if (containerRO == null) {
			containerRO = DME2Constants.DEFAULT_NA_VALUE;
		}
		containerEnv = config.getProperty(config.getProperty(DME2Constants.AFT_DME2_CONTAINER_ENV_KEY), System.getProperty(config.getProperty(DME2Constants.AFT_DME2_CONTAINER_ENV_KEY)));
		if (containerEnv == null) {
			containerEnv = DME2Constants.DEFAULT_NA_VALUE;
		}
		containerPlat = config.getProperty(config.getProperty(DME2Constants.AFT_DME2_CONTAINER_SCLD_PLATFORM_KEY), System.getProperty(config.getProperty(DME2Constants.AFT_DME2_CONTAINER_SCLD_PLATFORM_KEY)));
		if (containerPlat == null) {
			containerPlat = config.getProperty(config.getProperty(DME2Constants.AFT_DME2_CONTAINER_PLATFORM_KEY), System.getProperty(config.getProperty(DME2Constants.AFT_DME2_CONTAINER_PLATFORM_KEY)));
		}
		if (containerPlat == null) {
			containerPlat = DME2Constants.DEFAULT_NA_VALUE;
		}
		containerHost = config.getProperty(DME2Constants.AFT_DME2_CONTAINER_HOST_KEY);
		if (containerHost == null) {
			containerHost = host;
		}
		
		String pid = null;
		try {
			RuntimeMXBean rmxb = ManagementFactory.getRuntimeMXBean();
			String temp = rmxb.getName();
			if (temp != null && temp.contains("@")) {
				String temps[] = temp.split("@");
				pid = temps[0];
			}
		} catch (Throwable e) {
			logger.debug(null, "DME2ServiceStats", LogMessage.DEBUG_MESSAGE, "Throwable",e);
		}

		if (pid == null)
			pid = DME2Constants.DEFAULT_NA_VALUE;
		containerPid = config.getProperty(DME2Constants.AFT_DME2_CONTAINER_PID_KEY);

		if (containerPid == null)
			containerPid = DME2Constants.DEFAULT_NA_VALUE;
		
		if (containerRO != null)
			containerRO = containerRO.replaceAll("'", "");

		logger.debug( null, "ctor(String)", "containerName={},containerVersion={},containerRO={},containerEnv={},containerPlat={},containerHost={},containerPid={}", containerName, containerVersion, containerRO, containerEnv, containerPlat, containerHost, containerPid );
		try {
			String urlStr = DME2Utils.formatClientURIString(queueName);
			DmeUniformResource uniformResource = new DmeUniformResource(config, new URI(urlStr));
			service = uniformResource.getService();
			serviceVersion = uniformResource.getVersion();
			serviceEnv = uniformResource.getEnvContext();
		} catch (Exception e) {
			logger.debug(null, "DME2ServiceStats", LogMessage.DEBUG_MESSAGE,
					"error inside Service Stats Initialization metod DME2ServiceStats(String queueName", e);
			// ignore any exception since URI would have been validated already in 
			// execution stack
		}
	}

	
/**
 * Helper method to get Stats for a specific Service
 */
	
	public String[] getStats() {
		BigDecimal tpsValue = null;
		BigDecimal avgEl = null;
		try {
			tpsValue = new BigDecimal(this.currentHourRequestCount / 300.0);
			tpsValue = tpsValue.setScale(4, BigDecimal.ROUND_HALF_UP);
			if(this.replyCount == 0) {
				avgEl = new BigDecimal(0);
			}else{
				avgEl = new BigDecimal(this.totalElapsed / this.replyCount);
			}
			avgEl = avgEl.setScale(4, BigDecimal.ROUND_HALF_UP);
		} catch (Exception e) {
			logger.error(null, "getStats", "AFT-DME2-1901", new ErrorContext()
		     .add("queueName", this.queueName)
		     .add("extendedMessage", e.getMessage())
		     ,e);
		}

		String[] retString = new String[16];
		retString[0] = ("Statistics for");
		retString[1] = ("DME2 Queue=" + queueName);
		retString[2] = ("\tCreate Date=" + new Date(this.createTime));
		retString[3] = ("\tLast Touched="
				+ (lastTouchedTime == 0 ? new Date(this.createTime) : new Date(this.lastTouchedTime)));
		retString[4] = ("\tPuts=" + this.requestCount);
		retString[5] = ("\tGets=" + this.replyCount);
		retString[6] = ("\tCurrentHourTPS=" + (tpsValue != null ? tpsValue : 0));
		retString[7] = ("\tCurrentHourRequestCount=" + this.currentHourRequestCount);
		retString[8] = ("\tTotalRequestCount=" + this.requestCount);
		retString[9] = ("\tFailoverCount=" + this.failoverCount);
		retString[10] = ("\tLastRequestSize=" + this.lastRequestMsgSize);
		retString[11] = ("\tLastReplySize=" + this.lastReplyMsgSize);
		retString[12] = ("\tMinElapsed=" + this.minElapsed);
		retString[13] = ("\tMaxElapsed=" + this.maxElapsed);
		retString[14] = ("\tLastElapsed=" + this.lastRequestElapsed);
		retString[15] = ("\tAverageElapsed=" + (avgEl != null ? avgEl : 0));

		return retString;
	}

	public String toString() {
		BigDecimal tpsValue = new BigDecimal(requestCount / 300.0);
		tpsValue = tpsValue.setScale(4, BigDecimal.ROUND_HALF_UP);

		return "\tStatistics for " + "\t\tQueue\t\t:\t" + queueName + "\t\tCreate Date\t\t " + "\t\t Last Touched \t\t"
				+ "\t\t Current Size \t\t" + "\t\t Puts\t\t" + "\t\t Gets\t\t" + "\t\t Expired\t\t"
				+ "\t\t RequestCount\t\t" + requestCount + "\t\t Average TPS\t\t" + tpsValue
				+ "\t\t Max ElapsedTime\t\t" + "\t\t Min ElapsedTime\t\t" + "\t\t Avg RequestMsg Size \t\t"
				+ "\t\t Avg ResponseMsg Size\t\t";
	}

	public void setLastTouchedTime(long lastTouchedTime) {
		this.lastTouchedTime = lastTouchedTime;
	}

	public byte[] getStatsObjLock() {
		return statsObjLock;
	}

	@SuppressWarnings("unchecked")
	public long getCurrentHourRequestCount() {
		return currentHourRequestCount;
	}

	public void setCurrentHourRequestCount(long currentHourRequestCount) {
		this.currentHourRequestCount = currentHourRequestCount;
	}

	public void setStatsObjLock(byte[] statsObjLock) {
		this.statsObjLock = statsObjLock;
	}

	public long getCreateTime() {
		return createTime;
	}

	public void setCreateTime(long createTime) {
		this.createTime = createTime;
	}

	public long getRequestCount() {
		return requestCount;
	}

	public void setRequestCount(long requestCount) {
		this.requestCount = requestCount;
	}

	public long getReplyCount() {
		return replyCount;
	}

	public void setReplyCount(long replyCount) {
		this.replyCount = replyCount;
	}

	public long getFailoverCount() {
		return failoverCount;
	}

	public void setFailoverCount(long failoverCount) {
		this.failoverCount = failoverCount;
	}

	public long getMaxElapsed() {
		return maxElapsed;
	}

	public void setMaxElapsed(long maxElapsed) {
		this.maxElapsed = maxElapsed;
	}

	public long getMinElapsed() {
		return minElapsed;
	}

	public void setMinElapsed(long minElapsed) {
		this.minElapsed = minElapsed;
	}

	public long getLastRequestElapsed() {
		return lastRequestElapsed;
	}

	public void setLastRequestElapsed(long lastRequestElapsed) {
		this.lastRequestElapsed = lastRequestElapsed;
	}

	public long getLastRequestMsgSize() {
		return lastRequestMsgSize;
	}

	public void setLastRequestMsgSize(long lastRequestMsgSize) {
		this.lastRequestMsgSize = lastRequestMsgSize;
	}

	public long getLastReplyMsgSize() {
		return lastReplyMsgSize;
	}

	public void setLastReplyMsgSize(long lastReplyMsgSize) {
		this.lastReplyMsgSize = lastReplyMsgSize;
	}

	public String getQueueName() {
		return queueName;
	}

	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}

	public long getTotalElapsed() {
		return totalElapsed;
	}

	public void setTotalElapsed(long totalElapsed) {
		this.totalElapsed = totalElapsed;
	}

	public long getExpiredCount() {
		return expiredCount;
	}

	public void setExpiredCount(long expiredCount) {
		this.expiredCount = expiredCount;
	}

	public long getLastTouchedTime() {
		return lastTouchedTime;
	}

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}

	
}
