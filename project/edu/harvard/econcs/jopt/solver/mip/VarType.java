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
package edu.harvard.econcs.jopt.solver.mip;

import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * Enum of the types that variables can have.
 * 
 * @author Benjamin Lubin; Last modified by $Author: blubin $
 * @version $Revision: 1.6 $ on $Date: 2010/10/28 00:13:39 $
 * @since Apr 14, 2004
 **/
public class VarType implements Serializable {
	
	public static final VarType BOOLEAN = new VarType(0);
	public static final VarType INT     = new VarType(1);
	public static final VarType DOUBLE  = new VarType(2);

	private static final long serialVersionUID = 200404141902l;
	private int type;

	private VarType(int type) {
		this.type = type;
	}

	public String toString() {
        switch(type) {
        	case 0:
        		return "Boolean";
        	case 1:
        		return "Int";
        	case 2:
        		return "Double";
        }
        return "Unknown:ERROR";
	}
	
	/** Make serialization work. **/
	private Object readResolve () throws ObjectStreamException
    {
        switch(type) {
        	case 0:
        		return BOOLEAN;
        	case 1:
        		return INT;
        	case 2:
        		return DOUBLE;
        }
        throw new InvalidObjectException("Unknown type: " + type);
    }
	
	public boolean equals(Object obj) {
		return obj != null && 
		       obj.getClass().equals(VarType.class) &&
			   ((VarType)obj).type == type;
	}

	public int hashCode() {
		return type;
	}
}
