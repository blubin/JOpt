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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import edu.harvard.econcs.jopt.solver.IMIP;
import edu.harvard.econcs.jopt.solver.IMIPResult;
import edu.harvard.econcs.jopt.solver.MIPException;

/**
 * The results of running CPLEX.
 * 
 * @author Benjamin Lubin; Last modified by $Author: blubin $
 * @version $Revision: 1.16 $ on $Date: 2013/12/04 02:18:20 $
 * @since Apr 12, 2004
 **/
public class MIPResult implements IMIPResult {
    private static final long serialVersionUID = 176452143213L;
	private double objectiveValue;
	private Map<String,Double> values = new HashMap();
	private Map<Integer,Double> constraintidstoDuals = new HashMap();
	private long solveTime;
	
	public MIPResult(double objectiveValue, Map values, Map constraintidsToDuals) {
		this.objectiveValue = objectiveValue;
		this.values = values;
		this.constraintidstoDuals = constraintidsToDuals;
	}
	
	/* (non-Javadoc)
	 * @see edu.harvard.econcs.jopt.solver.IMIPResult#getObjectiveValue()
	 */
	public double getObjectiveValue() {
		return objectiveValue;
	}
	/* (non-Javadoc)
	 * @see edu.harvard.econcs.jopt.solver.IMIPResult#getValues()
	 */
	public Map getValues() {
		return values;
	}
	
	public double getValue(Variable variable) {
		return getValue(variable.getName());
	}

	public double getValue(String varName) {
		Double ret = (Double)values.get(varName);
		if(ret==null) {
			return Double.NaN;
		}
		return ret;
	}
	
	public double getDual(int constraintId) {
		if(constraintidstoDuals==null){
			throw new MIPException("Duals not available.  Specify SolveParam.CALC_DUALS to obtain them");
		}
		
		return ((Double) constraintidstoDuals.get(new Integer(constraintId))).doubleValue();
	}
	
	public long getSolveTime() {
		return solveTime;
	}
	
	public void setSolveTime(long solveTime) {
		this.solveTime = solveTime;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		//sb.append("MIPResult: \n").append(objectiveValue).append("\n");
		sb.append("Variables: \n");
		int max = 50;
		int zeroVars = 0;
		for (Iterator i = values.keySet().iterator(); i.hasNext(); ) {
			String var = (String) i.next();			
			Double value = (Double) values.get(var);
			if (value.doubleValue() == 0) {
				zeroVars++;
				continue;
			}
			sb.append(var);
			if (var.length() < max) {
				for (int j = 0; j < max-var.length(); j++) {
					sb.append(" ");
				}
			}
			sb.append(value);
				sb.append("\n");
		}
		if (zeroVars > 0) {
			sb.append("Remaining " + zeroVars + " variables (of " + values.size() + " are 0).\n");
		}
		sb.append("\nObjective Value: ");
		sb.append(objectiveValue);
		sb.append("\n");
		return sb.toString();
	}

	public String toString(IMIP mip) {
		ConstraintPrinter cp = new ConstraintPrinter();
		
		StringBuilder sb = new StringBuilder();
		sb.append("Objective: ");
		boolean first = true;		
		for(LinearTerm t : mip.getLinearObjectiveTerms()) {
			double c = t.getCoefficient();
			if(first) {
				first = false;
			} else{
				if(c >=0) {
					sb.append(" + ");
				} else {
					sb.append(" - ");
				}
			}
			sb.append(cp.printCoef(c));
			sb.append('*');
			sb.append(t.getVarName());
			sb.append('{');
			double val = (Double)getValues().get(t.getVarName());
			sb.append(cp.printVal(val));
			sb.append('}');
		}

		for(QuadraticTerm t : mip.getQuadraticObjectiveTerms()) {
			double c = t.getCoefficient();
			if(first) {
				first = false;
			} else{
				if(c >=0) {
					sb.append(" + ");
				} else {
					sb.append(" - ");
				}
			}
			sb.append(cp.printCoef(c));
			sb.append('*');
			sb.append(t.getVarNameA());
			sb.append('{');
			double val = (Double)getValues().get(t.getVarNameA());
			sb.append(cp.printVal(val));
			sb.append('}');
			sb.append('*');
			sb.append(t.getVarNameB());
			sb.append('{');
			val = (Double)getValues().get(t.getVarNameB());
			sb.append(cp.printVal(val));
			sb.append('}');

		}
		sb.append("\n");
		
		Set<Integer> idSet = mip.getConstraintIds();
		Integer[] ids = new Integer[idSet.size()];
		ids = idSet.toArray(ids);
		Arrays.sort(ids);
		
		for(int id : ids) {
			Constraint c = mip.getConstraint(id);
			sb.append(id).append(" : ").append(c.getDescription()).append(": ");
			sb.append(cp.print(c, this)).append("\n");
		}
		
		return sb.toString();
	}
	
	/**
	 * @author Benjamin Lubin; Last modified by $Author: blubin $
	 * @version $Revision: 1.16 $ on $Date: 2013/12/04 02:18:20 $
	 **/
	protected static class ConstraintPrinter {
		private String coefFormat = "%6.4f";
		private String valFormat  = "%7.4f";
		
		public String print(Constraint c, IMIPResult result) {
			if(c == null) {
				return "null";
			}
			Map<String,Double> values = result.getValues();
			StringBuilder sb = new StringBuilder();
			boolean first = true;			
			for (LinearTerm t : c.getSortedLinearTerms()) {
				if (t.getCoefficient() >= 0) {
					if (!first) {
						sb.append(" + ");
					}
				} else {
					sb.append(" - ");
				}
				sb.append(printCoef(Math.abs(t.getCoefficient())));
				sb.append(" ");
				String name = t.getVarName();
				sb.append(name);
				sb.append("{");
				Double v = values.get(name);
				if(v != null) {
					sb.append(printVal(v));
				}
				sb.append("}");
				first = false;
			}
			for (QuadraticTerm t : c.getSortedQuadraticTerms()) {
				if (t.getCoefficient() >= 0) {
					if (!first) {
						sb.append(" + ");
					}
				} else {
					sb.append(" - ");
				}
				sb.append(printCoef(Math.abs(t.getCoefficient())));
				sb.append(" ");
				String name = t.getVarNameA();
				sb.append(name);
				sb.append("{");
				Double v = values.get(name);
				if(v != null) {
					sb.append(printVal(v));
				}
				sb.append("}");
				sb.append(" ");
				name = t.getVarNameB();
				sb.append(name);
				sb.append("{");
				v = values.get(name);
				if(v != null) {
					sb.append(printVal(v));
				}
				sb.append("}");
				first = false;
			}
			sb.append(c.getType()).append(c.getConstant());
			return sb.toString();
		}
		
		public String printCoef(double d) {
			return print(coefFormat, d);
		}
		
		public String printVal(double d) {
			return print(valFormat, d);
		}

		private String print(String format, double d) {
			return String.format(format, d);
		}
	}

}
