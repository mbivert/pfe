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

package entropy.configuration.parser;

import entropy.configuration.Node;
import entropy.configuration.SimpleNode;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Fabien Hermenier
 */
public final class PBNodeSerializer {

    private PBNodeSerializer() {
    }

    public static Node read(PBNode.Node pbNode) {
        SimpleNode n = new SimpleNode(pbNode.getName());
        if (pbNode.hasNbOfCPUs()) {
            n.setNbOfCPUs(pbNode.getNbOfCPUs());
        }

        if (pbNode.hasCpuCapacity()) {
            n.setCPUCapacity(pbNode.getCpuCapacity());
        }

        if (pbNode.hasMemoryCapacity()) {
            n.setMemoryCapacity(pbNode.getMemoryCapacity());
        }

        if (pbNode.hasIp()) {
            n.setIPAddress(pbNode.getIp());
        }

        if (pbNode.hasMac()) {
            n.setMACAddress(pbNode.getMac());
        }

        for (PBNode.Node.Platform p : pbNode.getPlatformsList()) {
            String id = p.getName();
            Map<String, String> opts = new HashMap<String, String>();
            for (PBNode.Node.Platform.Option o : p.getOptionsList()) {
                String k = o.getKey();
                if (o.hasValue()) {
                    opts.put(k, o.getValue());
                } else {
                    opts.put(k, null);
                }
            }
            n.addPlatform(id, opts);
        }

        if (pbNode.hasCurrentPlatform()) {
            n.setCurrentPlatform(pbNode.getCurrentPlatform());
        }

        return n;
    }

    public static PBNode.Node write(Node n) {
        PBNode.Node.Builder pbn = PBNode.Node.newBuilder();
        pbn.setName(n.getName());
        pbn.setNbOfCPUs(n.getNbOfCPUs());
        pbn.setCpuCapacity(n.getCPUCapacity());
        pbn.setMemoryCapacity(n.getMemoryCapacity());

        String s = n.getIPAddress();
        if (s != null) {
            pbn.setIp(s);
        }
        s = n.getMACAddress();
        if (s != null) {
            pbn.setMac(s);
        }

        PBNode.Node.Platform.Builder b = PBNode.Node.Platform.newBuilder();
        PBNode.Node.Platform.Option.Builder ob = PBNode.Node.Platform.Option.newBuilder();
        for (String id : n.getAvailablePlatforms()) {
            b.setName(id);
            Map<String, String> opts = n.getPlatformOptions(id);
            for (Map.Entry<String, String> opt : opts.entrySet()) {
                ob.setKey(opt.getKey());
                String v = opt.getValue();
                if (v != null) {
                    ob.setValue(v);
                }
                b.addOptions(ob.build());
                ob.clear();
            }
            pbn.addPlatforms(b.build());
            b.clear();
        }
        return pbn.build();
    }

}
