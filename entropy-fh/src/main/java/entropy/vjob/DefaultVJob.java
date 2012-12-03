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
import entropy.configuration.SimpleManagedElementSet;
import entropy.configuration.VirtualMachine;
import gnu.trove.THashSet;

import java.util.Set;

/**
 * Basic, language neutral, implementatio of a VJob
 *
 * @author Fabien Hermenier
 */
public class DefaultVJob implements VJob {

    private String id;

    private ManagedElementSet<VirtualMachine> vms;

    private Set<PlacementConstraint> cstrs;

    public DefaultVJob(String id) {
        this.vms = new SimpleManagedElementSet<VirtualMachine>();
        this.cstrs = new THashSet<PlacementConstraint>();
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public ManagedElementSet<VirtualMachine> getVirtualMachines() {
        ManagedElementSet<VirtualMachine> all = vms.clone();
        for (PlacementConstraint c : cstrs) {
            all.addAll(c.getAllVirtualMachines());
        }
        return all;
    }

    @Override
    public ManagedElementSet<Node> getNodes() {
        ManagedElementSet<Node> all = new SimpleManagedElementSet<Node>();
        for (PlacementConstraint c : cstrs) {
            all.addAll(c.getNodes());
        }
        return all;
    }

    @Override
    public boolean addConstraint(PlacementConstraint c) {
        return this.cstrs.add(c);
    }

    @Override
    public boolean removeConstraint(PlacementConstraint c) {
        return this.cstrs.remove(c);
    }

    @Override
    public Set<PlacementConstraint> getConstraints() {
        return this.cstrs;
    }

    @Override
    public boolean addVirtualMachines(ManagedElementSet<VirtualMachine> e) {
        return vms.addAll(e);
    }

    @Override
    public boolean addVirtualMachine(VirtualMachine vm) {
        return vms.add(vm);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VJob that = (VJob) o;
        return id().equals(that.id()) &&
                vms.equals(that.getVirtualMachines()) &&
                getNodes().equals(that.getNodes()) &&
                cstrs.equals(that.getConstraints());
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (vms != null ? vms.hashCode() : 0);
        result = 31 * result + (cstrs != null ? cstrs.hashCode() : 0);
        return result;
    }

    public String toString() {
        StringBuilder b = new StringBuilder(this.id).append('(');
        b.append(getVirtualMachines().size()).append(" VM(s); ");
        b.append(getNodes().size()).append(" node(s); ");
        b.append(cstrs.size()).append(" constraint(s)").append(')');
        return b.toString();
    }
}
