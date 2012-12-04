package entropy.plan.choco.actionModel;

import choco.kernel.solver.variables.integer.IntDomainVar;
import entropy.configuration.Configuration;
import entropy.configuration.ManagedElementSet;
import entropy.configuration.VirtualMachine;
import entropy.configuration.Node;
import entropy.plan.action.Action;
import entropy.plan.action.Retype;
import entropy.plan.action.Shutdown;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.plan.choco.actionModel.slice.ConsumingSlice;
import entropy.plan.choco.actionModel.slice.DemandingSlice;

import java.util.List;
import java.util.ArrayList;

/**
 * An action to model the retyping of node, when
 * the new type is already known.
 */
public class RetypeNodeActionModel extends NodeActionModel {
    public final static int RETYPE_DURATION = 42;
    public final static String newPlatform = "XXX";

    public RetypeNodeActionModel(ReconfigurationProblem model, Node n, int dur) {
        super(n);
        cSlice = new ConsumingSlice(model, "retype(" + n.getName() + ")",
            n, n.getCPUCapacity(), n.getMemoryCapacity(), dur);
        dSlice = new DemandingSlice(model, "retype(" + n.getName() + ")",
            model.getNode(n), n.getCPUCapacity(), n.getMemoryCapacity());

        duration = model.createIntegerConstant("d(retype(" + n.getName() + ")",
            dur);

        ManagedElementSet<VirtualMachine> vms = model.getSourceConfiguration().getRunnings(n);

        for (VirtualMachine vm : vms) {
            ConsumingSlice c = model.getAssociatedAction(vm).getConsumingSlice();
            model.post(model.leq(c.end(), cSlice.end()));
        }

        /*
         * constrain only VMs that may move on this node.
         * take an arbitrary node where the VM can be located, and compare
         * its platform type to newPlatform
         */
        List<DemandingSlice> ds = model.getDemandingSlices();
        for (DemandingSlice d : ds)
            if (model.getNode(d.hoster().getInf()).getCurrentPlatform().equals(newPlatform))
                model.post(model.geq(d.start(), model.plus(dSlice.start(), RETYPE_DURATION)));
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

        l.add(new Shutdown(getNode(),
            start().getVal(),
            cSlice.end().getVal()));

		l.add(new Retype(getNode(),
			start().getVal(),
			end().getVal(),
			newPlatform));

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
