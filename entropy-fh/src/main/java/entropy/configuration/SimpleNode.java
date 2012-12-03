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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Fabien Hermenier
 */
public class SimpleNode implements Node, Cloneable {

    private String name;

    private int nbOfCPUs;

    private int memCapacity;

    private int cpuCapacity;

    private String ip;

    private String mac;

    private String currentPlatform;

    private Map<String, Map<String, String>> availablePlatforms;

    /**
     * Make a node without specifying any capacity.
     *
     * @param name the identifier of the node
     */
    public SimpleNode(String name) {
        this(name, 1, 0, 0, null, null);
    }

    /**
     * Make a node and specify its resource capacity
     *
     * @param name           the identifier of the node
     * @param nbOfCPUs       the number of physical CPUs available to the VMs
     * @param cpuCapacity    the capacity of each physical CPU
     * @param memoryCapacity the memory capacity of each node
     */
    public SimpleNode(String name, int nbOfCPUs, int cpuCapacity, int memoryCapacity) {
        this(name, nbOfCPUs, cpuCapacity, memoryCapacity, null, null);
    }

    /**
     * Make a node and specify its resource capacity
     *
     * @param name           the identifier of the node
     * @param nbOfCPUs       the number of physical CPUs available to the VMs
     * @param cpuCapacity    the capacity of each physical CPU
     * @param memoryCapacity the memory capacity of each node
     * @param ip             the IP address of the node
     * @param mac            the MAC address of the node
     */
    public SimpleNode(String name, int nbOfCPUs, int cpuCapacity, int memoryCapacity, String ip, String mac) {
        this.name = name;
        this.nbOfCPUs = nbOfCPUs;
        this.memCapacity = memoryCapacity;
        this.cpuCapacity = cpuCapacity;
        this.ip = ip;
        this.mac = mac;

        this.availablePlatforms = new HashMap<String, Map<String, String>>();
        this.currentPlatform = null;
    }

    @Override
    public void rename(String n) {
        this.name = n;
    }

    @Override
    public void setNbOfCPUs(int nb) {
        this.nbOfCPUs = nb;
    }

    @Override
    public int getNbOfCPUs() {
        return nbOfCPUs;
    }

    @Override
    public int getCPUCapacity() {
        return cpuCapacity;
    }

    @Override
    public int getMemoryCapacity() {
        return memCapacity;
    }

    @Override
    public void setCPUCapacity(int c) {
        this.cpuCapacity = c;
    }

    @Override
    public void setMemoryCapacity(int m) {
        this.memCapacity = m;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || (!(o instanceof Node))) {
            return false;
        }
        Node that = (Node) o;
        return name.equals(that.getName());
    }

    /**
     * Return the hash code of the node name
     *
     * @return {@code getName().hashCode();}
     */
    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public Node clone() {
        SimpleNode clone = new SimpleNode(name, nbOfCPUs, cpuCapacity, memCapacity);
        clone.ip = ip;
        clone.mac = mac;
        clone.currentPlatform = currentPlatform;
        for (String p : availablePlatforms.keySet()) {
            Map<String, String> cpy = new HashMap<String, String>();
            for (Map.Entry<String, String> op : availablePlatforms.get(p).entrySet()) {
                cpy.put(op.getKey(), op.getValue());
            }
            clone.availablePlatforms.put(p, cpy);
        }
        return clone;
    }

    @Override
    public String getMACAddress() {
        return this.mac;
    }

    @Override
    public String getIPAddress() {
        return this.ip;
    }

    @Override
    public void setMACAddress(String mac) {
        this.mac = mac;
    }

    @Override
    public void setIPAddress(String ip) {
        this.ip = ip;
    }


    @Override
    public String getCurrentPlatform() {
        return this.currentPlatform;
    }

    @Override
    public boolean setCurrentPlatform(String k) {
        if (this.availablePlatforms.containsKey(k)) {
            this.currentPlatform = k;
            return true;
        }
        return false;
    }

    @Override
    public boolean addPlatform(String id) {
        if (!this.availablePlatforms.containsKey(id)) {
            addPlatform(id, new HashMap<String, String>());
            return true;
        }
        return false;
    }

    @Override
    public void addPlatform(String id, Map<String, String> options) {
        this.availablePlatforms.put(id, options);
        if (currentPlatform == null) {
            this.currentPlatform = id;
        }
    }

    @Override
    public Set<String> getAvailablePlatforms() {
        return this.availablePlatforms.keySet();
    }

    @Override
    public Map<String, String> getPlatformOptions(String p) {
        return this.availablePlatforms.get(p);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder(name);
        b.append("[nbCpus=").append(nbOfCPUs);
        b.append(", cpu=").append(cpuCapacity);
        b.append(", mem=").append(memCapacity);
        if (currentPlatform != null) {
            b.append(", platform=").append(currentPlatform);
        }
        return b.append("]").toString();
    }

    /**
     * Unsupported operation.
     */
    @Override
    public String getStartupDriverID() {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported operation.
     */
    @Override
    public String getShutdownDriverID() {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported operation.
     */
    @Override
    public String getMigrationDriverID() {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported operation.
     */
    @Override
    public String getHypervisorID() {
        throw new UnsupportedOperationException();
    }


    /**
     * Unsupported operation.
     */
    @Override
    public void setStartupDriverID(String id) {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported operation.
     */
    @Override
    public void setShutdownDriverID(String id) {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported operation.
     */
    @Override
    public void setMigrationDriverID(String id) {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported operation.
     */
    @Override
    public void setHypervisorID(String id) {
        throw new UnsupportedOperationException();
    }


    /**
     * Unsupported operation.
     */
    @Override
    public String getSuspendDriverID() {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported operation.
     */
    @Override
    public void setSuspendDriverID(String id) {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported operation.
     */
    @Override
    public void setResumeDriverID(String id) {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported operation.
     */
    @Override
    public String getResumeDriverID() {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported operation.
     */
    @Override
    public String getRunDriverID() {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported operation.
     */
    @Override
    public void setRunDriverID(String id) {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported operation.
     */
    @Override
    public String getStopDriverID() {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported operation.
     */
    @Override
    public void setStopDriverID(String id) {
        throw new UnsupportedOperationException();
    }

}
