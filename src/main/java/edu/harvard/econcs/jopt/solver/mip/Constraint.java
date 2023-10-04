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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import edu.harvard.econcs.jopt.solver.MIPException;

/**
 * General Representation of a MIP constraint.
 * 
 * @author Benjamin Lubin; Last modified by $Author: blubin $
 * @version $Revision: 1.14 $ on $Date: 2013/12/04 02:54:09 $
 */
public class Constraint implements Serializable, Cloneable {
	private static final long serialVersionUID = 456346456l;

	private Collection<LinearTerm> linearTerms = null;
	private Collection<QuadraticTerm> quadraticTerms = null;
	private Collection<AbsTerm> absTerms = null;
	
	private double constant;
	private CompareType type;

	private String description = null;
	private Integer hashCode=null;
	
	/**
	 * @param constTerm
	 * @param type
	 */
	public Constraint(CompareType type, double constant) {
		MIP.checkMax(constant);
		this.constant = constant;
		this.type = type;
		this.linearTerms = new ArrayList();
	}
	
	public Constraint(CompareType type, double constant, String description) {
		this(type, constant);
		this.setDescription(description);
	}
		
	/**
	 * @return Returns the constTerm.
	 */
	public double getConstant() {
		return constant;
	}

	/**
	 * @param constant The constant to set.
	 */
	public void setConstant(double constant) {
		this.constant = constant;
		hashCode=null;
	}


	public String getDescription() {
		return description;
	}

	public void setDescription(String desc) {
		this.description = desc;
	}

	/**
	 * @return Returns the type.
	 */
	public CompareType getType() {
		return type;
	}
	
	public void setType(CompareType type) {
		this.type = type;
		hashCode=null;
	}
		
	// Linear Terms:
	
	public boolean hasLinearTerms() {
		return linearTerms != null;
	}

	public void addTerm(LinearTerm term) {
		if(linearTerms == null) {
			linearTerms = new ArrayList<LinearTerm>();
		}
		linearTerms.add(term);
		hashCode=null;
	}
	
	public void addTerm(double coefficient, Variable var) {
	    addTerm(new LinearTerm(coefficient, var));
	}
	
	public int linearSize() {
		if (linearTerms == null) {
			return 0;
		}
		return linearTerms.size();
	}

	public Collection<LinearTerm> getLinearTerms() {
		if(linearTerms == null) {
			return Collections.EMPTY_LIST;
		}
		return linearTerms;
	}
	
	public Collection<LinearTerm> getSortedLinearTerms() {
		if(linearTerms == null) {
			return Collections.EMPTY_LIST;
		}
		LinearTerm[] sorted = linearTerms.toArray(new LinearTerm[linearTerms.size()]);
		Arrays.sort(sorted, new Comparator<LinearTerm>(){
			public int compare(LinearTerm o1, LinearTerm o2) {
				return o1.getVarName().compareTo(o2.getVarName());
			}});
		return Arrays.asList(sorted);
	}	
	
	// ABs Terms:
	
	public boolean hasABsTerms() {
		return absTerms != null;
	}

	public void addTerm(AbsTerm term) {
		if(absTerms == null) {
			absTerms = new ArrayList<AbsTerm>();
		}
		absTerms.add(term);
		hashCode=null;
	}
		
	public int absSize() {
		if (absTerms == null) {
			return 0;
		}
		return absTerms.size();
	}

	public Collection<AbsTerm> getAbsTerms() {
		if(absTerms == null) {
			return Collections.EMPTY_LIST;
		}
		return absTerms;
	}
		
	public Collection<AbsTerm> getSortedAbsTerms() {
		if(absTerms == null) {
			return Collections.EMPTY_LIST;
		}
		AbsTerm[] sorted = absTerms.toArray(new AbsTerm[absTerms.size()]);
		Arrays.sort(sorted, new Comparator<AbsTerm>(){
			public int compare(AbsTerm o1, AbsTerm o2) {
				return o1.getVarName().compareTo(o2.getVarName());
			}});
		return Arrays.asList(sorted);
	}	

	// Quadratic Terms:
	
	public boolean hasQuadraticTerms() {
		return quadraticTerms != null;
	}	
	

	public void addTerm(QuadraticTerm term) {
		if(quadraticTerms == null) {
			quadraticTerms = new ArrayList<QuadraticTerm>();
		}
		quadraticTerms.add(term);
		hashCode=null;
	}
	
	public void addTerm(double coefficient, Variable varA, Variable varB) {
	    addTerm(new QuadraticTerm(coefficient, varA, varB));
	}
	
	public int quadraticSize() {
		if (quadraticTerms == null) {
			return 0;
		}
		return quadraticTerms.size();
	}
	
	public Collection<QuadraticTerm> getQuadraticTerms() {
		if(quadraticTerms == null) {
			return Collections.EMPTY_LIST;
		}
		return quadraticTerms;
	}
	
	public Collection<QuadraticTerm> getSortedQuadraticTerms() {
		if(quadraticTerms == null) {
			return Collections.emptyList();
		}
		QuadraticTerm[] sorted = quadraticTerms.toArray(new QuadraticTerm[quadraticTerms.size()]);
		Arrays.sort(sorted, new Comparator<QuadraticTerm>(){
			public int compare(QuadraticTerm o1, QuadraticTerm o2) {
				int ret = o1.getVarNameA().compareTo(o2.getVarNameA());
				if (ret == 0) {
					ret = o1.getVarNameB().compareTo(o2.getVarNameB());
				}
				return ret;
			}});
		return Arrays.asList(sorted);
	}	
	
	// Overall:
	
	public Collection<Term> getTerms() {
		//Could be done more efficiently:
		ArrayList ret = new ArrayList<Term>();
		ret.addAll(linearTerms);
		ret.addAll(quadraticTerms);
		ret.addAll(absTerms);
		return ret;
	}
	
	public int size() {
		return linearSize() + quadraticSize();
	}
	
	// General Functions:
	
	@Override
	public int hashCode() {
	    if(hashCode!=null){
	        return hashCode;
	    }
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(constant);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result
				+ ((linearTerms == null) ? 0 : linearTerms.hashCode());
		result = prime * result
				+ ((quadraticTerms == null) ? 0 : quadraticTerms.hashCode());
		result = prime * result
				+ ((absTerms == null) ? 0 : absTerms.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		this.hashCode=result;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if(hashCode()!=obj.hashCode()){
		    return false;
		}
		if (getClass() != obj.getClass())
			return false;
		Constraint other = (Constraint) obj;
		if (constant!= other.constant)
			return false;
		if (linearTerms == null) {
			if (other.linearTerms != null)
				return false;
		} else if (!linearTerms.equals(other.linearTerms))
			return false;
		if (quadraticTerms == null) {
			if (other.quadraticTerms != null)
				return false;
		} else if (!quadraticTerms.equals(other.quadraticTerms))
			return false;
		if (absTerms == null) {
			if (other.absTerms != null)
				return false;
		} else if (!absTerms.equals(other.absTerms))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}
	
	protected Object clone() throws CloneNotSupportedException {
		Constraint ret = (Constraint)super.clone();
		if(linearTerms != null) {		
			ret.linearTerms = new ArrayList();
			for (LinearTerm term : getLinearTerms()) {
				ret.linearTerms.add(term.typedClone());
			}
		}
		if(quadraticTerms != null) {		
			ret.quadraticTerms = new ArrayList();
			for (QuadraticTerm term : getQuadraticTerms()) {
				ret.quadraticTerms.add(term.typedClone());
			}
		}
		if(absTerms != null) {		
			ret.absTerms = new ArrayList();
			for (AbsTerm term : getAbsTerms()) {
				ret.absTerms.add(term.typedClone());
			}
		}
		return ret;
	}
	
	public Constraint typedClone() {
		try {
			return (Constraint)clone();
		} catch (CloneNotSupportedException e) {
			throw new MIPException("Problem in clone", e);
		}
	}
	
	//public String toString() {
	//	return "Constraint " + description + " {" + linearTerms + " " + quadraticTerms + getType() + getConstant() + "}";
	//}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		if(description != null) {
			sb.append(description).append(" ");
		}
		boolean first = true;
		for (LinearTerm t : getSortedLinearTerms()) {
			if (t.getCoefficient() >= 0) {
				if (!first) {
					sb.append(" + ");
				}
			} else {
				sb.append(" - ");
			}
			sb.append(Math.abs(t.getCoefficient())).append(" ").append(t.getVarName());
			first = false;
		}
		
		for (AbsTerm t : getSortedAbsTerms()) {
			if (t.getCoefficient() >= 0) {
				if (!first) {
					sb.append(" + |");
				}
			} else {
				sb.append(" - |");
			}
			sb.append(Math.abs(t.getCoefficient())).append(" ").append(t.getVarName());
			sb.append("|");
			first = false;
		}

		for (QuadraticTerm t : getSortedQuadraticTerms()) {
			if (t.getCoefficient() >= 0) {
				if (!first) {
					sb.append(" + ");
				}
			} else {
				sb.append(" - ");
			}
			sb.append(Math.abs(t.getCoefficient())).append(" ").append(t.getVarNameA()).append(" * ").append(t.getVarNameB());
			first = false;
		}

		sb.append(getType()).append(getConstant());
		return sb.toString();
	}
	
}
