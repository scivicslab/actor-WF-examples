/*
 * Copyright 2025 devteam@scivics-lab.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.scivicslab.turing;

import com.scivicslab.pojoactor.core.ActionResult;
import com.scivicslab.pojoactor.workflow.IIActorSystem;
import com.scivicslab.pojoactor.workflow.Interpreter;

import java.io.InputStream;

/**
 * Application that executes Turing machine workflows from YAML files.
 *
 * <p>This application demonstrates how to use the actor-WF workflow interpreter
 * to execute Turing machine operations defined in YAML workflow files.</p>
 *
 * <p>Usage:</p>
 * <pre>
 * # Run turing123 workflow
 * mvn exec:java -Dexec.mainClass="com.scivicslab.turing.TuringWorkflowApp" -Dexec.args="turing123"
 *
 * # Run turing134 workflow
 * mvn exec:java -Dexec.mainClass="com.scivicslab.turing.TuringWorkflowApp" -Dexec.args="turing134"
 * </pre>
 *
 * @author devteam@scivics-lab.com
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
     * @param yamlPath the resource path to the YAML workflow file
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

            // Load workflow
            System.out.println("Loading workflow from: " + yamlPath);
            InputStream yamlStream = getClass().getResourceAsStream(yamlPath);
            if (yamlStream == null) {
                System.err.println("ERROR: Workflow file not found: " + yamlPath);
                System.exit(1);
            }

            interpreter.readYaml(yamlStream);
            System.out.println("Workflow loaded successfully");

            // Execute workflow (200 iterations maximum)
            System.out.println("Executing workflow...\n");
            ActionResult result = interpreter.runUntilEnd(200);

            // Show result
            if (result.isSuccess()) {
                System.out.println("\nWorkflow completed successfully: " + result.getResult());
            } else {
                System.out.println("\nWorkflow finished: " + result.getResult());
            }

        } finally {
            // Clean up
            system.terminateIIActors();
            system.terminate();
        }
    }
}