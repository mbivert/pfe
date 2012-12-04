package entropy.plan.action;

import entropy.configuration.Configuration;
import entropy.configuration.Node;
import entropy.execution.TimedExecutionGraph;
import entropy.plan.parser.TimedReconfigurationPlanSerializer;
import entropy.plan.visualization.PlanVisualizer;

public class Retype extends Startup {
    private String newPlatform;

    public Retype(Node n, int s, int f, String newPlatform) {
        super(n, s, f);
        this.newPlatform = newPlatform;
    }

    public Retype(Node n, String newPlatform) {
        super(n);
        this.newPlatform = newPlatform;
    }

    public String getNewPlatform() {
        return newPlatform;
    }

    public void setNewPlatform(String newPlatform) {
        this.newPlatform = newPlatform;
    }
}
