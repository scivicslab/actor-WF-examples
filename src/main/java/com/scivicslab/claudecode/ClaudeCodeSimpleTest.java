package com.scivicslab.claudecode;

import com.scivicslab.pojoactor.ActorRef;
import com.scivicslab.pojoactor.ActorSystem;
import java.util.List;

public class ClaudeCodeSimpleTest {

    public static void main(String[] args) {
        ActorSystem system = new ActorSystem.Builder("claude-simple-system").build();

        String sessionName = "claude-simple-" + System.currentTimeMillis();
        ClaudeCodeSession session = new ClaudeCodeSession(sessionName);

        ActorRef<ClaudeCodeSession> sessionRef = system.actorOf("session", session);

        try {
            System.out.println("Creating session and starting Claude Code...");
            sessionRef.tell(s -> {
                try {
                    s.createSession();
                    s.startClaudeCode();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).get();

            Thread.sleep(3000);

            // Test 1
            System.out.println("\n=== Test 1: Simple math question ===");
            System.out.println("Sending: What is 2 + 2?");

            sessionRef.tell(s -> {
                try {
                    s.sendPrompt("What is 2 + 2?");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).get();

            System.out.println("Waiting 15 seconds for response...");
            Thread.sleep(15000);

            List<String> output1 = sessionRef.ask(s -> {
                try {
                    return s.captureOutput();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).get();

            System.out.println("\n--- Output (last 15 lines) ---");
            printLastNLines(output1, 15);

            // Test 2
            System.out.println("\n=== Test 2: Another math question ===");
            System.out.println("Sending: What is 5 * 3?");

            sessionRef.tell(s -> {
                try {
                    s.sendPrompt("What is 5 * 3?");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).get();

            System.out.println("Waiting 15 seconds for response...");
            Thread.sleep(15000);

            List<String> output2 = sessionRef.ask(s -> {
                try {
                    return s.captureOutput();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).get();

            System.out.println("\n--- Output (last 15 lines) ---");
            printLastNLines(output2, 15);

            // Test 3
            System.out.println("\n=== Test 3: Final question ===");
            System.out.println("Sending: What is 10 - 7?");

            sessionRef.tell(s -> {
                try {
                    s.sendPrompt("What is 10 - 7?");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).get();

            System.out.println("Waiting 15 seconds for response...");
            Thread.sleep(15000);

            List<String> output3 = sessionRef.ask(s -> {
                try {
                    return s.captureOutput();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).get();

            System.out.println("\n--- Output (last 15 lines) ---");
            printLastNLines(output3, 15);

            System.out.println("\n=== Test completed ===");
            System.out.println("Successfully sent 3 prompts and captured responses");

            // Cleanup
            System.out.println("\nCleaning up...");
            sessionRef.tell(s -> {
                try {
                    s.sendCtrlD();
                    Thread.sleep(1000);
                    s.killSession();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).get();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            system.terminate();
            System.out.println("Done.");
        }
    }

    private static void printLastNLines(List<String> lines, int n) {
        int start = Math.max(0, lines.size() - n);
        for (int i = start; i < lines.size(); i++) {
            System.out.println(lines.get(i));
        }
    }
}