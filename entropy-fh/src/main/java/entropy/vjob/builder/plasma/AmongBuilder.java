/*
 * Copyright (c) 2010 Ecole des Mines de Nantes.
 *
 *      This file is part of Entropy.
 *
 *      Entropy is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU Lesser General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      (at your option) any later version.
 *
 *      Entropy is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU Lesser General Public License for more details.
 *
 *      You should have received a copy of the GNU Lesser General Public License
 *      along with Entropy.  If not, see <http://www.gnu.org/licenses/>.
 */

package entropy.vjob.builder.plasma;

import entropy.configuration.ManagedElementSet;
import entropy.configuration.Node;
import entropy.configuration.VirtualMachine;
import entropy.vjob.Among;
import entropy.vjob.Fence;
import entropy.vjob.PlacementConstraint;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A builder to make OneOf constraint.
 *
 * @author Fabien Hermenier
 */
public class AmongBuilder implements PlacementConstraintBuilder {

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
    public PlacementConstraint buildConstraint(List<VJobElement> args) throws ConstraintBuilderException {

        if (args.size() != 2) {
            throw new ConstraintBuilderException(this);
        }
        try {
            ManagedElementSet<VirtualMachine> vms = args.get(0).getElements();
            Set<ManagedElementSet<Node>> l = new HashSet<ManagedElementSet<Node>>();
            for (ExplodedSet<Node> grp : ((VJobMultiSet<Node>) args.get(1)).expand()) {
                l.add(grp);
            }
            if (vms.isEmpty() || l.isEmpty()) {
                throw new ConstraintBuilderException("Empty set not allowed");
            }
            if (l.size() == 1) {
                //Optimize, a Fence constraint is more efficient
                return new Fence(vms, l.iterator().next());
            }
            for (ManagedElementSet<Node> ns : l) {
                if (ns.isEmpty()) {
                    throw new ConstraintBuilderException("Empty set not allowed");
                }
            }
            return new Among(vms, l);
        } catch (ClassCastException e) {
            throw new ConstraintBuilderException(getSignature(), e);
        }
    }
}