/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test.jms.util;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.att.aft.dme2.test.jms.TestJMSHeaderReplyJMSServer;
import com.att.aft.dme2.util.DME2Constants;

public class ServerLauncher {
  private String[] scriptArgs = null;
  private Process process;
  private boolean isRunning = false;
  private ProcessStreamReader inReader = null;
  private ProcessStreamReader errReader = null;
  private static final String thisOS = System.getProperty( "os.name" );
  boolean windows = false;
  private String killFile = null;
  private String jvmArgs;
  // List of libraries to include, since Windows is starting to hit the classpath limit on some machines. Ignored elsewhere
  private String[] includeLibs;

  public ServerLauncher( String[] includeLibs, String... args ) {
    this.includeLibs = includeLibs;
    this.scriptArgs = args;
    if ( thisOS != null && thisOS.startsWith( "Windows" ) ) {
      windows = true;
    } else {
      windows = false;
    }

    //System.setProperty("java.io.tmpdir", ""

  }

  public void launchTestJMSServer()
      throws InterruptedException {
    String tmpDir = System.getProperty( "java.io.tmpdir" );
    String currMill = String.valueOf( System.currentTimeMillis() );
    File f = new File( tmpDir, currMill );
    killFile = f.getAbsolutePath();
    Thread t = new Thread( new Launcher( "com.att.aft.dme2.test.jms.server.TestJMSServer" ) );
    t.start();
    while ( !isRunning ) {
      Thread.sleep( 500 );
    }

    Thread.sleep( 30000 );
  }

  public void launchTestGRMJMSServer() {
    String tmpDir = System.getProperty( "java.io.tmpdir" );
    String currMill = String.valueOf( System.currentTimeMillis() );
    File f = new File( tmpDir, currMill );
    killFile = f.getAbsolutePath();
    Thread t = new Thread( new Launcher( "com.att.aft.dme2.test.jms.server.TestGRMRegJMSServer" ) );
    t.start();
    while ( !isRunning ) {
      try {
        Thread.sleep( 500 );
      } catch ( InterruptedException e ) {
      }
    }
    try {
      Thread.sleep( 30000 );
    } catch ( Exception e ) {
    }
  }

  public void launchRemoteMsgSelectorJMSServer() {
    String tmpDir = System.getProperty( "java.io.tmpdir" );
    String currMill = String.valueOf( System.currentTimeMillis() );
    File f = new File( tmpDir, currMill );
    killFile = f.getAbsolutePath();
    Thread t = new Thread( new Launcher( "com.att.aft.dme2.test.jms.server.TestRemoteQueueMessageSelectorServer" ) );
    t.start();
    while ( !isRunning ) {
      try {
        Thread.sleep( 500 );
      } catch ( InterruptedException e ) {
      }
    }
    try {
      Thread.sleep( 30000 );
    } catch ( Exception e ) {
    }
  }

  public void launchTestMetricsJMSServer() {
    String tmpDir = System.getProperty( "java.io.tmpdir" );
    String currMill = String.valueOf( System.currentTimeMillis() );
    File f = new File( tmpDir, currMill );
    killFile = f.getAbsolutePath();
    Thread t = new Thread( new Launcher( "com.att.aft.dme2.test.jms.server.TestMetricsJMSServer" ) );
    t.start();
    while ( !isRunning ) {
      try {
        Thread.sleep( 500 );
      } catch ( InterruptedException e ) {
      }
    }
    try {
      Thread.sleep( 30000 );
    } catch ( Exception e ) {
    }
  }

  public void launchTestDME2ThrottleFilterJMSServer()
	{
		String tmpDir = System.getProperty("java.io.tmpdir");
		String currMill = String.valueOf(System.currentTimeMillis());
		File f = new File(tmpDir, currMill);
		killFile = f.getAbsolutePath();
		Thread t = new Thread(new Launcher("com.att.aft.dme2.test.jms.TestDME2ThrottleFilterJMSServer"));
		t.start();
		while( ! isRunning)
		{
			try { Thread.sleep(500); }
			catch(InterruptedException e) { }
		}
		try { Thread.sleep(30000); } catch(Exception e) { }
	}

  public void launchTestLrmROJMSServer() {
    String tmpDir = System.getProperty( "java.io.tmpdir" );
    String currMill = String.valueOf( System.currentTimeMillis() );
    File f = new File( tmpDir, currMill );
    killFile = f.getAbsolutePath();
    Thread t = new Thread( new Launcher( "com.att.aft.dme2.test.jms.server.TestLrmROJMSServer" ) );
    t.start();
    while ( !isRunning ) {
      try {
        Thread.sleep( 500 );
      } catch ( InterruptedException e ) {
      }
    }
    try {
      Thread.sleep( 30000 );
    } catch ( Exception e ) {
    }
  }

  public void launchTestJMSAuthServer() {
    String tmpDir = System.getProperty( "java.io.tmpdir" );
    String currMill = String.valueOf( System.currentTimeMillis() );
    File f = new File( tmpDir, currMill );
    killFile = f.getAbsolutePath();
    Thread t = new Thread( new Launcher( "com.att.aft.dme2.test.jms.server.TestJMSAuthenticationServer" ) );
    t.start();
    while ( !isRunning ) {
      try {
        Thread.sleep( 500 );
      } catch ( InterruptedException e ) {
      }
    }
    try {
      Thread.sleep( 30000 );
    } catch ( Exception e ) {
    }
  }

  public void launchLongRunTestJMSServer() {
    String tmpDir = System.getProperty( "java.io.tmpdir" );
    String currMill = String.valueOf( System.currentTimeMillis() );
    File f = new File( tmpDir, currMill );
    killFile = f.getAbsolutePath();
    Thread t = new Thread( new Launcher( "com.att.aft.dme2.test.jms.server.TestLongRunJMSServer" ) );
    t.start();
    while ( !isRunning ) {
      try {
        Thread.sleep( 500 );
      } catch ( InterruptedException e ) {
      }
    }
    try {
      Thread.sleep( 30000 );
    } catch ( Exception e ) {
    }
  }

  public void launchFastFailTestJMSServer() {
    String tmpDir = System.getProperty( "java.io.tmpdir" );
    String currMill = String.valueOf( System.currentTimeMillis() );
    File f = new File( tmpDir, currMill );
    killFile = f.getAbsolutePath();
    Thread t = new Thread( new Launcher( "com.att.aft.dme2.test.jms.server.FastFailJMSServer" ) );
    t.start();
    while ( !isRunning ) {
      try {
        Thread.sleep( 500 );
      } catch ( InterruptedException e ) {
      }
    }
    try {
      Thread.sleep( 30000 );
    } catch ( Exception e ) {
    }
  }

  public void launchFastFailRespondTestJMSServer() {
    String tmpDir = System.getProperty( "java.io.tmpdir" );
    String currMill = String.valueOf( System.currentTimeMillis() );
    File f = new File( tmpDir, currMill );
    killFile = f.getAbsolutePath();
    Thread t = new Thread( new Launcher( "com.att.aft.dme2.test.jms.server.FastFailRespondJMSServer" ) );
    t.start();
    while ( !isRunning ) {
      try {
        Thread.sleep( 500 );
      } catch ( InterruptedException e ) {
      }
    }
    try {
      Thread.sleep( 30000 );
    } catch ( Exception e ) {
    }
  }

  public void launchTestJMSReceiveServer() {
    String tmpDir = System.getProperty( "java.io.tmpdir" );
    String currMill = String.valueOf( System.currentTimeMillis() );
    File f = new File( tmpDir, currMill );
    killFile = f.getAbsolutePath();
    Thread t = new Thread( new Launcher( "com.att.aft.dme2.test.jms.server.TestReceiveJMSServer" ) );
    t.start();
    while ( !isRunning ) {
      try {
        Thread.sleep( 500 );
      } catch ( InterruptedException e ) {
      }
    }
    try {
      Thread.sleep( 30000 );
    } catch ( Exception e ) {
    }
  }

  public void launchEmptyReplyTestJMSServer() {
    String tmpDir = System.getProperty( "java.io.tmpdir" );
    String currMill = String.valueOf( System.currentTimeMillis() );
    File f = new File( tmpDir, currMill );
    killFile = f.getAbsolutePath();
    Thread t = new Thread( new Launcher( "com.att.aft.dme2.test.jms.server.TestEmptyReplyJMSServer" ) );
    t.start();
    while ( !isRunning ) {
      try {
        Thread.sleep( 500 );
      } catch ( InterruptedException e ) {
      }
    }
    try {
      Thread.sleep( 30000 );
    } catch ( Exception e ) {
    }
  }

  public void destroy() {
    try {
      System.out.println( "Calling destroy" );
      if ( process != null ) {
    	  process.destroy();
      }
    } catch ( Throwable t ) {
    	t.printStackTrace();
      System.out.println( "serverLauncher.destroy() caught throwable " + t );
      t.printStackTrace();
    } finally {
      try {
        System.out.println( "Creating kill file " + killFile );
        File f = new File( killFile );
        f.createNewFile();
      } catch ( IOException e ) {
        e.printStackTrace();
      }
    }
  }

  private void launchProcess( String className, String launcherScript ) {
    System.out.println( "launcherScript=" + launcherScript );
    File f = new File( launcherScript );

    String fileContents = this.getScriptFileContents( className );
    System.out.println( "script contents=" + fileContents );
    try {
      BufferedWriter writer = new BufferedWriter( new FileWriter( f ) );
      writer.write( fileContents );
      writer.flush();
      writer.close();
      f.setExecutable( true );
      f.deleteOnExit();

      if ( windows ) {
        process = Runtime.getRuntime().exec( f.getAbsolutePath() );
      } else {
        process = Runtime.getRuntime().exec( "/bin/ksh -x " + f.getAbsolutePath() );
      }

      inReader = new ProcessStreamReader( "INPUT", process.getInputStream() );
      errReader = new ProcessStreamReader( "ERROR", process.getErrorStream() );
      inReader.startProcessing();
      errReader.startProcessing();

      isRunning = true;
      int rc = process.waitFor();
    } catch ( Exception e ) {
      // TODO Auto-generated catch block
      e.printStackTrace();

    } finally {
      process.destroy();
    }

  }

	private String getScriptFileContents(String className)
	{
    jvmArgs = " -D" + DME2Constants.DME2_GRM_USER + "=mxxxxx -D" + DME2Constants.DME2_GRM_PASS + "=mxxxxx ";
  /*  if ( "com.att.aft.dme2.test.jms.server.FastFailJMSServer".equals( className ) ) {
      jvmArgs += "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=1044";
    }*/

    String classPath = System.getProperty("java.class.path");
    Map wildCardPaths = new HashMap<String,Boolean>();
    Pattern jarPattern = Pattern.compile( "[^\\/]+\\.jar");

    // Pre-compile the included libraries
    Pattern[] includeLibPatterns = new Pattern[includeLibs != null ? includeLibs.length : 0];

    if ( windows && includeLibs != null ) {
      for ( int i = 0; i < includeLibs.length; i++ ) {
        Pattern p = Pattern.compile( includeLibs[i] );
        includeLibPatterns[i] = p;
      }
    }
		if(classPath != null)
		{
			classPath = classPath.replaceAll("\\\\", "/");
			StringBuffer cpBuf = new StringBuffer();
			String[] toks = null;
			if(windows)
				toks = classPath.split(";");
			else
				toks = classPath.split(":");
			
			for(String tok:toks)
			{
				cpBuf.append("\"");
				cpBuf.append(tok);
				cpBuf.append("\"");
				if(windows)
					cpBuf.append(";");
				else
					cpBuf.append(":");

        wildCardPaths.put( tok, true );//jarPattern.matcher( tok ).replaceFirst( "*.jar" ), true );
			}
			//classPath=cpBuf.toString();
      StringBuffer cpBuf2 = new StringBuffer(  );
      for ( Object o : wildCardPaths.keySet() ) {
        String cp = (String) o;
        if ( windows && includeLibs != null ) {
          for ( Pattern includeLibPattern : includeLibPatterns ) {
            if ( includeLibPattern.matcher( cp ).find() ) {
              cpBuf2.append( "\"" ).append( cp ).append( "\"" ).append( windows ? ";" : ":" );
            }
          }
        } else {
          cpBuf2.append( "\"" ).append( cp ).append( "\"" ).append( windows ? ";" : ":" );
        }
      }
      classPath = cpBuf2.toString();

		}
		
		StringBuffer sb = new StringBuffer();
		
		if(windows)
		{
			sb.append("@echo off");
			sb.append("\n");
			sb.append("setlocal ENABLEDELAYEDEXPANSION");
			sb.append("\n");
		}
		else
		{
			sb.append("#!/bin/ksh");
			sb.append("\n");
		}
		String javaHome = System.getProperty("java.home",
				System.getProperty("JAVA_HOME", "C:/Program Files/Java/jdk1.6.0_16/"));
		String javaExe = null;
		if(windows)
		{
			javaExe = "\"" + javaHome + "/bin/java\"";
		}
		else
		{
			javaExe = javaHome + "/bin/java";
		}
		sb.append(javaExe);
		//Uncomment to attach debuger
		//sb.append(" -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005 ");
		sb.append(" -cp ");
		sb.append(classPath);
		sb.append(" ");
    sb.append(jvmArgs);
    sb.append(" ");
		sb.append(className);
		sb.append(" ");
		
		for(int i=0; i<scriptArgs.length; i++)
		{
			sb.append(scriptArgs[i]);
			sb.append(" ");
		}
		sb.append("-killfile ");
		sb.append(killFile);

		return sb.toString();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)
	throws InterruptedException
	{
		System.setProperty("platform", TestConstants.SCLD_PLATFORM_FOR_SANDBOX_DEV);
		System.setProperty("AFT_ENVIRONMENT", "AFTUAT");
		System.setProperty("AFT_LATITUDE", "33.4");
		System.setProperty("AFT_LONGITUDE", "90.6");
		if(args.length>0) {
            for (int i = 0; i < args.length; i++) {
				if ("-throttleConfig".equals(args[i])) 
					System.setProperty("AFT_DME2_THROTTLE_FILTER_CONFIG_FILE", args[i+1]);
			}
			if(args[0].equals("-receive")){
				ServerLauncher launcher = new ServerLauncher(null, "-city", "BHAM");
				launcher.launchTestJMSReceiveServer();
			}
			else if (args[0].equals("-port")){
				ServerLauncher launcher = new ServerLauncher(null, "-port", args[1]);
				launcher.launchFastFailTestJMSServer();
			}	
		}
		else {
		ServerLauncher launcher = new ServerLauncher(null, "-city", "BHAM");
		launcher.launchTestJMSServer();
		}
	}

	class Launcher implements Runnable
	{
		String className;
		Launcher(String className)
		{
			this.className = className;
		}
		public void run()
		{
			String binDir = System.getProperty("java.io.tmpdir");
			String launcherScript = null;
		
			String ext = windows?".bat":".ksh";
			launcherScript = binDir + "/test_jms_" + System.currentTimeMillis() + ext;
			launchProcess(this.className, launcherScript);
		}
	}

	public void launchContinuationReplyJMSServer() throws InterruptedException {
		// TODO Auto-generated method stub
		String tmpDir = System.getProperty("java.io.tmpdir");
		String currMill = String.valueOf(System.currentTimeMillis());
		File f = new File(tmpDir, currMill);
		killFile = f.getAbsolutePath();
		Thread t = new Thread(new Launcher("com.att.aft.dme2.test.jms.server.TestContinuationReplyServer"));
		t.start();		
		while( ! isRunning) { Thread.sleep(500); }
		
		Thread.sleep(30000);
	}
	public void launchTestJMSServer1()
	throws InterruptedException
	{
		String tmpDir = System.getProperty("java.io.tmpdir");
		String currMill = String.valueOf(System.currentTimeMillis());
		File f = new File(tmpDir, currMill);
		killFile = f.getAbsolutePath();
		Thread t = new Thread(new Launcher("com.att.aft.dme2.test.jms.server.TestJMSServer"));
		t.start();
		while( ! isRunning) { Thread.sleep(500); }
		
		Thread.sleep(30000);
	}

  public void launchJMSHeaderReplyJMSServer()
	{
		String tmpDir = System.getProperty("java.io.tmpdir");
		String currMill = String.valueOf(System.currentTimeMillis());
		File f = new File(tmpDir, currMill);
		killFile = f.getAbsolutePath();
		Thread t = new Thread(new Launcher(TestJMSHeaderReplyJMSServer.class.getName()));
		t.start();
		while( ! isRunning)
		{
			try { Thread.sleep(500); }
			catch(InterruptedException e) { }
		}
		try { Thread.sleep(30000); } catch(Exception e) { }
	}
}