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

package entropy.configuration.parser;

import entropy.configuration.Configuration;
import entropy.configuration.Node;
import entropy.configuration.SimpleConfiguration;
import entropy.configuration.VirtualMachine;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Serialize and un-serialize a configuration from/to a binary format compatible with
 * Google Protocol buffers.
 *
 * @author Fabien Hermenier
 */
public final class ProtobufConfigurationSerializer extends FileConfigurationSerializer {

    /**
     * The instance of the singleton.
     */
    private static final ProtobufConfigurationSerializer INSTANCE = new ProtobufConfigurationSerializer();

    /**
     * Private constructor, managed by the singleton.
     */
    private ProtobufConfigurationSerializer() {
    }

    /**
     * Get the unique instance.
     *
     * @return an instance
     */
    public static ProtobufConfigurationSerializer getInstance() {
        return INSTANCE;
    }

    @Override
    public Configuration unSerialize(InputStream in) throws IOException, ConfigurationSerializerException {
        try {
            return convert(PBConfiguration.Configuration.parseFrom(in));
        } catch (Exception e) {
            throw new ConfigurationSerializerException(e);
        }
    }

    @Override
    public void serialize(Configuration cfg, OutputStream out) throws IOException {
        PBConfiguration.Configuration b = convert(cfg);
        b.writeTo(out);
    }

    public static Configuration convert(PBConfiguration.Configuration c) {
        Configuration cfg = new SimpleConfiguration();
        for (PBNode.Node n : c.getOfflinesList()) {
            cfg.addOffline(PBNodeSerializer.read(n));
        }
        for (PBVirtualMachine.VirtualMachine vm : c.getWaitingsList()) {
            VirtualMachine vm2 = PBVirtualMachineSerializer.read(vm);
            cfg.addWaiting(vm2);
        }
        for (PBConfiguration.Configuration.Hoster h : c.getOnlinesList()) {
            PBNode.Node n = h.getNode();
            Node n2 = PBNodeSerializer.read(n);
            cfg.addOnline(n2);
            for (PBConfiguration.Configuration.Hosted hosted : h.getHostedList()) {
                PBConfiguration.Configuration.HostedVMState st = hosted.getState();
                PBVirtualMachine.VirtualMachine vm = hosted.getVm();
                VirtualMachine vm2 = PBVirtualMachineSerializer.read(vm);
                switch (st) {
                    case RUNNING:
                        cfg.setRunOn(vm2, n2);
                        break;
                    case SLEEPING:
                        cfg.setSleepOn(vm2, n2);
                        break;
                }
            }
        }
        return cfg;
    }

    public static PBConfiguration.Configuration convert(Configuration cfg) {
        PBConfiguration.Configuration.Builder b = PBConfiguration.Configuration.newBuilder();
        for (Node n : cfg.getOfflines()) {
            b.addOfflines(PBNodeSerializer.write(n));
        }
        for (VirtualMachine vm : cfg.getWaitings()) {
            b.addWaitings(PBVirtualMachineSerializer.write(vm));
        }
        for (Node n : cfg.getOnlines()) {
            PBNode.Node n2 = PBNodeSerializer.write(n);
            PBConfiguration.Configuration.Hoster.Builder hoster = PBConfiguration.Configuration.Hoster.newBuilder();
            hoster.setNode(n2);
            for (VirtualMachine vm : cfg.getRunnings(n)) {
                PBConfiguration.Configuration.Hosted.Builder hosted = PBConfiguration.Configuration.Hosted.newBuilder();
                hosted.setState(PBConfiguration.Configuration.HostedVMState.RUNNING);
                PBVirtualMachine.VirtualMachine vm2 = PBVirtualMachineSerializer.write(vm);
                hosted.setVm(vm2);
                hoster.addHosted(hosted.build());
            }

            for (VirtualMachine vm : cfg.getSleepings(n)) {
                PBConfiguration.Configuration.Hosted.Builder hosted = PBConfiguration.Configuration.Hosted.newBuilder();
                hosted.setState(PBConfiguration.Configuration.HostedVMState.SLEEPING);
                PBVirtualMachine.VirtualMachine vm2 = PBVirtualMachineSerializer.write(vm);
                hosted.setVm(vm2);
                hoster.addHosted(hosted.build());
            }
            b.addOnlines(hoster.build());
        }
        return b.build();
    }
}
