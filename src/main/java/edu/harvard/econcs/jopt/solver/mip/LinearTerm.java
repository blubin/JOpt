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
 * Represents a Linear Term
 * 
 * @author Benjamin Lubin; Last modified by $Author: blubin $
 * @version $Revision: 1.2 $ on $Date: 2013/12/04 02:54:09 $
 * @since Apr 13, 2004
 **/
public class LinearTerm implements Serializable, Cloneable, Term {
	private static final long serialVersionUID = 8777676765l;
	private double coefficient;
	private String varName;
	/**
	 * @param coefficient
	 * @param varIndex
	 */
	private LinearTerm(double coefficient, String varName) {
		MIP.checkMax(coefficient);
		this.coefficient = coefficient;
		this.varName = varName;
	}
	
	public LinearTerm(double coefficient, Variable var) {
	    this(coefficient, var.getName());
	}
	
	/**
	 * @return Returns the coefficient.
	 */
	public double getCoefficient() {
		return coefficient;
	}
	/**
	 * @return Returns the varIndex.
	 */
	public String getVarName() {
		return varName;
	}
	
	public String toString() {
		//return coefficient +"*V" + varName;
	    return coefficient +"*" + varName;
	}
	
	public Object clone() throws CloneNotSupportedException {
		LinearTerm ret = (LinearTerm)super.clone();
		return ret;
	}
	
	public LinearTerm typedClone() {
		try {
			return (LinearTerm)clone();
		} catch (CloneNotSupportedException e) {
			throw new MIPException("Problem in clone", e);
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(coefficient);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((varName == null) ? 0 : varName.hashCode());
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
		LinearTerm other = (LinearTerm) obj;
		if (Double.doubleToLongBits(coefficient) != Double
				.doubleToLongBits(other.coefficient))
			return false;
		if (varName == null) {
			if (other.varName != null)
				return false;
		} else if (!varName.equals(other.varName))
			return false;
		return true;
	}
}
