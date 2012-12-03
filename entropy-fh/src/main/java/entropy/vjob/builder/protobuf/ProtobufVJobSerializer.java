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
import entropy.configuration.VirtualMachine;
import entropy.vjob.VJob;
import entropy.vjob.builder.VJobFileSerializer;
import entropy.vjob.builder.VJobSerializer;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

/**
 * A BtrpPlaceVJob serializer that create protobuf messages.
 *
 * @author Fabien Hermenier
 */
public class ProtobufVJobSerializer extends VJobFileSerializer implements VJobSerializer {

    /**
     * The instance of the singleton.
     */
    private static final ProtobufVJobSerializer INSTANCE = new ProtobufVJobSerializer();

    /**
     * Private constructor, managed by the singleton.
     */
    private ProtobufVJobSerializer() {
    }

    /**
     * Get the unique instance.
     *
     * @return an instance
     */
    public static ProtobufVJobSerializer getInstance() {
        return INSTANCE;
    }

    @Override
    public void serialize(VJob vjob, OutputStream out) throws IOException {
        PBVJob.vjob.Builder b = PBVJob.vjob.newBuilder();
        b.setId(vjob.id());

        //All the virtual machines
        for (VirtualMachine vm : vjob.getVirtualMachines()) {
            PBVJob.vjob.VMDecl.Builder vmB = PBVJob.vjob.VMDecl.newBuilder();
            vmB.setId(vm.getName());
            if (vm.getTemplate() != null) {
                vmB.setTplName(vm.getTemplate());
            }
            for (String opt : vm.getOptions()) {
                PBVJob.vjob.Option.Builder optB = PBVJob.vjob.Option.newBuilder();
                optB.setId(opt);
                String v = vm.getOption(opt);
                if (v != null) {
                    optB.setValue(v);

                }
                vmB.addOption(optB.build());
            }
            b.addVm(vmB.build());
        }

        //All the constraints
        for (entropy.vjob.PlacementConstraint c : vjob.getConstraints()) {
            b.addConstraint(c.toProtobuf());
        }
        b.build().writeTo(out);
    }

    /**
     * Serialize a set of virtual machines.
     *
     * @param vms the set of virtual machines
     * @return the resulting set of virtual machines
     */
    public static PBVJob.vjob.Set getVMset(ManagedElementSet<VirtualMachine> vms) {
        PBVJob.vjob.Set.Builder b = PBVJob.vjob.Set.newBuilder();
        b.setType(PBVJob.vjob.Set.Type.VM);
        for (VirtualMachine vm : vms) {
            b.addRef(vm.getName());
        }
        return b.build();
    }

    /**
     * Serialize a set of nodes
     *
     * @param nodes the set of nodes
     * @return the resulting set of nodes
     */
    public static PBVJob.vjob.Set getNodeset(ManagedElementSet<Node> nodes) {
        PBVJob.vjob.Set.Builder b = PBVJob.vjob.Set.newBuilder();
        b.setType(PBVJob.vjob.Set.Type.NODE);
        for (Node n : nodes) {
            b.addRef(n.getName());
        }
        return b.build();
    }

    /**
     * Serialize a set of node sets.
     *
     * @param snodes the set of node sets
     * @return the resulting set of node sets.
     */
    public static PBVJob.vjob.Set getNodeBigSet(Set<ManagedElementSet<Node>> snodes) {
        PBVJob.vjob.Set.Builder b = PBVJob.vjob.Set.newBuilder();
        b.setType(PBVJob.vjob.Set.Type.SET);
        for (ManagedElementSet<Node> nodes : snodes) {
            b.addSet(getNodeset(nodes));
        }
        return b.build();
    }

    /**
     * Serialize a set of virtual machines sets.
     *
     * @param svms the set of virtual machines sets
     * @return the resulting set of virtual machines sets.
     */
    public static PBVJob.vjob.Set getVMBigSet(Set<ManagedElementSet<VirtualMachine>> svms) {
        PBVJob.vjob.Set.Builder b = PBVJob.vjob.Set.newBuilder();
        b.setType(PBVJob.vjob.Set.Type.SET);
        for (ManagedElementSet<VirtualMachine> vms : svms) {
            b.addSet(getVMset(vms));
        }
        return b.build();
    }

    @Override
    public String getFileExtension() {
        return "pbd";
    }
}
