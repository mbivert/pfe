package entropy.vjob;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import org.testng.Assert;
import org.testng.annotations.Test;

import entropy.configuration.*;
import entropy.plan.PlanException;
import entropy.plan.choco.ChocoCustomRP;
import entropy.plan.choco.constraint.pack.SatisfyDemandingSlicesHeightsSimpleBP;
import entropy.plan.durationEvaluator.MockDurationEvaluator;

public class MargingHostCPUTest {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(MargingHostCPUTest.class);

	@Test(groups = {"unit"})
	public void miniConfigTest() throws PlanException {
		SimpleConfiguration cfg = makeMiniConfig();
		ChocoCustomRP solver = generateClassSolver();
		VirtualMachine targetVM = cfg.getAllVirtualMachines().get("VM0");
		List<VJob> vjobs = new ArrayList<VJob>();
		VJob vjob = new DefaultVJob("vjob1");

		MargingHostCPU toTest = new MargingHostCPU(50, targetVM);
		vjob.addConstraint(toTest);

		vjobs.add(vjob);
		entropy.plan.TimedReconfigurationPlan result = solver.compute(cfg,
				cfg.getAllVirtualMachines(),
				new SimpleManagedElementSet<VirtualMachine>(),
				new SimpleManagedElementSet<VirtualMachine>(),
				new SimpleManagedElementSet<VirtualMachine>(), cfg.getAllNodes(),
				new SimpleManagedElementSet<Node>(), vjobs);
		logger.debug("compute ok");
		Configuration dest = result.getDestination();
		int load = 0;
		for (VirtualMachine vm : dest.getRunnings(dest.getLocation(targetVM))) {
			load += vm.getCPUDemand();
		}
		Assert.assertEquals(load, 30);
	}

	/**
	 * generate a config with 3VMs(VM0, VM1, VM2), at cpuUsage 30, memUsage 1, and
	 * 2 nodes(N0, N1) at loadCapacity 100, memCapacity 1024
	 */
	public static SimpleConfiguration makeMiniConfig() {
		SimpleConfiguration ret = new SimpleConfiguration();
		for (int i = 0; i < 2; i++) {
			Node n = new SimpleNode("N" + i, 1, 100, 1024);
			ret.addOnline(n);
		}
		for (int i = 0; i < 3; i++) {
			VirtualMachine vm = new SimpleVirtualMachine("VM" + i, 1, 30, 1);
			ret.addWaiting(vm);
		}
		return ret;
	}

	@Test(dependsOnMethods = "miniConfigTest", groups = {"unit"})
	public void simpleApplyingConstraint() throws PlanException {

		// we want one node with 100% load(not constrained VMs), and 3 nodes with
		// 25% load(the constrained VMs)
		int lowNodesLoad = 25;
		int nbVMs = 100 / 5 + 3 * lowNodesLoad / 5;
		SimpleConfiguration cfg = LazyNodeTest.makeIdleVMsConfiguration(nbVMs);
		// 3 nodes at 25%, each VM consumming 5%=> 3*25/5
		int nbMargingVMs = 3 * 25 / 5;
		VirtualMachine[] margingVMs = new VirtualMachine[nbMargingVMs];
		for (int i = 0; i < nbMargingVMs; i++) {
			margingVMs[i] = cfg.getAllVirtualMachines().get("VM" + i);
		}
		MargingHostCPU toTest = new MargingHostCPU(30, margingVMs);
		ChocoCustomRP solver = generateClassSolver();
		List<VJob> vjobs = new ArrayList<VJob>();
		VJob vjob = new DefaultVJob("vjob1");
		vjob.addConstraint(toTest);
		vjobs.add(vjob);
		entropy.plan.TimedReconfigurationPlan result = solver.compute(cfg,
				cfg.getAllVirtualMachines(),
				new SimpleManagedElementSet<VirtualMachine>(),
				new SimpleManagedElementSet<VirtualMachine>(),
				new SimpleManagedElementSet<VirtualMachine>(), cfg.getAllNodes(),
				new SimpleManagedElementSet<Node>(), vjobs);
		logger.debug("compute ok");
		Configuration dest = result.getDestination();
		Set<Node> lowUsedNodes = new HashSet<Node>();
		Map<String, Integer> loads = new HashMap<String, Integer>();
		for (Node n : dest.getAllNodes()) {
			int load = 0;
			for (VirtualMachine vm : dest.getRunnings(n)) {
				load += vm.getCPUDemand();
			}
			if (load <= 30) {
				lowUsedNodes.add(n);
			}
			loads.put(n.getName(), load);
		}
		Assert.assertEquals(lowUsedNodes.size(), 3, "nodes loads are : " + loads);
		for (VirtualMachine vm : margingVMs) {
			Node host = dest.getLocation(vm);
			Assert.assertTrue(lowUsedNodes.contains(host), "marging virtual machine "
					+ vm.getName() + " on host " + host.getName()
					+ " which is loaded at " + loads.get(host.getName()));
		}
	}

	/**
	 * huge configuration test: 50 nodes"N0" to "N50" with 1000 CPU capa and
	 * 8*1024 ram, 50 groups of VMs, each group consisting in 3VMs "VM0-0" to
	 * "VM0-2" with the spread rule, each VM requiring 100 CPU capa and 512 ram
	 * and its host having 50% free CPU
	 *
	 * @throws PlanException
	 */
	@Test(dependsOnMethods = "simpleApplyingConstraint")
	public void bigConfigurationTest() throws PlanException {
		int nbNodes = 20;
		int nbVMPerGroup = 4;
		int hostMaxLoad = 40;

		List<VJob> vjobs = new ArrayList<VJob>();
		ArrayList<ArrayList<SimpleVirtualMachine>> vmsPerGroup = new ArrayList<ArrayList<SimpleVirtualMachine>>();
		SimpleConfiguration cfg = createBigConfiguration(nbNodes, nbVMPerGroup,
				vjobs, hostMaxLoad, vmsPerGroup);

		ChocoCustomRP solver = generateClassSolver();
		entropy.plan.TimedReconfigurationPlan result = solver.compute(cfg,
				cfg.getAllVirtualMachines(),
				new SimpleManagedElementSet<VirtualMachine>(),
				new SimpleManagedElementSet<VirtualMachine>(),
				new SimpleManagedElementSet<VirtualMachine>(), cfg.getAllNodes(),
				new SimpleManagedElementSet<Node>(), vjobs);
		Configuration dest = result.getDestination();
		int maxLoad = 0;
		Map<String, Integer> nodesLoads = new HashMap<String, Integer>();
		for (Node n : cfg.getAllNodes()) {
			int load = 0;
			for (VirtualMachine vmr : dest.getRunnings(n)) {
				load += vmr.getCPUDemand();
			}
			nodesLoads.put(n.getName(), load);
			if (maxLoad < load) {
				maxLoad = load;
			}
		}
		for (ArrayList<SimpleVirtualMachine> grp : vmsPerGroup) {
			Set<Node> runners = new HashSet<Node>();
			for (VirtualMachine vm : grp) {
				runners.add(dest.getLocation(vm));
			}
			Assert.assertEquals(runners.size(), nbVMPerGroup, "group of VMs :" + grp
					+ " placed on nodes : " + runners + ", cfg=" + dest.toString());
		}
		Assert.assertTrue(maxLoad <= hostMaxLoad * NodeCPU / VMCPU, "nodes load : "
				+ nodesLoads + "\ndest : " + dest.toString());
	}

	/**
	 * generate a correct solver for the tests methods
	 */
	public static ChocoCustomRP generateClassSolver() {
		ChocoCustomRP solver = new ChocoCustomRP(new MockDurationEvaluator(1, 2, 3,
				4, 5, 6, 7, 8, 0));
		solver
				.setPackingConstraintClass(new SatisfyDemandingSlicesHeightsSimpleBP());
		solver.setRepairMode(false);
		return solver;
	}

	public static final int NodeCPU = 1000;
	public static final int VMCPU = 100;
	public static final int NodeRAM = 4 * 1024;
	public static final int VMRAM = 512;

	/**
	 * create a configuration of N nodes and N groups of VMs.<br />
	 * host config : {@value #NodeCPU} cpu available, {@value #NodeRAM} ram
	 * available.
	 *
	 * @param nbNodes      the number of Nodes and of groups of VMs
	 * @param nbVMPerGroup the number of VM per group of VM (and so, per Node)
	 * @param vjobs        the list to add created vjobs into. If null, not used
	 * @param hostMaxLoad  the marge of CPU the VMs require on their hosts. If <0, no marging
	 *                     constraint is added
	 * @param vmsPerGroup  the #group->#vm->Virtualmachine indexed map. Using an arraylist
	 *                     allows faster mapping. it is cleared on invocation. If null, not
	 *                     used
	 * @return the generated configuration. The generated vjobs are added into
	 *         vjobs, and the vms indexed per group and position in the group in
	 *         vmsPerGroup
	 */
	public static SimpleConfiguration createBigConfiguration(int nbNodes,
			int nbVMPerGroup, List<VJob> vjobs, int hostMaxLoad,
			ArrayList<ArrayList<SimpleVirtualMachine>> vmsPerGroup) {
		int nbVMGroup = nbNodes;
		int nbNode = 1000;

		SimpleConfiguration cfg = new SimpleConfiguration();
		if (vmsPerGroup != null) {
			vmsPerGroup.ensureCapacity(nbNodes);
			vmsPerGroup.clear();
		}
		for (int i = 0; i < nbNode; i++) {
			Node n = new SimpleNode("N" + i, 1, NodeCPU, NodeRAM);
			cfg.addOnline(n);
		}
		for (int i = 0; i < nbVMGroup; i++) {
			ArrayList<SimpleVirtualMachine> grp = new ArrayList<SimpleVirtualMachine>();
			if (vmsPerGroup != null) {
				vmsPerGroup.add(i, grp);
			}
			for (int j = 0; j < nbVMPerGroup; j++) {
				SimpleVirtualMachine vm = new SimpleVirtualMachine(
						"VM" + i + "-" + j, 1, VMCPU, VMRAM);
				cfg.addWaiting(vm);
				grp.add(vm);
			}
			if (vjobs != null) {
				VJob vjob = new DefaultVJob("vjob" + i);
				for (VirtualMachine vm : grp) {
					vjob.addVirtualMachine(vm);
				}
				if (hostMaxLoad > 0) {
					vjob.addConstraint(new MargingHostCPU(hostMaxLoad, grp
							.toArray(new VirtualMachine[]{})));
				}
				vjob.addConstraint(new ContinuousSpread(grp
						.toArray(new VirtualMachine[]{})));
				vjobs.add(vjob);
			}
		}
		return cfg;
	}

	public static void main(String[] args) throws IOException, PlanException {
		int nbVMPerGroup = 4;
		File out = new File(MargingHostCPUTest.class.getCanonicalName() + ".log");
		System.out.println("writing results to " + out.getPath());
		BufferedWriter bw = new BufferedWriter(new FileWriter(out)) {
			@Override
			public void write(String str) throws IOException {
				System.err.println(str);
				if (!str.endsWith("\n")) {
					str += "\n";
				}
				super.write(str);
				flush();
			}
		};
		for (int nbNodes : new int[]{32, 128, 512, 1024}) {
			bw.write("with " + nbNodes + " nodes :");
			for (int maxHostLoad : new int[]{-1, 40, 60, 80, 100}) {
				bw.write(" max host load : " + maxHostLoad);
				long startTime = System.currentTimeMillis();
				List<VJob> vjobs = new ArrayList<VJob>();
				ArrayList<ArrayList<SimpleVirtualMachine>> vmsPerGroup = new ArrayList<ArrayList<SimpleVirtualMachine>>();
				SimpleConfiguration cfg = createBigConfiguration(nbNodes,
						nbVMPerGroup, vjobs, maxHostLoad, vmsPerGroup);
				ChocoCustomRP solver = generateClassSolver();
				// entropy.plan.TimedReconfigurationPlan result =
						solver.compute(cfg,
								cfg.getAllVirtualMachines(),
								new SimpleManagedElementSet<VirtualMachine>(),
								new SimpleManagedElementSet<VirtualMachine>(),
								new SimpleManagedElementSet<VirtualMachine>(), cfg.getAllNodes(),
								new SimpleManagedElementSet<Node>(), vjobs);
						long endTime = System.currentTimeMillis();
						bw.write("  time elapsed : " + (endTime - startTime) + " ms");
			}
		}
		bw.close();
	}

	/**
	 * test if entropy will migrate a VM that is already in the good position,
	 * when having to start some other VMs<br />
	 * FIXME put this in the correct position
	 *
	 * @throws PlanException
	 */
	@Test
	public void simpleStartVMsTests() throws PlanException {
		SimpleConfiguration cfg = new SimpleConfiguration();
		Node s1 = new SimpleNode("s1", 1, 3000, 4000), s2 = new SimpleNode("s2", 1,
				300, 200), s3 = new SimpleNode("s3", 1, 3000, 4000), s4 = new SimpleNode(
						"s4", 1, 300, 200), s5 = new SimpleNode("s5", 1, 3000, 4000);
		for (Node n : new Node[]{s1, s2, s3, s4, s5}) {
			cfg.addOnline(n);
		}
		VirtualMachine vmIP = new SimpleVirtualMachine("VMIP", 1, 30, 10);
		cfg.setRunOn(vmIP, s2);
		for (int i : new int[]{1, 2, 3}) {
			cfg.addWaiting(new SimpleVirtualMachine("VM" + i, 1, 100, 64));
		}
		VirtualMachine[] vms = new VirtualMachine[10];
		for (int i = 1; i <= 9; i++) {
			VirtualMachine vm = new SimpleVirtualMachine("VM" + i, 1, i == 8 ? 30
					: 300, i < 7 ? i * 100 : 20);
			vms[i] = vm;
			if (i < 4) {
				cfg.setRunOn(vm, s1);
			} else if (i == 8) {
				cfg.setRunOn(vm, s2);
			} else {
				cfg.setRunOn(vm, s3);
			}
		}
		cfg.setRunOn(new SimpleVirtualMachine("myvm", 1, 0, 400), s5);
		VirtualMachine[] myClouds = new VirtualMachine[4];
		for (int i = 1; i < 4; i++) {
			VirtualMachine vm = new SimpleVirtualMachine("mycloud" + i, 1, 0, 400);
			myClouds[i] = vm;
			cfg.setSleepOn(vm, s5);
		}
		// System.out.println("start config :");
		// System.out.println(vmIP);
		// System.out.println(vms[8]);
		// System.out.println(cfg);
		ChocoCustomRP solver = generateClassSolver();
		List<VJob> vjobs = new ArrayList<VJob>();
		entropy.plan.TimedReconfigurationPlan result =
				solver.compute(cfg, cfg.getAllVirtualMachines(),
						new SimpleManagedElementSet<VirtualMachine>(),
						new SimpleManagedElementSet<VirtualMachine>(),
						new SimpleManagedElementSet<VirtualMachine>(), cfg.getAllNodes(),
						new SimpleManagedElementSet<Node>(), vjobs);
		Configuration dest = result.getDestination();
		// System.out.println("end config :");
		// System.out.println(dest);
		Assert.assertEquals(dest.getLocation(vmIP), s2);
	}

}
