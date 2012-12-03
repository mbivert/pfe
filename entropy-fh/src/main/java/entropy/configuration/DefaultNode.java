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

package entropy.configuration;


import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * An Hypervisor.
 *
 * @author Fabien Hermenier
 * @deprecated use {@link SimpleNode} instead
 */
public final class DefaultNode extends DefaultManagedElement implements Node {

    /**
     * Identifier for the number of CPUs.
     */
    public static final String CPU_NB = "cpu#nb";

    /**
     * Identifier for the total amount of memory.
     */
    public static final String MEMORY_TOTAL = "memory#total";

    /**
     * Identifier for the capacity of each CPU.
     */
    public static final String CPU_CAPACITY = "cpu#capacity";

    /**
     * The identifier of the hypervisor.
     */
    private String hypervisorID = null;

    /**
     * The identifier of the migration driver compatible with this node.
     */
    private String migrationDriverID = null;

    /**
     * The identifier of the shutdown driver for this node.
     */
    private String shutdownDriverID = null;

    /**
     * The identifier of the startup driver for this node.
     */
    private String startupDriverID = null;

    /**
     * The identifier of the suspend driver for this node.
     */
    private String suspendDriverID = null;

    /**
     * The identifier of the resume driver for this node.
     */
    private String resumeDriverID = null;

    /**
     * The identifier of the run driver for this node.
     */
    private String runDriverID = null;

    /**
     * The identifier of the stop driver for this node.
     */
    private String stopDriverID = null;


    /**
     * The MAC Address for this node.
     */
    private String macAddress = null;

    /**
     * The IP address for this node.
     */
    private String ipAddress = null;

    private String currentPlatform;

    private Map<String, Map<String, String>> availablePlatforms;

    /**
     * Instanciate a new node.
     *
     * @param id          The node identifier, must be unique
     * @param nbCPUs      The number of CPUs dedicated for virtual machines
     * @param cpuCapacity The capacity of each CPU
     * @param memoryTotal The amount of memory dedicated for virtual machines
     */
    public DefaultNode(String id, int nbCPUs, int cpuCapacity, int memoryTotal) {
        super(id);
        // make default values
        updateValue(DefaultNode.CPU_NB, nbCPUs);
        updateValue(DefaultNode.CPU_CAPACITY, cpuCapacity);
        updateValue(DefaultNode.MEMORY_TOTAL, memoryTotal);
        this.availablePlatforms = new HashMap<String, Map<String, String>>();
    }

    /**
     * Copy constructor. Make a deep copy of common values stored into a node
     *
     * @param ref the original node
     */
    public DefaultNode(Node ref) {
        this(ref.getName(), ref.getNbOfCPUs(), ref.getCPUCapacity(), ref.getMemoryCapacity());

        //Copy of the others private attributes
        this.setHypervisorID(ref.getHypervisorID());
        this.setMACAddress(ref.getMACAddress());
        this.setIPAddress(ref.getIPAddress());
        this.setMigrationDriverID(ref.getMigrationDriverID());
        this.setStartupDriverID(ref.getStartupDriverID());
        this.setShutdownDriverID(ref.getShutdownDriverID());
        this.setSuspendDriverID(ref.getSuspendDriverID());
        this.setResumeDriverID(ref.getResumeDriverID());
        this.setRunDriverID(ref.getRunDriverID());
        this.setStopDriverID(ref.getStopDriverID());
    }

    /**
     * Compare this instance to another.
     *
     * @param o The instance to compare with
     * @return true if o is an instance of Node and has the same name
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof Node) {
            return getName().equals(((Node) o).getName());
        }
        return false;
    }

    /**
     * Get the hashcode.
     *
     * @return the hashcode of the hostname
     */
    @Override
    public int hashCode() {
        return this.getName().hashCode();
    }

    /**
     * Return the number of CPUs dedicated for Virtual Machines.
     *
     * @return a positive integer
     */
    public int getNbOfCPUs() {
        return (Integer) getValue(DefaultNode.CPU_NB);
    }

    /**
     * Return the amount of memory dedicated for Virtual Machines.
     *
     * @return the amount of memory in MB
     */
    public int getMemoryCapacity() {
        return (Integer) getValue(DefaultNode.MEMORY_TOTAL);
    }

    @Override
    public void setNbOfCPUs(int nb) {
        updateValue(DefaultNode.CPU_NB, nb);
    }

    @Override
    public void setCPUCapacity(int c) {
        updateValue(DefaultNode.CPU_CAPACITY, c);
    }

    @Override
    public void setMemoryCapacity(int m) {
        updateValue(DefaultNode.MEMORY_TOTAL, m);
    }

    @Override
    public Node clone() {
        return new DefaultNode(this);
    }

    /**
     * Return the capacity of each CPU.
     *
     * @return a positive integer
     */
    public int getCPUCapacity() {
        return (Integer) getValue(DefaultNode.CPU_CAPACITY);
    }

    /**
     * Get the identifier of the startup driver.
     *
     * @return an identifier. May be null if there is no driver
     */
    public String getStartupDriverID() {
        return this.startupDriverID;
    }

    /**
     * Get the identifier of the shutdown driver.
     *
     * @return an identifier. May be null if there is no driver
     */
    public String getShutdownDriverID() {
        return this.shutdownDriverID;
    }

    /**
     * Get the identifier of the migration driver.
     *
     * @return an identifier. May be null if there is no driver
     */
    public String getMigrationDriverID() {
        return this.migrationDriverID;
    }

    /**
     * Get the identifier of the hypervisor.
     *
     * @return an identifier. May be null if there is no hypervisor associated to this node
     */
    public String getHypervisorID() {
        return this.hypervisorID;
    }

    /**
     * Get the MAC Address of this node.
     *
     * @return a MAC address that may be null
     */
    public String getMACAddress() {
        return this.macAddress;
    }

    /**
     * Get the IP address of this node.
     *
     * @return an IP address that may be null
     */
    public String getIPAddress() {
        return this.ipAddress;
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
    public String getCurrentPlatform() {
        return this.currentPlatform;
    }

    /**
     * Associate a startup driver to this node.
     *
     * @param id the identifier of the driver
     */
    public void setStartupDriverID(String id) {
        this.startupDriverID = id;
    }

    /**
     * Associate a shutdown driver to this node.
     *
     * @param id the identifier of the driver
     */
    public void setShutdownDriverID(String id) {
        this.shutdownDriverID = id;
    }

    /**
     * Associate a migration driver to this node.
     *
     * @param id the identifier of the driver
     */
    public void setMigrationDriverID(String id) {
        this.migrationDriverID = id;
    }

    /**
     * Associate an hypervisor to this node.
     *
     * @param id the identifier of the hypervisor
     */
    public void setHypervisorID(String id) {
        this.hypervisorID = id;
    }

    /**
     * Set the MAC address of this node.
     *
     * @param mac The MAC address
     */
    public void setMACAddress(String mac) {
        this.macAddress = mac;
    }

    /**
     * Set the IP address of this node.
     *
     * @param ip the IP address
     */
    public void setIPAddress(String ip) {
        this.ipAddress = ip;
    }

    /**
     * Get the identifier of the suspend driver.
     *
     * @return an identifier. May be null if there is no driver associated to this node
     */
    public String getSuspendDriverID() {
        return this.suspendDriverID;
    }

    /**
     * Associate a suspend driver to this node.
     *
     * @param id the identifier of the driver
     */
    public void setSuspendDriverID(String id) {
        this.suspendDriverID = id;
    }

    /**
     * Associate a resume driver to this node.
     *
     * @param id the identifier of the driver
     */
    public void setResumeDriverID(String id) {
        this.resumeDriverID = id;
    }

    /**
     * Get the identifier of the resume driver.
     *
     * @return an identifier. May be null if there is no driver associated to this node
     */

    public String getResumeDriverID() {
        return this.resumeDriverID;
    }

    /**
     * Get the identifier of the run driver.
     *
     * @return an identifier. May be null if there is no driver associated to this node
     */

    public String getRunDriverID() {
        return this.runDriverID;
    }

    /**
     * Associate a run driver to this node.
     *
     * @param id the identifier of the driver
     */
    public void setRunDriverID(String id) {
        this.runDriverID = id;
    }

    /**
     * Get the identifier of the stop driver.
     *
     * @return an identifier. May be null if there is no driver associated to this node
     */
    public String getStopDriverID() {
        return this.stopDriverID;
    }

    /**
     * Associate a stop driver to this node.
     *
     * @param id the identifier of the driver
     */
    public void setStopDriverID(String id) {
        this.stopDriverID = id;
    }
}
