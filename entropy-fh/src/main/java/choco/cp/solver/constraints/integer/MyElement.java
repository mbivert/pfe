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

package choco.cp.solver.constraints.integer;

import choco.cp.solver.variables.integer.IntVarEvent;
import choco.kernel.common.util.iterators.DisposableIntIterator;
import choco.kernel.common.util.tools.StringUtils;
import choco.kernel.solver.ContradictionException;
import choco.kernel.solver.constraints.integer.AbstractBinIntSConstraint;
import choco.kernel.solver.variables.integer.IntDomainVar;

public final class MyElement extends AbstractBinIntSConstraint {
    int[] lval;
    int cste;

    /**
     * To indicate the ordering of the index value
     */
    public static enum Sort {
        /**
         * Values are unordered
         */
        none,
        /**
         * Values are in the increasing order
         */
        ascending,
        /**
         * Values are in the decreasing order
         */
        descending,
        /**
         * Let the constraint detect the ordering, if any
         */
        detect
    }

    /**
     * The current ordering
     */
    private Sort s;

    public MyElement(IntDomainVar index, int[] values, IntDomainVar var, int offset, Sort s) {
        super(index, var);
        this.lval = values;
        this.cste = offset;
        this.s = s;
    }

    public MyElement(IntDomainVar index, int[] values, IntDomainVar var) {
        this(index, values, var, 0, Sort.none);
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public String toString() {
        return "Element";
    }

    public int getFilteredEventMask(int idx) {
        if (idx == 0)
            return IntVarEvent.INSTINT_MASK + IntVarEvent.REMVAL_MASK;
        else return IntVarEvent.REMVAL_MASK;
    }

    /**
     * <i>Propagation:</i>
     * Propagating the constraint until local consistency is reached.
     *
     * @throws choco.kernel.solver.ContradictionException
     *          contradiction exception
     */

    public void propagate() throws ContradictionException {
        this.updateIndexFromValue();
    }

    public String pretty() {
        return (this.v1.pretty() + " = nth(" + this.v0.pretty() + ", " + StringUtils.pretty(this.lval) + ")");
    }

    protected void updateValueFromIndex() throws ContradictionException {
        int minVal = Integer.MAX_VALUE;
        int maxVal = Integer.MIN_VALUE;

        if (s == Sort.descending) {
            this.v1.updateInf(this.lval[v0.getSup() - cste], this, false);
            this.v1.updateSup(this.lval[v0.getInf() - cste], this, false);
        } else if (s == Sort.ascending) {
            this.v1.updateInf(this.lval[v0.getInf() - cste], this, false);
            this.v1.updateSup(this.lval[v0.getSup() - cste], this, false);

        } else {
            DisposableIntIterator iter = this.v0.getDomain().getIterator();
            boolean isDsc = true;
            boolean isAsc = true;
            int prev = this.lval[v0.getInf() - cste];
            //System.err.println("Parse from " + prev);
            try {
                while (iter.hasNext()) {
                    int index = iter.next();
                    int val = this.lval[index - cste];
                    //System.err.println("current: " + val + " prev=" + prev + " " + isDsc + " " + isAsc);
                    if (minVal > val) {
                        minVal = val;
                    }
                    if (maxVal < val) {
                        maxVal = val;
                    }
                    if (s == Sort.detect) {
                        if (val > prev) {
                            isDsc = false;
                        }
                        if (val < prev) {
                            isAsc = false;
                        }
                    }
                    prev = val;
                }
                if (s == Sort.detect && isDsc) {
                    //ChocoLogging.getBranchingLogger().warning(isDsc + " " + isAsc + " Index is sorted decreasingly:" + Arrays.toString(lval));
                    //System.exit(0);
                    s = Sort.descending;
                } else if (s == Sort.detect && isAsc) {
                    //ChocoLogging.getBranchingLogger().warning(isDsc + " " + isAsc + " Index is sorted increasingly:" + Arrays.toString(lval));
                    s = Sort.ascending;
                } else if (s == Sort.detect && !isAsc && !isDsc) {
                    //ChocoLogging.getBranchingLogger().warning(isDsc + " " + isAsc + " Index is not sorted "+ Arrays.toString(lval));
                    s = Sort.none;
                }
                this.v1.updateInf(minVal, this, false);
                this.v1.updateSup(maxVal, this, false);
            } finally {
                iter.dispose();
            }
        }
        // todo : <hcambaza> : why it does not perform AC on the value variable ?
    }

    protected void updateIndexFromValue() throws ContradictionException {
        int minFeasibleIndex = Math.max(0 + cste, this.v0.getInf());
        int maxFeasibleIndex = Math.min(this.v0.getSup(), lval.length - 1 + cste);

        if (minFeasibleIndex > maxFeasibleIndex) {
            this.fail();
        }

        boolean forceAwake = !this.v1.hasEnumeratedDomain();

        while ((this.v0.canBeInstantiatedTo(minFeasibleIndex))
                && !(this.v1.canBeInstantiatedTo(lval[minFeasibleIndex - this.cste])))
            minFeasibleIndex++;
        this.v0.updateInf(minFeasibleIndex, this, forceAwake);

        while ((this.v0.canBeInstantiatedTo(maxFeasibleIndex))
                && !(this.v1.canBeInstantiatedTo(lval[maxFeasibleIndex - this.cste])))
            maxFeasibleIndex--;
        this.v0.updateSup(maxFeasibleIndex, this, forceAwake);

        if (this.v0.hasEnumeratedDomain()) {
            for (int i = minFeasibleIndex + 1; i <= maxFeasibleIndex - 1; i++) {
                if (this.v0.canBeInstantiatedTo(i) && !(this.v1.canBeInstantiatedTo(this.lval[i - this.cste])))
                    this.v0.removeVal(i, this, forceAwake);
            }
        }
    }

    public void awake() throws ContradictionException {
        this.updateIndexFromValue();
        this.updateValueFromIndex();
    }

    public void awakeOnInst(int i) throws ContradictionException {
        if (i == 0) {
            this.v1.instantiate(this.lval[this.v0.getVal() - this.cste], this, false);
        }

//    else
//      this.updateIndexFromValue();
    }

    public void awakeOnRem(int i, int x) throws ContradictionException {
        if (i == 0)
            this.updateValueFromIndex();
        else
            this.updateIndexFromValue();
    }

    public Boolean isEntailed() {
        if (this.v1.isInstantiated()) {
            boolean allVal = true;
            boolean oneVal = false;
            for (int val = v0.getInf(); val <= v0.getSup(); val = v0.getNextDomainValue(val)) {
                boolean b = (val - this.cste) >= 0
                        && (val - this.cste) < this.lval.length
                        && this.lval[val - this.cste] == this.v1.getVal();
                allVal &= b;
                oneVal |= b;
            }
            if (allVal) return Boolean.TRUE;
            if (oneVal) return null;
        } else {
            boolean b = false;
            for (int val = v0.getInf(); val <= v0.getSup() && !b; val = v0.getNextDomainValue(val)) {
                if ((val - this.cste) >= 0 &&
                        (val - this.cste) < this.lval.length) {
                    b = this.v1.canBeInstantiatedTo(this.lval[val - this.cste]);
                }
            }
            if (b) return null;
        }
        return Boolean.FALSE;
    }

    public boolean isSatisfied(int[] tuple) {
        if (tuple[0] - this.cste >= lval.length ||
                tuple[0] - this.cste < 0) return false;
        return this.lval[tuple[0] - this.cste] == tuple[1];
    }
}
