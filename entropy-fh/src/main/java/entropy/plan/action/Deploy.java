/*
 * Copyright (c) Fabien Hermenier
 *
 *        This file is part of Entropy.
 *
 *        Entropy is free software: you can redistribute it and/or modify
 *        it under the terms of the GNU Lesser General Public License as published by
 *        the Free Software Foundation, either version 3 of the License, or
 *        (at your option) any later version.
 *
 *        Entropy is distributed in the hope that it will be useful,
 *        but WITHOUT ANY WARRANTY; without even the implied warranty of
 *        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *        GNU Lesser General Public License for more details.
 *
 *        You should have received a copy of the GNU Lesser General Public License
 *        along with Entropy.  If not, see <http://www.gnu.org/licenses/>.
 */

package entropy.plan.action;

import entropy.configuration.Configuration;
import entropy.configuration.Node;
import entropy.execution.TimedExecutionGraph;
import entropy.plan.parser.TimedReconfigurationPlanSerializer;
import entropy.plan.visualization.PlanVisualizer;

import java.io.IOException;

/**
 * A node action to deploy an hosting platform on a node that
 * is compatible with.
 * The node must be online and not host any virtual machines.
 * At the end of the action, the node is online with the new
 * hosting platform ready to host VMs.
 *
 * @author Fabien Hermenier
 */
public class Deploy extends NodeAction {

    /**
     * The future hosting platform
     */
    private String p;

    /**
     * Make a new action.
     *
     * @param n the involved node
     * @param p the platform to deploy on the node. This platform must be available
     *          on this node
     */
    public Deploy(Node n, String p) {
        super(n);
        this.p = p;
    }

    /**
     * Make a new action.
     *
     * @param n  the involved node
     * @param p  the platform to deploy on the node. This platform must be available
     *           on this node
     * @param st the moment the action start
     * @param st the moment the action finishes
     */
    public Deploy(Node n, String p, int st, int ed) {
        super(n);
        this.p = p;
        this.setStartMoment(st);
        this.setFinishMoment(ed);
    }


    @Override
    public boolean isCompatibleWith(Configuration src) {
        Node n2 = src.getOnlines().get(getNode().getName());
        if (n2 == null) {
            return false;
        }

        return n2.getAvailablePlatforms().contains(p) && src.getRunnings(n2).isEmpty() && src.getSleepings(n2).isEmpty();
    }

    @Override
    public boolean isCompatibleWith(Configuration src, Configuration dst) {
        return isCompatibleWith(src)
                && dst.isOnline(getNode())
                && dst.getAllNodes().get(getNode().getName()).getCurrentPlatform().equals(p);
    }

    @Override
    public boolean apply(Configuration c) {
        Node n = c.getOnlines().get(getNode().getName());
        c.addOnline(n);
        return n.setCurrentPlatform(p);
    }

    @Override
    public boolean insertIntoGraph(TimedExecutionGraph graph) {
        return graph.getLockables(getNode()).add(this);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("deploy(").append(getNode().getName());
        b.append(',').append(p).append(")");
        return b.toString();
    }

    @Override
    public void injectToVisualizer(PlanVisualizer vis) {
        vis.inject(this);
    }

    @Override
    public void serialize(TimedReconfigurationPlanSerializer s) throws IOException {
        s.serialize(this);
    }

    /**
     * Get the platform that will be deployed
     *
     * @return the platform identifier
     */
    public String getPlatform() {
        return this.p;
    }
}
