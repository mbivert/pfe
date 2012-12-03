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
import entropy.vjob.PlacementConstraint;
import entropy.vjob.VJob;
import entropy.vjob.builder.VJobFileSerializer;
import entropy.vjob.builder.VJobSerializer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Set;

/**
 * Serializer to store a BtrPlaceVJob into a XML format that is valid wrt. the VJob XSD.
 *
 * @author Fabien Hermenier
 */
public class XmlVJobSerializer extends VJobFileSerializer implements VJobSerializer {

    /**
     * Singleton.
     */
    private static final XmlVJobSerializer INSTANCE = new XmlVJobSerializer();

    /**
     * Get the singleton.
     *
     * @return the unique instance of this class.
     */
    public static XmlVJobSerializer getInstance() {
        return INSTANCE;
    }

    @Override
    public void serialize(VJob vjob, OutputStream out) throws IOException {
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out));
        writer.print("<vjob id=\"");
        writer.print(vjob.id());
        writer.print("\">");

        //First the virtual machines
        writer.print("<vms>");
        for (VirtualMachine vm : vjob.getVirtualMachines()) {
            writer.print(getVMDeclaration(vm));
        }
        writer.print("</vms>");

        //Then, the constraints
        writer.print("<constraints>");
        for (PlacementConstraint c : vjob.getConstraints()) {
            writer.print(c.toXML());
        }
        writer.print("</constraints>");
        writer.print("</vjob>");
        writer.flush();
    }

    /**
     * Serialize a virtual machine declaration into XML.
     *
     * @param vm the virtual machine to serialize
     * @return a non-empty string containing XML
     */
    public static String getVMDeclaration(VirtualMachine vm) {
        StringBuilder b = new StringBuilder();
        b.append("<vm id=\"");
        b.append(vm.getName());
        b.append("\" tpl=\"");
        b.append(vm.getTemplate());
        b.append("\"");
        b.append("><options>");
        for (String opt : vm.getOptions()) {
            String v = vm.getOption(opt);
            if (v == null) {
                b.append("<option id=\"");
                b.append(opt);
                b.append("\"/>");
            } else {
                b.append("<option id=\"");
                b.append(opt);
                b.append("\">");
                b.append(v);
                b.append("</option>");
            }
        }
        b.append("</options></vm>");
        return b.toString();
    }

    /**
     * Serialize a virtual machine that has been already declared into XML.
     *
     * @param vm the virtual machine to serialize
     * @return a non-empty string containing XML
     */
    public static String getVMRef(VirtualMachine vm) {
        StringBuilder b = new StringBuilder();
        b.append("<vm ref=\"").append(vm.getName()).append("\"/>");
        return b.toString();
    }

    /**
     * Serialize a node into XML.
     *
     * @param n the node to serialize
     * @return a non-empty string containing XML
     */
    public static String getNodeRef(Node n) {
        StringBuilder b = new StringBuilder();
        b.append("<node ref=\"").append(n.getName()).append("\"/>");
        return b.toString();
    }

    /**
     * Serialize a set of virtual machine already declared into XML.
     *
     * @param vms the set of virtual machines
     * @return a non-empty string containing XML
     */
    public static String getVMset(ManagedElementSet<VirtualMachine> vms) {
        StringBuilder b = new StringBuilder();
        b.append("<set>");
        for (VirtualMachine vm : vms) {
            b.append(getVMRef(vm));
        }
        b.append("</set>");
        return b.toString();
    }

    /**
     * Serialize a set of nodes into XML.
     *
     * @param nodes the set of nodes
     * @return a non-empty string containing XML
     */
    public static String getNodeset(ManagedElementSet<Node> nodes) {
        StringBuilder b = new StringBuilder("<set>");
        for (Node n : nodes) {
            b.append(getNodeRef(n));
        }
        b.append("</set>");
        return b.toString();
    }

    /**
     * Serialize a set of node sets into XML.
     *
     * @param snodes the set of node sets
     * @return a non-empty string containing XML
     */
    public static String getNodeBigSet(Set<ManagedElementSet<Node>> snodes) {
        StringBuilder b = new StringBuilder("<set>");
        for (ManagedElementSet<Node> nodes : snodes) {
            b.append(getNodeset(nodes));
        }
        b.append("</set>");
        return b.toString();
    }

    /**
     * Serialize a set of virtual machine sets already declared into XML.
     *
     * @param svms the set of virtual machines sets
     * @return a non-empty string containing XML
     */
    public static String getVMBigSet(Set<ManagedElementSet<VirtualMachine>> svms) {
        StringBuilder b = new StringBuilder("<set>");
        for (ManagedElementSet<VirtualMachine> vms : svms) {
            b.append(getVMset(vms));
        }
        b.append("</set>");
        return b.toString();
    }

    /**
     * Serialize an integer.
     *
     * @param v the integer value
     * @return a non-empty string containing XML
     */
    public static String getInt(int v) {
        return new StringBuilder("<int value=\"").append(v).append("\"/>").toString();
    }

    /**
     * Serialize an double.
     *
     * @param v the double value
     * @return a non-empty string containing XML
     */
    public static String getDouble(double v) {
        return new StringBuilder("<double value=\"").append(v).append("\"/>").toString();
    }

    /**
     * Serialize an string.
     *
     * @param v the value
     * @return a non-empty string containing XML
     */
    public static String getDouble(String v) {
        return new StringBuilder("<string value=\"").append(v).append("\"/>").toString();
    }

    @Override
    public String getFileExtension() {
        return "xml";
    }
}
