package com.att.aft.dme2.iterator.dme2;

import com.att.aft.dme2.manager.registry.DME2Endpoint;

public class DME2JdbcEndpoint extends DME2Endpoint {
	/** The database name */
	private String databaseName;

	/** The user name required to perform health check operations on the endpoint */
	private String healthCheckUser;

	/** The password required to perform health check operations on the endpoint */
	private String healthCheckPassword;
	
	/** The JDBC driver required to perform health check operations on the endpoint */
	private String healthCheckDriver;

  public DME2JdbcEndpoint( double distanceToClient ) {
    super( distanceToClient );
  }

  public DME2JdbcEndpoint( String servicePath, double distanceToClient ) {
    super( servicePath, distanceToClient );
  }

  public String getDatabaseName()
	{
		return databaseName;
	}


	public void setDatabaseName(String databaseName)
	{
		this.databaseName = databaseName;
	}


	public String getHealthCheckUser()
	{
		return healthCheckUser;
	}


	public void setHealthCheckUser(String healthCheckUser)
	{
		this.healthCheckUser = healthCheckUser;
	}


	public String getHealthCheckPassword()
	{
		return healthCheckPassword;
	}


	public void setHealthCheckPassword(String healthCheckPassword)
	{
		this.healthCheckPassword = healthCheckPassword;
	}


	public String getHealthCheckDriver()
	{
		return healthCheckDriver;
	}


	public void setHealthCheckDriver(String healthCheckDriver)
	{
		this.healthCheckDriver = healthCheckDriver;
	}
	
}
