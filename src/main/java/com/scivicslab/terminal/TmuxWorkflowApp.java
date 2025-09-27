package com.scivicslab.terminal;

import java.io.InputStream;

import com.scivicslab.actorwf.IIActorSystem;
import com.scivicslab.actorwf.Interpreter;
import com.scivicslab.actorwf.InterpreterIIAR;
import com.scivicslab.actorwf.MatrixCode;
import com.scivicslab.pojoactor.ActorRef;

public class TmuxWorkflowApp {

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