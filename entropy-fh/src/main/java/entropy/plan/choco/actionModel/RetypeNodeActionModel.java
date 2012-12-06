package entropy.plan.choco.actionModel;

import choco.kernel.solver.ContradictionException;
import choco.kernel.solver.variables.integer.IntDomainVar;
import entropy.configuration.Configuration;
//import entropy.configuration.ManagedElementSet;
import entropy.configuration.SimpleNode;
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
public class RetypeNodeActionModel extends ManageableNodeActionModel {
    public final static int RETYPE_DURATION = 42;
//    public final static int RETYPE_START = 42;

    private String newPlatform;
    private IntDomainVar required;

    public RetypeNodeActionModel(ReconfigurationProblem model, Node n, int d) {
        this(model, n, d, "any");
    }

    public RetypeNodeActionModel(ReconfigurationProblem model, Node n, int dur, String newPlatform) {
        super(n);
        required = model.createIntegerConstant("", 1);
        //cSlice = new ConsumingSlice(model, "retype(" + n.getName() + ")", n, n.getCPUCapacity(), n.getMemoryCapacity());

        duration = model.createIntegerConstant("d(retype(" + n.getName() + ")",
            dur);

        this.newPlatform = newPlatform;
    }

    public String getNewPlatform() {
        return newPlatform;
    }

    public void setNewPlatform(String newPlatform) {
        this.newPlatform = newPlatform;
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

    @Override
    public List<Action> getDefinedAction(ReconfigurationProblem solver) {
        ArrayList<Action> l = new ArrayList<Action>();

        l.add(new Shutdown(getNode(), 0, 1));

		l.add(new Retype(getNode(), 1+RETYPE_DURATION, 2+RETYPE_DURATION, newPlatform));

        return l;
    }

    /* XXX seems ok? may need to change something in cfg */
    @Override
    public boolean putResult(ReconfigurationProblem solver, Configuration cfg) {
        return true;
    }

    @Override
    public String toString() {
        return "retype("+getNode().getName()+")";
    }

    public IntDomainVar getState() {
        return required;
    }
}
