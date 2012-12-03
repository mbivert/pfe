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
package entropy.vjob.queue;

import entropy.configuration.SimpleConfiguration;
import entropy.template.MockVirtualMachineTemplateFactory;
import entropy.template.VirtualMachineTemplateFactory;
import entropy.vjob.DefaultVJob;
import entropy.vjob.VJob;
import entropy.vjob.builder.DefaultVJobBuilderFactory;
import entropy.vjob.builder.DefaultVJobElementBuilder;
import entropy.vjob.builder.VJobElementBuilder;
import entropy.vjob.builder.plasma.PlasmaVJobBuilder;
import entropy.vjob.builder.xml.XMLVJobBuilderBuilder;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.*;
import java.util.List;

/**
 * Simple unit tests for FCFSPersistentQueue.
 *
 * @author Fabien Hermenier
 */
@Test(groups = {"unit"})
public class TestFCFSPersistentQueue {

    /**
     * The base of the test resources.
     */
    public static final String RESOURCES_DIR = "src/test/resources/entropy/vjob/queue/TestFCFSPersistentQueue.";

    private void putLeaseIntoQueue(File queue, String file) throws Exception {
        File f1 = new File(queue + File.separator + file);
        BufferedReader in = new BufferedReader(new FileReader("src/test/resources/entropy/vjob/queue/" + file));
        BufferedWriter out = new BufferedWriter(new FileWriter(f1));
        String line = in.readLine();
        while (line != null) {
            out.write(line);
            out.write("\n");
            line = in.readLine();
        }
        in.close();
        out.close();
    }

    /**
     * Test with an unexistant directory.
     * The directory is created after the first scan
     */
    public void testWithUnexistantDirectory() {
        File queueDir = new File(System.getProperty("java.io.tmpdir") + "/queue1/");
        queueDir.delete();
        Assert.assertFalse(queueDir.exists());
        try {
            DefaultVJobBuilderFactory vjobF = new DefaultVJobBuilderFactory();
            VirtualMachineTemplateFactory f = new MockVirtualMachineTemplateFactory();
            vjobF.add(new PlasmaVJobBuilder(new DefaultVJobElementBuilder(f), null));
            FCFSPersistentQueue queue = new FCFSPersistentQueue(vjobF, queueDir);
            Assert.assertEquals(queue.getFolder(), queueDir);
            Assert.assertTrue(queueDir.isDirectory());
            Assert.assertEquals(queue.getRunningPriorities().size(), 0);
        } catch (Exception e) {
            Assert.fail(e.getMessage(), e);
        }
        for (File f : queueDir.listFiles()) {
            f.delete();
        }
        queueDir.delete();

    }

    /**
     * Test with an existant directory.
     */
    public void testWithExistantDirectory() {
        File queueDir = new File(System.getProperty("java.io.tmpdir") + "/queue2/");
        queueDir.mkdirs();
        for (File f : queueDir.listFiles()) {
            f.delete();
        }
        Assert.assertTrue(queueDir.exists());
        try {
            DefaultVJobBuilderFactory vjobF = new DefaultVJobBuilderFactory();
            VirtualMachineTemplateFactory f = new MockVirtualMachineTemplateFactory();
            vjobF.add(new PlasmaVJobBuilder(new DefaultVJobElementBuilder(f), null));
            FCFSPersistentQueue queue = new FCFSPersistentQueue(vjobF, queueDir);
            Assert.assertEquals(queue.getFolder(), queueDir);
            Assert.assertTrue(queueDir.isDirectory());
            Assert.assertEquals(queue.getRunningPriorities().size(), 0);
        } catch (Exception e) {
            Assert.fail(e.getMessage(), e);
        } finally {
            for (File f : queueDir.listFiles()) {
                f.delete();
            }
            queueDir.delete();
        }
    }

    /**
     * Test with an existant directory.
     */
    public void testWithNonEmptyDirectory() {
        File queueDir = new File("src/test/resources/entropy/vjob/queue");
        Assert.assertTrue(queueDir.exists());
        Assert.assertTrue(queueDir.isDirectory());
        MockVirtualMachineTemplateFactory f = new MockVirtualMachineTemplateFactory();
        f.getMockBuilder().farm.add("VM1");
        f.getMockBuilder().farm.add("VM4");
        f.getMockBuilder().farm.add("TOTO");
        f.getMockBuilder().farm.add("tinkieWinky");
        f.getMockBuilder().farm.add("po");
        f.getMockBuilder().farm.add("nala");
        try {
            DefaultVJobBuilderFactory vjobF = new DefaultVJobBuilderFactory();
            VJobElementBuilder mvb = new DefaultVJobElementBuilder(f);
            vjobF.add(new XMLVJobBuilderBuilder().build(mvb));
            FCFSPersistentQueue queue = new FCFSPersistentQueue(vjobF, queueDir);
            Assert.assertEquals(queue.getFolder(), queueDir);
            Assert.assertEquals(queue.getRunningPriorities().size(), 4);
        } catch (Exception e) {
            Assert.fail(e.getMessage(), e);
        }
    }

    /**
     * Test with an addition of a vjob.
     */
    //@Test(dependsOnMethods = {"testWithUnexistantDirectory"})
    public void testAdditionIntoFolder() {
        File queueDir = new File(System.getProperty("java.io.tmpdir") + "/queue3/");
        queueDir.mkdirs();
        for (File f : queueDir.listFiles()) {
            f.delete();
        }
        try {
            MockVirtualMachineTemplateFactory f = new MockVirtualMachineTemplateFactory();
            f.getMockBuilder().farm.add("VM1");
            VJobElementBuilder mvb = new DefaultVJobElementBuilder(f);
            mvb.useConfiguration(new SimpleConfiguration());
            DefaultVJobBuilderFactory vjobF = new DefaultVJobBuilderFactory();
            vjobF.add(new XMLVJobBuilderBuilder().build(mvb));
            FCFSPersistentQueue queue = new FCFSPersistentQueue(vjobF, queueDir);
            Assert.assertEquals(queue.getFolder(), queueDir);
            this.putLeaseIntoQueue(queueDir, "lease1.xml");
            Assert.assertEquals(queue.getRunningPriorities().size(), 1);
            Assert.assertEquals(queue.getRunningPriorities().get(0).id(), "lease1");
        } catch (Exception e) {
            Assert.fail(e.getMessage(), e);
        } finally {
            for (File f : queueDir.listFiles()) {
                f.delete();
            }
            queueDir.delete();
        }
    }

    public void testAddFromMethod() {
        File queueDir = new File(System.getProperty("java.io.tmpdir") + "/queue4/");
        queueDir.mkdirs();
        for (File f : queueDir.listFiles()) {
            f.delete();
        }
        try {
            MockVirtualMachineTemplateFactory f = new MockVirtualMachineTemplateFactory();
            f.getMockBuilder().farm.add("VM1");
            VJobElementBuilder mvb = new DefaultVJobElementBuilder(f);
            VJob v = new DefaultVJob("V1");
            DefaultVJobBuilderFactory vjobF = new DefaultVJobBuilderFactory();
            vjobF.add(new XMLVJobBuilderBuilder().build(mvb));
            FCFSPersistentQueue queue = new FCFSPersistentQueue(vjobF, queueDir);
            Assert.assertTrue(queue.add(v));
            Assert.assertEquals(queue.getFolder(), queueDir);
            Assert.assertEquals(queue.getRunningPriorities().size(), 1);
            Assert.assertEquals(queue.getRunningPriorities().get(0).id(), "V1");
        } catch (Exception e) {
            Assert.fail(e.getMessage(), e);
        } finally {
            for (File f : queueDir.listFiles()) {
                f.delete();
            }
            queueDir.delete();
        }
    }

    /**
     * Test with the removal of a vjob.
     */
    public void testRemovalFromDir() {
        File queueDir = new File(System.getProperty("java.io.tmpdir") + "/queue4/");
        try {
            MockVirtualMachineTemplateFactory f = new MockVirtualMachineTemplateFactory();
            f.getMockBuilder().farm.add("VM1");
            VJobElementBuilder vb = new DefaultVJobElementBuilder(f);
            DefaultVJobBuilderFactory vjobF = new DefaultVJobBuilderFactory();
            vjobF.add(new XMLVJobBuilderBuilder().build(vb));
            FCFSPersistentQueue queue = new FCFSPersistentQueue(vjobF, queueDir);
            Assert.assertEquals(queue.getFolder(), queueDir);
            this.putLeaseIntoQueue(queueDir, "lease1.xml");
            Assert.assertEquals(queue.getRunningPriorities().size(), 1);
            Assert.assertEquals(queue.getRunningPriorities().get(0).id(), "lease1");
            File fi = new File(queueDir.getAbsolutePath() + File.separator + "lease1.xml");
            Assert.assertTrue(fi.exists());
            Assert.assertTrue(fi.delete());
            Assert.assertEquals(queue.getRunningPriorities().size(), 0);
        } catch (Exception e) {
            Assert.fail(e.getMessage(), e);
        } finally {
            for (File f : queueDir.listFiles()) {
                f.delete();
            }
            queueDir.delete();
        }
    }

    /**
     * Test with the removal of a vjob.
     */
    public void testRemovalFromMethod() {
        File queueDir = new File(System.getProperty("java.io.tmpdir") + "/queue5/");
        try {
            MockVirtualMachineTemplateFactory f = new MockVirtualMachineTemplateFactory();
            f.getMockBuilder().farm.add("VM1");
            DefaultVJobBuilderFactory vjobF = new DefaultVJobBuilderFactory();
            VJobElementBuilder vb = new DefaultVJobElementBuilder(f);
            vjobF.add(new XMLVJobBuilderBuilder().build(vb));
            FCFSPersistentQueue queue = new FCFSPersistentQueue(vjobF, queueDir);
            Assert.assertEquals(queue.getFolder(), queueDir);
            this.putLeaseIntoQueue(queueDir, "lease1.xml");
            List<VJob> q = queue.getRunningPriorities();
            Assert.assertEquals(q.size(), 1);
            Assert.assertEquals(q.get(0).id(), "lease1");
            File fi = new File(queueDir.getAbsolutePath() + File.separator + "lease1.xml");
            Assert.assertTrue(fi.exists());
            queue.remove(q.get(0));
            Assert.assertEquals(queue.getRunningPriorities().size(), 0);
            Assert.assertFalse(fi.exists());
        } catch (Exception e) {
            Assert.fail(e.getMessage(), e);
        } finally {
            for (File f : queueDir.listFiles()) {
                f.delete();
            }
            queueDir.delete();
        }
    }
}
