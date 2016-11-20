package com.att.aft.dme2.iterator.domain;

import com.att.aft.dme2.event.EventType;
/**
 * There are few attributes that are not required to be exposed to the client through IteratorMetricsEvent entity. <br>
 * So for additional attributes related to event that are needed to be set internally are to be set in here in this attribute
 */
public class IteratorMetricsEventHolder {
	private IteratorMetricsEvent iteratorMetricsEvent;
	private EventType eventType;
	private long elapsedTime=-1l;
	public IteratorMetricsEvent getIteratorMetricsEvent() {
		return iteratorMetricsEvent;
	}
	public void setIteratorMetricsEvent(IteratorMetricsEvent iteratorMetricsEvent) {
		this.iteratorMetricsEvent = iteratorMetricsEvent;
	}
	public EventType getEventType() {
		return eventType;
	}
	public void setEventType(EventType eventType) {
		this.eventType = eventType;
	}
	public long getElapsedTime() {
		return elapsedTime;
	}
	public void setElapsedTime(long elapsedTime) {
		this.elapsedTime = elapsedTime;
	}
	@Override
	public String toString() {
		return "IteratorMetricsEventHolder [iteratorMetricsEvent=" + iteratorMetricsEvent + ", eventType=" + eventType
				+ ", elapsedTime=" + elapsedTime + "]";
	}
}