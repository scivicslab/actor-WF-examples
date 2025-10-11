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
package com.scivicslab.pojoactor.workflow.examples.terminal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a tmux terminal session that can be controlled programmatically.
 * This class provides methods to create, interact with, and manage tmux sessions,
 * including sending commands, capturing output, and detecting shell prompts.
 *
 * <p>Tmux sessions are created in detached mode with predefined dimensions
 * (120 columns x 30 rows) and can be controlled through the tmux command-line interface.</p>
 */
public class TmuxSession {

    /** The name of the tmux session */
    private final String sessionName;

    /** Indicates whether the tmux session is currently active */
    private boolean active = false;

    /**
     * Creates a new TmuxSession instance with the specified session name.
     *
     * @param sessionName the name to assign to the tmux session
     */
    public TmuxSession(String sessionName) {
        this.sessionName = sessionName;
    }

    /**
     * Creates a new tmux session in detached mode with dimensions of 120x30.
     * Sets the session to active state upon successful creation.
     *
     * @throws IOException if the tmux command fails to execute or returns a non-zero exit code
     * @throws InterruptedException if the process is interrupted while waiting
     */
    public void createSession() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
            "tmux", "new-session", "-d", "-s", sessionName, "-x", "120", "-y", "30"
        );

        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new IOException("Failed to create tmux session: " + sessionName);
        }

        active = true;
    }

    /**
     * Sends a command to the tmux session followed by the Enter key.
     * The command will be executed in the shell within the tmux session.
     *
     * @param command the command string to send to the tmux session
     * @throws IllegalStateException if the tmux session is not active
     * @throws IOException if the tmux command fails to execute or returns a non-zero exit code
     * @throws InterruptedException if the process is interrupted while waiting
     */
    public void sendCommand(String command) throws IOException, InterruptedException {
        if (!active) {
            throw new IllegalStateException("Tmux session is not active");
        }

        ProcessBuilder pb = new ProcessBuilder(
            "tmux", "send-keys", "-t", sessionName, command, "Enter"
        );

        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new IOException("Failed to send command to tmux session");
        }
    }

    /**
     * Sends raw keystrokes to the tmux session without automatically appending Enter.
     * This allows for more fine-grained control over input.
     *
     * @param keys the keystrokes to send to the tmux session
     * @throws IllegalStateException if the tmux session is not active
     * @throws IOException if the tmux command fails to execute or returns a non-zero exit code
     * @throws InterruptedException if the process is interrupted while waiting
     */
    public void sendKeys(String keys) throws IOException, InterruptedException {
        if (!active) {
            throw new IllegalStateException("Tmux session is not active");
        }

        ProcessBuilder pb = new ProcessBuilder(
            "tmux", "send-keys", "-t", sessionName, keys
        );

        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new IOException("Failed to send keys to tmux session");
        }
    }

    /**
     * Captures the current contents of the tmux pane as a list of lines.
     * Each line represents one line of text from the terminal output.
     *
     * @return a list of strings representing the captured pane contents
     * @throws IllegalStateException if the tmux session is not active
     * @throws IOException if the tmux command fails to execute or returns a non-zero exit code
     * @throws InterruptedException if the process is interrupted while waiting
     */
    public List<String> capturePane() throws IOException, InterruptedException {
        if (!active) {
            throw new IllegalStateException("Tmux session is not active");
        }

        ProcessBuilder pb = new ProcessBuilder(
            "tmux", "capture-pane", "-t", sessionName, "-p"
        );

        Process process = pb.start();

        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new IOException("Failed to capture pane from tmux session");
        }

        return lines;
    }

    /**
     * Retrieves the last non-empty line from the tmux pane.
     * Searches backwards from the end of the captured output to find the first non-empty line.
     *
     * @return the last non-empty line, or an empty string if no non-empty lines are found
     * @throws IOException if the tmux command fails to execute
     * @throws InterruptedException if the process is interrupted while waiting
     */
    public String getLastLine() throws IOException, InterruptedException {
        List<String> lines = capturePane();
        if (lines.isEmpty()) {
            return "";
        }

        for (int i = lines.size() - 1; i >= 0; i--) {
            String line = lines.get(i).trim();
            if (!line.isEmpty()) {
                return line;
            }
        }

        return "";
    }

    /**
     * Checks whether the last line of the tmux pane ends with a common shell prompt character.
     * Detects prompts ending with '$', '#', or '>' which are typical shell prompt indicators.
     *
     * @return true if a shell prompt is detected, false otherwise
     * @throws IOException if the tmux command fails to execute
     * @throws InterruptedException if the process is interrupted while waiting
     */
    public boolean hasPrompt() throws IOException, InterruptedException {
        String lastLine = getLastLine();
        return lastLine.endsWith("$") || lastLine.endsWith("#") || lastLine.endsWith(">");
    }

    /**
     * Terminates the tmux session and sets the active state to false.
     * If the session is not active, this method does nothing.
     *
     * @throws IOException if the tmux command fails to execute
     * @throws InterruptedException if the process is interrupted while waiting
     */
    public void killSession() throws IOException, InterruptedException {
        if (!active) {
            return;
        }

        ProcessBuilder pb = new ProcessBuilder(
            "tmux", "kill-session", "-t", sessionName
        );

        Process process = pb.start();
        process.waitFor();

        active = false;
    }

    /**
     * Checks whether the tmux session is currently active.
     *
     * @return true if the session is active, false otherwise
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Gets the name of the tmux session.
     *
     * @return the session name
     */
    public String getSessionName() {
        return sessionName;
    }
}