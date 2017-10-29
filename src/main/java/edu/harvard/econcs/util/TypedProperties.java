/*
 * Copyright (c) 2005-2017 Benjamin Lubin
 * Copyright (c) 2005-2017 The President and Fellows of Harvard College
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * 
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * 
 * - Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.harvard.econcs.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Properties;

/**
 * Special type of properties object that knows how to cast to
 * primitive types.
 * <br>
 * <B>Note: Copied from lubinlib.</B>
 * <br> 
 * @author Benjamin Lubin; Last modified by $Author: blubin $
 * @version $Revision: 1.6 $ on $Date: 2010/10/28 00:11:26 $
 * @since Mar 15, 2004
 **/
public class TypedProperties extends Properties {
	
	private static final long serialVersionUID = 3761403118801139760L;
	private static final Logger logger = LogManager.getLogger(TypedProperties.class);
	
	public String getString(String name, String def){
		return getProperty(name,def);
	}
	
	public int getInt(String name, int def){
		try{
			String val=getProperty(name);
			if(val==null)
				return def;
			return Integer.parseInt(val);
		}catch(NumberFormatException nfe){
			logger.warn("Expected integer in property: "+name);
		}
		return def;
	}
	
	public long getLong(String name, long def){
		try{
			String val=getProperty(name);
			if(val==null)
				return def;
			return Long.parseLong(val);
		}catch(NumberFormatException nfe){
			logger.warn("Expected long in property: "+name);
		}
		return def;
	}
	
	/*
	 * While this code is likely correct, I don't want people using floats at all.
	 * Use double instead.
	 *
	public float getFloat(String name, float def){
		try{
			String val=getProperty(name);
			if(val==null)
				return def;
			return Float.parseFloat(val);
		}catch(NumberFormatException nfe){
			logger.warn("Expected float in property: "+name);
		}
		return def;
	}
	*/
	
	public double getDouble(String name, double def){
		try{
			String val=getProperty(name);
			if(val==null)
				return def;
			return Double.parseDouble(val);
		}catch(NumberFormatException nfe){
			logger.warn("Expected double in property: "+name);
		}
		return def;
	}
	
	public boolean getBoolean(String name, boolean def){
		String val=getProperty(name);
		if(val==null)
			return def;
		val=val.toLowerCase();
		if(val.equals("1")||
				val.equals("y")||
				val.equals("yes")||
				val.equals("t")||
				val.equals("true"))
			return true;
		else if(val.equals("0")||
				val.equals("n")||
				val.equals("no")||
				val.equals("f")||
				val.equals("false"))
			return false;
		else
			logger.warn("Expected boolean in property: "+name);
		return def;
	}
	
	public int getEnum(String name, String[] enumArray, int def){
		String val=getProperty(name);
		if(val!=null){
			for(int i=0;i<enumArray.length;i++){
				if(enumArray[i].equals(val))
					return i;
			}
			throw new RuntimeException("Unknown value for property '" + name + "': " + val);
		}
		return def;
	}	
}
