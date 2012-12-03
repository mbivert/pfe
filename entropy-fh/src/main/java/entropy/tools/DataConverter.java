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

package entropy.tools;

import entropy.configuration.Configuration;
import entropy.configuration.parser.ConfigurationSerializerException;
import entropy.configuration.parser.FileConfigurationSerializer;
import entropy.configuration.parser.FileConfigurationSerializerFactory;
import entropy.plan.TimedReconfigurationPlan;
import entropy.plan.parser.FileTimedReconfigurationPlanSerializer;
import entropy.plan.parser.FileTimedReconfigurationPlanSerializerFactory;
import entropy.plan.parser.TimedReconfigurationPlanSerializerException;
import entropy.vjob.VJob;
import entropy.vjob.builder.*;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * A tool to convert serialized configuration or plan in different format.
 *
 * @author Fabien Hermenier
 */
public final class DataConverter {

    public static final String PLAN_MODE = "-plan";

    public static final String CFG_MODE = "-cfg";

    public static final String VJOB_MODE = "-vjob";

    public static final String OUT_FORMAT_FLAG = "-of";

    private DataConverter() {
    }

    private static void fatal(String msg) {
        System.err.println(msg);
        System.exit(1);
    }

    /**
     * Launcher.
     *
     * @param args arguments to pass
     */
    public static void main(String[] args) {

        if (args.length < 5) {
            System.out.println("Convert configuration file");
            System.out.println("Usage: dataConv [-plan | -cfg | -vjob] -of output_format input_files output");
            System.out.println("input_files: series of files. Format will de  inferred from the files extension");
            System.out.println("output: the ouput folder");
            System.out.println("Supported datafile:");
            System.out.println("\t-plan reconfiguration plan");
            System.out.println("\t-cfg configuration");
            System.out.println("\t-vjob configuration");
            System.out.println();
            System.out.println("supported formats:");
            System.out.println("\tpbd - protobuf format");
            System.out.println("\ttxt - plain text format");
            System.out.println("\txml - XML format (vjobs only)");
            System.out.println("Warning, as Plasma is untyped, a configuration must be given as a first input file to make"
                    + "\nplasma detect the nodes and the virtual machines.");
            if (args.length == 0) {
                System.exit(0);
            }
            System.exit(1);
        }

        String outputPath;
        String outputFormat = null;
        String mode = null;
        List<String> inputs = new LinkedList<String>();
        int i = 0;
        while (i < args.length) {
            if (args[i].equals(PLAN_MODE) || args[i].equals(CFG_MODE) || args[i].equals(VJOB_MODE)) {
                mode = args[i];
            } else if (args[i].equals(OUT_FORMAT_FLAG)) {
                outputFormat = args[i + 1];
                i++;
            } else if (i != args.length - 1) {
                inputs.add(args[i]);
            }
            i++;
        }

        outputPath = args[args.length - 1];

        //Check the args

        if (mode == null) {
            fatal("Type of files must be specified");
        }
        if (outputFormat == null) {
            fatal("Output format must be specified");
        }

        if (inputs.isEmpty()) {
            fatal("No input configurations");
        }

        if (mode.equals(PLAN_MODE)) {
            convertPlan(inputs, outputFormat, outputPath);
        } else if (mode.equals(CFG_MODE)) {
            convertConfiguration(inputs, outputFormat, outputPath);
        } else if (mode.equals(VJOB_MODE))
            convertVJobs(inputs, outputFormat, outputPath);
        else {
            fatal("Unsupported mode '" + mode + "'");
        }
    }

    private static void convertConfiguration(List<String> inputs, String outputFormat, String outputPath) {
        FileConfigurationSerializer out = FileConfigurationSerializerFactory.getInstance().getSerializer(outputFormat);
        if (out == null) {
            fatal("Unsupported output format: " + outputFormat);
        }

        for (String input : inputs) {
            FileConfigurationSerializer src = FileConfigurationSerializerFactory.getInstance().getSerializer(input);
            if (src == null) {
                System.err.println("Skipping '" + input + "': not compatible with the input format");
                continue;
            }

            String outputName = convertFileName(input, outputPath, outputFormat);
            Configuration cfg = null;
            try {
                cfg = src.read(input);
            } catch (IOException e) {
                fatal("Error while reading '" + input + "': " + e.getMessage());
            } catch (ConfigurationSerializerException e) {
                fatal("Error while parsing '" + input + "': " + e.getMessage());
            }

            try {
                out.write(cfg, outputName);
            } catch (IOException e) {
                fatal("Error while writing '" + outputName + "': " + e.getMessage());
            }
        }
    }

    private static void convertPlan(List<String> inputs, String outputFormat, String outputPath) {
        FileTimedReconfigurationPlanSerializer out = FileTimedReconfigurationPlanSerializerFactory.getInstance().getSerializer(outputFormat);
        if (out == null) {
            fatal("Unsupported output format: " + outputFormat);
        }

        for (String input : inputs) {
            FileTimedReconfigurationPlanSerializer src;
            src = FileTimedReconfigurationPlanSerializerFactory.getInstance().getSerializer(input);
            if (src == null) {
                System.err.println("Skipping '" + input + "': not compatible with the input format");
                continue;
            }

            String outputName = convertFileName(input, outputPath, outputFormat);
            TimedReconfigurationPlan plan = null;
            try {
                plan = src.read(input);
            } catch (IOException e) {
                fatal("Error while reading '" + input + "': " + e.getMessage());
            } catch (TimedReconfigurationPlanSerializerException e) {
                fatal("Error while parsing '" + input + "': " + e.getMessage());
            }

            try {
                out.write(plan, outputName);
            } catch (IOException e) {
                fatal("Error while writing '" + outputName + "': " + e.getMessage());
            }
        }
    }

    private static void convertVJobs(List<String> inputs, String outputFormat, String outputPath) {
        VJobFileSerializerFactory out = DefaultVJobFileSerializerFactory.getInstance();
        if (!out.getManagedExtensions().contains(outputFormat)) {
            fatal("Unsupported output format: " + outputFormat);
        }

        VJobBuilderFactory in = null;
        try {
            in = new VJobBuilderFactoryBuilderFromProperties().build();
        } catch (VJobBuilderFactoryBuilderException e) {
            fatal(e.getMessage());
        }

        Iterator<String> ite = inputs.iterator();
        String first = ite.next();
        Configuration cfg;
        try {
            cfg = FileConfigurationSerializerFactory.getInstance().read(first);
            System.out.println("Consider " + first + " as a configuration");
            in.useConfiguration(cfg);
        } catch (Exception e) {

        }
        while (ite.hasNext()) {
            String input = ite.next();
            VJob v = null;
            try {
                System.out.println("Converting " + input);
                v = in.build(input);
            } catch (IOException e) {
                fatal("Error while reading '" + input + "': " + e.getMessage());
            } catch (VJobBuilderException e) {
                fatal("Error while parsing '" + input + "': " + e.getMessage());
            }
            String outputName = convertFileName(input, outputPath, outputFormat);
            try {
                out.write(v, outputName);
            } catch (IOException e) {
                fatal("Error while writing '" + outputName + "': " + e.getMessage());
            }
        }
    }

    private static String convertFileName(String path, String outputPath, String outputFormat) {
        File x = new File(path);
        String name = x.getName().substring(0, x.getName().lastIndexOf("."));
        return new StringBuilder(outputPath).append('/').append(name).append('.').append(outputFormat).toString();
    }
}
