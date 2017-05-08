/*
 * Copyright (c) 2005
 *	The President and Fellows of Harvard College.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE UNIVERSITY AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE UNIVERSITY OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
package edu.harvard.econcs.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Simple log for class.
 *
 * @author Benjamin Lubin; Last modified by $Author: blubin $
 * @version $Revision: 1.7 $ on $Date: 2010/10/28 00:39:55 $
 * @since Mar 20, 2004
 **/
public class Log {
	
	public static Level ERROR = new ErrorLevel();
	//                  WARNING
	public static Level MAIN = new MainLevel();
	//                  INFO
	public static Level TRACE = new TraceLevel();
	public static Level DEBUG = new DebugLevel();
	public static Level TRIVIAL = new TrivialLevel();
	
	static {
		InputStream is = Log.class.getResourceAsStream("log.config");
		setupLogging(is);
		String file = System.getProperty("java.util.logging.config.file", "config" + File.pathSeparator + "log.config");		
		File config = new File(file);
		if (config.exists()) {
			System.out.println("Reading log config file: "+file);
			try {
				setupLogging(new FileInputStream(config));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	private static void setupLogging(InputStream is) {
		if (is == null) {
			System.err.println("Can't read log config stream");
			return;
		}
		Properties p = new Properties();
		try {
			p.load(is);
			Properties levelProps = new Properties();
			for (Iterator iter = p.keySet().iterator(); iter.hasNext(); ) {
				String key = (String)iter.next();
				String value = p.getProperty(key);
				if (key.endsWith(".level") &&
				    !key.equals(".level") &&
				    !key.startsWith("java.util.logging")) {
					iter.remove();
					levelProps.put(key, value);
				}
			}
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			p.store(os, "");
			os.close();
			is = new ByteArrayInputStream(os.toByteArray());
			LogManager.getLogManager().readConfiguration(is);
			
			for (Iterator iter = levelProps.keySet().iterator(); iter.hasNext(); ) {
				String key = (String)iter.next();
				String value = levelProps.getProperty(key);
				if (key.endsWith(".level")) {
					key = key.substring(0, key.length()-6);
					Logger.getLogger(key).setLevel(parse(value));
				}
			}
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}				
	}
		
	/** The java 1.4 logger backing this guy... **/
	private Logger logger;
	
	/**
	 * Create a log by handing it the class you are running in.
	 * Typically you want to create a static member variable to hold
	 * your logger.  See TypedProperties in this package for an example.
	 */
	public Log(Class myClass) {
		logger = Logger.getLogger(myClass.getName());
	}
	
	public boolean isErrorEnabled() {
		return logger.isLoggable(ERROR);
	}	
	
	/** wildly unexpected error condition. **/
	public void error(String msg) {
		logger.log(ERROR, msg);
	}
	
	/** wildly unexpected error condition. **/
	public void error(String msg, Throwable t) {
		logger.log(ERROR, msg, t);
	}

	public boolean isErrorWarn() {
		return logger.isLoggable(Level.WARNING);
	}	

	/** recoverable condition. **/
	public void warn(String msg) {
		logger.log(Level.WARNING, msg);
	}
	
	/** recoverable condition. **/
	public void warn(String msg, Throwable t) {
		logger.log(Level.WARNING, msg, t);
	}

	public boolean isMainEnabled() {
		return logger.isLoggable(MAIN);
	}	
	
	/** Highest level message. **/
	public void main(String msg) {
		logger.log(MAIN, msg);
	}
	
	/** Highest level message. **/
	public void main(String msg, Throwable t) {
		logger.log(MAIN, msg, t);
	}

	public boolean isInfoEnabled() {
		return logger.isLoggable(Level.INFO);
	}	

	/** lower level message.**/
	public void info(String msg) {
		logger.log(Level.INFO, msg);
	}
	
	/** lower level message.**/
	public void info(String msg, Throwable t) {
		logger.log(Level.INFO, msg, t);
	}

	public boolean isTraceEnabled() {
		return logger.isLoggable(TRACE);
	}	
	
	/** messages at about the level of high order function calls. **/
	public void trace(String msg) {
		logger.log(TRACE, msg);
	}
	
	/** messages at about the level of high order function calls. **/
	public void trace(String msg, Throwable t) {
		logger.log(TRACE, msg, t);
	}
	
	public boolean isDebugEnabled() {
		return logger.isLoggable(DEBUG);
	}	
	
	/** low level debugging. **/
	public void debug(String msg) {
		logger.log(DEBUG, msg);
	}
	
	/** low level debugging. **/
	public void debug(String msg, Throwable t) {
		logger.log(DEBUG, msg, t);
	}

	public boolean isTrivialEnabled() {
		return logger.isLoggable(TRIVIAL);
	}	
	
	/** very minute messages about tiny events. **/
	public void trivial(String msg) {
		logger.log(TRIVIAL, msg);
	}
	
	/** very minute messages about tiny events. **/
	public void trivial(String msg, Throwable t) {
		logger.log(TRIVIAL, msg, t);
	}
	
	//Nested Classes:
	/////////////////
	
	private static class ErrorLevel extends Level{
		/**
		 * 
		 */
		private static final long serialVersionUID = 3256444720031871028L;

		public ErrorLevel() {
			super("ERROR", Level.SEVERE.intValue());
		}
	}

	private static class MainLevel extends Level{
		/**
		 * 
		 */
		private static final long serialVersionUID = 3257849883023913010L;

		public MainLevel() {
			super("MAIN", Level.INFO.intValue() + 1);
		}
	}
	
	private static class TraceLevel extends Level{
		/**
		 * 
		 */
		private static final long serialVersionUID = 3544390296986203958L;

		public TraceLevel() {
			super("TRACE", Level.CONFIG.intValue());
		}
	}
	
	private static class DebugLevel extends Level{
		/**
		 * 
		 */
		private static final long serialVersionUID = 3832905463745362230L;

		public DebugLevel() {
			super("DEBUG", Level.FINER.intValue());
		}
	}
	
	private static class TrivialLevel extends Level{
		/**
		 * 
		 */
		private static final long serialVersionUID = 3618134546222953779L;

		public TrivialLevel() {
			super("TRIVIAL", Level.FINEST.intValue());
		}
	}
	
	private static Level parse(String lev) {
		if (lev.equals("ERROR")) {
			return ERROR;
		}
		if (lev.equals("WARN")) {
			return Level.WARNING;
		}
		if (lev.equals("MAIN")) {
			return MAIN;
		}
		//INFO...
		if (lev.equals("TRACE")) {
			return TRACE;
		}
		if (lev.equals("DEBUG")) {
			return DEBUG;
		}
		if (lev.equals("TRIVIAL")) {
			return TRIVIAL;
		}
		return Level.parse(lev);
	}
}

