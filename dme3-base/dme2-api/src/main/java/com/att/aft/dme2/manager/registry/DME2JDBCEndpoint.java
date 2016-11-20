package com.att.aft.dme2.manager.registry;

public class DME2JDBCEndpoint extends DME2Endpoint {
  private String databaseName;
  private String healthCheckUser;
  private String healthCheckPassword;
  private String healthCheckDriver;

  public DME2JDBCEndpoint( double distanceToClient ) {
    super( distanceToClient );
  }

  public DME2JDBCEndpoint( String servicePath, double distanceToClient ) {
    super( servicePath, distanceToClient );
  }

  public void setDatabaseName( String databaseName ) {
    this.databaseName = databaseName;
  }

  public String getDatabaseName() {
    return databaseName;
  }

  public void setHealthCheckUser( String healthCheckUser ) {
    this.healthCheckUser = healthCheckUser;
  }

  public String getHealthCheckUser() {
    return healthCheckUser;
  }

  public void setHealthCheckPassword( String healthCheckPassword ) {
    this.healthCheckPassword = healthCheckPassword;
  }

  public String getHealthCheckPassword() {
    return healthCheckPassword;
  }

  public void setHealthCheckDriver( String healthCheckDriver ) {
    this.healthCheckDriver = healthCheckDriver;
  }

  public String getHealthCheckDriver() {
    return healthCheckDriver;
  }
}
