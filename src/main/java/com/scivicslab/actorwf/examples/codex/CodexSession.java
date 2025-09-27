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
package com.scivicslab.actorwf.examples.codex;

import com.scivicslab.actorwf.examples.claudecode.ClaudeCodeOutput;
import com.scivicslab.actorwf.examples.claudecode.ClaudeCodeParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages an OpenAI Codex session using the exec/resume pattern.
 *
 * This class provides programmatic control of Codex CLI by:
 * 1. Using codex exec for initial prompt execution
 * 2. Using codex exec resume for session continuity
 * 3. Using 'script' command to provide pseudo-TTY environment
 *
 * Session management is handled by Codex itself. This class only
 * tracks whether it's the first prompt (to use exec vs exec resume).
 *
 * Example usage:
 * <pre>
 * CodexSession session = new CodexSession("my-session");
 * ClaudeCodeOutput output1 = session.sendPrompt("What is 2 + 2?");
 * ClaudeCodeOutput output2 = session.sendPrompt("What was my previous question?");
 * </pre>
 *
 * @see ClaudeCodeOutput
 * @see ClaudeCodeParser
 */
public class CodexSession {

    private final String sessionName;
    private final ClaudeCodeParser parser;
    private boolean isFirstPrompt = true;
    private Process currentProcess = null;
    private Thread readerThread = null;

    /**
     * Creates a new Codex session.
     *
     * @param sessionName the name for this session (currently unused, kept for API compatibility)
     */
    public CodexSession(String sessionName) {
        this.sessionName = sessionName;
        this.parser = new ClaudeCodeParser();
    }

    /**
     * Sends a prompt to Codex and returns the response.
     *
     * This method:
     * 1. Constructs command: "codex exec [resume] \"prompt\""
     * 2. Wraps it with 'script' for pseudo-TTY: script -q -c "..." /dev/null
     * 3. Executes the process and captures output
     * 4. Parses the output into ClaudeCodeOutput
     *
     * The first prompt uses "codex exec", subsequent prompts use "codex exec resume"
     * to maintain conversation context.
     *
     * @param prompt the prompt to send to Codex
     * @return ClaudeCodeOutput containing the response
     * @throws IOException if process execution fails
     * @throws InterruptedException if process is interrupted
     */
    public ClaudeCodeOutput sendPrompt(String prompt) throws IOException, InterruptedException {
        ProcessBuilder pb;
        boolean useStdin = false;

        if (isFirstPrompt) {
            // First prompt: use codex exec with prompt as argument
            pb = new ProcessBuilder("codex", "exec", prompt);
        } else {
            // Resume session: use stdin to send prompt
            pb = new ProcessBuilder("codex", "exec", "resume", "--last");
            useStdin = true;
        }

        pb.redirectErrorStream(true);
        Process process = pb.start();

        synchronized (this) {
            currentProcess = process;
        }

        // If resuming, write prompt to stdin
        if (useStdin) {
            try (java.io.BufferedWriter writer = new java.io.BufferedWriter(
                    new java.io.OutputStreamWriter(process.getOutputStream()))) {
                writer.write(prompt);
                writer.write("\n");
                writer.flush();
                writer.close(); // Close stdin to signal end of input
            }
        }

        // Read output
        List<String> outputLines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                outputLines.add(line);
            }
        }

        int exitCode = process.waitFor();

        synchronized (this) {
            currentProcess = null;
        }

        if (exitCode != 0 || isSessionExpiredError(outputLines)) {
            if (!isFirstPrompt) {
                isFirstPrompt = true;
                return sendPrompt(prompt);
            }
            throw new IOException("Codex exited with code: " + exitCode);
        }

        isFirstPrompt = false;

        return parser.parse(outputLines);
    }

    /**
     * Checks if the output indicates a session expiration error.
     *
     * @param output the output lines from Codex
     * @return true if session expired error detected
     */
    private boolean isSessionExpiredError(List<String> output) {
        String text = String.join(" ", output).toLowerCase();
        return text.contains("session not found") ||
               text.contains("no active session") ||
               text.contains("session expired");
    }

    /**
     * Resets the session state, forcing the next prompt to start a new session.
     *
     * This is useful when you explicitly want to start a fresh conversation
     * without any previous context.
     */
    public void resetSession() {
        isFirstPrompt = true;
    }

    /**
     * Gets the session name.
     *
     * @return the session name
     */
    public String getSessionName() {
        return sessionName;
    }

    /**
     * Checks if this is the first prompt in the current session.
     *
     * @return true if no prompts have been sent yet, or after resetSession()
     */
    public boolean isFirstPrompt() {
        return isFirstPrompt;
    }

    /**
     * Interrupts the currently running Codex process.
     *
     * @return true if a process was interrupted, false if no process was running
     */
    public synchronized boolean interrupt() {
        if (currentProcess != null && currentProcess.isAlive()) {
            currentProcess.destroy();
            try {
                if (!currentProcess.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    currentProcess.destroyForcibly();
                }
            } catch (InterruptedException e) {
                currentProcess.destroyForcibly();
                Thread.currentThread().interrupt();
            }
            return true;
        }
        return false;
    }

    /**
     * Sends a prompt with a timeout.
     *
     * @param prompt the prompt to send to Codex
     * @param timeoutMillis timeout in milliseconds
     * @return ClaudeCodeOutput containing the response
     * @throws IOException if process execution fails or timeout occurs
     * @throws InterruptedException if process is interrupted
     */
    public ClaudeCodeOutput sendPromptWithTimeout(String prompt, long timeoutMillis)
            throws IOException, InterruptedException {

        final IOException[] ioException = new IOException[1];
        final InterruptedException[] interruptedException = new InterruptedException[1];
        final ClaudeCodeOutput[] result = new ClaudeCodeOutput[1];

        Thread promptThread = new Thread(() -> {
            try {
                result[0] = sendPrompt(prompt);
            } catch (IOException e) {
                ioException[0] = e;
            } catch (InterruptedException e) {
                interruptedException[0] = e;
                Thread.currentThread().interrupt();
            }
        });

        synchronized (this) {
            readerThread = promptThread;
        }

        promptThread.start();
        promptThread.join(timeoutMillis);

        if (promptThread.isAlive()) {
            interrupt();
            promptThread.join(5000);
            throw new IOException("Prompt execution timed out after " + timeoutMillis + "ms");
        }

        synchronized (this) {
            readerThread = null;
        }

        if (ioException[0] != null) {
            throw ioException[0];
        }
        if (interruptedException[0] != null) {
            throw interruptedException[0];
        }

        return result[0];
    }

    /**
     * Sends a prompt that requests numbered choices and waits for Codex's response.
     *
     * @param prompt the prompt requesting choices
     * @return ClaudeCodeOutput containing the choices
     * @throws IOException if process execution fails
     * @throws InterruptedException if process is interrupted
     */
    public ClaudeCodeOutput requestChoices(String prompt) throws IOException, InterruptedException {
        return sendPrompt(prompt);
    }

    /**
     * Selects a choice by number and optionally provides additional input.
     *
     * @param choiceNumber the choice number to select (1-based)
     * @param additionalInput optional additional input after selection (can be null)
     * @return ClaudeCodeOutput containing Codex's response to the selection
     * @throws IOException if process execution fails
     * @throws InterruptedException if process is interrupted
     */
    public ClaudeCodeOutput selectChoice(int choiceNumber, String additionalInput)
            throws IOException, InterruptedException {

        StringBuilder prompt = new StringBuilder("I choose option ");
        prompt.append(choiceNumber);

        if (additionalInput != null && !additionalInput.trim().isEmpty()) {
            prompt.append(". ").append(additionalInput);
        }

        return sendPrompt(prompt.toString());
    }

    /**
     * Selects a choice by number without additional input.
     *
     * @param choiceNumber the choice number to select (1-based)
     * @return ClaudeCodeOutput containing Codex's response to the selection
     * @throws IOException if process execution fails
     * @throws InterruptedException if process is interrupted
     */
    public ClaudeCodeOutput selectChoice(int choiceNumber) throws IOException, InterruptedException {
        return selectChoice(choiceNumber, null);
    }

    /**
     * Sends a follow-up prompt after a choice has been made.
     *
     * @param followUpPrompt the follow-up question or request
     * @return ClaudeCodeOutput containing Codex's response
     * @throws IOException if process execution fails
     * @throws InterruptedException if process is interrupted
     */
    public ClaudeCodeOutput sendFollowUp(String followUpPrompt)
            throws IOException, InterruptedException {
        return sendPrompt(followUpPrompt);
    }

    /**
     * Interrupts the current operation and immediately sends a new prompt.
     *
     * @param newPrompt the new prompt to send after interruption
     * @return ClaudeCodeOutput containing Codex's response to the new prompt
     * @throws IOException if process execution fails
     * @throws InterruptedException if process is interrupted
     */
    public ClaudeCodeOutput interruptAndSendNew(String newPrompt)
            throws IOException, InterruptedException {

        interrupt();
        Thread.sleep(500);
        return sendPrompt(newPrompt);
    }

    /**
     * Sends a prompt, waits for response, and if it takes too long, interrupts
     * and sends an alternative prompt.
     *
     * @param initialPrompt the initial prompt to try
     * @param timeoutMillis timeout in milliseconds for the initial prompt
     * @param fallbackPrompt the prompt to send if initial prompt times out
     * @return ClaudeCodeOutput containing response from either initial or fallback prompt
     * @throws IOException if both prompts fail
     * @throws InterruptedException if process is interrupted
     */
    public ClaudeCodeOutput sendWithFallback(String initialPrompt, long timeoutMillis, String fallbackPrompt)
            throws IOException, InterruptedException {

        try {
            return sendPromptWithTimeout(initialPrompt, timeoutMillis);
        } catch (IOException e) {
            if (e.getMessage().contains("timed out")) {
                System.out.println("Initial prompt timed out, sending fallback...");
                return sendPrompt(fallbackPrompt);
            }
            throw e;
        }
    }
}