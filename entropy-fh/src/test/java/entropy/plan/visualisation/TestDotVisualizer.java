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

package entropy.plan.visualisation;

import entropy.configuration.*;
import entropy.plan.DefaultTimedReconfigurationPlan;
import entropy.plan.TimedReconfigurationPlan;
import entropy.plan.action.*;
import entropy.plan.action.Shutdown;
import entropy.plan.visualization.DotVisualizer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Unit tests for DotVisualizer.
 *
 * @author Fabien Hermenier
 */
@Test(groups = {"unit"})
public class TestDotVisualizer {

    private static String validContent = "digraph TimedExecutionGraph{\n" +
            "rankdir=LR;\n" +
            "Nbar -> Nbar [label=\"deploy(kvm)\"]\n" +
            "FORGE -> N1 [label=\"run(VM6)\", headlabel=\"15\",taillabel=\"20\", color=\"red\"]\n" +
            "iVMfoo [label=\"\", shape=none]\n" +
            "iVMfoo -> FORGE [label=\"instantiate(VMfoo)\"]\n" +
            "N2 -> N2 [label=\"suspend(VM1)\", headlabel=\"0\",taillabel=\"15\", color=\"blue\"]\n" +
            "bN0 -> N0 [label=\"boot\", headlabel=\"0\",taillabel=\"10\",color=\"blue\"]\n" +
            "bN0 [label=\"\", shape=none]\n" +
            "N3 -> N2 [label=\"resume(VM2)\", headlabel=\"15\",taillabel=\"25\", color=\"red\"]\n" +
            "N1 -> N0 [label=\"migrate(VM0)\", headlabel=\"10\",taillabel=\"15\",color=\"green\"]\n" +
            "N5 -> N5 [label=\"halt\", headlabel=\"3\",taillabel=\"5\", color=\"red\"]\n" +
            "N5 -> sVM5 [label=\"stop(VM5)\", headlabel=\"0\",taillabel=\"3\", color=\"blue\"]\n" +
            "sVM5 [label=\"\", shape=none]\n" +
            "}\n";

    /**
     * Pseudo test for storeToDot().
     * It may be useful to specify a filename to check the resulting file
     */
    public void testToDot() {
        Configuration cfg = new SimpleConfiguration();
        VirtualMachine[] vms = new VirtualMachine[7];
        Node[] ns = new Node[vms.length];

        for (int i = 0; i < vms.length; i++) {
            vms[i] = new SimpleVirtualMachine("VM" + i, 1, 1, 1);
            ns[i] = new SimpleNode("N" + i, 1, 1, 1);
            cfg.addOnline(ns[i]);
        }
        cfg.addOffline(ns[0]);
        cfg.setRunOn(vms[0], ns[1]);
        cfg.setRunOn(vms[1], ns[2]);
        cfg.setSleepOn(vms[2], ns[2]);
        cfg.setRunOn(vms[3], ns[3]);
        cfg.setRunOn(vms[4], ns[0]);
        cfg.setRunOn(vms[5], ns[5]);
        cfg.addWaiting(vms[6]);

        VirtualMachine vmFoo = new SimpleVirtualMachine("VMfoo", 1, 1, 1);

        Node dep = new SimpleNode("Nbar", 1, 1, 1);
        dep.addPlatform("xen");
        dep.addPlatform("kvm");
        cfg.addOnline(dep);
        TimedReconfigurationPlan p = new DefaultTimedReconfigurationPlan(cfg);

        Assert.assertTrue(p.add(new Startup(ns[0], 0, 10)));
        Assert.assertTrue(p.add(new Migration(vms[0], ns[1], ns[0], 10, 15)));
        Assert.assertTrue(p.add(new Suspend(vms[1], ns[2], ns[2], 0, 15)));
        Assert.assertTrue(p.add(new Resume(vms[2], ns[3], ns[2], 15, 25)));
        //Assert.assertTrue(p.add(new Pause(vms[3], ns[3], 0, 3)));
        //Assert.assertTrue(p.add(new UnPause(vms[4], ns[0], 0, 5)));
        Assert.assertTrue(p.add(new Stop(vms[5], ns[5], 0, 3)));
        Assert.assertTrue(p.add(new Shutdown(ns[5], 3, 5)));
        Assert.assertTrue(p.add(new Run(vms[6], ns[1], 15, 20)));
        Assert.assertTrue(p.add(new Instantiate(vmFoo, 0, 1)));
        Assert.assertTrue(p.add(new Deploy(dep, "kvm", 0, 60)));
        BufferedReader in = null;
        try {
            File f = File.createTempFile("tmp", ".dot");
            DotVisualizer vis = new DotVisualizer(f.getAbsolutePath());
            Assert.assertTrue(vis.buildVisualization(p));
            Assert.assertTrue(f.exists());
            in = new BufferedReader(new FileReader(f));
            String line = in.readLine();
            StringBuilder buf = new StringBuilder();
            while (line != null) {
                buf.append(line);
                buf.append("\n");
                line = in.readLine();
            }
            //Just a length comparison because the output is not deterministic (but to set usage ?)
            Assert.assertEquals(buf.toString().length(), validContent.length(), buf.toString());
            Assert.assertTrue(f.delete());
        } catch (IOException e) {
            Assert.fail(e.getMessage(), e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    Assert.fail(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * test getters and setters.
     */
    public void testBasics() {
        DotVisualizer vis = new DotVisualizer("tmp.dot");
        Assert.assertEquals(vis.getOutputFile(), "tmp.dot");
        vis.setOutputFile("out");
        Assert.assertEquals(vis.getOutputFile(), "out");
    }
}
