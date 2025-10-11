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
package com.scivicslab.pojoactor.workflow.examples.claudecode;

import com.scivicslab.pojoactor.workflow.IIActorSystem;
import com.scivicslab.pojoactor.workflow.Interpreter;
import com.scivicslab.pojoactor.workflow.InterpreterIIAR;

import java.io.InputStream;

/**
 * Application that executes Claude Code workflows from YAML files.
 *
 * <p>This application demonstrates how to use the actor-WF workflow interpreter
 * to execute Claude Code session operations defined in YAML workflow files.</p>
 *
 * <p>Usage:</p>
 * <pre>
 * mvn exec:java -Dexec.mainClass="com.scivicslab.pojoactor.workflow.examples.claudecode.ClaudeCodeWorkflowApp" -Dexec.args="claude-code-basic"
 * </pre>
 *
 * @author Scivics Lab
 * @version 1.0
 */
public class ClaudeCodeWorkflowApp {

    /**
     * Main entry point for the Claude Code workflow application.
     *
     * @param args command line arguments - expects workflow name (e.g., "claude-code-basic")
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: ClaudeCodeWorkflowApp <workflow-name>");
            System.err.println("Example: ClaudeCodeWorkflowApp claude-code-basic");
            System.exit(1);
        }

        String workflowName = args[0];
        String yamlPath = "/code/" + workflowName + ".yaml";

        ClaudeCodeWorkflowApp app = new ClaudeCodeWorkflowApp();
        app.runWorkflow(yamlPath);
    }

    /**
     * Executes a Claude Code workflow from a YAML file.
     *
     * @param yamlPath the resource path to the YAML workflow file
     */
    public void runWorkflow(String yamlPath) {
        IIActorSystem system = new IIActorSystem("claude-code-workflow-system");

        try {
            String sessionName = "claude-workflow-" + System.currentTimeMillis();
            ClaudeCodeSession session = new ClaudeCodeSession(sessionName);
            ClaudeCodeSessionIIAR sessionActor = new ClaudeCodeSessionIIAR("session", session, system);
            system.addIIActor(sessionActor);

            Interpreter interpreter = new Interpreter.Builder()
                    .loggerName("ClaudeCodeWorkflow")
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

            System.out.println("=== Starting Claude Code Workflow ===");
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