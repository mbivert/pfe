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

package entropy.vjob.builder.xml;

import entropy.configuration.ManagedElementSet;
import entropy.configuration.Node;
import entropy.configuration.VirtualMachine;
import entropy.vjob.Among;
import entropy.vjob.Fence;
import entropy.vjob.PlacementConstraint;
import entropy.vjob.builder.VJobElementBuilder;

import java.util.List;
import java.util.Set;

/**
 * A builder to make Among constraint.
 *
 * @author Fabien Hermenier
 */
public class AmongBuilder implements XMLPlacementConstraintBuilder {

    @Override
    public String getIdentifier() {
        return "among";
    }

    @Override
    public String getSignature() {
        return "among(<vmset>, <multinodeset>)";
    }

    /**
     * Build a constraint.
     *
     * @param args the argument. Must be a non-empty set of virtual machines and a multiset of nodes with
     *             at least two non-empty sets. If the multi set contains only one set, a {@code Fence} constraint is created
     * @return the constraint
     * @throws ConstraintBuilderException if an error occurred while building the constraint
     */
    @Override
    public PlacementConstraint buildConstraint(VJobElementBuilder eBuilder, List<Param> args) throws ConstraintBuilderException {
        XMLPlacementConstraintBuilders.ensureArity(this, args, 2);
        ManagedElementSet<VirtualMachine> vms = XMLPlacementConstraintBuilders.makeVMs(eBuilder, args.get(0));
        XMLPlacementConstraintBuilders.noEmptySets(args.get(0), vms);
        Set<ManagedElementSet<Node>> nss = XMLPlacementConstraintBuilders.makeNodesSet(eBuilder, args.get(1));
        XMLPlacementConstraintBuilders.noEmptySets(args.get(1), nss);
        for (ManagedElementSet<Node> s : nss) {
            XMLPlacementConstraintBuilders.noEmptySets(args.get(1), s);
        }
        if (nss.size() == 1) {
            return new Fence(vms, nss.iterator().next());
        }
        return new Among(vms, nss);
    }
}