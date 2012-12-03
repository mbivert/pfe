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
import entropy.configuration.SimpleManagedElementSet;
import entropy.configuration.VirtualMachine;
import entropy.vjob.builder.VJobElementBuilder;

import java.util.*;

/**
 * A set of tools to convert BtrpOperand to Entropy sets and checkers.
 *
 * @author Fabien Hermenier
 */
public final class XMLPlacementConstraintBuilders {

    /**
     * No instantiation.
     */
    private XMLPlacementConstraintBuilders() {
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
    public static void ensureArity(XMLPlacementConstraintBuilder b, List<Param> elems, int x) throws ConstraintBuilderException {
        int s = elems.size();
        if (s != x) {
            throw new ConstraintBuilderException(new StringBuilder(b.getSignature()).append(" expects " + x + " arguments. ").append(s).append(" given.").toString());
        }
    }

    /**
     * Chech the set is not empty.
     *
     * @param buf a message that will prefix the exception
     * @param s   the set
     * @throws ConstraintBuilderException if the set is empty. {@code buf} is added at the beginning of the error message
     */
    public static void noEmptySets(Object buf, Collection s) throws ConstraintBuilderException {
        if (s.isEmpty()) {
            throw new ConstraintBuilderException(new StringBuilder(buf.toString()).append(" is an empty set ").toString());
        }
    }

    /**
     * Convert an operand to a set of virtual machines sets.
     *
     * @param p the operand to convert
     * @return a list of set of virtual machines
     * @throws ConstraintBuilderException if the conversion is not possible (incompatible type or degree)
     */
    public static List<ManagedElementSet<VirtualMachine>> makeVirtualMachinesSet(VJobElementBuilder eBuilder, Param p) throws ConstraintBuilderException {
        if (p.type != Param.Type.set) {
            throw new ConstraintBuilderException(p + " must be a set");
        }
        List<ManagedElementSet<VirtualMachine>> set = new ArrayList<ManagedElementSet<VirtualMachine>>();
        List<Param> ps = (List<Param>) p.value;
        for (Param px : ps) {
            set.add(makeVMs(eBuilder, px));
        }
        return set;

    }

    /**
     * Convert an operand to a set of virtual machines.
     *
     * @param p the operand to convert
     * @return a set of virtual machines
     * @throws ConstraintBuilderException if the conversion is not possible (incompatible type or degree)
     */
    public static ManagedElementSet<VirtualMachine> makeVMs(VJobElementBuilder eBuilder, Param p) throws ConstraintBuilderException {
        if (p.type != Param.Type.set) {
            throw new ConstraintBuilderException(p + " must be a set");
        }
        List<Param> ps = (List<Param>) p.value;
        ManagedElementSet<VirtualMachine> nodes = new SimpleManagedElementSet<VirtualMachine>();
        for (Param p2 : ps) {
            nodes.add(matchExistingVirtualMachine(eBuilder, p2));
        }
        return nodes;
    }

    /**
     * Convert an operand to a set of node sets.
     *
     * @param elems the operand to convert
     * @return a list of set of node
     * @throws ConstraintBuilderException if the conversion is not possible (incompatible type or degree)
     */
    public static Set<ManagedElementSet<Node>> makeNodesSet(VJobElementBuilder elementBuilder, Param elems) throws ConstraintBuilderException {
        if (elems.type != Param.Type.set) {
            throw new ConstraintBuilderException(elems + " must be a set");
        }
        Set<ManagedElementSet<Node>> set = new HashSet<ManagedElementSet<Node>>();
        List<Param> ps = (List<Param>) elems.value;
        for (Param px : ps) {
            set.add(makeNodes(elementBuilder, px));
        }
        return set;
    }

    /**
     * Convert an operand to a set of nodes.
     *
     * @param p the operand to convert
     * @return a set of nodes
     * @throws ConstraintBuilderException if the conversion is not possible (incompatible type or degree)
     */
    public static ManagedElementSet<Node> makeNodes(VJobElementBuilder eBuilder, Param p) throws ConstraintBuilderException {
        if (p.type != Param.Type.set) {
            throw new ConstraintBuilderException(p + " must be a set");
        }
        List<Param> ps = (List<Param>) p.value;
        ManagedElementSet<Node> nodes = new SimpleManagedElementSet<Node>();
        for (Param p2 : ps) {
            nodes.add(matchNode(eBuilder, p2));
        }
        return nodes;
    }

    public static Node matchNode(VJobElementBuilder eBuilder, Param p) throws ConstraintBuilderException {
        if (p.type != Param.Type.node) {
            throw new ConstraintBuilderException(p + " must be a node. Given is '" + p.type + "'");
        }
        String ref = p.value.toString();
        Node n = eBuilder.matchAsNode(ref);
        if (n == null) {
            throw new ConstraintBuilderException("Node " + ref + " unknown");
        }
        return n;
    }

    public static VirtualMachine matchExistingVirtualMachine(VJobElementBuilder eBuilder, Param p) throws ConstraintBuilderException {
        if (p.type != Param.Type.vm) {
            throw new ConstraintBuilderException(p + " must be a vm");
        }
        String ref = p.value.toString();
        VirtualMachine vm = eBuilder.matchVirtualMachine(ref);
        if (vm == null) {
            throw new ConstraintBuilderException("Virtual machine " + ref + " unknown");
        }
        return vm;
    }

    /**
     * Convert an operand to an int
     *
     * @param p the operand to convert
     * @return an int
     * @throws ConstraintBuilderException if the conversion is not possible (incompatible type or degree)
     */
    public static int makeInt(Param p) throws ConstraintBuilderException {
        if (p.type != Param.Type.integer) {
            throw new ConstraintBuilderException(p + " must be an integer");
        }
        return (Integer) p.value;
    }

    public static double makeDouble(Param p) throws ConstraintBuilderException {
        System.err.println(p);
        if (p.type != Param.Type.real) {
            throw new ConstraintBuilderException(p + " must be a real number");
        }
        return (Double) p.value;
    }

    public static String makeString(Param p) throws ConstraintBuilderException {
        if (p.type != Param.Type.string) {
            throw new ConstraintBuilderException(p + " must be a string");
        }
        return (String) p.value;
    }
}
