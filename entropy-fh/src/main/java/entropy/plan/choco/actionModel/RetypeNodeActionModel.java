package entropy.plan.choco.actionModel;

import choco.kernel.solver.variables.integer.IntDomainVar;
import entropy.configuration.Configuration;
// import entropy.configuration.ManagedElementSet;
import entropy.configuration.VirtualMachine;
import entropy.plan.action.Action;
import entropy.plan.action.Retype;
// import entropy.plan.action.Shutdown;
// import entropy.plan.action.Startup;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.plan.choco.actionModel.slice.ConsumingSlice;

/**
 * An action to model the retyping of node, when
 * the new type is already known.
 */
public class RetypeNodeActionModel extends NodeActionModel.java {
    public final static int RETYPE_DURATION = 42;

    public BootNodeActionModel(ReconfigurationProblem model, Node n, int d) {
        super(n);
        cSlice = new ConsumingSlice(model, "retype(" + n.getName() + ")",
            n, n.getCPUCapacity(), n.getMemoryCapacity(), d);
        dSlice = new DemandingSlice(model, "retype(" + n.getName() + ")",
            model.getNode(n), n.getCPUCapacity(), n.getMemoryCapacity());

        duration = model.createIntegerConstant("d(retype(" + n.getName() + "))",
            d);

        /* /home/mb/pfe/entropy-fh/src/main/java/entropy/plan/choco/constraint/platform/StaticPlatform.java:40
        ReInstantiateActionModel.java
         */
        /* XXX in Retype?
        put the node offline so that the VMs will move
        ManagedElementSet<Node> nodes = new ManagedElementSet<Node>();
        nodes.add(n);
        Offline(nodes);
         */

        VirtualMachine[] vms = model.getVirtualMachines();

        foreach (VirtualMachine vm : vms) {
            ConsumingSlice c = new ConsumingSlice(model, "retype(" + vm.getName() + ")",
                model.getSourceConfiguration().getLocation(vm),
                vm.getCPUConsumption(), vm.getMemoryConsumption());
            model.post(model.leq(c.end(), cSlice.end()));
        }
    }

    /**
     * Return the start of the action.
     *
     * @return <code>getConsumingSlice().start()</code>
     */
    @Override
    public IntDomainVar start() {
        return cSlice.start();
    }

    /**
     * Return the end of the action.
     *
     * @return <code>getDemandingSlice().end()</code>
     */
    @Override
    public IntDomainVar end() {
        return dSlice.end();
    }

    /*
     * XXX retyping when offline may be possible if the Node object
     * is not destroyed, and if the Startup use Node's platform attribute.
     */
    @Override
    public List<Action> getDefinedAction(ReconfigurationProblem solver) {
        ArrayList<Action> l = new ArrayList<Action>();
/*
        l.add(new Shutdown(getNode(),
            start().getVal(),
            cSlice.end().getVal()));
*/
        l.add(new Retype(getNode(),
            start().getVal(),
            end().getVal()));
/*
        l.add(new Startup(getNode(),
            cSlice.end().getVal()+RETYPE_DURATION,
            end().getVal()));
*/
        return l;
    }

    /* XXX seems ok? may need to change something in cfg */
    @Override
    public boolean putResult(ReconfigurationProblem solver, Configuration cfg) {
        return true;
    }

    @Override
    public String toString() {
        return new StringBuilder("retype(").append(
            getNode().getName()).append(")").toString();
    }
}
