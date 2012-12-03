/*
 * Copyright (c) 2010 Fabien Hermenier
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

package entropy.plan.choco.actionModel;

import choco.kernel.solver.variables.integer.IntDomainVar;
import entropy.configuration.Node;

/**
 * An action model that denote a node where its state can be managed through constraints.
 *
 * @author Fabien Hermenier
 */
public abstract class ManageableNodeActionModel extends NodeActionModel {

    public ManageableNodeActionModel(Node n) {
        super(n);
    }

    /**
     * Indicate the state of the node at the end of the reconfiguration process.
     * If the variable is not instantiated, then the state
     * of the node is uncertain.
     *
     * @return if instantiated, {@value 1} if the node will be online. {@value 0} if the node will be offline
     */
    public abstract IntDomainVar getState();
}
