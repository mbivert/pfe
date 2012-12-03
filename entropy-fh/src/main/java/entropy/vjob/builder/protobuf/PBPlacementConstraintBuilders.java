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

import entropy.configuration.*;
import entropy.vjob.builder.VJobElementBuilder;

import java.util.*;

/**
 * A set of tools to convert BtrpOperand to Entropy sets and checkers.
 *
 * @author Fabien Hermenier
 */
public final class PBPlacementConstraintBuilders {

    /**
     * No instantiation.
     */
    private PBPlacementConstraintBuilders() {
    }

    /**
     * Check the number of parameters for a constraint.
     * if the size of {@code elems} is not equals to {@code x}, then an exception is throw and specifies
     * the excepted signature
     *
     * @param b     the constraint to check
     * @param elems the parameters given to the constraint
     * @param x     the expected arity
     * @throws ConstraintBuilderException if the actual arity is not equals to {@code x}
     */
    public static void ensureArity(PBPlacementConstraintBuilder b, List<PBVJob.vjob.Param> elems, int x) throws ConstraintBuilderException {
        int s = elems.size();
        if (s != x) {
            throw new ConstraintBuilderException(new StringBuilder(b.getSignature()).append(" expects 2 arguments. ").append(s).toString());
        }
    }

    /**
     * Chech the set is not empty.
     *
     * @param buf a message that will prefix the exception
     * @param s   the set
     * @throws ConstraintBuilderException if the set is empty. {@code buf} is added at the beginning of the error message
     */
    public static void noEmptySets(PBVJob.vjob.Param buf, Collection s) throws ConstraintBuilderException {
        if (s.isEmpty()) {
            throw new ConstraintBuilderException(new StringBuilder(buf.toString()).append(" is an empty set ").toString());
        }
    }

    /**
     * Convert an operand to a set of virtual machines sets.
     *
     * @param elems the operand to convert
     * @return a list of set of virtual machines
     * @throws ConstraintBuilderException if the conversion is not possible (incompatible type or degree)
     */
    public static List<ManagedElementSet<VirtualMachine>> makeVirtualMachinesSet(Configuration cfg, PBVJob.vjob.Param elems) throws ConstraintBuilderException {
        if (elems.getType() != PBVJob.vjob.Param.Type.SET || elems.getSet().getType() != PBVJob.vjob.Set.Type.SET || elems.getSet().getSet(0).getType() != PBVJob.vjob.Set.Type.VM) {
            throw new ConstraintBuilderException(elems + " must be a set of vmsets");
        }
        List<ManagedElementSet<VirtualMachine>> set = new ArrayList<ManagedElementSet<VirtualMachine>>();
        for (PBVJob.vjob.Set e : elems.getSet().getSetList()) {
            ManagedElementSet<VirtualMachine> vms = new SimpleManagedElementSet<VirtualMachine>();
            for (String ref : e.getRefList()) {
                VirtualMachine vm = cfg.getAllVirtualMachines().get(ref);
                if (vm == null) {
                    throw new ConstraintBuilderException("Virtual machine '" + ref + "' unknown");
                }
                vms.add(vm);
            }
            set.add(vms);
        }
        return set;
    }

    /**
     * Convert an operand to a set of virtual machines.
     *
     * @param elems the operand to convert
     * @return a set of virtual machines
     * @throws ConstraintBuilderException if the conversion is not possible (incompatible type or degree)
     */
    public static ManagedElementSet<VirtualMachine> makeVMs(VJobElementBuilder eBuilder, PBVJob.vjob.Param elems) throws ConstraintBuilderException {
        ManagedElementSet<VirtualMachine> vms = new SimpleManagedElementSet<VirtualMachine>();
        if (elems.getType() != PBVJob.vjob.Param.Type.SET || elems.getSet().getType() != PBVJob.vjob.Set.Type.VM) {
            throw new ConstraintBuilderException(elems + " must be a set of virtual machines");
        }
        for (String ref : elems.getSet().getRefList()) {
            VirtualMachine vm = eBuilder.matchVirtualMachine(ref);
            if (vm == null) {
                throw new ConstraintBuilderException("Virtual machine '" + ref + "' unknown");
            }
            vms.add(vm);
        }
        return vms;
    }

    /**
     * Convert an operand to a set of node sets.
     *
     * @param elems the operand to convert
     * @return a list of set of node
     * @throws ConstraintBuilderException if the conversion is not possible (incompatible type or degree)
     */
    public static Set<ManagedElementSet<Node>> makeNodesSet(VJobElementBuilder elementBuilder, PBVJob.vjob.Param elems) throws ConstraintBuilderException {
        if (elems.getType() != PBVJob.vjob.Param.Type.SET || elems.getSet().getType() != PBVJob.vjob.Set.Type.SET || elems.getSet().getSet(0).getType() != PBVJob.vjob.Set.Type.NODE) {
            throw new ConstraintBuilderException(elems + " must be a set of nodesets");
        }
        Set<ManagedElementSet<Node>> set = new HashSet<ManagedElementSet<Node>>();
        for (PBVJob.vjob.Set e : elems.getSet().getSetList()) {
            ManagedElementSet<Node> nodes = new SimpleManagedElementSet<Node>();
            for (String ref : e.getRefList()) {
                Node n = elementBuilder.matchAsNode(ref);
                if (n == null) {
                    throw new ConstraintBuilderException("Node '" + ref + "' unknown");
                }
                nodes.add(n);
            }
            set.add(nodes);
        }
        return set;
    }

    /**
     * Convert an operand to a set of nodes.
     *
     * @param elems the operand to convert
     * @return a set of nodes
     * @throws ConstraintBuilderException if the conversion is not possible (incompatible type or degree)
     */
    public static ManagedElementSet<Node> makeNodes(VJobElementBuilder eBuilder, PBVJob.vjob.Param elems) throws ConstraintBuilderException {
        if (elems.getType() != PBVJob.vjob.Param.Type.SET || elems.getSet().getType() != PBVJob.vjob.Set.Type.NODE) {
            throw new ConstraintBuilderException(elems + " must be a set of nodes");
        }
        ManagedElementSet<Node> nodes = new SimpleManagedElementSet<Node>();
        for (String ref : elems.getSet().getRefList()) {
            Node n = eBuilder.matchAsNode(ref);
            if (n == null) {
                throw new ConstraintBuilderException("Node '" + ref + "' unknown");
            }
            nodes.add(n);
        }
        return nodes;
    }

    /**
     * Convert an operand to an int
     *
     * @param elem the operand to convert
     * @return an int
     * @throws ConstraintBuilderException if the conversion is not possible (incompatible type or degree)
     */
    public static int makeInt(PBVJob.vjob.Param elem) throws ConstraintBuilderException {
        if (elem.getType() != PBVJob.vjob.Param.Type.INT) {
            throw new ConstraintBuilderException(elem + " must be a number");
        }
        return elem.getVal();
    }

    /**
     * Convert an operand to a double
     *
     * @param elem the operand to convert
     * @return a double
     * @throws ConstraintBuilderException if the conversion is not possible (incompatible type or degree)
     */
    public static double makeDouble(PBVJob.vjob.Param elem) throws ConstraintBuilderException {
        if (elem.getType() != PBVJob.vjob.Param.Type.DOUBLE) {
            throw new ConstraintBuilderException(elem + " must be a number");
        }
        return elem.getDval();
    }
}
