package com.scivicslab.claudecode;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class ClaudeCodeMonitor {

    private final ClaudeCodeSession session;
    private final AtomicBoolean monitoring = new AtomicBoolean(false);
    private Thread monitorThread;
    private ClaudeCodeOutput latestOutput;
    private final int intervalMs;
    private Consumer<ClaudeCodeOutput> onOutputChange;

    public ClaudeCodeMonitor(ClaudeCodeSession session, int intervalMs) {
        this.session = session;
        this.intervalMs = intervalMs;
    }

    public void setOnOutputChange(Consumer<ClaudeCodeOutput> callback) {
        this.onOutputChange = callback;
    }

    public void startMonitoring() {
        if (monitoring.get()) {
            return;
        }

        monitoring.set(true);

        monitorThread = Thread.ofVirtual().start(() -> {
            while (monitoring.get() && session.isActive()) {
                try {
                    ClaudeCodeOutput output = session.captureAndParse();

                    if (hasChanged(output)) {
                        latestOutput = output;

                        if (onOutputChange != null) {
                            onOutputChange.accept(output);
                        }
                    }

                    Thread.sleep(intervalMs);

                } catch (IOException e) {
                    System.err.println("Error capturing Claude Code output: " + e.getMessage());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    public void stopMonitoring() {
        monitoring.set(false);

        if (monitorThread != null) {
            monitorThread.interrupt();
            try {
                monitorThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public ClaudeCodeOutput getLatestOutput() {
        return latestOutput;
    }

    public boolean isMonitoring() {
        return monitoring.get();
    }

    public void printLatestOutput() {
        if (latestOutput != null) {
            System.out.println("=== Claude Code Output ===");
            System.out.println("Type: " + latestOutput.getPromptType());

            if (latestOutput.getQuestion() != null) {
                System.out.println("Question: " + latestOutput.getQuestion());
            }

            if (latestOutput.hasChoices()) {
                System.out.println("Choices:");
                List<String> choices = latestOutput.getChoices();
                for (int i = 0; i < choices.size(); i++) {
                    System.out.println("  " + (i + 1) + ". " + choices.get(i));
                }
            }

            System.out.println("\nOutput:");
            for (String line : latestOutput.getLines()) {
                System.out.println(line);
            }
            System.out.println("==========================");
        }
    }

    private boolean hasChanged(ClaudeCodeOutput newOutput) {
        if (latestOutput == null) {
            return true;
        }

        if (latestOutput.getPromptType() != newOutput.getPromptType()) {
            return true;
        }

        if (latestOutput.getLines().size() != newOutput.getLines().size()) {
            return true;
        }

        return false;
    }
}