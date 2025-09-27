package com.scivicslab.claudecode;

import com.scivicslab.pojoactor.ActorRef;
import com.scivicslab.pojoactor.ActorSystem;

public class ClaudeCodeConversationApp {

    public static void main(String[] args) {
        ActorSystem system = new ActorSystem.Builder("claude-conversation-system").build();

        String sessionName = "claude-conv-" + System.currentTimeMillis();
        ClaudeCodeSession session = new ClaudeCodeSession(sessionName);

        ActorRef<ClaudeCodeSession> sessionRef = system.actorOf("session", session);
        ActorRef<ClaudeCodeMonitor> monitorRef = system.actorOf("monitor",
            new ClaudeCodeMonitor(session, 2000));

        try {
            System.out.println("=== Creating session ===");
            sessionRef.tell(s -> {
                try {
                    s.createSession();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).get();

            System.out.println("\n=== Starting Claude Code ===");
            sessionRef.tell(s -> {
                try {
                    s.startClaudeCode();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).get();

            monitorRef.tell(m -> m.startMonitoring()).get();

            Thread.sleep(5000);

            System.out.println("\n=== Initial State ===");
            sessionRef.tell(s -> {
                try {
                    var output = s.captureOutput();
                    for (String line : output) {
                        System.out.println(line);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).get();

            // First exchange
            System.out.println("\n=== Turn 1: Sending first prompt ===");
            String prompt1 = "What is 2 + 2? Please answer briefly.";
            System.out.println("Prompt: " + prompt1);
            sessionRef.tell(s -> {
                try {
                    s.sendPrompt(prompt1);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).get();

            System.out.println("Waiting for response (30 seconds)...");
            Thread.sleep(30000);

            System.out.println("\n=== Turn 1: Claude's response ===");
            sessionRef.tell(s -> {
                try {
                    var output = s.captureOutput();
                    int startLine = Math.max(0, output.size() - 20);
                    for (int i = startLine; i < output.size(); i++) {
                        System.out.println(output.get(i));
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).get();

            // Second exchange
            System.out.println("\n=== Turn 2: Sending second prompt ===");
            String prompt2 = "What is 5 * 3? Please answer briefly.";
            System.out.println("Prompt: " + prompt2);
            sessionRef.tell(s -> {
                try {
                    s.sendPrompt(prompt2);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).get();

            System.out.println("Waiting for response (30 seconds)...");
            Thread.sleep(30000);

            System.out.println("\n=== Turn 2: Claude's response ===");
            sessionRef.tell(s -> {
                try {
                    var output = s.captureOutput();
                    int startLine = Math.max(0, output.size() - 20);
                    for (int i = startLine; i < output.size(); i++) {
                        System.out.println(output.get(i));
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).get();

            // Third exchange
            System.out.println("\n=== Turn 3: Sending third prompt ===");
            String prompt3 = "What is 10 - 7? Please answer briefly.";
            System.out.println("Prompt: " + prompt3);
            sessionRef.tell(s -> {
                try {
                    s.sendPrompt(prompt3);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).get();

            System.out.println("Waiting for response (30 seconds)...");
            Thread.sleep(30000);

            System.out.println("\n=== Turn 3: Claude's response ===");
            sessionRef.tell(s -> {
                try {
                    var output = s.captureOutput();
                    int startLine = Math.max(0, output.size() - 20);
                    for (int i = startLine; i < output.size(); i++) {
                        System.out.println(output.get(i));
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).get();

            System.out.println("\n=== Conversation Summary ===");
            System.out.println("Completed 3 exchanges with Claude Code");
            System.out.println("1. " + prompt1);
            System.out.println("2. " + prompt2);
            System.out.println("3. " + prompt3);

            System.out.println("\n=== Cleaning up ===");
            monitorRef.tell(m -> m.stopMonitoring()).get();

            System.out.println("Sending Ctrl-D to exit...");
            sessionRef.tell(s -> {
                try {
                    s.sendCtrlD();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).get();

            Thread.sleep(2000);

            System.out.println("Killing session...");
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