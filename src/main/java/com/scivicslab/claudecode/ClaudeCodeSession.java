package com.scivicslab.claudecode;

import com.scivicslab.terminal.TmuxSession;

import java.io.IOException;
import java.util.List;

public class ClaudeCodeSession {

    private final TmuxSession tmuxSession;
    private final ClaudeCodeParser parser;
    private boolean claudeCodeStarted = false;

    public ClaudeCodeSession(String sessionName) {
        this.tmuxSession = new TmuxSession(sessionName);
        this.parser = new ClaudeCodeParser();
    }

    public void createSession() throws IOException, InterruptedException {
        tmuxSession.createSession();
    }

    public void startClaudeCode() throws IOException, InterruptedException {
        if (claudeCodeStarted) {
            return;
        }

        tmuxSession.sendCommand("claude");

        Thread.sleep(3000);

        claudeCodeStarted = true;
    }

    public void sendPrompt(String prompt) throws IOException, InterruptedException {
        if (!claudeCodeStarted) {
            throw new IllegalStateException("Claude Code not started");
        }

        String escapedPrompt = prompt.replace("\"", "\\\"");
        tmuxSession.sendCommand(escapedPrompt);
    }

    public void sendRawKeys(String keys) throws IOException, InterruptedException {
        tmuxSession.sendKeys(keys);
    }

    public void sendChoice(int choice) throws IOException, InterruptedException {
        tmuxSession.sendCommand(String.valueOf(choice));
    }

    public void sendYes() throws IOException, InterruptedException {
        tmuxSession.sendCommand("y");
    }

    public void sendNo() throws IOException, InterruptedException {
        tmuxSession.sendCommand("n");
    }

    public void sendEnter() throws IOException, InterruptedException {
        tmuxSession.sendKeys("Enter");
    }

    public void sendCtrlC() throws IOException, InterruptedException {
        tmuxSession.sendKeys("C-c");
    }

    public void sendCtrlD() throws IOException, InterruptedException {
        tmuxSession.sendKeys("C-d");
    }

    public ClaudeCodeOutput captureAndParse() throws IOException, InterruptedException {
        List<String> lines = tmuxSession.capturePane();
        return parser.parse(lines);
    }

    public List<String> captureOutput() throws IOException, InterruptedException {
        return tmuxSession.capturePane();
    }

    public void killSession() throws IOException, InterruptedException {
        tmuxSession.killSession();
        claudeCodeStarted = false;
    }

    public boolean isActive() {
        return tmuxSession.isActive();
    }

    public boolean isClaudeCodeStarted() {
        return claudeCodeStarted;
    }

    public String getSessionName() {
        return tmuxSession.getSessionName();
    }
}