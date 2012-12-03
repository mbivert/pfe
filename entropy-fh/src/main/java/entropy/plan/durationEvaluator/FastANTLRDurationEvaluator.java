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

import entropy.configuration.Node;
import entropy.configuration.VirtualMachine;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.Token;
import org.antlr.runtime.tree.CommonTreeAdaptor;

/**
 * An ANTLR-Based duration evaluator to parse the cost functions in the properties file.
 * Accept classical math expression and some variables:
 * VM#memory, VM#cpu_consumption, VM#cpu_demand, VM#cpu_nb
 * node#memory, node#cpu_capacity, node#cpu_nb
 *
 * @author Fabien Hermenier
 */
public class FastANTLRDurationEvaluator extends CommonTreeAdaptor implements DurationEvaluator {

    /**
     * Index for the migration.
     */
    private static final int MIGRATE_STREAM = 0;

    /**
     * Index for the run.
     */
    private static final int RUN_STREAM = 1;

    /**
     * Index for the stop.
     */
    private static final int STOP_STREAM = 2;

    /**
     * Index for the local suspend.
     */
    private static final int LOCAL_SUSPEND_STREAM = 3;

    /**
     * Index for the local resume.
     */
    private static final int LOCAL_RESUME_STREAM = 4;

    /**
     * Index for the remote resume.
     */
    private static final int REMOTE_RESUME_STREAM = 5;

    /**
     * Index for the startup.
     */
    private static final int STARTUP_STREAM = 6;

    /**
     * Index for the shutdown.
     */
    private static final int SHUTDOWN_STREAM = 7;

    /**
     * Index for the forge.
     */
    private static final int FORGE_STREAM = 8;

    private EvaluatorTree[] exprs;

    public FastANTLRDurationEvaluator(String migrationExpr,
                                      String stopExpr,
                                      String runExpr,
                                      String localSuspendExpr,
                                      String localResumeExpr,
                                      String remoteResumeExpr,
                                      String startupExpr,
                                      String shutdownExpr,
                                      String forgeExpr) throws DurationEvaluationException {
        this.exprs = new EvaluatorTree[9];
        try {
            this.exprs[RUN_STREAM] = buildAST(runExpr);
            this.exprs[MIGRATE_STREAM] = buildAST(migrationExpr);
            this.exprs[STOP_STREAM] = buildAST(stopExpr);
            this.exprs[REMOTE_RESUME_STREAM] = buildAST(remoteResumeExpr);
            this.exprs[LOCAL_RESUME_STREAM] = buildAST(localResumeExpr);
            this.exprs[LOCAL_SUSPEND_STREAM] = buildAST(localSuspendExpr);
            this.exprs[STARTUP_STREAM] = buildAST(startupExpr);
            this.exprs[SHUTDOWN_STREAM] = buildAST(shutdownExpr);
            this.exprs[FORGE_STREAM] = buildAST(forgeExpr);
        } catch (RecognitionException e) {
            throw new DurationEvaluationException(e.getMessage(), e);
        }
    }

    @Override
    public Object create(Token payload) {
        if (payload == null) {
            return new EvaluatorTree(payload);
        }
        switch (payload.getType()) {
            case ANTLRDurationEvaluator2Parser.DIV:
            case ANTLRDurationEvaluator2Parser.PLUS:
            case ANTLRDurationEvaluator2Parser.MINUS:
            case ANTLRDurationEvaluator2Parser.POW:
            case ANTLRDurationEvaluator2Parser.MULTIPLY:
                return new Operator(payload);
            case ANTLRDurationEvaluator2Parser.INT:
            case ANTLRDurationEvaluator2Parser.FLOAT:
            case ANTLRDurationEvaluator2Parser.VAR:
                return new Operand(payload);
            default:
                throw new UnsupportedOperationException("Type " + ANTLRDurationEvaluator2Parser.tokenNames[payload.getType()]);
        }
    }

    private EvaluatorTree buildAST(String str) throws RecognitionException {
        ANTLRDurationEvaluator2Lexer lexer = new ANTLRDurationEvaluator2Lexer(new ANTLRStringStream(str));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ANTLRDurationEvaluator2Parser parser = new ANTLRDurationEvaluator2Parser(tokens);

        parser.setTreeAdaptor(this);
        return (EvaluatorTree) parser.evaluate().getTree();
    }

    @Override
    public int evaluateMigration(VirtualMachine vm) throws DurationEvaluationException {
        return (int) exprs[MIGRATE_STREAM].evaluate(vm);
    }

    @Override
    public int evaluateRun(VirtualMachine vm) throws DurationEvaluationException {
        return (int) exprs[RUN_STREAM].evaluate(vm);
    }

    @Override
    public int evaluateStop(VirtualMachine vm) throws DurationEvaluationException {
        return (int) exprs[STOP_STREAM].evaluate(vm);
    }

    @Override
    public int evaluateLocalSuspend(VirtualMachine vm) throws DurationEvaluationException {
        return (int) exprs[LOCAL_SUSPEND_STREAM].evaluate(vm);
    }

    @Override
    public int evaluateLocalResume(VirtualMachine vm) throws DurationEvaluationException {
        return (int) exprs[LOCAL_RESUME_STREAM].evaluate(vm);
    }

    @Override
    public int evaluateRemoteResume(VirtualMachine vm) throws DurationEvaluationException {
        return (int) exprs[REMOTE_RESUME_STREAM].evaluate(vm);
    }

    @Override
    public int evaluateForge(VirtualMachine vm) throws DurationEvaluationException {
        return (int) exprs[FORGE_STREAM].evaluate(vm);
    }

    @Override
    public int evaluateStartup(Node node) throws DurationEvaluationException {
        return (int) exprs[STARTUP_STREAM].evaluate(node);
    }

    @Override
    public int evaluateShutdown(Node node) throws DurationEvaluationException {
        return (int) exprs[SHUTDOWN_STREAM].evaluate(node);
    }
}
