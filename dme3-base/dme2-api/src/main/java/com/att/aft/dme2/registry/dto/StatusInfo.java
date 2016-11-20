package com.att.aft.dme2.registry.dto;

import javax.xml.datatype.XMLGregorianCalendar;

public class StatusInfo {

    private String status;
    private String statusReasonCode;
    private String statusReasonDescription;
    private XMLGregorianCalendar statusCheckTime;
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getStatusReasonCode() {
		return statusReasonCode;
	}
	public void setStatusReasonCode(String statusReasonCode) {
		this.statusReasonCode = statusReasonCode;
	}
	public String getStatusReasonDescription() {
		return statusReasonDescription;
	}
	public void setStatusReasonDescription(String statusReasonDescription) {
		this.statusReasonDescription = statusReasonDescription;
	}
	public XMLGregorianCalendar getStatusCheckTime() {
		return statusCheckTime;
	}
	public void setStatusCheckTime(XMLGregorianCalendar statusCheckTime) {
		this.statusCheckTime = statusCheckTime;
	}
}
