package com.scivicslab.claudecode;

import java.io.InputStream;

import com.scivicslab.actorwf.IIActorSystem;
import com.scivicslab.actorwf.Interpreter;
import com.scivicslab.actorwf.InterpreterIIAR;
import com.scivicslab.actorwf.MatrixCode;
import com.scivicslab.pojoactor.ActorRef;

public class ClaudeCodeWorkflowApp {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java ClaudeCodeWorkflowApp <workflow-name>");
            System.err.println("Example: java ClaudeCodeWorkflowApp claude-code-basic");
            System.exit(1);
        }

        String workflowName = args[0];
        String yamlPath = "/code/" + workflowName + ".yaml";

        IIActorSystem system = new IIActorSystem("claude-code-workflow-system");

        try {
            String sessionName = "claude-workflow-" + System.currentTimeMillis();
            ClaudeCodeSession session = new ClaudeCodeSession(sessionName);
            ClaudeCodeMonitor monitor = new ClaudeCodeMonitor(session, 1000);

            ClaudeCodeSessionIIAR sessionActor = new ClaudeCodeSessionIIAR("session", session);
            ClaudeCodeMonitorIIAR monitorActor = new ClaudeCodeMonitorIIAR("monitor", monitor);

            system.addIIActor(sessionActor);
            system.addIIActor(monitorActor);

            Interpreter interpreter = new Interpreter.Builder()
                .loggerName("ClaudeCodeWorkflow")
                .team(system)
                .build();
            InterpreterIIAR interpreterActor = new InterpreterIIAR("@ip", interpreter, system);
            system.addIIActor(interpreterActor);

            System.out.println("Loading workflow from: " + yamlPath);
            try (InputStream yamlInput = ClaudeCodeWorkflowApp.class.getResourceAsStream(yamlPath)) {
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