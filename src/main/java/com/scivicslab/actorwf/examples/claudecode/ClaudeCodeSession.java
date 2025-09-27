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
package com.scivicslab.actorwf.examples.claudecode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages a Claude Code session using the --print --continue pattern.
 *
 * This class provides programmatic control of Claude Code CLI by:
 * 1. Using --print option for non-interactive output
 * 2. Using --continue option to maintain session continuity
 * 3. Using 'script' command to provide pseudo-TTY environment
 *
 * Session management is handled by Claude Code itself. This class only
 * tracks whether it's the first prompt (to omit --continue flag).
 *
 * Example usage:
 * <pre>
 * ClaudeCodeSession session = new ClaudeCodeSession("my-session");
 * ClaudeCodeOutput output1 = session.sendPrompt("What is 2 + 2?");
 * ClaudeCodeOutput output2 = session.sendPrompt("What was my previous question?");
 * </pre>
 *
 * @see ClaudeCodeOutput
 * @see ClaudeCodeParser
 */
public class ClaudeCodeSession {

    private final String sessionName;
    private final ClaudeCodeParser parser;
    private boolean isFirstPrompt = true;
    private Process currentProcess = null;
    private Thread readerThread = null;

    /**
     * Creates a new Claude Code session.
     *
     * @param sessionName the name for this session (currently unused, kept for API compatibility)
     */
    public ClaudeCodeSession(String sessionName) {
        this.sessionName = sessionName;
        this.parser = new ClaudeCodeParser();
    }

    /**
     * Sends a prompt to Claude Code and returns the response.
     *
     * This method:
     * 1. Constructs command: "claude --print [--continue] \"prompt\""
     * 2. Wraps it with 'script' for pseudo-TTY: script -q -c "..." /dev/null
     * 3. Executes the process and captures output
     * 4. Parses the output into ClaudeCodeOutput
     *
     * The --continue flag is omitted for the first prompt, then included
     * for all subsequent prompts to maintain conversation context.
     *
     * If the session expires on Claude Code's side, this method will
     * automatically retry without --continue flag, effectively starting
     * a new session.
     *
     * @param prompt the prompt to send to Claude Code
     * @return ClaudeCodeOutput containing the response
     * @throws IOException if process execution fails
     * @throws InterruptedException if process is interrupted
     */
    public ClaudeCodeOutput sendPrompt(String prompt) throws IOException, InterruptedException {
        StringBuilder commandStr = new StringBuilder("claude --print");

        if (!isFirstPrompt) {
            commandStr.append(" --continue");
        }

        // Escape double quotes in prompt
        commandStr.append(" \"").append(prompt.replace("\"", "\\\"")).append("\"");

        // Use 'script' command to provide pseudo-TTY environment
        // -q: quiet mode (suppress script start/done messages)
        // -c: run command in pseudo-TTY
        // /dev/null: discard typescript file
        ProcessBuilder pb = new ProcessBuilder("script", "-q", "-c", commandStr.toString(), "/dev/null");
        pb.redirectErrorStream(true);
        Process process = pb.start();

        synchronized (this) {
            currentProcess = process;
        }

        // Read all output
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

        // Check for session expired error
        if (exitCode != 0 || isSessionExpiredError(outputLines)) {
            // If session expired, retry as first prompt
            if (!isFirstPrompt) {
                isFirstPrompt = true;
                return sendPrompt(prompt);
            }
            // If already first prompt and still failing, throw error
            throw new IOException("Claude Code exited with code: " + exitCode);
        }

        isFirstPrompt = false;

        return parser.parse(outputLines);
    }

    /**
     * Checks if the output indicates a session expiration error.
     *
     * @param output the output lines from Claude Code
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
     * Interrupts the currently running Claude Code process.
     * <p>
     * This method can be called from another thread to interrupt a long-running
     * prompt. After interruption, you can send a new prompt to continue the session.
     * The session context is preserved across interruptions.
     * </p>
     * <p>
     * Note: The interrupt is performed by destroying the process. This may result
     * in an InterruptedException or IOException in the sendPrompt() method.
     * </p>
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
     * <p>
     * If the prompt takes longer than the specified timeout, the process will be
     * interrupted and an IOException will be thrown. The session context is preserved,
     * allowing you to send a new prompt after the timeout.
     * </p>
     *
     * @param prompt the prompt to send to Claude Code
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
     * Sends a prompt that requests numbered choices and waits for Claude's response.
     * <p>
     * This is a convenience method for prompts that are expected to result in
     * numbered choices. It sends the prompt and returns the output, which should
     * be of type NUMBERED_CHOICE if Claude responds with options.
     * </p>
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
     * <p>
     * This method is used after receiving numbered choices from Claude.
     * It sends a selection like "I choose option 2" or with additional context.
     * </p>
     *
     * Example usage:
     * <pre>
     * ClaudeCodeOutput choices = session.requestChoices("Suggest 3 programming languages");
     * ClaudeCodeOutput response = session.selectChoice(2, "Tell me more about it");
     * </pre>
     *
     * @param choiceNumber the choice number to select (1-based)
     * @param additionalInput optional additional input after selection (can be null)
     * @return ClaudeCodeOutput containing Claude's response to the selection
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
     * <p>
     * This is a convenience overload of selectChoice for simple selection scenarios.
     * </p>
     *
     * @param choiceNumber the choice number to select (1-based)
     * @return ClaudeCodeOutput containing Claude's response to the selection
     * @throws IOException if process execution fails
     * @throws InterruptedException if process is interrupted
     */
    public ClaudeCodeOutput selectChoice(int choiceNumber) throws IOException, InterruptedException {
        return selectChoice(choiceNumber, null);
    }

    /**
     * Sends a follow-up prompt after a choice has been made.
     * <p>
     * This is used when you want to ask additional questions after making a selection.
     * The session context (including the previous choice) is maintained.
     * </p>
     *
     * Example usage:
     * <pre>
     * session.selectChoice(2);
     * ClaudeCodeOutput moreInfo = session.sendFollowUp("What are the disadvantages?");
     * </pre>
     *
     * @param followUpPrompt the follow-up question or request
     * @return ClaudeCodeOutput containing Claude's response
     * @throws IOException if process execution fails
     * @throws InterruptedException if process is interrupted
     */
    public ClaudeCodeOutput sendFollowUp(String followUpPrompt)
            throws IOException, InterruptedException {
        return sendPrompt(followUpPrompt);
    }

    /**
     * Interrupts the current operation and immediately sends a new prompt.
     * <p>
     * This method simulates pressing Escape to cancel the current operation,
     * then immediately sending a new prompt. This is useful when you want to
     * change direction mid-conversation or cancel a long-running operation.
     * </p>
     * <p>
     * The session context is preserved, so Claude will still remember the
     * conversation history up to the point of interruption.
     * </p>
     *
     * Example usage:
     * <pre>
     * // Start a long operation in background
     * Thread longTask = new Thread(() -> {
     *     session.sendPrompt("Write a comprehensive guide...");
     * });
     * longTask.start();
     *
     * // User changes mind after 2 seconds
     * Thread.sleep(2000);
     * ClaudeCodeOutput newResponse = session.interruptAndSendNew(
     *     "Actually, just give me a brief summary instead"
     * );
     * </pre>
     *
     * @param newPrompt the new prompt to send after interruption
     * @return ClaudeCodeOutput containing Claude's response to the new prompt
     * @throws IOException if process execution fails
     * @throws InterruptedException if process is interrupted
     */
    public ClaudeCodeOutput interruptAndSendNew(String newPrompt)
            throws IOException, InterruptedException {

        // Interrupt any running process
        interrupt();

        // Wait a moment for cleanup
        Thread.sleep(500);

        // Send the new prompt
        return sendPrompt(newPrompt);
    }

    /**
     * Sends a prompt, waits for response, and if it takes too long, interrupts
     * and sends an alternative prompt.
     * <p>
     * This is a higher-level convenience method that combines timeout and
     * interrupt-and-retry logic. If the initial prompt times out, it automatically
     * sends a fallback prompt (typically a simpler or shorter version).
     * </p>
     *
     * Example usage:
     * <pre>
     * ClaudeCodeOutput response = session.sendWithFallback(
     *     "Generate comprehensive documentation for this API",
     *     10000,  // 10 second timeout
     *     "Just give me a brief summary of this API"
     * );
     * </pre>
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