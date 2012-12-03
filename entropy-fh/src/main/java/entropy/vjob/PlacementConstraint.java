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
package entropy.vjob;

import entropy.configuration.Configuration;
import entropy.configuration.ManagedElementSet;
import entropy.configuration.Node;
import entropy.configuration.VirtualMachine;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.vjob.builder.protobuf.PBVJob;

/**
 * An interface to specify some constraints related to the planification of the action.
 *
 * @author Fabien Hermenier
 */
public interface PlacementConstraint {

    /**
     * Textual representation of the constraint.
     *
     * @return a String
     */
    String toString();

    /**
     * Apply the constraint on a planner module.
     */
    void inject(ReconfigurationProblem core);

    /**
     * Check that the constraint is satified in a configuration.
     *
     * @param cfg the configuration to check
     * @return true if the constraint is satistied
     */
    boolean isSatisfied(Configuration cfg);

    /**
     * Get the virtual machines involved in the constraints.
     *
     * @return a set of virtual machines.
     */
    ManagedElementSet<VirtualMachine> getAllVirtualMachines();

    /**
     * Get the nodes explicitely involved in the constraints.
     *
     * @return a set of nodes that may be empty
     */
    ManagedElementSet<Node> getNodes();

    /**
     * Get all the mis-placed virtual machines in a configuration.
     *
     * @param cfg the configuration
     * @return a set of virtual machines where their position violate the constraint.
     */
    ManagedElementSet<VirtualMachine> getMisPlaced(Configuration cfg);

    /**
     * Serialize the constraint into XML.
     *
     * @return a non-empty String containing XML
     */
    String toXML();

    /**
     * Serialize the constraint to a protobuf Constraint message.
     *
     * @return a protobuf constraint message
     */
    PBVJob.vjob.Constraint toProtobuf();

    /**
     * The possible types for the constraint.
     */
    enum Type {
        /**
         * The constraint restricts the placement of VMs to nodes. Nodes and VMs are given
         */
        absolute,
        /**
         * The constraint restrict the relative placement of VMs or the hosting capacities of nodes.
         */
        relative
    }

    /**
     * Get the type of the constraint.
     *
     * @return a possible type
     */
    Type getType();
}
