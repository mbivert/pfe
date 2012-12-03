package entropy.plan.action;

import entropy.configuration.Configuration;
import entropy.configuration.Node;
import entropy.execution.TimedExecutionGraph;
import entropy.plan.parser.TimedReconfigurationPlanSerializer;
import entropy.plan.visualization.PlanVisualizer;

public class Retype extends NodeAction {
    String newPlatform = "";

    /**
     * Create a new time-unbounded retype action on an offline node.
     *
     * @param n The node to start
     */
    public Retype(Node n) {
        this(n, 0, 0);
    }

    /**
     * Create a new time-bounded retype action on an offline node.
     *
     * @param n The node to start
     * @param s the moment the action starts
     * @param f the moment the action is finished
     */
    public Retype(Node n, int s, int f) {
        super(n, s, f);
    }

    /**
     * Test the equality with another object.
     *
     * @param obj The object to compare with
     * @return true if o is an instance of Retype and if both actions act on the same node
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        } else if (obj == this) {
            return true;
        } else if (obj.getClass() == this.getClass()) {
            return this.getNode().equals(((Retype) obj).getNode());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.getNode().hashCode();
    }

    /**
     * Textual representation of the retype action.
     *
     * @return a String
     */
    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder("retype(");
        buffer.append(this.getNode().getName());
        buffer.append(")");
        return buffer.toString();
    }

    /**
     * XXX
     *
     * @param c the configuration
     */
    @Override
    public boolean apply(Configuration c) {
        c.addOffline(node);
        node.setCurrentPlatform(newPlatform);
        return true;
    }

    /**
     * XXX
     *
     * @param src the configuration to check
     * @return {@code true}
     */
    @Override
    public boolean isCompatibleWith(Configuration src) {
        return true;
    }


    /**
     * XXX
     * 
     *
     * @param src the source configuration
     * @param dst the configuration to reach
     * @return true 
     */
    @Override
    public boolean isCompatibleWith(Configuration src, Configuration dst) {
        return true;
    }

    /**
     * Insert the action as an outgoing action. In creates resources!
     *
     * @param g the graph to use
     * @return true if the insertion succeed
     */
    @Override
    public boolean insertIntoGraph(TimedExecutionGraph g) {
        return g.getUnlockings(this.getNode()).add(this);
    }

    @Override
    public void injectToVisualizer(PlanVisualizer vis) {
        vis.inject(this);
    }

    @Override
    public void serialize(TimedReconfigurationPlanSerializer s) throws IOException {
        s.serialize(this);
    }

}
