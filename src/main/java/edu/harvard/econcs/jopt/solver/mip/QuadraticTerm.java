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
 * @version $Revision: 1.3 $ on $Date: 2013/12/04 02:54:09 $
 * @since Apr 13, 2004
 **/
public class QuadraticTerm implements Serializable, Cloneable, Term{

	private static final long serialVersionUID = 5224343193471392593L;
	private double coefficient;
	private String varNameA;
	private String varNameB;

	/**
	 * @param coefficient
	 * @param varIndex
	 */
	private QuadraticTerm(double coefficient, String varNameA, String varNameB) {
		MIP.checkMax(coefficient);
		this.coefficient = coefficient;
		this.varNameA = varNameA;
		this.varNameB = varNameB;
	}
	
	public QuadraticTerm(double coefficient, Variable varA, Variable varB) {
	    this(coefficient, varA.getName(), varB.getName());
	}
	
	/**
	 * @return Returns the coefficient.
	 */
	public double getCoefficient() {
		return coefficient;
	}

	public String getVarNameA() {
		return varNameA;
	}

	public String getVarNameB() {
		return varNameB;
	}

	public String toString() {
		//return coefficient +"*V" + varName;
	    return coefficient +"*" + varNameA + "*" + varNameB;
	}
	
	public Object clone() throws CloneNotSupportedException {
		QuadraticTerm ret = (QuadraticTerm)super.clone();
		return ret;
	}
	
	public QuadraticTerm typedClone() {
		try {
			return (QuadraticTerm)clone();
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
		result = prime * result
				+ ((varNameA == null) ? 0 : varNameA.hashCode());
		result = prime * result
				+ ((varNameB == null) ? 0 : varNameB.hashCode());
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
		QuadraticTerm other = (QuadraticTerm) obj;
		if (Double.doubleToLongBits(coefficient) != Double
				.doubleToLongBits(other.coefficient))
			return false;
		if (varNameA == null) {
			if (other.varNameA != null)
				return false;
		} else if (!varNameA.equals(other.varNameA))
			return false;
		if (varNameB == null) {
			if (other.varNameB != null)
				return false;
		} else if (!varNameB.equals(other.varNameB))
			return false;
		return true;
	}
}
