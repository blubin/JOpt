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
package edu.harvard.econcs.jopt.solver;

import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import edu.harvard.econcs.jopt.solver.mip.Constraint;
import edu.harvard.econcs.jopt.solver.mip.Variable;

/**
 * Exception that is thrown when evaluating a MIP, that contains the
 * infeasible set information
 * 
 * @author Benjamin Lubin; Last modified by $Author: blubin $
 * @version $Revision: 1.9 $ on $Date: 2013/12/04 02:18:20 $
 * @since Apr 19, 2005
 **/
public class MIPInfeasibleException extends MIPException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3618981187340022066L;
	/** Variables to Cause objects **/
	Map<Variable,Cause> infeasibleVariables = new HashMap<Variable,Cause>();
	/** Constraints to Cause objects **/
	Collection<Constraint> infeasibleConstraints = new LinkedList<Constraint>();
	
	
	public MIPInfeasibleException(String msg) {
		super("MIP Infeasible for unknown reason: " + msg);		
	}
	
	public MIPInfeasibleException(Map<Variable,Cause> infeasibleVariables, Collection<Constraint> infeasibleConstraints) {
		super("MIP Infeasible");
		this.infeasibleVariables = infeasibleVariables;
		this.infeasibleConstraints = infeasibleConstraints;
	}
	
	public boolean isReasonKnown() {
		return !infeasibleVariables.isEmpty() || !infeasibleConstraints.isEmpty();
	}

	public Set<Variable> getInfeasibleVariables() {
		return Collections.unmodifiableSet(infeasibleVariables.keySet());
	}
	
	public Cause getCause(Variable v) {
		return infeasibleVariables.get(v);
	}
	
	public Collection<Constraint> getInfeasibleConstraints() {
		return Collections.unmodifiableCollection(infeasibleConstraints);
	}
	
	public String getCauseDescription() {
		StringBuffer sb = new StringBuffer();
		sb.append(getVariableDescription()).append("\n").append(getConstraintDescription());
		return sb.toString();
	}
	
	protected String getVariableDescription() {
		StringBuffer sb = new StringBuffer("Variables causing infeasibility:\n");
		Set inf = getInfeasibleVariables();
		Variable[] vars = (Variable[])inf.toArray(new Variable[inf.size()]);
		Arrays.sort(vars, new Comparator(){
			public int compare(Object o1, Object o2) {
				return ((Variable)o1).getName().compareTo(((Variable)o2).getName());
			}});
		for (int i=0; i<vars.length; i++) {
			Variable v=vars[i];
			sb.append(v).append(" pushed to ").append(getCause(v)).append("\n");
		}
		return sb.toString();
	}
	
	protected String getConstraintDescription() {
		StringBuffer sb = new StringBuffer("Constraints causing infeasibility:\n");
		for (Iterator iter = getInfeasibleConstraints().iterator(); iter.hasNext(); ) {
			Constraint c = (Constraint)iter.next();
			if (c==null) {
				sb.append("NULL CONSTRAINT?!");
			} else {
				sb.append("Constraint").append(": ").append(c).append("\n");
			}
		}
		return sb.toString();
		
	}
	
	public String getMessage() {
		String ret = super.getMessage();
		if (isReasonKnown()) {
			ret += "\n" + getCauseDescription();
		}
		return ret;
	}
	
	public static class Cause implements Serializable {
		public static Cause LOWER = new Cause(1);
		public static Cause UPPER = new Cause(2);
		private static final long serialVersionUID = 200504191645l;
		private int type;
		
		public Cause(int type) {
			this.type = type;
		}
		
		public boolean equals(Object o) {
			return o != null && o.getClass().equals(Cause.class) && ((Cause)o).type == type;
		}
		
		public int hashCode() {
			return type;
		}
		
		public String toString() {
	        switch(type) {
	        	case 1:
	        		return "Lower";
	        	case 2:
	        		return "Upper";
	        }
	        return "Unknown:ERROR";
		}
		
		/** Make serialization work. **/
		private Object readResolve () throws ObjectStreamException
	    {
	        switch(type) {
	        	case 1:
	        		return LOWER;
	        	case 2:
	        		return UPPER;
	        }
	        throw new InvalidObjectException("Unknown type: " + type);
	    }

	}
}
