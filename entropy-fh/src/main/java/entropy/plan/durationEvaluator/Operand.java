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

package entropy.plan.durationEvaluator;

import entropy.configuration.ManagedElement;
import entropy.configuration.Node;
import entropy.configuration.VirtualMachine;
import org.antlr.runtime.Token;

/**
 * @author Fabien Hermenier
 */
public class Operand extends EvaluatorTree {

    private double val;

    private String var = null;

    public Operand(Token payload) {
        super(payload);
        switch (payload.getType()) {
            case ANTLRDurationEvaluator2Parser.INT:
            case ANTLRDurationEvaluator2Parser.FLOAT:
                val = Double.parseDouble(payload.getText());
                break;
            case ANTLRDurationEvaluator2Parser.VAR:
                var = payload.getText();
                break;
            default:
                throw new UnsupportedOperationException("Unsupported type: " + ANTLRDurationEvaluator2Parser.tokenNames[payload.getType()]);
        }
    }

    @Override
    public double evaluate(ManagedElement e) {
        if (var == null) {
            return val;
        } else {
            if (e instanceof VirtualMachine) {
                /*VM#memory, VM#cpu_consumption, VM#cpu_demand, VM#cpu_nb */

                VirtualMachine vm = (VirtualMachine) e;
                if (var.equals("VM#memory")) {
                    return vm.getMemoryConsumption();
                } else if (var.equals("VM#cpu_consumption") || var.equals("VM#cpu_cons")) {
                    return vm.getCPUConsumption();
                } else if (var.equals("VM#cpu_demand")) {
                    return vm.getCPUDemand();
                } else if (var.equals("VM#cpu_nb")) {
                    return vm.getNbOfCPUs();
                } else {
                    throw new UnsupportedOperationException("Unsupported variable: " + var);
                }
            } else if (e instanceof Node) {
                /*Variables node#memory, node#cpu_capacity, node#cpu_nb can be used */
                Node n = (Node) e;
                if (var.equals("node#memory")) {
                    return n.getMemoryCapacity();
                } else if (var.equals("node#cpu_capacity") || var.equals("node#cpu_capa")) {
                    return n.getCPUCapacity();
                } else if (var.equals("node#cpu_nb")) {
                    return n.getNbOfCPUs();
                } else {
                    throw new UnsupportedOperationException("Unsupported variable: " + var);
                }
            }
            throw new UnsupportedOperationException("Uuk: " + e);
        }

    }
}
