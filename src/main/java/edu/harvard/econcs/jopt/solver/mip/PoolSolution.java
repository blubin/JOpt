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

import edu.harvard.econcs.jopt.solver.ISolution;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;

public class PoolSolution implements Serializable, ISolution {

    private static final long serialVersionUID = -7265174347585717488L;
    private double objectiveValue;
    private final Map<String, Double> values;
    private double relativeGap;
    private double absoluteGap;
    private double poolRelativeGap = -1;
    private double poolAbsoluteGap = -1;

    public PoolSolution(double objectiveValue, double bestObjectiveValue, Map<String, Double> values) {
        this.objectiveValue = objectiveValue;
        this.values = values;
        this.absoluteGap = Math.abs(bestObjectiveValue - objectiveValue);
        this.relativeGap = this.absoluteGap / (1e-10 + Math.abs(bestObjectiveValue));
    }

    public void setPoolGaps(double optimalObjectiveValue) {
        this.poolAbsoluteGap = Math.abs(optimalObjectiveValue - objectiveValue);
        this.poolRelativeGap = this.poolAbsoluteGap / (1e-10 + Math.abs(optimalObjectiveValue));
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.harvard.econcs.jopt.solver.mip.Solution#getObjectiveValue()
     */
    @Override
    public double getObjectiveValue() {
        return objectiveValue;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.harvard.econcs.jopt.solver.ISolution#getValues()
     */
    @Override
    public Map<String, Double> getValues() {
        return values;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * edu.harvard.econcs.jopt.solver.ISolution#getValue(edu.harvard.econcs
     * .jopt.solver.mip.Variable)
     */
    @Override
    public double getValue(Variable variable) {
        return getValue(variable.getName());
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * edu.harvard.econcs.jopt.solver.ISolution#getValue(java.lang.String)
     */
    @Override
    public double getValue(String varName) {
        Double ret = values.get(varName);
        if (ret == null) {
            return Double.NaN;
        }
        return ret;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("PoolSolution: ");
        sb.append("Variables: \n");
        int varNamePadLength = 50;
        int zeroVars = 0;
        for (Iterator i = values.keySet().iterator(); i.hasNext();) {
            String var = (String) i.next();
            Double value = values.get(var);
            if (value.doubleValue() == 0) {
                zeroVars++;
                continue;
            }
            sb.append(var);
            if (var.length() < varNamePadLength) {
                for (int j = 0; j < varNamePadLength - var.length(); j++) {
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
    

    public int hashCode() {
        return values.hashCode();
    }

    public boolean equal(Object other) {
        ISolution otherSolution = (ISolution) other;
        return this.objectiveValue == otherSolution.getObjectiveValue() && getValues().equals(otherSolution.getValues());
    }

    @Override
    public double getRelativeGap() {
        return relativeGap;
    }

    @Override
    public double getAbsoluteGap() {
        return absoluteGap;
    }


    @Override
    public long getSolveTime() {
        return 0;
    }

    /**
     * @return The relative gap compared to the optimal feasible solution, if available. Else -1.
     */
    public double getPoolRelativeGap() {
        return poolRelativeGap;
    }

    /**
     * @return The absolute gap compared to the optimal feasible solution, if available. Else -1.
     */
    public double getPoolAbsoluteGap() {
        return poolAbsoluteGap;
    }
}
