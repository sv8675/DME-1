package com.att.aft.dme2.util;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.manager.registry.DME2EndpointRegistry;
import com.att.aft.dme2.request.DmeUniformResource;
import com.att.scld.grm.types.v1.ClientJVMInstance;

public class DME2GRMJVMRegistration {

  private volatile static DME2GRMJVMRegistration INSTANCE;
  private static final Logger logger = LoggerFactory.getLogger( DME2GRMJVMRegistration.class.getName() );

  public static void getInstance( DME2Manager manager, DmeUniformResource uniformResource ) {
    getOrCreateInstance( manager, uniformResource );
  }

  private static DME2GRMJVMRegistration getOrCreateInstance( DME2Manager manager, DmeUniformResource uniformResource ) {
    // https://en.wikipedia.org/wiki/Double-checked_locking#Usage_in_Java
    DME2GRMJVMRegistration result = INSTANCE;
    if ( result == null ) {
      synchronized ( DME2GRMJVMRegistration.class ) {
        result = INSTANCE;
        if ( INSTANCE == null ) {
          INSTANCE = result = new DME2GRMJVMRegistration( manager, uniformResource );
        }
      }
    }
    return result;
  }

  public DME2GRMJVMRegistration( DME2Manager manager, DmeUniformResource uniformResource ) {

    logger.debug( null, "ctor", LogMessage.METHOD_ENTER );
    try {

      if ( manager.getConfig().getBoolean( "AFT_DME2_REGISTER_JVM_INSTANCE_ON_GRM" ) ) {
        // Register this JVM on GRM
        DME2EndpointRegistry registry = manager.getEndpointRegistry();
        String user = manager.getConfig().getProperty( DME2Constants.DME2_GRM_USER );
        //12345@GACDTRW.....
        String pid = ManagementFactory.getRuntimeMXBean().getName();
        String pidToRegister = pid.contains( "@" ) ? pid.substring( 0, pid.indexOf( "@" ) ) : pid;

        GregorianCalendar gcal = new GregorianCalendar();
        gcal.add( Calendar.DAY_OF_MONTH, 1 );
        XMLGregorianCalendar expTime = DatatypeFactory.newInstance().newXMLGregorianCalendar( gcal );

        ClientJVMInstance instanceInfo = new ClientJVMInstance();
        instanceInfo.setApplicationId( DME2Utils.getRunningInstanceName( manager.getConfig() ) );
        instanceInfo.setDme2Environment( uniformResource.getEnvContext() );
        instanceInfo.setDme2Version( DME2Manager.getVersion() );
        instanceInfo.setHostAddress(
            manager.getHostname() == null ? InetAddress.getLocalHost().getCanonicalHostName() : manager.getHostname() );
        instanceInfo.setJavaVersion( Runtime.class.getPackage().getImplementationVersion() );
        instanceInfo.setMechId( user );
        instanceInfo.setProcessId( pidToRegister );
        instanceInfo.setProcessOwner( System.getProperty( "user.name" ) );

        String envContext = uniformResource.getEnvContext();

        scheduleJVMRegister( envContext, instanceInfo, manager );

        if ( manager.getConfig().getBoolean( "AFT_DME2_DEREGISTER_JVM_INSTANCE_ON_GRM" ) ) {
          // Add shutdown hook to deregister JVM
          DME2DeregisterJVMThread shutdown = DME2DeregisterJVMThread.getInstance( registry, envContext, instanceInfo );
          Runtime.getRuntime().addShutdownHook( shutdown );
        }

        if ( manager.getConfig().getBoolean( "AFT_DME2_REFRESH_JVM_INSTANCE_ON_GRM" ) ) {
          // Schedule JVM Renewal Timer
          scheduleJVMRenewLease( envContext, instanceInfo, manager );
        }

      }

    } catch ( Exception e ) {
      logger.warn( null, "ctor(DME2Manager,DmeUniformResource)", LogMessage.ERROR_REGISTERING_JVM, e );
    }
    logger.debug( null, "ctor", LogMessage.METHOD_EXIT );
  }

  private void scheduleJVMRegister( String envContext, ClientJVMInstance instanceInfo, DME2Manager manager ) {
    logger.debug( null, "scheduleJVMRegister", LogMessage.METHOD_ENTER );
    Timer registerJVMLeaseTimer = null;
    final Date date = new Date();
    /* Initiate timer for register of JVM */
    final String env = envContext;
    final ClientJVMInstance instance = instanceInfo;
    final DME2Manager mgr = manager;
    final DME2EndpointRegistry managerRegistry = mgr.getEndpointRegistry();
    registerJVMLeaseTimer = new Timer( "DME2::DME2Server::jvmRegisterTimer", true );
    registerJVMLeaseTimer.schedule( new TimerTask() {
      int attempt = 1;

      @Override
      public void run() {
        while ( attempt <= mgr.getConfig().getInt( "DME2_JVM_LEASE_REGISTER_RETRY_ATTEMPT" ) ) {
          try {
            register();
            break;
          } catch ( Exception e ) {
            logger.warn( null, "scheduleJVMRegister", LogMessage.ERROR_REGISTERING_JVM, e.getMessage(), attempt,
                mgr.getConfig().getInt( "DME2_JVM_LEASE_REGISTER_RETRY_ATTEMPT" ), e );
            attempt++;
          }
        }
      }

      private void register() throws Exception {
        managerRegistry.registerJVM( env, instance );
      }

    }, date );
    logger.debug( null, "scheduleJVMRegister", LogMessage.METHOD_EXIT );
  }

  private void scheduleJVMRenewLease( String envContext, ClientJVMInstance instanceInfo, DME2Manager manager ) {

    Integer jvmLeaseRenewFrequency = null;
    Timer renewJVMLeaseTimer = null;
    DME2EndpointRegistry managerRegistry = manager.getEndpointRegistry();

		/* Initiate timer for extending lease of JVM */
    jvmLeaseRenewFrequency = manager.getConfig().getInt( "DME2_JVM_LEASE_RENEW_FREQUENCY_MS" );
    final String env = envContext;
    final ClientJVMInstance instance = instanceInfo;
    final DME2EndpointRegistry registry = managerRegistry;
    renewJVMLeaseTimer = new Timer( "DME2::DME2Server::jvmRenewTimer", true );
    renewJVMLeaseTimer.scheduleAtFixedRate( new TimerTask() {

      @Override
      public void run() {
        try {
          ClientJVMInstance instanceAfter = instance;
          registry.updateJVM( env, instanceAfter );
        } catch ( Throwable e ) {
          logger.warn( null, "scheduleJVMRenewLease", LogMessage.ERROR_RENEWING_ALL, e );
        }
      }

    }, jvmLeaseRenewFrequency, jvmLeaseRenewFrequency );

  }

  static class DME2DeregisterJVMThread extends Thread {
    private static DME2DeregisterJVMThread instance = null;
    private static final Logger logger = LoggerFactory.getLogger( DME2DeregisterJVMThread.class.getName() );
    private DME2EndpointRegistry registry = null;
    private String envContext = null;
    private ClientJVMInstance instanceInfo = null;

    public static DME2DeregisterJVMThread getInstance( DME2EndpointRegistry registry, String envContext,
                                                       ClientJVMInstance instanceInfo ) {
      if ( instance == null ) {
        return new DME2DeregisterJVMThread( registry, envContext, instanceInfo );
      }
      return instance;
    }

    private DME2DeregisterJVMThread( DME2EndpointRegistry registry, String envContext,
                                     ClientJVMInstance instanceInfo ) {
      this.registry = registry;
      this.envContext = envContext;
      this.instanceInfo = instanceInfo;
    }

    @Override
    public void run() {

      try {
        registry.deregisterJVM( envContext, instanceInfo );
      } catch ( Exception e ) {
        logger.warn( null, "DME2DeregisterJVMThread.run", LogMessage.ERROR_DEREGISTERING_JVM, e );
      }
    }

  }
}
