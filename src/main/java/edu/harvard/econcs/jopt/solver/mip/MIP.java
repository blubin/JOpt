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

import edu.harvard.econcs.jopt.solver.IMIP;
import edu.harvard.econcs.jopt.solver.MIPException;
import edu.harvard.econcs.jopt.solver.SolveParam;

import java.io.Serializable;
import java.util.*;

/**
 * Basic implementation of a mip.
 *
 * @author Benjamin Lubin; Last modified by $Author: blubin $
 * @version $Revision: 1.31 $ on $Date: 2013/12/04 02:59:49 $
 * @since April 12, 2004
 **/
public class MIP implements IMIP, Serializable, Cloneable {

    private static final long serialVersionUID = 1152496442L;

    /**
     * Never give mip a number bigger than this, FOR ANY reason This should
     * never be greater than 1E9 per
     * https://support.ilog.com/public/products/faq
     * .cfm?Product=CPLEX&FAQ=75&CFID=
     * 96044&CFTOKEN=40609383&jsessionid=92301560341115251663761
     **/
    public static final int MAX_VALUE = 536870910;

    private Map<String, Variable> vars = new HashMap();
    private Map<Variable, Object> proposedValuesForVars = null;
    private List<Constraint> constraints = Collections.synchronizedList(new LinkedList<>());
    private Collection<LinearTerm> linearObjectiveTerms = null;
    private Collection<QuadraticTerm> quadraticObjectiveTerms = null;
    private boolean isMax;
    private Map<SolveParam, Object> solveParams = new HashMap<>();
    private Collection<Variable> variablesOfInterest = null;

    public MIP() {
        resetDefaultSolveParams();
    }

    // Variables:
    // //////////

    public boolean containsVar(Variable var) {
        return vars.containsKey(var.getName());
    }
    public boolean containsVar(String name) {
        return vars.containsKey(name);
    }


    public Map<String, Variable> getVars() {
        return Collections.unmodifiableMap(vars);
    }

    public Variable getVar(String name) {
        return vars.get(name);
    }

    public String add(Variable var) {
        String name = var.getName();
        if (name == null) {
            throw new MIPException("Invalid variable name");
        } else if (vars.get(name) != null) {
            // FUTURE: This is fine for now
            // jeffsh: actually, not an error? But this code should be very
            // general.
            // so I think we treat this as an error and push it back to caller.
            // log.debug("JS: Name->Var already exists. Ok to re-use previous reference?");
            throw new MIPException("Tried to overwrite existing var: " + name);
        }
        vars.put(name, var);
        return name;
    }

    public int getNumVars() {
        return vars.size();
    }

    public void remove(Variable var) {
        String name = var.getName();
        if (name == null) {
            throw new MIPException("Invalid variable name");
        }
        if (vars.remove(name) == null) {
            throw new MIPException("Tried to remove constraint that does not exist");
        }
    }

    public Collection<Variable> getVariablesOfInterest() {
        return variablesOfInterest;
    }

    public void setVariablesOfInterest(Collection<Variable> variablesOfInterest) {
        this.variablesOfInterest = variablesOfInterest;
    }

    // Proposed Values:
    // ////////////////

    private void checkProposedHashMap() {
        if (proposedValuesForVars == null) {
            proposedValuesForVars = new HashMap<>();
        }
    }

    /**
     * Sets proposed value for this variable
     */
    public void proposeValue(Variable var, double value) {
        checkProposedHashMap();
        proposedValuesForVars.put(var, new Double(value));
    }

    /**
     * Sets proposed value for this variable
     */
    public void proposeValue(Variable var, int value) {
        checkProposedHashMap();
        if (!((value == 0) && (getBooleanSolveParam(SolveParam.ZERO_MISSING_PROPOSED, new Boolean(false)) == true))) {
            // optimization to cut down serialization size
            proposedValuesForVars.put(var, new Integer(value));
        }
    }

    /**
     * Sets proposed value for this variable
     */
    public void proposeValue(Variable var, boolean value) {
        checkProposedHashMap();
        if (!((value == false) && (getBooleanSolveParam(SolveParam.ZERO_MISSING_PROPOSED, new Boolean(false)) == true))) {
            // optimization to cut down serialization size
            proposedValuesForVars.put(var, new Boolean(value));
        }
    }

    /**
     * Removes proposed value for this variable
     */
    public void removeProposedValue(Variable var) {
        if (proposedValuesForVars == null) {
            return;
        }
        proposedValuesForVars.remove(var);
        if (proposedValuesForVars.isEmpty()) {
            proposedValuesForVars = null;
        }
    }

    /**
     * Clears proposed value for this variable
     */
    public void clearProposedValues() {
        proposedValuesForVars = null;
    }

    /**
     * useful for copying proposed var/vals to a new MIP
     */
    public Map<Variable, Object> getProposedValuesMap() {
        return proposedValuesForVars;
    }

    /**
     * useful for copying proposed var/vals to a new MIP
     */
    public void setProposedValues(Map<Variable, Object> proposedValues) {
        this.proposedValuesForVars = proposedValues;
    }

    /**
     * useful for copying proposed var/vals to a new MIP
     */
    public Set<Variable> getVarsWithProposedValues() {
        if (proposedValuesForVars == null) {
            return Collections.EMPTY_SET;
        }
        return Collections.unmodifiableSet(proposedValuesForVars.keySet());
    }

    public int getProposedIntValue(Variable var) {
        Object i = proposedValuesForVars.get(var);
        if (i == null) {
            throw new MIPException("Variable " + var + " has no proposed value");
        }
        if (!(i instanceof Integer)) {
            throw new MIPException("Variable " + var + " not an Integer");
        }
        return ((Integer) i).intValue();
    }

    public double getProposedDoubleValue(Variable var) {
        Object i = proposedValuesForVars.get(var);
        if (i == null) {
            throw new MIPException("Variable " + var + " has no proposed value");
        }
        if (!(i instanceof Double)) {
            throw new MIPException("Variable " + var + " not a Double");
        }
        return ((Double) i).doubleValue();
    }

    public boolean getProposedBooleanValue(Variable var) {
        Object i = proposedValuesForVars.get(var);
        if (i == null) {
            throw new MIPException("Variable " + var + " has no proposed value");
        }
        if (!(i instanceof Boolean)) {
            throw new MIPException("Variable " + var + " not an Boolean");
        }
        return ((Boolean) i).booleanValue();
    }

    // Objective Terms:
    // ////////////////

    // Linear:

    public Collection<LinearTerm> getLinearObjectiveTerms() {
        if (linearObjectiveTerms == null) {
            return Collections.EMPTY_LIST;
        }
        return linearObjectiveTerms;
    }

    public Collection<LinearTerm> getSortedLinearObjectiveTerms() {
        if (linearObjectiveTerms == null) {
            return Collections.EMPTY_LIST;
        }
        LinearTerm[] sorted = linearObjectiveTerms.toArray(new LinearTerm[linearObjectiveTerms.size()]);
        Arrays.sort(sorted, (o1, o2) -> o1.getVarName().compareTo(o2.getVarName()));
        return Arrays.asList(sorted);
    }

    public void addObjectiveTerm(LinearTerm term) {
        if (linearObjectiveTerms == null) {
            linearObjectiveTerms = new ArrayList<LinearTerm>();
        }
        linearObjectiveTerms.add(term);
    }

    public void addObjectiveTerm(double coefficient, Variable var) {
        LinearTerm term = new LinearTerm(coefficient, var);
        addObjectiveTerm(term);
    }

    public void removeObjectiveTerm(LinearTerm term) {
        if (linearObjectiveTerms == null || !linearObjectiveTerms.remove(term)) {
            throw new MIPException("Tried to remove constraint that does not exist");
        }
    }

    // Quadratic:

    public Collection<QuadraticTerm> getQuadraticObjectiveTerms() {
        if (quadraticObjectiveTerms == null) {
            return Collections.EMPTY_LIST;
        }
        return quadraticObjectiveTerms;
    }

    public Collection<QuadraticTerm> getSortedQuadraticObjectiveTerms() {
        if (quadraticObjectiveTerms == null) {
            return Collections.EMPTY_LIST;
        }
        QuadraticTerm[] sorted = quadraticObjectiveTerms.toArray(new QuadraticTerm[quadraticObjectiveTerms.size()]);
        Arrays.sort(sorted, new Comparator<QuadraticTerm>() {
            public int compare(QuadraticTerm o1, QuadraticTerm o2) {
                int ret = o1.getVarNameA().compareTo(o2.getVarNameA());
                if (ret == 0) {
                    ret = o1.getVarNameB().compareTo(o2.getVarNameB());
                }
                return ret;
            }
        });
        return Arrays.asList(sorted);
    }

    public void addObjectiveTerm(QuadraticTerm term) {
        if (quadraticObjectiveTerms == null) {
            quadraticObjectiveTerms = new ArrayList<QuadraticTerm>();
        }
        quadraticObjectiveTerms.add(term);
    }

    public void addObjectiveTerm(double coefficient, Variable varA, Variable varB) {
        QuadraticTerm term = new QuadraticTerm(coefficient, varA, varB);
        addObjectiveTerm(term);
    }

    public void removeObjectiveTerm(QuadraticTerm term) {
        if (quadraticObjectiveTerms == null || !quadraticObjectiveTerms.remove(term)) {
            throw new MIPException("Tried to remove constraint that does not exist");
        }
    }

    @Override
    public boolean clearObjective() {
        if (linearObjectiveTerms == null || quadraticObjectiveTerms == null) {
            this.linearObjectiveTerms = null;
            this.quadraticObjectiveTerms = null;
            return true;
        } else {
            return false;
        }
    }

    // Both

    public Collection<Term> getObjectiveTerms() {
        int arrSize=(linearObjectiveTerms==null?0:linearObjectiveTerms.size())+(quadraticObjectiveTerms==null?0:quadraticObjectiveTerms.size());
        if(arrSize==0){
            return Collections.emptyList();
        }
        // This could be done without the extra array.
        List<Term> ret = new ArrayList(arrSize);
        if (linearObjectiveTerms != null) {
            ret.addAll(linearObjectiveTerms);
        }
        if (quadraticObjectiveTerms != null) {
            ret.addAll(quadraticObjectiveTerms);
        }
        return ret;
    }

    public boolean isObjectiveMax() {
        return isMax;
    }

    public boolean isObjectiveMin() {
        return !isMax;
    }

    public void setObjectiveMax(boolean isMax) {
        this.isMax = isMax;
    }

    // Constraints:
    // ////////////

    public List<Constraint> getConstraints() {
        return constraints;
    }

    public void add(Constraint constraint) {
        constraints.add(constraint);
    }

    // Solve Parameters:
    // /////////////////

    public Object getSolveParam(SolveParam param) {
        return solveParams.get(param);
    }

    public int getIntSolveParam(SolveParam param, Integer defaultValue) {
        Object o = getSolveParam(param);
        if (o == null) {
            if (defaultValue == null) {
                throw new MIPException("Parameter not specified: " + param);
            } else {
                return defaultValue.intValue();
            }
        }
        if (o instanceof Integer) {
            return ((Integer) o).intValue();
        }
        throw new MIPException("Parameter " + param + " not an Integer: " + o);
    }

    public int getIntSolveParam(SolveParam param, int defaultValue) {
        return getIntSolveParam(param, new Integer(defaultValue));
    }

    public int getIntSolveParam(SolveParam param) {
        return getIntSolveParam(param, null);
    }

    public double getDoubleSolveParam(SolveParam param, Double defaultValue) {
        Object o = getSolveParam(param);
        if (o == null) {
            if (defaultValue == null) {
                throw new MIPException("Parameter not specified: " + param);
            } else {
                return defaultValue.doubleValue();
            }
        }
        if (o instanceof Double) {
            return ((Double) o).doubleValue();
        }
        throw new MIPException("Parameter " + param + " not a Double: " + o);
    }

    public double getDoubleSolveParam(SolveParam param, double defaultValue) {
        return getDoubleSolveParam(param, new Double(defaultValue));
    }

    public double getDoubleSolveParam(SolveParam param) {
        return getDoubleSolveParam(param, null);
    }

    public boolean getBooleanSolveParam(SolveParam param, Boolean defaultValue) {
        Object o = getSolveParam(param);
        if (o == null) {
            if (defaultValue == null) {
                throw new MIPException("Parameter not specified: " + param);
            } else {
                return defaultValue.booleanValue();
            }
        }
        if (o instanceof Boolean) {
            return ((Boolean) o).booleanValue();
        }
        throw new MIPException("Parameter " + param + " not a Boolean: " + o);
    }

    public boolean getBooleanSolveParam(SolveParam param, boolean defaultValue) {
        return getBooleanSolveParam(param, new Boolean(defaultValue));
    }

    public boolean getBooleanSolveParam(SolveParam param) {
        return getBooleanSolveParam(param, null);
    }

    public String getStringSolveParam(SolveParam param, String defaultValue) {
        Object o = getSolveParam(param);
        if (o == null) {
            if (defaultValue == null) {
                throw new MIPException("Parameter not specified: " + param);
            } else {
                return defaultValue;
            }
        }
        if (o instanceof String) {
            return (String) o;
        }
        throw new MIPException("Parameter " + param + " not a String: " + o);
    }

    public String getStringSolveParam(SolveParam param) {
        return getStringSolveParam(param, null);
    }

    public void setSolveParam(SolveParam param, Object value) {
        if (param.getType().isAssignableFrom(value.getClass())) {
            solveParams.put(param, value);
        }
    }

    public void setSolveParam(SolveParam param, String value) {
        if (!param.isString()) {
            throw new MIPException("Parameter " + param + " is a " + param.getTypeDescription() + ", not String");
        }
        solveParams.put(param, value);
    }

    public Set<SolveParam> getSpecifiedSolveParams() {
        return Collections.unmodifiableSet(solveParams.keySet());
    }

    public void clearSolveParams() {
        solveParams.clear();
    }

    /**
     * Resets CPLEX/JOpt parameters to: Wall clock, 10 minute timelimit, strict
     * IIS calculation, no-output problem file, and zero missing proposed
     * variables.
     */
    public void resetDefaultSolveParams() {
        setSolveParam(SolveParam.CLOCK_TYPE, 2); // 1 CPU, 2 wall clock
        setSolveParam(SolveParam.TIME_LIMIT, 600d);// 10 minutes.

        // When we seed values of MIPs, this make all missing int/bool variables
        // == 0.
        setSolveParam(SolveParam.ZERO_MISSING_PROPOSED, true);
        setSolveParam(SolveParam.PROBLEM_FILE, "");
        setSolveParam(SolveParam.CONSTRAINT_BACKOFF_LIMIT, .1d);
        setSolveParam(SolveParam.DISPLAY_OUTPUT, false);
        setSolveParam(SolveParam.CALCULATE_CONFLICT_SET, true);
        setSolveParam(SolveParam.MAX_OBJ_THRESHOLD, 1e75);
        setSolveParam(SolveParam.MIN_OBJ_VALUE, -1e75);

        setSolveParam(SolveParam.SOLUTION_POOL_CAPACITY, 0);
        setSolveParam(SolveParam.SOLUTION_POOL_MODE, 0);

    }

    public boolean isSolveParamSpecified(SolveParam param) {
        return solveParams.containsKey(param);
    }

    // General Functions:
    // ///////////////////

    /**
     * Throws exception if n is greater than MIP.MAX_VALUE
     *
     * @param n
     */
    static void checkMax(double n) {
        if (n > MIP.MAX_VALUE) {
            throw new MIPException("Value (" + n + ") must be less than MIP.MAX_VALUE: " + MIP.MAX_VALUE);
        }
    }

    protected Object clone() throws CloneNotSupportedException {
        MIP ret = (MIP) super.clone();
        ret.constraints = new LinkedList<Constraint>();
        for (Constraint constraint : constraints) {
            ret.constraints.add(constraint.typedClone());
        }

        if (linearObjectiveTerms != null) {
            ret.linearObjectiveTerms = new ArrayList();
            for (LinearTerm t : getLinearObjectiveTerms()) {
                ret.linearObjectiveTerms.add(t.typedClone());
            }
        }

        if (quadraticObjectiveTerms != null) {
            ret.quadraticObjectiveTerms = new ArrayList();
            for (QuadraticTerm t : getQuadraticObjectiveTerms()) {
                ret.quadraticObjectiveTerms.add(t.typedClone());
            }
        }

        ret.vars = new HashMap();
        for (String name : vars.keySet()) {
            Variable val = vars.get(name);
            ret.vars.put(name, val.typedClone());
        }

        checkProposedHashMap();
        ret.proposedValuesForVars = new HashMap();
        for (Variable v : proposedValuesForVars.keySet()) {
            Object val = proposedValuesForVars.get(v);
            ret.proposedValuesForVars.put(ret.getVar(v.getName()), val);
        }
        ret.solveParams = new HashMap(solveParams);

        return ret;
    }

    public IMIP typedClone() {
        try {
            return (MIP) clone();
        } catch (CloneNotSupportedException e) {
            throw new MIPException("Problem in clone", e);
        }
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("Vars:\n");
        ArrayList<String> varNames = new ArrayList();
        varNames.addAll(getVars().keySet());
        Collections.sort(varNames);
        for (String var : varNames) {
            sb.append("    ").append(vars.get(var).toStringPretty()).append("\n");
        }
        sb.append("Objective Function: ").append(isMax ? "Max" : "Min").append(" ");

        boolean first = true;
        for (LinearTerm t : getSortedLinearObjectiveTerms()) {
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

        for (QuadraticTerm t : getSortedQuadraticObjectiveTerms()) {
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
        sb.append("\nConstraints:\n");
        for (Constraint cons : getConstraints()) {
            sb.append("    ").append(cons).append("\n");
        }
        return sb.toString();
    }

    @Override
    public int getNumConstraints() {
        return constraints.size();
    }

    @Override
    public boolean remove(Constraint constraint) {
        return constraints.remove(constraint);
    }

}
