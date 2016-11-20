package com.att.aft.dme2.event;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.ErrorContext;


/**
 * The DME2ServiceStatManager class provides a Management class to hold different Service Stats.
 * It stores different service stats in a hashmap.
 *  
 * 
 * @author po704t
 *
 */
public class DME2ServiceStatManager implements Runnable {

	static ConcurrentHashMap<String, DME2ServiceStats> instanceMap = new ConcurrentHashMap<String, DME2ServiceStats>();
	private static ArrayList<String> queueNames = new ArrayList<String>();
	static final Map<String, DME2Event> requestMap = Collections.synchronizedMap(new HashMap<String, DME2Event>());
	private DME2Configuration config;

	private long checkInterval = DME2Constants.CHECK_INTERVAL;
	private long expiryInterval = DME2Constants.EXPIRY_INTERVAL;

	private byte[] statsObjLock = new byte[0];
	private byte[] instanceObjLock = new byte[0];

	private static Logger logger = LoggerFactory.getLogger(DME2ServiceStatManager.class.getName());

	private boolean timerInitiated = false;

	private static DME2ServiceStatManager INSTANCE;

	//metrics collector interface can not be used until that is modified to dme2
	private boolean disableMetrics = false;//true;
	private int expiredCount;
	private boolean disableCleanup = false;

  private Timer expiredMsgs;

	public boolean isDisableCleanup() {
		return disableCleanup;
	}

	public void setDisableCleanup(boolean disableCleanup) {
		this.disableCleanup = disableCleanup;
	}

	public static ArrayList<String> getQueueNames() {
		return queueNames;
	}

	public static void setQueueNames(ArrayList<String> tempQueueNames) {
		queueNames = tempQueueNames;
	}

	public long getCheckInterval() {
		return checkInterval;
	}

	public byte[] getStatsObjLock() {
		return statsObjLock;
	}

	public byte[] getInstanceObjLock() {
		return instanceObjLock;
	}

	public boolean isTimerInitiated() {
		return timerInitiated;
	}

	public boolean isDisableMetrics() {
		return disableMetrics;
	}

	public void setDisableMetrics(boolean tempDisableMetrics) {
		disableMetrics = tempDisableMetrics;
	}

	public int getExpiredCount() {
		return expiredCount;
	}

	public void setExpiredCount(int expiredCount) {
		this.expiredCount = expiredCount;
	}

/**
 * Constructor for DME2ServiceStatManager
 */
	private DME2ServiceStatManager(DME2Configuration config) {
		this.config = config;
		try {
			checkInterval = Long.parseLong(config.getProperty("DME2_QS_TIMER_INT"));
		} catch (Exception e) {
			checkInterval = DME2Constants.CHECK_INTERVAL;
		}
		try {
			expiryInterval = Long.parseLong(config.getProperty("DME2_QS_MSGEXP_INT"));
		} catch (Exception e) {
			expiryInterval = DME2Constants.EXPIRY_INTERVAL;
		}

		try {
			disableMetrics = Boolean.parseBoolean(config.getProperty("AFT_DME2_DISABLE_METRICS"));
			logger.debug( null, "ctor(DME2Configuration)", "inside DME2ServiceStatManager - setting disableMetrics flag : {}", disableMetrics);
		} catch (Exception e) {
			disableMetrics = false;
		}		
		
		if (!timerInitiated) {
			expiredMsgs = new Timer("DME2::DME2QueueStats::FindExpiredMessagesTimer", true);
			logger.debug(null, "DME2ServiceStatManager", "inside DME2ServiceStatManager - creating expiredMessageCleanup Schedule Task");
			
			try {
				expiredMsgs.schedule(new TimerTask() {
					@Override
					public void run() {
						logger.debug(null, "ctor(DME2Configuration)", "inside FindExpiredMessagesTimer = checkInterval : {} - expiryInterval : {}", checkInterval, expiryInterval);
						cleanUpExpiredMessage();
					}
				}, checkInterval * 1000, checkInterval * 1000);
				timerInitiated = true;
			} catch (Exception e) {
				logger.error( null, "ctor(DME2Configuration)", LogMessage.Q_STATS_FAILED, e);
				logger.error(null, "ctor(DME2Configuration)", "Error in creating Scheduled Task", e);
			}
		}
    Runtime.getRuntime().addShutdownHook( new Thread(this) );
	}

	public static ArrayList<String> getServiceNames() {
		return queueNames;
	}


	/**
	 * This method returns the Service statistics helpd for all services.
	 */
	public String[] diagnostics() throws Exception {
		logger.debug(null, "diagnostics", "entering diagnostics method");
		ArrayList<String> retList = new ArrayList<String>();
		String[] retStr = null;
		try {
			Iterator<String> it = getServiceNames().iterator();
			while (it.hasNext()) {
				boolean ignoreQueue = false;
				String serviceName = it.next();
				String splitStr[] = config.getProperty(DME2Constants.AFT_DME2_QLIST_IGNORE).split(",");
				for(int i=0; i<splitStr.length;i++) {
					if ( serviceName != null && serviceName.contains(splitStr[i])) {
						ignoreQueue = true;
						break;
					}
				}
				// If queueName is found in ignore list, ignore getting stats
				if(ignoreQueue){
					continue;
				}
				DME2ServiceStats sstats = getServiceStats(serviceName);
				String statsArr[] = sstats.getStats();
				for (int j = 0; j < statsArr.length; j++) {
					retList.add(statsArr[j]);
				}
			}
		} catch (Exception e) {
			logger.error(null, "diagnostics", "Error in diagnostics() ", e);
		}
		retStr = new String[retList.size()];
		logger.debug(null, "diagnostics", "exiting diagnostics method - retStr :{}", retStr);
		return retList.toArray(retStr);
	}
	
	/**
	 * The getServiceStats method provides the DME2ServiceStats object for the specific service.
	 * @param serviceName
	 * @return
	 */
	public DME2ServiceStats getServiceStats(String serviceName) {
		synchronized (instanceMap) {
			if (instanceMap.get(serviceName) != null) {
				return instanceMap.get(serviceName);
			} else {
				DME2ServiceStats instance = new DME2ServiceStats(config, serviceName);
				instanceMap.put(serviceName, instance);
				if (serviceName != null) {
					if (serviceName.indexOf("service=") != -1) {
						queueNames.add(serviceName);
					}
				}
				return instance;
			}
		}
	}

	public long getExpiryInterval() {
		return expiryInterval;
	}

	public ConcurrentHashMap<String, DME2ServiceStats> getInstanceMap() {
		return instanceMap;
	}

	public Map<String, DME2Event> getRequestmap() {
		return requestMap;
	}

	public void setExpiryInterval(long tempExpiryInterval) {
		expiryInterval = tempExpiryInterval;
	}

/**
 * Helper method for getting Service Stats for a specific service.
 * It relies on DME2ServiceStats class for service stats.
 */
	public String[] getStats(String serviceName) {
		DME2ServiceStats ss = getServiceStats(serviceName);
		return ss.getStats();
	}

	private boolean isCurrentHourMillis(long millis) {
		Calendar cal = Calendar.getInstance(TimeZone.getDefault());

		Calendar cal1 = Calendar.getInstance(TimeZone.getDefault());
		cal1.setTimeInMillis(millis);
		if (cal1.get(Calendar.HOUR_OF_DAY) == cal.get(Calendar.HOUR_OF_DAY)) {
			return true;
		}

		return false;
	}

/**
 * The cleanUpExpiredMessage method is called every few configured minutes and looks at the requestMap holder and cleans up expired messages.
 */
	@SuppressWarnings("static-access")
	private void cleanUpExpiredMessage() {
		logger.debug(null, "cleanUpExpiredMessage", "entering cleanUpExpiredMessage() - disableCleanup : {}", disableCleanup);
		if(!disableCleanup){
			synchronized (statsObjLock) {
				Set<String> set = requestMap.keySet();
				logger.debug( null, "cleanUpExpiredMessage","DME2ServiceStatManager cleanUpExpiredMessage requestMap size={}  ; expiredMessagesCount={}; hashCode={}", instanceMap.size(), expiredCount, instanceMap.hashCode());
				try {
					if (set != null) {
						ArrayList<String> expiredIdList = new ArrayList<String>();
						expiredIdList.addAll(set);
						for (String msgId : expiredIdList) {
							DME2Event data = (DME2Event) requestMap.get(msgId);
							long eventTime = data.getEventTime();
							if ((eventTime + (expiryInterval * 1000)) <= System.currentTimeMillis()) {
								expiredCount++;
								requestMap.remove(msgId);
								logger.debug(null, "cleanUpExpiredMessage", "inside cleanUpExpiredMessage() : incrementing expiredCount : {} and deleting msgId : {}",expiredCount, msgId);
							}
						}
					}
				} catch (Exception e) {
					logger.debug(null, "cleanUpExpiredMessage", "AFT-DME2-1902", 
							new ErrorContext().add("queueName", this.queueNames.toString()).add("extendedMessage", e.getMessage()), e);
				}
			}
		}
		logger.debug(null, "cleanUpExpiredMessage", "exiting cleanUpExpiredMessage() ");
	}

/**
 * This method returns the static singleton instance of DME2ServiceStatManager
 */
	public static DME2ServiceStatManager getInstance(DME2Configuration config) {
		if(INSTANCE == null) {
			INSTANCE = new DME2ServiceStatManager(config);
		}
		return INSTANCE;
	}

  @Override
  public void run() {
    logger.debug( null, "run", LogMessage.METHOD_ENTER );
    if ( expiredMsgs != null ) {
      logger.debug( null, "run", "Shutting down timer task" );
      expiredMsgs.cancel();
    }
    logger.debug( null, "run", LogMessage.METHOD_EXIT );
  }
}
