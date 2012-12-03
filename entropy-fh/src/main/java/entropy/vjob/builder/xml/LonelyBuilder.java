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
import entropy.configuration.VirtualMachine;
import entropy.vjob.Lonely;
import entropy.vjob.builder.VJobElementBuilder;

import java.util.List;

/**
 * A builder for the placement constraint Lonely.
 *
 * @author Fabien Hermenier
 */
public class LonelyBuilder implements XMLPlacementConstraintBuilder {

    @Override
    public String getIdentifier() {
        return "lonely";
    }

    @Override
    public String getSignature() {
        return "lonely(<vmset>)";
    }

    /**
     * Build a constraint.
     *
     * @param args the parameters of the constraint. Must be one non-empty set of virtual machines.
     * @return the constraint
     * @throws ConstraintBuilderException if an error occurred while building the constraint
     */
    @Override
    public Lonely buildConstraint(VJobElementBuilder eBuilder, List<Param> args) throws ConstraintBuilderException {
        XMLPlacementConstraintBuilders.ensureArity(this, args, 1);
        ManagedElementSet<VirtualMachine> vms = XMLPlacementConstraintBuilders.makeVMs(eBuilder, args.get(0));
        XMLPlacementConstraintBuilders.noEmptySets(args.get(0), vms);
        return new Lonely(vms);
    }
}
