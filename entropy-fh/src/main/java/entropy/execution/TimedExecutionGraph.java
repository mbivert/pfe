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

package entropy.execution;

import entropy.configuration.ManagedElement;
import entropy.plan.action.Action;

import java.util.*;

/**
 * A graph to represent dependencies between the
 * actions to perform on a graph.
 * A managed element in the graph represents a potential lock.
 * A lock may be the source and the target of outgoing or incoming actions, respectively.
 *
 * @author Fabien Hermenier
 */
public class TimedExecutionGraph {

    /**
     * All the actions may have to wait for a unlock.
     */
    private Map<ManagedElement, Set<Action>> lockable;

    /**
     * All the actions that unlock a dependency.
     */
    private Map<ManagedElement, Set<Action>> unlockers;


    /**
     * Instantiate a new empty graph.
     */
    public TimedExecutionGraph() {
        this.lockable = new HashMap<ManagedElement, Set<Action>>();
        this.unlockers = new HashMap<ManagedElement, Set<Action>>();
    }

    /**
     * Extracts all the dependencies of the graph.
     * A dependency occurs when the start moment of a lockable action
     * is greater or equals to the finish moment of unlocker action that involve the
     * same managed element.
     *
     * @return a list of dependencies. May be empty
     */
    public Set<Dependencies> extractDependencies() {
        Set<Dependencies> l = new HashSet<Dependencies>();
        for (Map.Entry<ManagedElement, Set<Action>> e : this.lockable.entrySet()) {
            for (Action a : e.getValue()) {

                Dependencies dep = new Dependencies(a);
                for (Action a2 : getUnlockings(e.getKey())) {
                    if (a2 != a && a.getStartMoment() >= a2.getFinishMoment()) {
                        dep.addDependency(a2);
                    }
                }
                l.add(dep);
            }
        }
        return l;
    }

    /**
     * Get the lockable actions on an element.
     * A lockable action is an action that may have to wait for something on the element in order
     * to be executed (most of the time, just resources)
     *
     * @param n the element
     * @return a list, may be empty
     */
    public Set<Action> getLockables(ManagedElement n) {
        if (!this.lockable.containsKey(n)) {
            this.lockable.put(n, new HashSet<Action>());
        }
        return this.lockable.get(n);
    }

    /**
     * Get the unlocking actions on a element
     * An unlocking action is an action that participate in the dependencies destruction.
     * when it is performed
     *
     * @param n the element
     * @return a list, may be empty
     */
    public Set<Action> getUnlockings(ManagedElement n) {
        if (!this.unlockers.containsKey(n)) {
            this.unlockers.put(n, new HashSet<Action>());
        }
        return this.unlockers.get(n);
    }

    /**
     * Format the graph as an event-oriented plan using the direct dependencies
     * between the actions.
     *
     * @return a String
     */
    public String toEventAgenda() {
        StringBuilder buffer = new StringBuilder();
        Set<Dependencies> deps = this.extractDependencies();
        Map<Set<Action>, Set<Action>> events = new HashMap<Set<Action>, Set<Action>>();
        for (Dependencies dep : deps) {
            if (!events.containsKey(dep.getUnsatisfiedDependencies())) {
                events.put(dep.getUnsatisfiedDependencies(), new HashSet<Action>());
            }
            events.get(dep.getUnsatisfiedDependencies()).add(dep.getAction());
        }
        for (Map.Entry<Set<Action>, Set<Action>> e : events.entrySet()) {
            if (e.getKey().isEmpty()) {
                buffer.append("     ");
            } else {
                for (Iterator<Action> a = e.getKey().iterator(); a.hasNext(); ) {
                    buffer.append("!").append(a.next());
                    if (a.hasNext()) {
                        buffer.append("& ");
                    }
                }
            }
            buffer.append(" -> ");
            for (Iterator<Action> a = e.getValue().iterator(); a.hasNext(); ) {
                buffer.append(a.next());
                if (a.hasNext()) {
                    buffer.append(" & ");
                }
            }
            buffer.append("\n");
        }
        return buffer.toString();
    }
}
