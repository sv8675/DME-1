package com.att.aft.dme2.registry.dto;

import java.util.Properties;

import javax.xml.datatype.XMLGregorianCalendar;

public class ServiceEndpoint {

	private String name;
	private String version;
	private String env;
	private String hostAddress;
	private String port;
	private String latitude;
	private String longitude;
	private String containerName;
	private String containerVersion;
	private String containerRouteOffer;
	private String containerHost;
	private String pid;
	private XMLGregorianCalendar registrationTime;
	private XMLGregorianCalendar expirationTime;
	private String contextPath;
	private String routeOffer;
	private String containerVersionDefinitionName;
	private StatusInfo statusInfo;
	private String protocol;
	private Properties additionalProperties;
	private String clientSupportedVersions;
	private String dmeVersion;
	private String dmeJDBCDatabaseName;
	private String dmeJDBCHealthCheckUser;
	private String dmeJDBCHealthCheckPassword;
	private String dmeJDBCHealthCheckDriver;
	private String createdBy;
	private String updatedBy;
	private XMLGregorianCalendar createdTimestamp;
	private XMLGregorianCalendar updatedTimestamp;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getEnv() {
		return env;
	}

	public void setEnv(String env) {
		this.env = env;
	}

	public String getHostAddress() {
		return hostAddress;
	}

	public void setHostAddress(String hostAddress) {
		this.hostAddress = hostAddress;
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public String getLatitude() {
		return latitude;
	}

	public void setLatitude(String latitude) {
		this.latitude = latitude;
	}

	public String getLongitude() {
		return longitude;
	}

	public void setLongitude(String longitude) {
		this.longitude = longitude;
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

	public String getContainerRouteOffer() {
		return containerRouteOffer;
	}

	public void setContainerRouteOffer(String containerRouteOffer) {
		this.containerRouteOffer = containerRouteOffer;
	}

	public String getContainerHost() {
		return containerHost;
	}

	public void setContainerHost(String containerHost) {
		this.containerHost = containerHost;
	}

	public String getPid() {
		return pid;
	}

	public void setPid(String pid) {
		this.pid = pid;
	}

	public XMLGregorianCalendar getRegistrationTime() {
		return registrationTime;
	}

	public void setRegistrationTime(XMLGregorianCalendar registrationTime) {
		this.registrationTime = registrationTime;
	}

	public XMLGregorianCalendar getExpirationTime() {
		return expirationTime;
	}

	public void setExpirationTime(XMLGregorianCalendar expirationTime) {
		this.expirationTime = expirationTime;
	}

	public String getContextPath() {
		return contextPath;
	}

	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	public String getRouteOffer() {
		return routeOffer;
	}

	public void setRouteOffer(String routeOffer) {
		this.routeOffer = routeOffer;
	}

	public String getContainerVersionDefinitionName() {
		return containerVersionDefinitionName;
	}

	public void setContainerVersionDefinitionName(
			String containerVersionDefinitionName) {
		this.containerVersionDefinitionName = containerVersionDefinitionName;
	}

	public StatusInfo getStatusInfo() {
		return statusInfo;
	}

	public void setStatusInfo(StatusInfo statusInfo) {
		this.statusInfo = statusInfo;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public Properties getAdditionalProperties() {
		return additionalProperties;
	}

	public void setAdditionalProperties(Properties properties) {
		this.additionalProperties = properties;
	}

	public String getClientSupportedVersions() {
		return clientSupportedVersions;
	}

	public void setClientSupportedVersions(String clientSupportedVersions) {
		this.clientSupportedVersions = clientSupportedVersions;
	}

	public String getDmeVersion() {
		return dmeVersion;
	}

	public void setDmeVersion(String dmeVersion) {
		this.dmeVersion = dmeVersion;
	}

	public String getDmeJDBCDatabaseName() {
		return dmeJDBCDatabaseName;
	}

	public void setDmeJDBCDatabaseName(String dmejdbcDatabaseName) {
		dmeJDBCDatabaseName = dmejdbcDatabaseName;
	}

	public String getDmeJDBCHealthCheckUser() {
		return dmeJDBCHealthCheckUser;
	}

	public void setDmeJDBCHealthCheckUser(String dmejdbcHealthCheckUser) {
		dmeJDBCHealthCheckUser = dmejdbcHealthCheckUser;
	}

	public String getDmeJDBCHealthCheckPassword() {
		return dmeJDBCHealthCheckPassword;
	}

	public void setDmeJDBCHealthCheckPassword(
			String dmejdbcHealthCheckPassword) {
		dmeJDBCHealthCheckPassword = dmejdbcHealthCheckPassword;
	}

	public String getDmeJDBCHealthCheckDriver() {
		return dmeJDBCHealthCheckDriver;
	}

	public void setDmeJDBCHealthCheckDriver(String dmejdbcHealthCheckDriver) {
		dmeJDBCHealthCheckDriver = dmejdbcHealthCheckDriver;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public String getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}

	public XMLGregorianCalendar getCreatedTimestamp() {
		return createdTimestamp;
	}

	public void setCreatedTimestamp(XMLGregorianCalendar createdTimestamp) {
		this.createdTimestamp = createdTimestamp;
	}

	public XMLGregorianCalendar getUpdatedTimestamp() {
		return updatedTimestamp;
	}

	public void setUpdatedTimestamp(XMLGregorianCalendar updatedTimestamp) {
		this.updatedTimestamp = updatedTimestamp;
	}
	
	public boolean equals(ServiceEndpoint other){
	    if(other == null){
	        return false;
	    } 
	    else{
	        if((additionalProperties != null && other.additionalProperties == null) || 
	        	(additionalProperties == null && other.additionalProperties != null) ||
	        		!additionalProperties.equals(other.additionalProperties)) {
	        	return false;
	        } else if ((clientSupportedVersions != null && other.clientSupportedVersions == null) ||
	        		(clientSupportedVersions == null && other.clientSupportedVersions != null) ||
	        		!clientSupportedVersions.equals(other.clientSupportedVersions == null)) {
	        	return false;
	        } else if ((containerHost != null && other.containerHost == null) ||
	        		(containerHost == null && other.containerHost != null) ||
	        		!containerHost.equals(other.containerHost == null)) {
	        	return false;
	        } else if ((containerRouteOffer != null && other.containerRouteOffer == null) ||
	        		(containerRouteOffer == null && other.containerRouteOffer != null) ||
	        		!containerRouteOffer.equals(other.containerRouteOffer == null)) {
	        	return false;
	        } else if ((containerVersion != null && other.containerVersion == null) ||
	        		(containerVersion == null && other.containerVersion != null) ||
	        		!containerVersion.equals(other.containerVersion == null)) {
	        	return false;
	        } else if ((containerVersionDefinitionName != null && other.containerVersionDefinitionName == null) ||
	        		(containerVersionDefinitionName == null && other.containerVersionDefinitionName != null) ||
	        		!containerVersionDefinitionName.equals(other.containerVersionDefinitionName == null)) {
	        	return false;
	        } else if ((contextPath != null && other.contextPath == null) ||
	        		(contextPath == null && other.contextPath != null) ||
	        		!contextPath.equals(other.contextPath == null)) {
	        	return false;
	        } else if ((createdBy != null && other.createdBy == null) ||
	        		(createdBy == null && other.createdBy != null) ||
	        		!createdBy.equals(other.createdBy == null)) {
	        	return false;
	        } else if ((createdTimestamp != null && other.createdTimestamp == null) ||
	        		(createdTimestamp == null && other.createdTimestamp != null) ||
	        		!createdTimestamp.equals(other.createdTimestamp == null)) {
	        	return false;
	        } else if ((dmeJDBCDatabaseName != null && other.dmeJDBCDatabaseName == null) ||
	        		(dmeJDBCDatabaseName == null && other.dmeJDBCDatabaseName != null) ||
	        		!dmeJDBCDatabaseName.equals(other.dmeJDBCDatabaseName == null)) {
	        	return false;
	        } else if ((dmeJDBCHealthCheckDriver != null && other.dmeJDBCHealthCheckDriver == null) ||
	        		(dmeJDBCHealthCheckDriver == null && other.dmeJDBCHealthCheckDriver != null) ||
	        		!dmeJDBCHealthCheckDriver.equals(other.dmeJDBCHealthCheckDriver == null)) {
	        	return false;
	        } else if ((dmeJDBCHealthCheckPassword != null && other.dmeJDBCHealthCheckPassword == null) ||
	        		(dmeJDBCHealthCheckPassword == null && other.dmeJDBCHealthCheckPassword != null) ||
	        		!dmeJDBCHealthCheckPassword.equals(other.dmeJDBCHealthCheckPassword == null)) {
	        	return false;
	        } else if ((dmeJDBCHealthCheckUser != null && other.dmeJDBCHealthCheckUser == null) ||
	        		(dmeJDBCHealthCheckUser == null && other.dmeJDBCHealthCheckUser != null) ||
	        		!dmeJDBCHealthCheckUser.equals(other.dmeJDBCHealthCheckUser == null)) {
	        	return false;
	        } else if ((dmeVersion != null && other.dmeVersion == null) ||
	        		(dmeVersion == null && other.dmeVersion != null) ||
	        		!dmeVersion.equals(other.dmeVersion == null)) {
	        	return false;
	        } else if ((env != null && other.env == null) ||
	        		(env == null && other.env != null) ||
	        		!env.equals(other.env == null)) {
	        	return false;
	        } else if ((longitude != null && other.longitude == null) ||
	        		(longitude == null && other.longitude != null) ||
	        		!longitude.equals(other.longitude == null)) {
	        	return false;
	        } else if ((hostAddress != null && other.hostAddress == null) ||
	        		(hostAddress == null && other.hostAddress != null) ||
	        		!hostAddress.equals(other.hostAddress == null)) {
	        	return false;
	        } else if ((latitude != null && other.latitude == null) ||
	        		(latitude == null && other.latitude != null) ||
	        		!latitude.equals(other.latitude == null)) {
	        	return false;
	        } else if ((name != null && other.name == null) ||
	        		(name == null && other.name != null) ||
	        		!name.equals(other.name == null)) {
	        	return false;
	        } else if ((pid != null && other.pid == null) ||
	        		(pid == null && other.pid != null) ||
	        		!pid.equals(other.pid == null)) {
	        	return false;
	        } else if ((port != null && other.port == null) ||
	        		(port == null && other.port != null) ||
	        		!port.equals(other.port == null)) {
	        	return false;
	        } else if ((protocol != null && other.protocol == null) ||
	        		(protocol == null && other.protocol != null) ||
	        		!protocol.equals(other.protocol == null)) {
	        	return false;
	        } else if ((registrationTime != null && other.registrationTime == null) ||
	        		(registrationTime == null && other.registrationTime != null) ||
	        		!registrationTime.equals(other.registrationTime == null)) {
	        	return false;
	        } else if ((routeOffer != null && other.routeOffer == null) ||
	        		(routeOffer == null && other.routeOffer != null) ||
	        		!routeOffer.equals(other.routeOffer == null)) {
	        	return false;
	        }
	        		
	    } 	

	    return true;
	} 

}
