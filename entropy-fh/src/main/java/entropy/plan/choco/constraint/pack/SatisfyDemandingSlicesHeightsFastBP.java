/*
 * Copyright (c) Fabien Hermenier
 *
 *        This file is part of Entropy.
 *
 *        Entropy is free software: you can redistribute it and/or modify
 *        it under the terms of the GNU Lesser General Public License as published by
 *        the Free Software Foundation, either version 3 of the License, or
 *        (at your option) any later version.
 *
 *        Entropy is distributed in the hope that it will be useful,
 *        but WITHOUT ANY WARRANTY; without even the implied warranty of
 *        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *        GNU Lesser General Public License for more details.
 *
 *        You should have received a copy of the GNU Lesser General Public License
 *        along with Entropy.  If not, see <http://www.gnu.org/licenses/>.
 */

package entropy.plan.choco.constraint.pack;

import choco.kernel.solver.ContradictionException;
import choco.kernel.solver.variables.integer.IntDomainVar;
import entropy.configuration.Configuration;
import entropy.configuration.ManagedElementSet;
import entropy.configuration.Node;
import entropy.configuration.VirtualMachine;
import entropy.plan.Plan;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.plan.choco.actionModel.slice.DemandingSlice;
import entropy.plan.choco.actionModel.slice.SliceComparator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A constraint to assign a host with a sufficient amount of resources to satisfy
 * all the heights of the demanding slices.
 * The constraint is based on two dynamic bin packing constraints.
 *
 * @author Fabien Hermenier
 */
public class SatisfyDemandingSlicesHeightsFastBP implements SatisfyDemandingSliceHeights {

    private FastBinPacking cPack;

    private FastBinPacking mPack;

    public SatisfyDemandingSlicesHeightsFastBP() {

    }

    @Override
    public void add(ReconfigurationProblem rp) {
        //SetVar []bins = new SetVar[rp.getNodes().length];
        List<DemandingSlice> demandingCPU = new ArrayList<DemandingSlice>();//rp.getDemandingSlices());
        List<DemandingSlice> demandingMem = new ArrayList<DemandingSlice>();//rp.getDemandingSlices());


        //Remove slices with an height = 0
        for (DemandingSlice d : rp.getDemandingSlices()) {
            if (d.getCPUheight() != 0) {
                demandingCPU.add(d);
            }
            if (d.getMemoryheight() != 0) {
                demandingMem.add(d);
            }
        }

        Node[] ns = rp.getNodes();
        if (!demandingCPU.isEmpty()) {
            IntDomainVar[] demandCPU = new IntDomainVar[demandingCPU.size()];
            IntDomainVar[] assignsCPU = new IntDomainVar[demandingCPU.size()];

            IntDomainVar[] capaCPU = new IntDomainVar[ns.length];
            for (int i = 0; i < ns.length; i++) {
                capaCPU[i] = rp.getUsedCPU(ns[i]);
            }

            //Sort in descending order
            Collections.sort(demandingCPU, new SliceComparator(false, SliceComparator.ResourceType.cpuConsumption));
            for (int i = 0; i < demandingCPU.size(); i++) {
                DemandingSlice s = demandingCPU.get(i);
                demandCPU[i] = rp.createIntegerConstant(""/*i + " #dCPU"*/, s.getCPUheight());
                assignsCPU[i] = s.hoster();
            }

            cPack = new FastBinPacking(rp.getEnvironment(),
                    capaCPU,
                    demandCPU,
                    assignsCPU
            );
            rp.post(cPack);
        } else {
            for (Node n : ns) {
                try {
                    rp.getUsedCPU(n).setVal(0);
                } catch (ContradictionException e) {
                    Plan.logger.error("No CPU demand but unable to set the CPU usage to 0: " + e.getMessage(), e);
                }
            }
        }

        if (!demandingMem.isEmpty()) {
            IntDomainVar[] demandMem = new IntDomainVar[demandingMem.size()];
            IntDomainVar[] assignsMem = new IntDomainVar[demandingMem.size()];
            IntDomainVar[] capaMem = new IntDomainVar[ns.length];
            for (int i = 0; i < ns.length; i++) {
                capaMem[i] = rp.getUsedMem(ns[i]);
            }

            Collections.sort(demandingMem, new SliceComparator(false, SliceComparator.ResourceType.memoryConsumption));
            for (int i = 0; i < demandingMem.size(); i++) {
                DemandingSlice task = demandingMem.get(i);
                demandMem[i] = rp.createIntegerConstant(""/*task.getName() + "#dMem"*/, task.getMemoryheight());
                assignsMem[i] = task.hoster();
            }

            mPack = new FastBinPacking(rp.getEnvironment(),
                    capaMem,
                    demandMem,
                    assignsMem
            );
            rp.post(mPack);
        } else {
            for (Node n : ns) {
                try {
                    rp.getUsedMem(n).setVal(0);
                } catch (ContradictionException e) {
                    Plan.logger.error("No memory demand but unable to set the memory usage to 0: " + e.getMessage(), e);
                }
            }
        }
        Plan.logger.debug("SatisfyDemandingSlicesHeightsFastBP branched");
    }

    @Override
    public CustomPack getCoreCPUPacking() {
        return cPack;
    }

    @Override
    public CustomPack getCoreMemPacking() {
        return mPack;
    }

    @Override
    public boolean isSatisfied(Configuration cfg) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ManagedElementSet<VirtualMachine> getAllVirtualMachines() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ManagedElementSet<VirtualMachine> getMisPlaced(Configuration cfg) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getRemainingCPU(int bin) {
        return cPack == null ? Integer.MAX_VALUE : cPack.getRemainingSpace(bin);
    }

    @Override
    public int getRemainingMemory(int bin) {
        return mPack == null ? Integer.MAX_VALUE : mPack.getRemainingSpace(bin);
    }
}
