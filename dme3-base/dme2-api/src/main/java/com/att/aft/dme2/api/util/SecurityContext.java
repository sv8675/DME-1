/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api.util;

import org.apache.commons.lang.StringUtils;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.ErrorContext;

public class SecurityContext {

  private final String username;
  private final String password;
  private boolean isSSL;

  private SecurityContext(String username, String password, boolean isSSL) throws DME2Exception {

    this.username=username;
    this.password=password;
    this.isSSL = isSSL;

    if( StringUtils.isEmpty( username ) || StringUtils.isEmpty(password)){
      ErrorContext error = new ErrorContext();
      error.add("username", username);
      error.add("password", password);

      throw new DME2Exception("AFT-DME2-0917", error);
    }

  }

  public static SecurityContext create(String username, String password, boolean isSSL)throws DME2Exception{
    return new SecurityContext(username, password, isSSL);
  }

  public static SecurityContext create(DME2Configuration config) throws DME2Exception{
    String user = config.getProperty( DME2Constants.DME2_GRM_USER );
    String pass = config.getProperty( DME2Constants.DME2_GRM_PASS );
    return new SecurityContext(user,pass, false);
  }

  public void setSSL(boolean isSSL) {
    this.isSSL = isSSL;
  }

  /**
   * @return the username
   */
  public String getUsername() {
    return username;
  }



  /**
   * @return the password
   */
  public String getPassword() {
    return password;
  }

  /**
   * @return the isSSL
   */
  public boolean isSSL() {
    return isSSL;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (isSSL ? 1231 : 1237);
    result = prime * result + ((password == null) ? 0 : password.hashCode());
    result = prime * result + ((username == null) ? 0 : username.hashCode());
    return result;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof SecurityContext)) {
      return false;
    }
    SecurityContext other = (SecurityContext) obj;
    if (isSSL != other.isSSL) {
      return false;
    }
    if (password == null) {
      if (other.password != null) {
        return false;
      }
    }
    else if (!password.equals(other.password)) {
      return false;
    }
    if (username == null) {
      if (other.username != null) {
        return false;
      }
    }
    else if (!username.equals(other.username)) {
      return false;
    }
    return true;
  }


  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "SecurityContext [username=" + username + ", password=" + password + ", isSSL=" + isSSL + "]";
  }









}

