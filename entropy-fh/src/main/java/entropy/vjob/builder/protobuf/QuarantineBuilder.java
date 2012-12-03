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
import entropy.vjob.Quarantine;
import entropy.vjob.builder.VJobElementBuilder;

import java.util.List;

/**
 * A builder to make Quarantine constraints.
 *
 * @author Fabien Hermenier
 */
public class QuarantineBuilder implements PBPlacementConstraintBuilder {

    @Override
    public String getIdentifier() {
        return "quarantine";
    }

    @Override
    public String getSignature() {
        return "quarantine(<nodeset>)";
    }

    @Override
    public Quarantine buildConstraint(VJobElementBuilder eBuilder, List<PBVJob.vjob.Param> args) throws ConstraintBuilderException {
        PBPlacementConstraintBuilders.ensureArity(this, args, 1);
        ManagedElementSet<Node> ns = PBPlacementConstraintBuilders.makeNodes(eBuilder, args.get(0));
        if (ns.isEmpty()) {
            throw new ConstraintBuilderException(args.get(0) + " is an empty set");
        }
        return new Quarantine(ns);
    }
}
