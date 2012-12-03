package entropy.plan.choco;

import entropy.configuration.Configuration;
import entropy.configuration.ManagedElementSet;
import entropy.configuration.Node;
import entropy.configuration.VirtualMachine;
import entropy.plan.PlanException;
import entropy.plan.TimedReconfigurationPlan;
import entropy.plan.durationEvaluator.DurationEvaluator;
import entropy.vjob.PlacementConstraint;
import entropy.vjob.VJob;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: fhermeni
 * Date: 19/06/12
 * Time: 14:41
 * To change this template use File | Settings | File Templates.
 */
public class DummyHeuristic extends ChocoCustomRP {


    /**
     * Make a new plan module.
     *
     * @param eval to evaluate the duration of the actions.
     */
    public DummyHeuristic(DurationEvaluator eval) {
        super(eval);
    }


    @Override
    public TimedReconfigurationPlan compute(Configuration src, ManagedElementSet<VirtualMachine> run, ManagedElementSet<VirtualMachine> wait, ManagedElementSet<VirtualMachine> sleep, ManagedElementSet<VirtualMachine> stop, ManagedElementSet<Node> on, ManagedElementSet<Node> off, List<VJob> q) throws PlanException {

        List<PlacementConstraint> cstr = new ArrayList<PlacementConstraint>();
        for (VJob v : q) {
            cstr.addAll(v.getConstraints());
        }

        Configuration cfg = src.clone();

        //Remove all the VMs
        for (VirtualMachine vm : cfg.getAllVirtualMachines()) {
            cfg.remove(vm);
        }

        //Place the VMs with regards to the constraints
        for (VirtualMachine vm : run) {
            //Pick a node

        }
        return super.compute(src, run, wait, sleep, stop, on, off, q);    //To change body of overridden methods use File | Settings | File Templates.
    }
}
