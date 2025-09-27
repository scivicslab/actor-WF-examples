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
package com.scivicslab.actorwf.examples.terminal;

import java.io.InputStream;

import com.scivicslab.actorwf.IIActorSystem;
import com.scivicslab.actorwf.Interpreter;
import com.scivicslab.actorwf.InterpreterIIAR;
import com.scivicslab.actorwf.MatrixCode;
import com.scivicslab.pojoactor.ActorRef;

/**
 * Workflow-based application that executes tmux operations through YAML workflow definitions.
 * This application integrates TmuxSession and TmuxMonitor with the workflow interpreter,
 * allowing complex tmux interactions to be defined declaratively in YAML files.
 *
 * <p>The application expects a workflow name as a command-line argument and loads
 * the corresponding YAML file from the classpath at /code/[workflow-name].yaml</p>
 *
 * <p>Workflow execution is performed step-by-step through the interpreter,
 * with support for action-based control of tmux sessions and monitoring.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * java TmuxWorkflowApp tmux-demo
 * </pre>
 */
public class TmuxWorkflowApp {

    /**
     * Main entry point for the TmuxWorkflowApp.
     * Requires a workflow name as the first command-line argument.
     *
     * @param args command-line arguments where args[0] is the workflow name
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java TmuxWorkflowApp <workflow-name>");
            System.err.println("Example: java TmuxWorkflowApp tmux-demo");
            System.exit(1);
        }

        String workflowName = args[0];
        String yamlPath = "/code/" + workflowName + ".yaml";

        IIActorSystem system = new IIActorSystem("tmux-workflow-system");

        try {
            String sessionName = "workflow-session-" + System.currentTimeMillis();
            TmuxSession session = new TmuxSession(sessionName);
            TmuxMonitor monitor = new TmuxMonitor(session, 1000);

            TmuxSessionIIAR sessionActor = new TmuxSessionIIAR("session", session);
            TmuxMonitorIIAR monitorActor = new TmuxMonitorIIAR("monitor", monitor);

            system.addIIActor(sessionActor);
            system.addIIActor(monitorActor);

            Interpreter interpreter = new Interpreter.Builder()
                .loggerName("TmuxWorkflow")
                .team(system)
                .build();

            InterpreterIIAR interpreterActor = new InterpreterIIAR("@ip", interpreter, system);
            system.addIIActor(interpreterActor);

            System.out.println("Loading workflow from: " + yamlPath);
            try (InputStream yamlInput = TmuxWorkflowApp.class.getResourceAsStream(yamlPath)) {
                if (yamlInput == null) {
                    throw new RuntimeException("Workflow file not found: " + yamlPath);
                }

                interpreterActor.tell(i -> i.readYaml(yamlInput)).get();
            }

            System.out.println("Workflow loaded successfully");

            MatrixCode code = interpreterActor.ask(i -> i.getCode()).get();
            System.out.println("Executing workflow: " + code.getName());
            System.out.println();

            for (int i = 0; i < 50; i++) {
                interpreterActor.tell(interp -> interp.execCode()).get();
            }

            System.out.println("\nWorkflow execution completed");

        } catch (Exception e) {
            System.err.println("Error executing workflow: " + e.getMessage());
            e.printStackTrace();
        } finally {
            system.terminate();
            System.out.println("Actor system terminated");
        }
    }
}