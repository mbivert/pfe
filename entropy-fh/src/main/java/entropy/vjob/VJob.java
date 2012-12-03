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

package entropy.vjob;

import entropy.configuration.ManagedElementSet;
import entropy.configuration.Node;
import entropy.configuration.VirtualMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Interface to specify a VJob.
 * A Vjob is composed of virtual machines and physical nodes.
 *
 * @author Fabien Hermenier
 */
public interface VJob {

    Logger logger = LoggerFactory.getLogger("VJob");

    /**
     * The identifier of the vjob.
     *
     * @return a String
     */
    String id();

    /**
     * The virtual machines involved in the vjob.
     *
     * @return a set of virtual machines. May be empty
     */
    ManagedElementSet<VirtualMachine> getVirtualMachines();

    /**
     * The nodes involved in the vjob.
     *
     * @return a set of nodes. May be empty
     */
    ManagedElementSet<Node> getNodes();

    /**
     * Add a placement constraint on the vjob.
     *
     * @param c the placement constraint
     * @return {@code true} if the constraint is added
     */
    boolean addConstraint(PlacementConstraint c);

    /**
     * Remove a placement constraint on the vjob.
     *
     * @param c the constraint to remove
     * @return {@code true} if the constraint is removed.
     */
    boolean removeConstraint(PlacementConstraint c);

    /**
     * List all the placement constraint of the vjob.
     *
     * @return a list of constraints. May be empty
     */
    Set<PlacementConstraint> getConstraints();

    /**
     * Add virtual machines to the vjob.
     *
     * @param e the virtual machines to add
     * @return {@code true} if the virtual machines where added
     */
    boolean addVirtualMachines(ManagedElementSet<VirtualMachine> e);

    /**
     * Add a virtual machine.
     *
     * @param vm the virtual machine to add
     * @return {@code true} if the virtual machine is added
     */
    boolean addVirtualMachine(VirtualMachine vm);
}
