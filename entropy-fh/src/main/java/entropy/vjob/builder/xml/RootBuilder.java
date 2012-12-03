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
import entropy.vjob.Root;
import entropy.vjob.builder.VJobElementBuilder;

import java.util.List;

/**
 * A Builder to make Root constraints.
 *
 * @author Fabien Hermenier
 */
public class RootBuilder implements XMLPlacementConstraintBuilder {

    @Override
    public String getIdentifier() {
        return "root";
    }

    @Override
    public String getSignature() {
        return "root(<vmset>)";
    }


    @Override
    public Root buildConstraint(VJobElementBuilder eBuilder, List<Param> args) throws ConstraintBuilderException {
        XMLPlacementConstraintBuilders.ensureArity(this, args, 1);
        ManagedElementSet<VirtualMachine> vms = XMLPlacementConstraintBuilders.makeVMs(eBuilder, args.get(0));
        XMLPlacementConstraintBuilders.noEmptySets(args.get(0), vms);
        return new Root(vms);
    }
}
