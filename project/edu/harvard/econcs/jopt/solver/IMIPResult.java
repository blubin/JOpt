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
package edu.harvard.econcs.jopt.solver;

import java.io.Serializable;
import java.util.Map;

import edu.harvard.econcs.jopt.solver.mip.Variable;

/**
 * The result of running a linear or mixed integer program
 * 
 * @author Benjamin Lubin; Last modified by $Author: blubin $
 * @version $Revision: 1.10 $ on $Date: 2013/12/03 23:24:23 $
 * @since Apr 12, 2004
 **/
public interface IMIPResult extends Serializable {
	
	/** Returns the objective value calculated by the solver */
	public double getObjectiveValue();
	
	/** Returns a Map from strings to Double objects */
	public Map<String,Double> getValues();
	
	public double getValue(Variable variable);
	
	public double getValue(String varName);
	
	/** Returns the dual of a constraint that was added with IMIP.add(constraint, constraintId) */
	public double getDual(int constraintId);
	
	/** Length to solve the mip (not including conversion time etc.) in milis **/
	public long getSolveTime();

	/** Dump the results out to std out, using the MIP to make it pretty **/
	public String toString(IMIP mip);
}
