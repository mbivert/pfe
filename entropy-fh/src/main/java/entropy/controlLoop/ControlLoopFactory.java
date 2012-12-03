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
package entropy.controlLoop;

import antlr.SemanticException;
import entropy.PropertiesHelper;
import entropy.PropertiesHelperException;
import entropy.execution.TimedReconfigurationExecuter;
import entropy.execution.driver.DriverFactory;
import entropy.monitoring.Monitor;
import entropy.plan.durationEvaluator.DurationEvaluator;
import entropy.plan.durationEvaluator.FastDurationEvaluatorFactory;
import entropy.vjob.builder.VJobBuilderFactory;
import entropy.vjob.builder.plasma.ConstraintsCalalogBuilderException;
import entropy.vjob.queue.FCFSPersistentQueue;
import entropy.vjob.queue.VJobsPool;
import org.antlr.runtime.RecognitionException;

import java.io.File;
import java.io.IOException;

/**
 * Factory to create and initialize control loop from a property file.
 * TODO: Define the different properties
 *
 * @author Fabien Hermenier
 */
public class ControlLoopFactory {

    public static final String CONTROL_LOOP = "controlLoop";
    /**
     * The identifier of the property that indicates the implementation of the control loop.
     */
    public static final String CONTROL_LOOP_IMPL_PROP = CONTROL_LOOP + "." + "impl";

    /**
     * The value to use for the property {@value #CONTROL_LOOP_IMPL_PROP} to use the control loop {@link CustomizableControlLoop}.
     */
    public static final String CUSTOM = "custom";

    /**
     * The identifier of the property that indicates the directory where to put logged configurations.
     */
    public static final String LOG_DIR_IDENTIFIER = "controlLoop.logsDir";

    /**
     * The current implementation.
     */
    private String impl;

    /**
     * The properties to use.
     */
    private PropertiesHelper properties;

    /**
     * Make a new factory using specific properties.
     *
     * @param props the properties to use
     * @throws entropy.PropertiesHelperException
     *          if an error occured while instantiate the control loop
     */
    public ControlLoopFactory(PropertiesHelper props) throws PropertiesHelperException {
        impl = props.getRequiredProperty(CONTROL_LOOP_IMPL_PROP);
        this.properties = props;
    }

    /**
     * Make a new control loop using properties and specific components.
     *
     * @param monitoring the monitoring module to use
     * @return an initialized control loop
     * @throws ControlLoopFactoryException if an error occurred while instantiating the control loop
     * @throws PropertiesHelperException   if an error occurred while reading the properties to configure the loop
     */
    public ControlLoop makeControlLoop(Monitor monitoring, VJobBuilderFactory fact) throws ControlLoopFactoryException, PropertiesHelperException {
        ControlLoop loop;

        if (impl.equals(CUSTOM)) {
            try {
                loop = makeCustom(monitoring, fact);
            } catch (Exception e) {
                throw new ControlLoopFactoryException(e.getMessage(), e);
            }
        } else {
            throw new ControlLoopFactoryException("No control loop implementation for '" + this.impl + "'");
        }
        loop.setLogsDir(properties.getRequiredProperty(LOG_DIR_IDENTIFIER));
        return loop;
    }

    private ControlLoop makeCustom(Monitor monitoring, VJobBuilderFactory fact) throws PropertiesHelperException, SemanticException, IOException, RecognitionException, InstantiationException, ConstraintsCalalogBuilderException {
        DurationEvaluator eval = FastDurationEvaluatorFactory.readFromProperties(properties);
        VJobsPool pool = new FCFSPersistentQueue(fact, new File(properties.getRequiredProperty(CONTROL_LOOP + "." + CUSTOM + ".vjobsPath")));
        CustomizableControlLoop loop = new CustomizableControlLoop(monitoring, pool, fact, eval, new TimedReconfigurationExecuter(new DriverFactory(this.properties)));
        loop.setAssignTimeout(properties.getRequiredPropertyAsInt(CONTROL_LOOP + "." + CUSTOM + ".assignTimeout"));
        loop.setPlanTimeout(properties.getRequiredPropertyAsInt(CONTROL_LOOP + "." + CUSTOM + ".planTimeout"));
        loop.setPredictionStep(properties.getRequiredPropertyAsInt(CONTROL_LOOP + "." + CUSTOM + ".prediction.step"));
        loop.allowReconfiguration(properties.getRequiredPropertyAsBoolean(CONTROL_LOOP + "." + CUSTOM + ".reconfigure"));
        return loop;
    }
}
