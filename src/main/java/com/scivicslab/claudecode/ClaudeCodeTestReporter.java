package com.scivicslab.claudecode;

import com.scivicslab.pojoactor.ActorRef;
import com.scivicslab.pojoactor.ActorSystem;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ClaudeCodeTestReporter {

    private static class TestExchange {
        String prompt;
        long timestamp;
        List<String> response;
        long waitTimeMs;
        PromptType promptType;
        boolean responseReceived;

        TestExchange(String prompt, long timestamp, long waitTimeMs) {
            this.prompt = prompt;
            this.timestamp = timestamp;
            this.waitTimeMs = waitTimeMs;
            this.response = new ArrayList<>();
            this.responseReceived = false;
        }
    }

    private final List<TestExchange> exchanges = new ArrayList<>();
    private String sessionName;
    private long testStartTime;
    private long testEndTime;

    public static void main(String[] args) {
        ClaudeCodeTestReporter reporter = new ClaudeCodeTestReporter();
        reporter.runTest();
    }

    public void runTest() {
        ActorSystem system = new ActorSystem.Builder("claude-test-reporter-system").build();

        sessionName = "claude-test-" + System.currentTimeMillis();
        testStartTime = System.currentTimeMillis();

        ClaudeCodeSession session = new ClaudeCodeSession(sessionName);
        ActorRef<ClaudeCodeSession> sessionRef = system.actorOf("session", session);
        ActorRef<ClaudeCodeMonitor> monitorRef = system.actorOf("monitor",
            new ClaudeCodeMonitor(session, 2000));

        try {
            System.out.println("=== Starting Claude Code Test Reporter ===\n");

            sessionRef.tell(s -> {
                try {
                    s.createSession();
                    s.startClaudeCode();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).get();

            monitorRef.tell(m -> m.startMonitoring()).get();

            System.out.println("Waiting for Claude Code to start (5 seconds)...");
            Thread.sleep(5000);

            String[] testPrompts = {
                "What is 2 + 2? Please answer in one word.",
                "What is the capital of France? Please answer in one word.",
                "Is Java an object-oriented language? Please answer yes or no."
            };

            for (int i = 0; i < testPrompts.length; i++) {
                String prompt = testPrompts[i];
                System.out.println("\n--- Test " + (i + 1) + " ---");
                System.out.println("Sending: " + prompt);

                TestExchange exchange = new TestExchange(
                    prompt,
                    System.currentTimeMillis(),
                    20000
                );

                sessionRef.tell(s -> {
                    try {
                        s.sendPrompt(prompt);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).get();

                System.out.println("Waiting " + (exchange.waitTimeMs / 1000) + " seconds for response...");
                Thread.sleep(exchange.waitTimeMs);

                ClaudeCodeOutput output = sessionRef.ask(s -> {
                    try {
                        return s.captureAndParse();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).get();

                exchange.response = output.getLines();
                exchange.promptType = output.getPromptType();
                exchange.responseReceived = !exchange.response.isEmpty() &&
                    exchange.response.size() > 5;

                exchanges.add(exchange);

                System.out.println("Response received: " + exchange.responseReceived);
                System.out.println("Prompt type: " + exchange.promptType);
                System.out.println("Output lines: " + exchange.response.size());
            }

            testEndTime = System.currentTimeMillis();

            System.out.println("\n=== Cleaning up ===");
            monitorRef.tell(m -> m.stopMonitoring()).get();

            sessionRef.tell(s -> {
                try {
                    s.sendCtrlD();
                    Thread.sleep(1000);
                    s.killSession();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).get();

            String reportPath = generateReport();
            System.out.println("\n=== Test Complete ===");
            System.out.println("Report saved to: " + reportPath);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            system.terminate();
        }
    }

    private String generateReport() {
        String timestamp = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
        );
        String filename = "reports/claude-code-test-report-" + timestamp + ".md";

        try (FileWriter writer = new FileWriter(filename)) {
            writer.write("# Claude Code Test Report\n\n");
            writer.write("**Generated:** " + LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            ) + "\n\n");
            writer.write("**Session Name:** " + sessionName + "\n\n");
            writer.write("**Total Test Duration:** " +
                (testEndTime - testStartTime) / 1000 + " seconds\n\n");

            writer.write("---\n\n");
            writer.write("## Test Summary\n\n");
            writer.write("| Test # | Prompt | Response Received | Prompt Type | Wait Time |\n");
            writer.write("|--------|--------|-------------------|-------------|------------|\n");

            for (int i = 0; i < exchanges.size(); i++) {
                TestExchange ex = exchanges.get(i);
                writer.write(String.format("| %d | %s | %s | %s | %d sec |\n",
                    i + 1,
                    truncate(ex.prompt, 50),
                    ex.responseReceived ? "✓" : "✗",
                    ex.promptType,
                    ex.waitTimeMs / 1000
                ));
            }

            writer.write("\n---\n\n");
            writer.write("## Detailed Results\n\n");

            for (int i = 0; i < exchanges.size(); i++) {
                TestExchange ex = exchanges.get(i);
                writer.write("### Test " + (i + 1) + "\n\n");
                writer.write("**Prompt:** " + ex.prompt + "\n\n");
                writer.write("**Timestamp:** " +
                    LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(ex.timestamp),
                        java.time.ZoneId.systemDefault()
                    ).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    + "\n\n");
                writer.write("**Wait Time:** " + ex.waitTimeMs / 1000 + " seconds\n\n");
                writer.write("**Response Received:** " +
                    (ex.responseReceived ? "Yes ✓" : "No ✗") + "\n\n");
                writer.write("**Prompt Type:** " + ex.promptType + "\n\n");
                writer.write("**Output Lines:** " + ex.response.size() + "\n\n");

                if (!ex.response.isEmpty()) {
                    writer.write("**Terminal Output (last 30 lines):**\n\n");
                    writer.write("```\n");
                    int start = Math.max(0, ex.response.size() - 30);
                    for (int j = start; j < ex.response.size(); j++) {
                        writer.write(ex.response.get(j) + "\n");
                    }
                    writer.write("```\n\n");
                } else {
                    writer.write("**Terminal Output:** (empty)\n\n");
                }

                writer.write("---\n\n");
            }

            writer.write("## Analysis\n\n");

            long successCount = exchanges.stream()
                .filter(ex -> ex.responseReceived)
                .count();

            writer.write("**Success Rate:** " + successCount + " / " +
                exchanges.size() + " (" +
                (exchanges.size() > 0 ? (successCount * 100 / exchanges.size()) : 0) +
                "%)\n\n");

            if (successCount == 0) {
                writer.write("**Status:** ⚠️ **CRITICAL ISSUE** - No responses received from Claude Code\n\n");
                writer.write("**Possible Causes:**\n");
                writer.write("- Claude Code CLI not authenticated\n");
                writer.write("- Network connectivity issues\n");
                writer.write("- API key not configured\n");
                writer.write("- Insufficient wait time (though 20 seconds should be adequate)\n");
                writer.write("- Claude Code not starting properly in tmux session\n\n");
                writer.write("**Recommended Actions:**\n");
                writer.write("1. Verify Claude Code authentication: `claude --version`\n");
                writer.write("2. Test Claude Code manually: `claude`\n");
                writer.write("3. Check network connectivity\n");
                writer.write("4. Increase wait time to 30-60 seconds\n");
                writer.write("5. Check tmux session manually: `tmux attach -t " + sessionName + "`\n");
            } else if (successCount < exchanges.size()) {
                writer.write("**Status:** ⚠️ Partial success - Some responses not received\n\n");
                writer.write("**Failed Tests:**\n");
                for (int i = 0; i < exchanges.size(); i++) {
                    if (!exchanges.get(i).responseReceived) {
                        writer.write("- Test " + (i + 1) + ": " + exchanges.get(i).prompt + "\n");
                    }
                }
            } else {
                writer.write("**Status:** ✓ All tests successful\n\n");
            }

            return filename;

        } catch (IOException e) {
            throw new RuntimeException("Failed to write report", e);
        }
    }

    private String truncate(String str, int maxLength) {
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }
}