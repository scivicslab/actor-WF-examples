package com.scivicslab.claudecode;

import com.scivicslab.pojoactor.ActorRef;
import com.scivicslab.pojoactor.ActorSystem;

public class ClaudeCodeApp {

    public static void main(String[] args) {
        ActorSystem system = new ActorSystem.Builder("claude-code-system").build();

        String sessionName = "claude-session-" + System.currentTimeMillis();
        ClaudeCodeSession session = new ClaudeCodeSession(sessionName);

        ActorRef<ClaudeCodeSession> sessionRef = system.actorOf("session", session);
        ActorRef<ClaudeCodeMonitor> monitorRef = system.actorOf("monitor",
            new ClaudeCodeMonitor(session, 1000));

        try {
            System.out.println("Creating tmux session: " + sessionName);
            sessionRef.tell(s -> {
                try {
                    s.createSession();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).get();

            System.out.println("Starting Claude Code...");
            sessionRef.tell(s -> {
                try {
                    s.startClaudeCode();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).get();

            System.out.println("Starting monitor...");
            monitorRef.tell(m -> m.startMonitoring()).get();

            Thread.sleep(5000);

            System.out.println("\n=== Initial State ===");
            monitorRef.tell(m -> m.printLatestOutput()).get();

            System.out.println("\nSending prompt: Hello, can you help me?");
            sessionRef.tell(s -> {
                try {
                    s.sendPrompt("Hello, can you help me?");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).get();

            System.out.println("Waiting for response...");
            Thread.sleep(10000);

            System.out.println("\n=== After First Prompt ===");
            monitorRef.tell(m -> m.printLatestOutput()).get();

            ClaudeCodeOutput output = monitorRef.ask(m -> m.getLatestOutput()).get();
            if (output != null && output.isWaitingForInput()) {
                System.out.println("\nClaude Code is waiting for input!");
                System.out.println("Prompt type: " + output.getPromptType());

                if (output.hasChoices()) {
                    System.out.println("Available choices:");
                    for (int i = 0; i < output.getChoices().size(); i++) {
                        System.out.println("  " + (i + 1) + ". " + output.getChoices().get(i));
                    }
                }
            }

            System.out.println("\nStopping monitor...");
            monitorRef.tell(m -> m.stopMonitoring()).get();

            System.out.println("Sending Ctrl-D to exit Claude Code...");
            sessionRef.tell(s -> {
                try {
                    s.sendCtrlD();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).get();

            Thread.sleep(2000);

            System.out.println("Killing tmux session...");
            sessionRef.tell(s -> {
                try {
                    s.killSession();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).get();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            system.terminate();
            System.out.println("\nActor system terminated");
        }
    }
}