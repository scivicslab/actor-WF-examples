package com.scivicslab.terminal;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class TmuxMonitor {

    private final TmuxSession session;
    private final AtomicBoolean monitoring = new AtomicBoolean(false);
    private Thread monitorThread;
    private TmuxOutput latestOutput;
    private final int intervalMs;

    public TmuxMonitor(TmuxSession session, int intervalMs) {
        this.session = session;
        this.intervalMs = intervalMs;
    }

    public void startMonitoring() {
        if (monitoring.get()) {
            return;
        }

        monitoring.set(true);

        monitorThread = Thread.ofVirtual().start(() -> {
            while (monitoring.get() && session.isActive()) {
                try {
                    List<String> lines = session.capturePane();
                    boolean hasPrompt = session.hasPrompt();

                    latestOutput = new TmuxOutput(lines, hasPrompt);

                    Thread.sleep(intervalMs);

                } catch (IOException e) {
                    System.err.println("Error capturing tmux output: " + e.getMessage());
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

    public TmuxOutput getLatestOutput() {
        return latestOutput;
    }

    public boolean isMonitoring() {
        return monitoring.get();
    }

    public void printLatestOutput() {
        if (latestOutput != null) {
            System.out.println("=== Tmux Output (Prompt: " + latestOutput.hasPrompt() + ") ===");
            for (String line : latestOutput.getLines()) {
                System.out.println(line);
            }
            System.out.println("======================================");
        }
    }
}