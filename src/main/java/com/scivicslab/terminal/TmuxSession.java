package com.scivicslab.terminal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class TmuxSession {

    private final String sessionName;
    private boolean active = false;

    public TmuxSession(String sessionName) {
        this.sessionName = sessionName;
    }

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

    public boolean hasPrompt() throws IOException, InterruptedException {
        String lastLine = getLastLine();
        return lastLine.endsWith("$") || lastLine.endsWith("#") || lastLine.endsWith(">");
    }

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

    public boolean isActive() {
        return active;
    }

    public String getSessionName() {
        return sessionName;
    }
}