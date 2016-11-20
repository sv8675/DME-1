/*
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 * Developed and maintained by the Common Services System Architecture (CSSA) Group
 */
package com.att.aft.dme2.server.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.DME2Constants;

/**
 * The Class ServerControllerLauncher.
 */
public class ServerControllerLauncher {
  private static final Logger logger = LoggerFactory.getLogger(ServerControllerLauncher.class);

  /**
   * The Class Launcher.
   */
  class Launcher implements Runnable {

    /**
     * The launch proc.
     */
    int launchProc = -1;

    /**
     * Instantiates a new launcher.
     *
     * @param launchProc the launch proc
     */
    Launcher( int launchProc ) {
      this.launchProc = launchProc;
    }


    /*
     * (non-Javadoc)
     *
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
      // String binDir = props.getProperty("bin");
      String binDir = System.getProperty( "java.io.tmpdir" );
      String launcherScript = null;
      String className = null;

      String ext = null;

      if ( windows ) {
        ext = ".bat";
      } else {
        ext = ".ksh";
      }

      switch ( launchProc ) {
        case ServerControllerLauncher.LAUNCH_DME2:
          launcherScript = binDir + "/test-" + System.currentTimeMillis() + ext;
          className = "com.att.aft.dme2.test.server.DME2ServerController";
          break;
        case ServerControllerLauncher.LAUNCH_JETTY:
          launcherScript = binDir + "/testwebserver-" + System.currentTimeMillis() + ext;
          className = "com.att.aft.dme2.test.server.JettyServer";
          break;
        case ServerControllerLauncher.LAUNCH_SSL:
          launcherScript = binDir + "/test-"  + System.currentTimeMillis() + ext;
          className = "com.att.aft.dme2.test.server.SSLServer";
          break;
        case ServerControllerLauncher.LAUNCH_SSL_1:
          launcherScript = binDir + "/test-"  + System.currentTimeMillis() + ext;
          className = "com.att.aft.dme2.test.server.SSLServer1";
          break;
        case ServerControllerLauncher.LAUNCH_SSL_2:
          launcherScript = binDir + "/test-" + System.currentTimeMillis() + ext;
          className = "com.att.aft.dme2.test.server.SSLServerExcludeProtocol";
          break;
      }

      System.out.println( "***1********************************************" );
      launchProcess( className, launcherScript );
    }
  }

  /**
   * The Constant LAUNCH_DME2.
   */
  private static final int LAUNCH_DME2 = 1;

  /**
   * The Constant LAUNCH_SSL.
   */
  private static final int LAUNCH_SSL = 3;

  /**
   * The Constant LAUNCH_SSL.
   */
  private static final int LAUNCH_SSL_1 = 4;

  /**
   * The Constant LAUNCH_JETTY.
   */
  private static final int LAUNCH_JETTY = 2;

  /**
   * The Constant LAUNCH_SSL_2 for exclude protocol.
   */
  private static final int LAUNCH_SSL_2 = 5;

  /**
   * The Constant thisOS.
   */
  private static final String thisOS = System.getProperty( "os.name" );

  /**
   * The main method.
   *
   * @param args the arguments
   */
  public static void main( String[] args ) {
    new ServerControllerLauncher( args ).launch();

  }

  /**
   * The base props file.
   */
  String basePropsFile = "src/main/resources/dme-api_defaultConfigs.properties";

  /**
   * The err reader.
   */
  ProcessStreamReader errReader = null;

  /**
   * The in reader.
   */
  ProcessStreamReader inReader = null;

  /**
   * The kill file.
   */
  String killFile = null;

  /**
   * The process.
   */
  Process process;

  /**
   * The props.
   */
  Properties props = PropsLoader.getProperties( basePropsFile );

  /**
   * The script args.
   */
  String[] scriptArgs;

  /**
   * jvm args
   */
  String jvmArgs;

  /**
   * The windows.
   */
  boolean windows = false;

  /**
   * Instantiates a new server controller launcher.
   *
   * @param args the args
   */
  public ServerControllerLauncher( String... args ) {
    this.scriptArgs = args;
    if ( thisOS != null && thisOS.startsWith( "Windows" ) ) {
      windows = true;
    } else {
      windows = false;
    }
    StringBuffer jvmArgsList = new StringBuffer();
    for ( int i = 0; i < args.length; i++ ) {
      if ( args[i].startsWith( "-D" ) ) {
        jvmArgsList.append( args[i] + " " );
      }
    }
	jvmArgsList.append("-D" + DME2Constants.DME2_GRM_USER + "= ");
	jvmArgsList.append("-D" + DME2Constants.DME2_GRM_PASS + "= ");
    //uncomment if you want to run teh java process in debug mode
  //jvmArgsList.append(" -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005 ");
    jvmArgs = jvmArgsList.toString();
  }

  /**
   * Destroy.
   */
  public void destroy() {
    logger.debug( null, "destroy", LogMessage.METHOD_ENTER );
    File f = new File( killFile );
    try {
      f.createNewFile();
      //f.deleteOnExit();
      System.out.println( "Created kill file " + killFile );
    } catch ( IOException e ) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    try {
		Thread.sleep(10000);
	} catch (InterruptedException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
    if ( process != null ) {
      process.destroy();
    }

    logger.debug( null, "destroy", LogMessage.METHOD_EXIT );
  }

  public void launchSSLServer() {
    String tmpDir = System.getProperty( "java.io.tmpdir" );
    String currMill = String.valueOf( System.currentTimeMillis() );
    File f = new File( tmpDir, currMill );
    killFile = f.getAbsolutePath();
    Thread t = new Thread( new Launcher( ServerControllerLauncher.LAUNCH_SSL ) );
    t.start();
  }

  public void launchSSLServer1() {
    String tmpDir = System.getProperty( "java.io.tmpdir" );
    String currMill = String.valueOf( System.currentTimeMillis() );
    File f = new File( tmpDir, currMill );
    killFile = f.getAbsolutePath();
    Thread t = new Thread( new Launcher( ServerControllerLauncher.LAUNCH_SSL_1 ) );
    t.start();
  }

  public void launchSSLServerExcludeProtocol() {
    String tmpDir = System.getProperty( "java.io.tmpdir" );
    String currMill = String.valueOf( System.currentTimeMillis() );
    File f = new File( tmpDir, currMill );
    killFile = f.getAbsolutePath();
    Thread t = new Thread( new Launcher( ServerControllerLauncher.LAUNCH_SSL_2 ) );
    t.start();
  }

  /**
   * Gets the err stream contents.
   *
   * @return the err stream contents
   */
  public String getErrStreamContents() {
    return errReader.getStreamContents();
  }

  /**
   * Gets the in stream contents.
   *
   * @return the in stream contents
   */
  public String getInStreamContents() {
    return inReader.getStreamContents();
  }

  /**
   * private void launchProcess(String launcherScript) { StringBuffer sb = new StringBuffer(); //sb.append("cmd /c ");
   * sb.append(launcherScript); sb.append(" ");
   * <p/>
   * for(int i=0; i<scriptArgs.length; i++) { sb.append(scriptArgs[i]); sb.append(" "); } sb.append("-killfile ");
   * sb.append(killFile); //System.out.println(sb.toString()); try { process = Runtime.getRuntime().exec(sb.toString());
   * inReader = new ProcessStreamReader("INPUT", process.getInputStream()); errReader = new ProcessStreamReader("ERROR",
   * process.getErrorStream()); inReader.startProcessing(); errReader.startProcessing(); int rc = process.waitFor();
   * //System.out.println("RC=" + rc); //System.out.println(inReader.getStreamContents());
   * //System.err.println(errReader.getStreamContents()); //System.out.println("counter=" + counter); } catch (Exception
   * e) { // TODO Auto-generated catch block e.printStackTrace(); }
   * <p/>
   * }
   *
   * @param className the class name
   * @return the script file contents
   */
  private String getScriptFileContents( String className ) {
    String userDir = System.getProperty( "user.dir" );
    String pathSep = System.getProperty( "path.separator" );
    String fileSep = System.getProperty( "file.separator" );
    String wsDir = "workspace/aft-dme2-api-2.0/1.0";
    String etcDir = userDir + fileSep + "src" + fileSep + "test" + fileSep + "etc" + fileSep;

    String unixUserDirAppend =
        userDir + pathSep + userDir + pathSep + userDir + fileSep + wsDir + pathSep + etcDir + pathSep;
    String classPath = System.getProperty( "java.class.path" );
    if ( classPath != null ) {
      classPath = classPath.replaceAll( "\\\\", "/" );
      StringBuffer cpBuf = new StringBuffer();
      String[] toks = null;
      toks = classPath.split( pathSep );
      cpBuf.append( "\"" );
      for ( String tok : toks ) {
        cpBuf.append( tok );
        cpBuf.append( pathSep );
      }
      cpBuf.append( unixUserDirAppend );
      cpBuf.append( "\"" );
      classPath = cpBuf.toString();
    } else {
      StringBuffer cpBuf = new StringBuffer();
      cpBuf.append( "\"" );
      cpBuf.append( unixUserDirAppend );
      cpBuf.append( "\"" );
      classPath = cpBuf.toString();
    }

    StringBuffer sb = new StringBuffer();

    /**if (windows) {
     sb.append("@echo off");
     sb.append("\n");
     sb.append("setlocal ENABLEDELAYEDEXPANSION");
     sb.append("\n");
     } else {*/
    if ( !windows ) {
      sb.append( "#!/bin/ksh" );
      sb.append( "\n" );
    }
    String javaHome = System.getProperty( "java.home",
        "C:/Program Files/Java/jdk1.6.0_16/" );
    String javaExe = null;
    if ( windows ) {
      javaExe = "\"" + javaHome + "/bin/java\"";
    } else {
      javaExe = javaHome + "/bin/java";
    }
    sb.append( javaExe );
    sb.append( " -cp " );
    sb.append( classPath );
    sb.append( " " );
    sb.append( jvmArgs );
    sb.append( " " );
    sb.append( className );
    sb.append( " " );

    for ( String scriptArg : scriptArgs ) {
      sb.append( scriptArg );
      sb.append( " " );
    }
    sb.append( "-killfile " );
    if ( windows ) {
      sb.append( killFile.replaceAll( "\\\\", "/" ) );
    } else {
      sb.append( killFile );
    }

    return sb.toString();
  }

  /**
   * Launch.
   */
  public void launch() {
    String tmpDir = System.getProperty( "java.io.tmpdir" );
    String currMill = String.valueOf( System.currentTimeMillis() );
    File f = new File( tmpDir, currMill );
    killFile = f.getAbsolutePath();
    // System.out.println("Kill file = " + killFile);
    Thread t = new Thread(
        new Launcher( ServerControllerLauncher.LAUNCH_DME2 ) );
    // t.setDaemon(true);
    t.start();
    try {
      Thread.sleep( 1000 );
    } catch ( Exception ex ) {
    }
    // System.out.println("completed...");
  }

  /**
   * Launch process.
   *
   * @param className      the class name
   * @param launcherScript the launcher script
   */
  private void launchProcess( String className, String launcherScript ) {
    System.out.println( "*2**1********************************************" );
    System.out.println( "launcherScript=" + launcherScript );
    File f = new File( launcherScript );
    f.deleteOnExit();
    String fileContents = this.getScriptFileContents( className );
    System.out.println( "comamnd line: " + fileContents );
    try {
      BufferedWriter writer = new BufferedWriter( new FileWriter( f ) );
      writer.write( fileContents );
      writer.flush();
      writer.close();
      System.out.println( "Creating process" );
      if ( windows ) {
    	//process = Runtime.getRuntime().exec( "cmd /c start " + f.getAbsolutePath() );
		  process = Runtime.getRuntime().exec(f.getAbsolutePath());

      } else {
        process = Runtime.getRuntime().exec( "/bin/ksh -x " + f.getAbsolutePath() );
      }
      inReader = new ProcessStreamReader( "INPUT", process.getInputStream() );
      errReader = new ProcessStreamReader( "ERROR", process.getErrorStream() );
      inReader.startProcessing();
      errReader.startProcessing();
      int rc = -1;
      while ( true ) {
        try {
          Thread.sleep( 1000 );
          rc = process.exitValue();
        } catch ( InterruptedException ie ) {
          if ( Thread.interrupted() ) {
            break;
          } else {
            continue;
          }
        } catch ( IllegalThreadStateException iltse ) {
          continue;
        }
        break;
      }
      //int rc = process.waitFor();
      logger.info( null, "launchProcess", "RC={}" , rc );
      System.out.println( inReader.getStreamContents() );
      System.err.println( errReader.getStreamContents() );
    } catch ( Exception e ) {
      // TODO Auto-generated catch block

      e.printStackTrace();
    } finally {
      if ( process != null ) {
        System.out.println( "Destroying process" );
        process.destroy();
        System.out.println( "Process isAlive=" + process.isAlive() );
      }
    }

  }

  /**
   * Launch web server.
   */
  public void launchWebServer() {
    String tmpDir = System.getProperty( "java.io.tmpdir" );
    String currMill = String.valueOf( System.currentTimeMillis() );
    File f = new File( tmpDir, currMill );
    killFile = f.getAbsolutePath();
    // System.out.println("Kill file = " + killFile);
    Thread t = new Thread( new Launcher(
        ServerControllerLauncher.LAUNCH_JETTY ) );
    t.setDaemon( true );
    t.start();
    try {
      Thread.sleep( 1000 );
    } catch ( Exception ex ) {
    }

  }

  /**
   * class Launcher implements Runnable { int launchProc = -1; Launcher(int
   * launchProc) { this.launchProc = launchProc; } public void run() { String
   * binDir = props.getProperty("bin"); String launcherScript = null; String
   * ext = null; if(System.getProperty("os.name").startsWith("Windows")) { ext
   * = ".bat"; } else { ext = ".ksh"; } switch(launchProc) { case
   * ServerControllerLauncher.LAUNCH_DME2: launcherScript = binDir +
   * "/launcher" + ext; break; case ServerControllerLauncher.LAUNCH_JETTY:
   * launcherScript = binDir + "/launchwebserver"+ext; break; }
   *
   * launchProcess(launcherScript); } }
   **/
}