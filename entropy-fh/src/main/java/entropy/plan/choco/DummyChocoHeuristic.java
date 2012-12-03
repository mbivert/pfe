/*
 * Copyright (c) Fabien Hermenier
 *
 * This file is part of Entropy.
 *
 * Entropy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Entropy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Entropy.  If not, see <http://www.gnu.org/licenses/>.
 */

package entropy.plan.choco;


import choco.cp.solver.search.integer.branching.AssignVar;
import choco.kernel.common.util.iterators.DisposableIterator;
import choco.kernel.solver.ContradictionException;
import choco.kernel.solver.constraints.AbstractSConstraint;
import choco.kernel.solver.constraints.SConstraint;
import choco.kernel.solver.search.IntBranchingTrace;
import entropy.configuration.Configuration;
import entropy.configuration.ManagedElementSet;
import entropy.configuration.Node;
import entropy.configuration.VirtualMachine;
import entropy.plan.PlanException;
import entropy.plan.durationEvaluator.DurationEvaluator;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A dummy placement heuristic.
 * Branch on all the variables in a static manner, and select the minimum value for each selected variable.
 *
 * @author Fabien Hermenier
 */
public class DummyChocoHeuristic extends DefaultReconfigurationProblem {

    private static final Logger logger = LoggerFactory
            .getLogger(ReconfigurationProblem.class);

    private IntBranchingTrace tr;

    private AssignVar currentBranching;


    public DummyChocoHeuristic(Configuration src, ManagedElementSet<VirtualMachine> run, ManagedElementSet<VirtualMachine> wait, ManagedElementSet<VirtualMachine> sleep, ManagedElementSet<VirtualMachine> stop, ManagedElementSet<Node> on, ManagedElementSet<Node> off, DurationEvaluator eval) throws PlanException {
        super(src, run, wait, sleep, stop, on, off, eval);
    }

    public DummyChocoHeuristic(Configuration src, ManagedElementSet<VirtualMachine> run, ManagedElementSet<VirtualMachine> wait, ManagedElementSet<VirtualMachine> sleep, ManagedElementSet<VirtualMachine> stop, ManagedElementSet<VirtualMachine> manageable, ManagedElementSet<Node> on, ManagedElementSet<Node> off, DurationEvaluator eval) throws PlanException {
        super(src, run, wait, sleep, stop, manageable, on, off, eval);
    }

    private boolean first = true;

    @Override
    public void propagate() throws ContradictionException {
        if (first) {
            first = false;
            DisposableIterator ite = getConstraintIterator();
            try {
                while (ite.hasNext()) {
                    AbstractSConstraint cstr = (AbstractSConstraint) ite.next();
                    cstr.awake();
                }
            } finally {
                ite.dispose();
            }
        }
        if (!isConsistent()) {
            logger.debug("-> not viable");
            throw ContradictionException.build();
        }

    }

    @Override
    public boolean isConsistent() {
        TObjectIntHashMap m = new TObjectIntHashMap();
        DisposableIterator<SConstraint> ctit = this.getConstraintIterator();
        boolean f = true;
        try {
            while (ctit.hasNext()) {
                AbstractSConstraint cstr = (AbstractSConstraint) ctit.next();
                String name = cstr.getClass().getSimpleName();
                m.put(name, m.get(name) + 1);
                if (!cstr.isConsistent()) {
                    logger.error("/!\\ " + cstr.getClass().getSimpleName() + ": " + cstr.pretty());
                    f = false;
                    return false;
                }
            }
        } finally {
            ctit.dispose();
        }
        //logger.error(m.toString());
        return f;
    }

    /*@Override
    public void launch() {

        currentBranching = (AssignVar) strategy.mainGoal;
        IntDomainVar v = null;
        do {
            try {
                v = selectVar();
                if (v == null) {
                    logger.debug("End of the search heuristic");
                    break;
                } else {
                    logger.error("Select var " + v.pretty());
                }
            } catch (ContradictionException e) {
                logger.error(e.getMessage(), e);
            }
            //Go to the first compatible value
            int val = selectVal(v);
            this.worldPush();
            while (val >= 0) {
                logger.error("\ttry " + val);
                try {
                    v.setVal(val);
                    v.canBeInstantiatedTo(val);

                    if (!isConsistent()) {
                        logger.debug("-> not viable");
                        try {
                            v.remVal(val);
                        } catch (ContradictionException e2) {
                            logger.error("Erreur while removing the value: " + e2.getMessage(), e2);
                        }
                    }
                } catch (ContradictionException e) {
                    try {
                        v.remVal(val);
                    } catch (ContradictionException e2) {
                        logger.error(e2.getMessage(), e2);
                    }
                }
                val = selectVal(v);
            }


        } while(v != null);
        if (this.isCompletelyInstantiated()) {
            setFeasible(Boolean.TRUE);
        }
    }



    private IntDomainVar selectVar() throws ContradictionException {
        IntDomainVar v = (IntDomainVar) currentBranching.selectBranchingObject();
        while (v == null) {
            currentBranching = (AssignVar) currentBranching.getNextBranching();
            if (currentBranching == null) {
                break;
            } else {
                v = (IntDomainVar) currentBranching.selectBranchingObject();
            }
        }
        return v;
    }

    private int selectVal(IntDomainVar v) {
        return currentBranching.getValSelector().getBestVal(v);
    }      */
}
