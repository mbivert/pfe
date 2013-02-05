package entropy.configuration;

import java.util.HashMap;
import java.util.Map;

public class TypedNode extends SimpleNode {
    Map<String, Integer> types;

    public TypedNode(String name) {
        super(name);
    }

    public TypedNode(String name, int nbOfCPUs, int cpuCapacity, int memoryCapacity) {
        super(name, nbOfCPUs, cpuCapacity, memoryCapacity);
    }

    public TypedNode(String name, int nbOfCPUs, int cpuCapacity, int memoryCapacity, String ip, String mac) {
        super(name, nbOfCPUs, cpuCapacity, memoryCapacity, ip,mac);
    }

    public TypedNode(String name, Map<String, Integer> types) {
        super(name);
        this.types = types;
    }

    public void setTypes(Map<String, Integer> types) {
        this.types = types;
    }

    @Override
    public Node clone() {
        TypedNode clone = new TypedNode(getName(), getNbOfCPUs(), getCPUCapacity(), getMemoryCapacity());
        clone.types = types;
        clone.setIPAddress(getIPAddress());
        clone.setMACAddress(getMACAddress());
        clone.setCurrentPlatform(getCurrentPlatform());

        Map<String, Integer> ctypes = new HashMap<String, Integer>();
        for (Map.Entry<String, Integer> t : types.entrySet())
            ctypes.put(t.getKey(), t.getValue());

        clone.setTypes(ctypes);
        return clone;
    }
}
