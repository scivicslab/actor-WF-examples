/*
 * Copyright 2025 Scivics Lab
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.scivicslab.actorwf.examples.turing;

import com.scivicslab.pojoactor.workflow.IIActorSystem;
import com.scivicslab.pojoactor.workflow.Interpreter;
import com.scivicslab.pojoactor.workflow.InterpreterIIAR;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;

/**
 * Application that executes Turing machine workflows from YAML files.
 *
 * <p>This application demonstrates how to use the actor-WF workflow interpreter
 * to execute Turing machine operations defined in YAML workflow files. It provides
 * a declarative approach to Turing machine programming where the state machine logic
 * is defined in YAML rather than Java code.</p>
 *
 * <p>The application creates an actor system with a Turing machine actor and an
 * interpreter actor. The interpreter loads and executes workflow definitions from
 * YAML files located in the resources directory.</p>
 *
 * <p>Usage:</p>
 * <pre>
 * # Run turing123 workflow
 * mvn exec:java -Dexec.mainClass="com.scivicslab.actorwf.examples.turing.TuringWorkflowApp" -Dexec.args="turing123"
 *
 * # Run turing134 workflow
 * mvn exec:java -Dexec.mainClass="com.scivicslab.actorwf.examples.turing.TuringWorkflowApp" -Dexec.args="turing134"
 * </pre>
 *
 * <p>Key features:</p>
 * <ul>
 * <li>YAML-based workflow definition for Turing machines</li>
 * <li>Actor-based execution model with isolation and concurrency</li>
 * <li>Interpreter pattern for workflow execution</li>
 * <li>Support for multiple workflow files</li>
 * </ul>
 */
public class TuringWorkflowApp {

    /**
     * Main entry point for the Turing workflow application.
     *
     * @param args command line arguments - expects workflow name (e.g., "turing123" or "turing134")
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: TuringWorkflowApp <workflow-name>");
            System.err.println("Example: TuringWorkflowApp turing123");
            System.exit(1);
        }

        String workflowName = args[0];
        String yamlPath = "/code/" + workflowName + ".yaml";

        TuringWorkflowApp app = new TuringWorkflowApp();
        app.runWorkflow(yamlPath);
    }

    /**
     * Executes a Turing machine workflow from a YAML file.
     *
     * <p>This method sets up an actor system with a Turing machine actor and an
     * interpreter actor, loads the specified YAML workflow, and executes it for
     * up to 50 iterations. The workflow execution is logged and the final tape
     * state is displayed.</p>
     *
     * <p>The method handles the complete lifecycle:</p>
     * <ul>
     * <li>Creating the actor system</li>
     * <li>Registering the Turing machine and interpreter actors</li>
     * <li>Loading the YAML workflow from resources</li>
     * <li>Executing the workflow iteratively</li>
     * <li>Printing the final tape state</li>
     * <li>Cleaning up actors and system resources</li>
     * </ul>
     *
     * @param yamlPath the resource path to the YAML workflow file (e.g., "/code/turing123.yaml")
     */
    public void runWorkflow(String yamlPath) {
        IIActorSystem system = new IIActorSystem("turing-system");

        try {
            // Create Turing machine actor
            Turing turing = new Turing();
            TuringIIAR turingActor = new TuringIIAR("turing", turing, system);
            system.addIIActor(turingActor);

            // Create interpreter
            Interpreter interpreter = new Interpreter.Builder()
                    .loggerName("TuringWorkflow")
                    .team(system)
                    .build();

            // Create interpreter actor
            InterpreterIIAR interpreterActor = new InterpreterIIAR("interpreter", interpreter, system);
            system.addIIActor(interpreterActor);

            // Load and execute workflow
            System.out.println("Loading workflow from: " + yamlPath);
            InputStream yamlStream = getClass().getResourceAsStream(yamlPath);
            if (yamlStream == null) {
                System.err.println("ERROR: Workflow file not found: " + yamlPath);
                System.exit(1);
            }

            interpreterActor.tell(i -> i.readYaml(yamlStream)).get();
            System.out.println("Workflow loaded successfully");

            // Execute workflow (50 iterations maximum)
            System.out.println("Executing workflow...\n");
            for (int i = 0; i < 50; i++) {
                interpreterActor.tell(interp -> interp.execCode()).get();
                // Continue executing until workflow completes or max iterations reached
            }

            // Final tape output
            turingActor.tell(t -> t.printTape()).get();

        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error executing workflow: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Clean up
            system.terminateIIActors();
            system.terminate();
        }
    }
}