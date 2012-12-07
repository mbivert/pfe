/*
 * Copyright (c) Fabien Hermenier
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

package entropy.configuration;

import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation of Configuration.
 * <p/>
 *
 * @author Fabien Hermenier
 */
public class SimpleConfiguration implements Configuration, Cloneable {

    private static final int IDX_SHIFT = -1;

    private static final int RUNNINGS = 1;

    private static final int SLEEPINGS = 2;

    private static final int WAITINGS = 3;

    private static final int ONLINES = 1;

    private static final int OFFLINES = 2;

    private ManagedElementSet<Node> allNodes;

    private ManagedElementSet<VirtualMachine> allVMs;

    private ManagedElementSet<Node>[] nodesByState;

    private ManagedElementSet<VirtualMachine>[] vmsByState;

    private TObjectIntHashMap<VirtualMachine> vmState;

    private TObjectIntHashMap<Node> nodeState;

    private Map<VirtualMachine, Node> vmPlace;

    private Map<Node, ManagedElementSet<VirtualMachine>>[] hosted;

    /**
     * Build an empty configuration.
     */
    public SimpleConfiguration() {
        this.vmsByState = new ManagedElementSet[3];
        this.vmState = new TObjectIntHashMap<VirtualMachine>();
        this.nodeState = new TObjectIntHashMap<Node>();

        nodesByState = new ManagedElementSet[2];
        for (int i = 0; i < nodesByState.length; i++) {
            nodesByState[i] = new SimpleManagedElementSet<Node>();
        }

        for (int i = 0; i < vmsByState.length; i++) {
            vmsByState[i] = new SimpleManagedElementSet<VirtualMachine>();
        }
        this.hosted = new HashMap[2];
        for (int i = 0; i < hosted.length; i++) {
            this.hosted[i] = new HashMap<Node, ManagedElementSet<VirtualMachine>>();
        }
        this.allNodes = new SimpleManagedElementSet<Node>();
        this.allVMs = new SimpleManagedElementSet<VirtualMachine>();
        this.vmPlace = new HashMap<VirtualMachine, Node>();
    }

    private boolean switchState(Node n, int newState) {
        int curState = nodeState.get(n);
        if (curState == newState) {
            return true;
        }
        if (curState > 0) {
            nodesByState[curState + IDX_SHIFT].remove(n);
        }
        nodeState.put(n, newState);
        return nodesByState[newState + IDX_SHIFT].add(n);
    }

    private boolean replace(VirtualMachine vm, Node newNode, int newState) {
        Node oldNode = vmPlace.put(vm, newNode);
        int oldState = vmState.get(vm);
        if (oldState == 0) { //Unknown VM
            allVMs.add(vm);
        }
        if (oldState == 0 || (oldState != newState)) {
            if (oldState != 0) {
                vmsByState[oldState + IDX_SHIFT].remove(vm);
            }
            vmsByState[newState + IDX_SHIFT].add(vm);
            vmState.put(vm, newState);
        }

        //Change the state
        if (oldNode != null) {
            hosted[oldState + IDX_SHIFT].get(oldNode).remove(vm);
        }
        hosted[newState + IDX_SHIFT].get(newNode).add(vm);
        return true;
    }

    @Override
    public boolean setRunOn(VirtualMachine vm, Node node) {
        return nodeState.get(node) == ONLINES && replace(vm, node, RUNNINGS);
    }

    @Override
    public boolean setSleepOn(VirtualMachine vm, Node node) {
        return nodeState.get(node) == ONLINES && replace(vm, node, SLEEPINGS);
    }

    @Override
    public void addWaiting(VirtualMachine vm) {
        int curState = vmState.get(vm);
        if (curState == 0) { //Not in the configuration.
            allVMs.add(vm);
            vmState.put(vm, WAITINGS);
            vmsByState[WAITINGS + IDX_SHIFT].add(vm);
        }

        if (curState > 0 && (curState + IDX_SHIFT != WAITINGS)) { //Already in and non-waiting
            //Change state
            Node oldNode = vmPlace.remove(vm);
            vmsByState[curState + IDX_SHIFT].remove(vm);
            vmsByState[WAITINGS + IDX_SHIFT].add(vm);
            vmState.put(vm, WAITINGS);
            //Change hoster
            hosted[curState + IDX_SHIFT].get(oldNode).remove(vm);
        }
    }

    @Override
    public void remove(VirtualMachine vm) {
        int curState = vmState.remove(vm);
        if (curState > 0) {
            vmsByState[curState + IDX_SHIFT].remove(vm);
            allVMs.remove(vm);
            Node oldNode = vmPlace.remove(vm);
            if (oldNode != null) {
                hosted[curState + IDX_SHIFT].get(oldNode).remove(vm);
            }
        }
    }

    @Override
    public boolean remove(Node n) {
        int curState = nodeState.remove(n);
        if (curState > 0) {
            if (isUsed(n)) {
                return false;
            }
            for (int i = 0; i < hosted.length; i++) {
                hosted[i].remove(n);
            }
            nodesByState[curState + IDX_SHIFT].remove(n);
            allNodes.remove(n);
        }
        return true;
    }

    @Override
    public void addOnline(Node n) {
        if (nodeState.get(n) != ONLINES) {
            hosted[RUNNINGS + IDX_SHIFT].put(n, new SimpleManagedElementSet<VirtualMachine>());
            hosted[SLEEPINGS + IDX_SHIFT].put(n, new SimpleManagedElementSet<VirtualMachine>());
        }
        this.allNodes.add(n);
        switchState(n, ONLINES);
    }

    /**
     * Check whether a node is hosting a virtual machine or not.
     *
     * @param n the node to check
     * @return {@code true} if the node host running or sleeping virtual machines.
     */
    private boolean isUsed(Node n) {
        for (int i = 0; i < hosted.length; i++) {
            ManagedElementSet<VirtualMachine> s = hosted[i].get(n);
            if (s != null && !s.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean addOffline(Node n) {
        if (isUsed(n)) {
            return false;
        }
        hosted[RUNNINGS + IDX_SHIFT].put(n, new SimpleManagedElementSet<VirtualMachine>());
        hosted[SLEEPINGS + IDX_SHIFT].put(n, new SimpleManagedElementSet<VirtualMachine>());
        allNodes.add(n);
        return switchState(n, OFFLINES);
    }

    @Override
    public ManagedElementSet<Node> getOnlines() {
        return nodesByState[ONLINES + IDX_SHIFT];
    }

    @Override
    public ManagedElementSet<Node> getOfflines() {
        return nodesByState[OFFLINES + IDX_SHIFT];
    }

    @Override
    public ManagedElementSet<VirtualMachine> getRunnings() {
        return vmsByState[RUNNINGS + IDX_SHIFT];
    }

    @Override
    public ManagedElementSet<VirtualMachine> getSleepings() {
        return vmsByState[SLEEPINGS + IDX_SHIFT];
    }

    @Override
    public ManagedElementSet<VirtualMachine> getWaitings() {
        return vmsByState[WAITINGS + IDX_SHIFT];
    }


    @Override
    public ManagedElementSet<VirtualMachine> getSleepings(Node n) {
        return hosted[SLEEPINGS + IDX_SHIFT].get(n);
    }

    @Override
    public ManagedElementSet<VirtualMachine> getRunnings(Node n) {
        return hosted[RUNNINGS + IDX_SHIFT].get(n);
    }

    @Override
    public ManagedElementSet<VirtualMachine> getRunnings(ManagedElementSet<Node> ns) {
        ManagedElementSet<VirtualMachine> vms = new SimpleManagedElementSet<VirtualMachine>();
        for (Node n : ns) {
            vms.addAll(getRunnings(n));
        }
        return vms;
    }


    @Override
    public ManagedElementSet<VirtualMachine> getAllVirtualMachines() {
        return allVMs;
    }

    @Override
    public ManagedElementSet<Node> getAllNodes() {
        return allNodes;
    }

    @Override
    public Node getSleepingLocation(VirtualMachine vm) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Node getRunningLocation(VirtualMachine vm) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Node getLocation(VirtualMachine vm) {
        return vmPlace.get(vm);
    }

    @Override
    public boolean isOnline(Node n) {
        return nodeState.get(n) == ONLINES;
    }

    @Override
    public boolean isOffline(Node n) {
        return nodeState.get(n) == OFFLINES;
    }

    @Override
    public boolean isRunning(VirtualMachine vm) {
        return vmState.get(vm) == RUNNINGS;
    }

    @Override
    public boolean isWaiting(VirtualMachine vm) {
        return vmState.get(vm) == WAITINGS;
    }

    @Override
    public boolean isSleeping(VirtualMachine vm) {
        return vmState.get(vm) == SLEEPINGS;
    }

    @Override
    public Configuration clone() {
        //TODO: a better implementation using internal structure should be possible
        final Configuration c = new SimpleConfiguration();
        for (Node n : getOfflines()) {
            c.addOffline(n);
        }

        for (VirtualMachine vm : getWaitings()) {
            c.addWaiting(vm);
        }

        for (Node n : getOnlines()) {
            c.addOnline(n);
            for (VirtualMachine vm : getRunnings(n)) {
                c.setRunOn(vm, n);
            }
            for (VirtualMachine vm : getSleepings(n)) {
                c.setSleepOn(vm, n);
            }
        }
        return c;
    }

    @Override
    public int hashCode() {
        return vmState.hashCode() + nodeState.hashCode() * 31 + vmPlace.hashCode() * 31 * 31;
    }

    @Override
    public boolean contains(Node n) {
        return nodeState.get(n) > 0;
    }

    @Override
    public boolean contains(VirtualMachine vm) {
        return vmState.get(vm) > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (o == this) {
            return true;
        } else if (o instanceof Configuration) {
            Configuration ref = (Configuration) o;
            if (!ref.getOfflines().equals(getOfflines())
                    || !ref.getOnlines().equals(getOnlines())
                    || !ref.getWaitings().equals(getWaitings())) {
                return false;
            }
            for (Node n : ref.getOnlines()) {
                if (!ref.getRunnings(n).equals(getRunnings(n))
                        || !ref.getSleepings(n).equals(getSleepings(n))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Textual representation of the configuration.
     *
     * @return the textual representation
     */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        for (Node n : allNodes) {
            if (nodeState.get(n) == OFFLINES) {
                buf.append("(").append(n.getName()).append(")");
            } else {
                buf.append(n.getName());
            }
            buf.append(":");
            if (this.getRunnings(n).isEmpty() && this.getSleepings(n).isEmpty()) {
                buf.append(" - ");
            }
            for (VirtualMachine vm : this.getRunnings(n)) {
                buf.append(" ");
                buf.append(vm.getName());
            }
            for (VirtualMachine vm : this.getSleepings(n)) {
                buf.append(" (");
                buf.append(vm.getName());
                buf.append(")");
            }
            buf.append("\n");
        }
        buf.append("FARM");
        for (VirtualMachine vm : this.getWaitings()) {
            buf.append(" ");
            buf.append(vm.getName());
        }
        buf.append("\n");
        return buf.toString();
    }
}
