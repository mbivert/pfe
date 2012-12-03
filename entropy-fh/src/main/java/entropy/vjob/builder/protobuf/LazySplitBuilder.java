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

package entropy.vjob.builder.protobuf;

import entropy.configuration.ManagedElementSet;
import entropy.configuration.VirtualMachine;
import entropy.vjob.LazySplit;
import entropy.vjob.builder.VJobElementBuilder;

import java.util.List;

/**
 * A builder for LazySplitBuilder.
 *
 * @author Fabien Hermenier
 */
public class LazySplitBuilder implements PBPlacementConstraintBuilder {

    @Override
    public String getIdentifier() {
        return "lSplit";
    }

    @Override
    public String getSignature() {
        return "lSplit(<vmset>,<vmset>)";
    }

    /**
     * Build a constraint.
     *
     * @param args the parameters of the constraint. Must be 2 non-empty set of virtual machines.
     * @return the constraint
     * @throws ConstraintBuilderException if an error occurred while building the constraint
     */
    @Override
    public LazySplit buildConstraint(VJobElementBuilder eBuilder, List<PBVJob.vjob.Param> args) throws ConstraintBuilderException {
        PBPlacementConstraintBuilders.ensureArity(this, args, 2);
        ManagedElementSet<VirtualMachine> vms1 = PBPlacementConstraintBuilders.makeVMs(eBuilder, args.get(0));
        PBPlacementConstraintBuilders.noEmptySets(args.get(0), vms1);
        ManagedElementSet<VirtualMachine> vms2 = PBPlacementConstraintBuilders.makeVMs(eBuilder, args.get(1));
        PBPlacementConstraintBuilders.noEmptySets(args.get(1), vms2);
        return new LazySplit(vms1, vms2);
    }
}
