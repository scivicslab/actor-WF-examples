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

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Monitors a tmux session continuously by capturing pane output at regular intervals.
 * This class runs a background virtual thread that periodically captures the tmux pane
 * contents and stores the latest output for retrieval.
 *
 * <p>The monitor can be started and stopped independently, and uses virtual threads
 * for efficient concurrent execution.</p>
 */
public class TmuxMonitor {

    /** The tmux session being monitored */
    private final TmuxSession session;

    /** Atomic flag indicating whether monitoring is currently active */
    private final AtomicBoolean monitoring = new AtomicBoolean(false);

    /** The virtual thread running the monitoring loop */
    private Thread monitorThread;

    /** The most recently captured output from the tmux session */
    private TmuxOutput latestOutput;

    /** The interval in milliseconds between consecutive captures */
    private final int intervalMs;

    /**
     * Creates a new TmuxMonitor for the specified session with the given capture interval.
     *
     * @param session the tmux session to monitor
     * @param intervalMs the interval in milliseconds between captures
     */
    public TmuxMonitor(TmuxSession session, int intervalMs) {
        this.session = session;
        this.intervalMs = intervalMs;
    }

    /**
     * Starts monitoring the tmux session in a background virtual thread.
     * The monitor will capture pane output at the configured interval until stopped.
     * If monitoring is already active, this method does nothing.
     *
     * <p>The monitoring loop runs until either stopMonitoring() is called,
     * the session becomes inactive, or the thread is interrupted.</p>
     */
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

    /**
     * Stops the monitoring of the tmux session.
     * Interrupts the monitoring thread and waits up to 2 seconds for it to terminate.
     */
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

    /**
     * Gets the most recently captured output from the tmux session.
     *
     * @return the latest TmuxOutput, or null if no output has been captured yet
     */
    public TmuxOutput getLatestOutput() {
        return latestOutput;
    }

    /**
     * Checks whether the monitor is currently active.
     *
     * @return true if monitoring is active, false otherwise
     */
    public boolean isMonitoring() {
        return monitoring.get();
    }

    /**
     * Prints the latest captured output to standard output.
     * Includes a header with the prompt detection status and all captured lines.
     * If no output has been captured yet, this method does nothing.
     */
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