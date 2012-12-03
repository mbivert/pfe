/**
 *  Copyright (c) 1999-2010, Ecole des Mines de Nantes
 *  All rights reserved.
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *      * Redistributions of source code must retain the above copyright
 *        notice, this list of conditions and the following disclaimer.
 *      * Redistributions in binary form must reproduce the above copyright
 *        notice, this list of conditions and the following disclaimer in the
 *        documentation and/or other materials provided with the distribution.
 *      * Neither the name of the Ecole des Mines de Nantes nor the
 *        names of its contributors may be used to endorse or promote products
 *        derived from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND ANY
 *  EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 *  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package choco.cp.solver.constraints.global;

import choco.cp.solver.variables.integer.IntVarEvent;
import choco.kernel.common.util.iterators.DisposableIntIterator;
import choco.kernel.common.util.tools.ArrayUtils;
import choco.kernel.memory.IEnvironment;
import choco.kernel.memory.IStateBitSet;
import choco.kernel.memory.IStateInt;
import choco.kernel.solver.ContradictionException;
import choco.kernel.solver.constraints.integer.AbstractLargeIntSConstraint;
import choco.kernel.solver.variables.integer.IntDomainVar;

import java.util.BitSet;

/**
 * Enforces two sets of variables values to be disjoint
 * created sofdem - 08/09/11
 *
 * @author Sophie Demassey
 */
public class Disjoint extends AbstractLargeIntSConstraint {


    /**
     * number of variables in the first set (group 0)
     */
    final int nbX;

    /**
     * the variable domains must be included in [0, nbValues-1]
     */
    final int nbValues;

    /**
     * candidates[group][value] = number of variables in group which can be assigned to value,
     * with group = 0,1 and value=0,...nbValues-1
     */
    IStateInt[][] candidates;
    /**
     * required[group][value] = 1 iff at least one variable in group is assigned to value,
     * with group = 0,1 and value=0,...nbValues-1
     */
    IStateBitSet[] required;


    /**
     * @param environment solver environment
     * @param x           first set of variables (group 0)
     * @param y           second set of variables (group 1)
     * @param nbValues    max variable value + 1
     */
    public Disjoint(IEnvironment environment, IntDomainVar[] x, IntDomainVar[] y, int nbValues) {
        super(ArrayUtils.append(x, y));
        this.nbX = x.length;
        this.nbValues = nbValues;
        candidates = new IStateInt[2][nbValues];
        required = new IStateBitSet[2];
        required[0] = environment.makeBitSet(nbValues);
        required[1] = environment.makeBitSet(nbValues);
        for (int v = 0; v < nbValues; v++) {
            candidates[0][v] = environment.makeInt(0);
            candidates[1][v] = environment.makeInt(0);
        }
    }

    @Override
    public int getFilteredEventMask(int idx) {
        return IntVarEvent.REMVAL_MASK + IntVarEvent.INSTINT_MASK;
    }

    /**
     * init the internal data according to the domain of a variable
     *
     * @param var   variable
     * @param group variable group
     */
    private void initVar(IntDomainVar var, int group) {
        if (var.isInstantiated()) {
            required[group].set(var.getVal());
        } else {
            DisposableIntIterator it = var.getDomain().getIterator();
            try {
                while (it.hasNext()) {
                    int val = it.next();
                    candidates[group][val].add(1);
                }
            } finally {
                it.dispose();
            }
        }
    }

    /**
     * init the internal data according to the variable domains
     */
    public void init() {
        int i = 0;
        for (; i < nbX; i++) {
            initVar(vars[i], 0);
        }
        for (; i < vars.length; i++) {
            initVar(vars[i], 1);
        }
    }

    @Override
    public void awake() throws ContradictionException {
        init();
        propagate();
    }

    /**
     * update the internal data and filter when a variable is newly instantiated
     * 1) fail if a variable in the other group is already instantiated to this value
     * 2) remove the value of the domains of all the variables of the other group
     *
     * @param val   the new assigned value
     * @param group the group of the new instantiated variable
     * @param other the other group (other = 1-group)
     * @throws ContradictionException when some variables in both groups are instantiated to the same value
     */
    public void setRequired(int val, int group, int other) throws ContradictionException {
        if (required[other].get(val)) fail();
        if (candidates[other][val].get() > 0) {
            int n = 0;
            int i = (other == 0) ? 0 : nbX;
            int end = (other == 0) ? nbX : vars.length;
            for (; i < end; i++) {
                if (vars[i].removeVal(val, this, false)) {
                    n++;
                    if (vars[i].isInstantiated()) {
                        setRequired(vars[i].getVal(), other, group);
                    }
                }
            }
            assert n == candidates[other][val].get();
            candidates[other][val].set(0);
        }
        required[group].set(val);
    }

    @Override
    public void awakeOnInst(int idx) throws ContradictionException {
        int group = (idx < nbX) ? 0 : 1;
        setRequired(vars[idx].getVal(), group, 1 - group);
    }

    @Override
    public void awakeOnRemovals(int idx, DisposableIntIterator deltaDomain) throws ContradictionException {
        int group = (idx < nbX) ? 0 : 1;
        while (deltaDomain.hasNext()) {
            candidates[group][deltaDomain.next()].add(-1);
        }
    }

    @Override
    public void propagate() throws ContradictionException {
        for (int v = 0; v < nbValues; v++) {
            if (required[0].get(v)) setRequired(v, 0, 1);
            if (required[1].get(v)) setRequired(v, 1, 0);
        }
    }

    @Override
    public boolean isSatisfied(int[] tuple) {
        BitSet valuesOne = new BitSet(nbValues);
        int i = 0;
        for (; i < nbX; i++) {
            valuesOne.set(tuple[i]);
        }
        for (; i < tuple.length; i++) {
            if (valuesOne.get(tuple[i])) return false;
        }
        return true;
    }

}
