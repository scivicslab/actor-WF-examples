/*
 * Copyright 2025 Scivicslab
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
package com.scivicslab.pojoactor.workflow.examples.codex;

import com.scivicslab.pojoactor.workflow.IIActorSystem;
import com.scivicslab.pojoactor.workflow.Interpreter;
import com.scivicslab.pojoactor.workflow.InterpreterIIAR;

import java.io.InputStream;

/**
 * Application that executes Codex workflows from YAML files.
 *
 * <p>This application demonstrates how to use the actor-WF workflow interpreter
 * to execute Codex session operations defined in YAML workflow files.</p>
 *
 * <p>Usage:</p>
 * <pre>
 * mvn exec:java -Dexec.mainClass="com.scivicslab.pojoactor.workflow.examples.codex.CodexWorkflowApp" -Dexec.args="codex-basic"
 * </pre>
 *
 * @author Scivics Lab
 * @version 1.0
 */
public class CodexWorkflowApp {

    /**
     * Main entry point for the Codex workflow application.
     *
     * @param args command line arguments - expects workflow name (e.g., "codex-basic")
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: CodexWorkflowApp <workflow-name>");
            System.err.println("Example: CodexWorkflowApp codex-basic");
            System.exit(1);
        }

        String workflowName = args[0];
        String yamlPath = "/code/" + workflowName + ".yaml";

        CodexWorkflowApp app = new CodexWorkflowApp();
        app.runWorkflow(yamlPath);
    }

    /**
     * Executes a Codex workflow from a YAML file.
     *
     * @param yamlPath the resource path to the YAML workflow file
     */
    public void runWorkflow(String yamlPath) {
        IIActorSystem system = new IIActorSystem("codex-workflow-system");

        try {
            String sessionName = "codex-workflow-" + System.currentTimeMillis();
            CodexSession session = new CodexSession(sessionName);
            CodexSessionIIAR sessionActor = new CodexSessionIIAR("session", session, system);
            system.addIIActor(sessionActor);

            Interpreter interpreter = new Interpreter.Builder()
                    .loggerName("CodexWorkflow")
                    .team(system)
                    .build();

            InterpreterIIAR interpreterActor = new InterpreterIIAR("interpreter", interpreter, system);
            system.addIIActor(interpreterActor);

            System.out.println("Loading workflow from: " + yamlPath);
            InputStream yamlStream = getClass().getResourceAsStream(yamlPath);
            if (yamlStream == null) {
                System.err.println("ERROR: Workflow file not found: " + yamlPath);
                System.exit(1);
            }

            interpreterActor.tell(i -> i.readYaml(yamlStream)).get();
            System.out.println("Workflow loaded successfully");

            System.out.println("=== Starting Codex Workflow ===");
            System.out.println("Workflow: " + yamlPath);
            System.out.println();

            for (int i = 0; i < 50; i++) {
                interpreterActor.tell(interp -> interp.execCode()).get();
            }

            System.out.println("\n=== Workflow Finished ===");

        } catch (Exception e) {
            System.err.println("Error executing workflow: " + e.getMessage());
            e.printStackTrace();
        } finally {
            system.terminateIIActors();
            system.terminate();
        }
    }
}