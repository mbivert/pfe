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
import entropy.configuration.Node;
import entropy.configuration.VirtualMachine;
import entropy.vjob.Ban;
import entropy.vjob.builder.VJobElementBuilder;

import java.util.List;

/**
 * A builder to create Ban constraints.
 *
 * @author Fabien Hermenier
 */
public class BanBuilder implements PBPlacementConstraintBuilder {

    @Override
    public String getIdentifier() {
        return "ban";
    }

    @Override
    public String getSignature() {
        return "ban(<vmset>,<nodeset>)";
    }

    /**
     * Build a ban constraint.
     *
     * @param args must be 2 VJobset, first contains virtual machines and the second nodes. Each set must not be empty
     * @return a constraint
     * @throws ConstraintBuilderException if arguments are not compatible with the constraint
     */
    @Override
    public Ban buildConstraint(VJobElementBuilder eBuilder, List<PBVJob.vjob.Param> args) throws ConstraintBuilderException {
        PBPlacementConstraintBuilders.ensureArity(this, args, 2);
        ManagedElementSet<VirtualMachine> vms = PBPlacementConstraintBuilders.makeVMs(eBuilder, args.get(0));
        PBPlacementConstraintBuilders.noEmptySets(args.get(0), vms);

        ManagedElementSet<Node> ns = PBPlacementConstraintBuilders.makeNodes(eBuilder, args.get(1));
        PBPlacementConstraintBuilders.noEmptySets(args.get(1), ns);
        return new Ban(vms, ns);
    }
}
