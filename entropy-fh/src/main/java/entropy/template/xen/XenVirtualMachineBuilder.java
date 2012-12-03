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
package entropy.template.xen;

import entropy.MissingRequiredPropertyException;
import entropy.PropertiesHelper;
import entropy.configuration.SimpleVirtualMachine;
import entropy.configuration.VirtualMachine;
import entropy.template.VirtualMachineBuilderException;
import entropy.template.VirtualMachineTemplate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A Template that is not a real template.
 * This template read an existing configuration file for the Xen Hypervisor and
 * create the VM depending on the content of the configuration file.
 *
 * @author Fabien Hermenier
 */
public class XenVirtualMachineBuilder implements VirtualMachineTemplate {

    /**
     * The root of the configs.
     */
    private String configsDir;

    private String props;

    /**
     * The REGEX to read the amount of memory to allocate.
     */
    public static final Pattern MEMORY_PATTERN = Pattern.compile("^\\s*memory\\s*=\\s*'?(\\d+)'?.*");

    /**
     * The REGEX to read the number of VCPU.
     */
    public static final Pattern VCPU_PATTERN = Pattern.compile("^\\s*vcpus\\s*=\\s*(\\d+).*");

    public static final String CONFIG_FILE = "config/xenTemplate.properties";

    public XenVirtualMachineBuilder() throws MissingRequiredPropertyException, IOException {
        this.props = CONFIG_FILE;
        PropertiesHelper hlp = new PropertiesHelper(props);
        configsDir = hlp.getRequiredProperty("xen.domUsPath");

    }

    public XenVirtualMachineBuilder(String file) throws MissingRequiredPropertyException, IOException {
        PropertiesHelper hlp = new PropertiesHelper(file);
        configsDir = hlp.getRequiredProperty("xen.domUsPath");
    }

    /**
     * Get the root of the config files.
     *
     * @return a path
     */
    public String getConfigDir() {
        return this.configsDir;
    }

    @Override
    public VirtualMachine build(String id, Map<String, String> options) throws VirtualMachineBuilderException {
        int mem = 0;
        int vcpu = 1;
        //Open the config file
        File f = new File(configsDir + "/" + id);
        if (!f.exists()) {
            return null;
        }
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(f));
            String line = in.readLine();
            while (line != null) {
                Matcher m = MEMORY_PATTERN.matcher(line);
                if (m.matches()) {
                    mem = Integer.parseInt(m.group(1));
                }
                m = VCPU_PATTERN.matcher(line);
                if (m.matches()) {
                    vcpu = Integer.parseInt(m.group(1));
                }
                line = in.readLine();
            }
        } catch (IOException e) {
            throw new VirtualMachineBuilderException("Unable to build VM '" + id + ": " + e.getMessage(), e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    throw new VirtualMachineBuilderException(e.getMessage(), e);
                }
            }
        }

        if (mem == 0) {
            throw new VirtualMachineBuilderException("Fail at building the VM '" + id + "': Unable to read the amount of memory to allocate");
        }
        VirtualMachine vm = new SimpleVirtualMachine(id, vcpu, 0, mem);
        vm.setHostingPlatform("xen");
        return vm;
    }

    @Override
    public String getIdentifier() {
        return "xen";
    }
}
