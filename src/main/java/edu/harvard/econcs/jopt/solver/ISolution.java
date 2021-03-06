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

import edu.harvard.econcs.jopt.solver.mip.Variable;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Comparing based on Objective Value
 * 
 * @author Benedikt
 * 
 */
public interface ISolution extends Comparable<ISolution> {
    /** Returns the objective value calculated by the solver */

    double getObjectiveValue();

    /** Returns a Map from strings to Double objects */
    Map<String, Double> getValues();

    double getValue(Variable variable);

    double getValue(String varName);

    /** Length to solve the mip (not including conversion time etc.) in milis **/
    long getSolveTime();

    /**
     * Compares this {@link ISolution} to another {@link ISolution} based on the
     * objective
     */
    default int compareTo(ISolution o) {
        return Double.compare(getObjectiveValue(), o.getObjectiveValue());
    }

    /**
     * @return the relative difference in the objective value compared to the relaxation of the problem
     */
    double getRelativeGap();

    /**
     * @return the absolute difference in the objective value compared to the relaxation of the problem
     */
    double getAbsoluteGap();

    /**
     * To take advantage of the advanced structure of variables of interest, this method checks if for all collections
     * collections of variables, the sum of these variables are equal.
     *
     * @see IMIP#setAdvancedVariablesOfInterest(Collection)
     *
     * @param o The other solution
     * @param variableSetsOfInterest A collection of collections of variables of interest
     * @return true, if the sum of the values in each collection of variables of interest is equal; else false
     */
    default boolean isDuplicateAdvanced(ISolution o, Collection<Collection<Variable>> variableSetsOfInterest) {
        if (variableSetsOfInterest == null) return false;
        double e = 1e-8;
        for (Collection<Variable> variableCollection : variableSetsOfInterest) {
            double thisSum = variableCollection.stream().mapToDouble(this::getValue).sum();
            double otherSum = variableCollection.stream().mapToDouble(o::getValue).sum();
            if (thisSum < otherSum - e
                || thisSum > otherSum + e) {
                return false;
            }
        }
        return true;
    }

    /**
     * Given a collection of variables of interest, this method checks if this solution is a duplicate to another
     * solution
     *
     * @param o The other solution
     * @param variablesOfInterest A single collection of variables of interest
     * @return true, if this solution has the same value for all variables of interest; else false
     */
    default boolean isDuplicate(ISolution o, Collection<Variable> variablesOfInterest) {
        if (variablesOfInterest == null) return false;
        return isDuplicateAdvanced(o,
                variablesOfInterest
                        .stream()
                        .map(v -> Stream.of(v).collect(Collectors.toSet()))
                        .collect(Collectors.toSet()));
    }

}