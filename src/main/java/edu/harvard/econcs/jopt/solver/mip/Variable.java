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
package edu.harvard.econcs.jopt.solver.mip;

import java.io.Serializable;

import edu.harvard.econcs.jopt.solver.MIPException;

/**
 * Basic MIP Variable.
 * 
 * @author Benjamin Lubin Last modified by $Author: blubin $
 * @version $Revision: 1.8 $ on $Date: 2013/12/03 23:52:56 $
 * @since Apr 13, 2004
 */

public class Variable implements Serializable, Cloneable {
	private static final long serialVersionUID = 45646456456l;
	private String name;
	private double lowerBound;
	private double upperBound;
	private VarType type;
	private boolean ignore = false;
	
	/**
	 * @param name
	 * @param upperBound
	 * @param lowerBound
	 * @param type
	 */
	public Variable(String name, VarType type, double lowerBound, double upperBound) {
		super();
		MIP.checkMax(lowerBound);
		if (lowerBound > upperBound) {
			throw new MIPException("Lowerbound must be less than upperBound");
		}
		this.name = name;
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
		this.type = type;
	}


	public void setLowerBound(double d) {
		lowerBound = d;
	}
	
	/**
	 * @return Returns the lowerBound.
	 */
	public double getLowerBound() {
		return lowerBound;
	}
	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}
	/**
	 * @return Returns the type.
	 */
	public VarType getType() {
		return type;
	}
	/**
	 * @return Returns the upperBound.
	 */
	public double getUpperBound() {
		return upperBound;
	}
	
	public void setUpperBound(double d) {
		upperBound = d;
	}

	public String toString() {
		String ret=getName();
		if (ret == null) {
			ret = "Var";
		}
		return ret;
	}	
	
	public String toStringPretty() {
		String ret=getName();
		if (ret == null) {
			ret = "Var";
		}
		ret += " {" + getType() + ", " + getLowerBound() + ", " + getUpperBound() +"}";
		return ret;
	}
	
	/**
	 * @param ignore To ignore this variable during MIP solving. Only WDResult
	 * Determination people should be using this right now.
	 */
	public void setIgnore(boolean ignore) {
		this.ignore = ignore;
	}
	
	public boolean ignore() {
		return ignore;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Variable other = (Variable) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	protected Object clone() throws CloneNotSupportedException {
		Variable ret = (Variable)super.clone();		
		return ret;
	}
	
	public Variable typedClone() {
		try {
			return (Variable)clone();
		} catch (CloneNotSupportedException e) {
			throw new MIPException("Problem in clone", e);
		}
	}
}
