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

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the output captured from a Claude Code CLI session.
 * <p>
 * This class encapsulates the terminal output lines, the type of prompt detected,
 * any available choices (for numbered choice prompts), and the question text.
 * It provides an immutable view of the captured state with timestamp information.
 * </p>
 *
 * @author Scivics Lab
 * @version 1.0
 */
public class ClaudeCodeOutput {

    /**
     * The captured terminal output lines.
     */
    private final List<String> lines;

    /**
     * The type of prompt detected in the output.
     */
    private final PromptType promptType;

    /**
     * The available choices for numbered choice prompts (empty list if not applicable).
     */
    private final List<String> choices;

    /**
     * The question text extracted from the output (null if not applicable).
     */
    private final String question;

    /**
     * The timestamp when this output was captured (milliseconds since epoch).
     */
    private final long timestamp;

    /**
     * Constructs a new ClaudeCodeOutput instance.
     *
     * @param lines the terminal output lines to capture
     * @param promptType the type of prompt detected
     * @param choices the available choices for numbered prompts (may be null)
     * @param question the question text extracted from the output (may be null)
     */
    public ClaudeCodeOutput(List<String> lines, PromptType promptType,
                           List<String> choices, String question) {
        this.lines = new ArrayList<>(lines);
        this.promptType = promptType;
        this.choices = choices != null ? new ArrayList<>(choices) : new ArrayList<>();
        this.question = question;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Returns a defensive copy of the captured terminal output lines.
     *
     * @return a new list containing all captured output lines
     */
    public List<String> getLines() {
        return new ArrayList<>(lines);
    }

    /**
     * Returns the type of prompt detected in the output.
     *
     * @return the prompt type
     */
    public PromptType getPromptType() {
        return promptType;
    }

    /**
     * Returns a defensive copy of the available choices.
     *
     * @return a new list containing all available choices (empty if none)
     */
    public List<String> getChoices() {
        return new ArrayList<>(choices);
    }

    /**
     * Returns the question text extracted from the output.
     *
     * @return the question text, or null if not applicable
     */
    public String getQuestion() {
        return question;
    }

    /**
     * Returns the timestamp when this output was captured.
     *
     * @return the timestamp in milliseconds since epoch
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Checks if this output contains any choices.
     *
     * @return true if choices are available, false otherwise
     */
    public boolean hasChoices() {
        return !choices.isEmpty();
    }

    /**
     * Checks if Claude Code is waiting for user input.
     * <p>
     * Returns true for prompt types that require user interaction:
     * YES_NO, NUMBERED_CHOICE, TOOL_APPROVAL, and READY.
     * </p>
     *
     * @return true if waiting for input, false otherwise
     */
    public boolean isWaitingForInput() {
        return promptType == PromptType.YES_NO ||
               promptType == PromptType.NUMBERED_CHOICE ||
               promptType == PromptType.TOOL_APPROVAL ||
               promptType == PromptType.READY;
    }

    @Override
    public String toString() {
        return String.format("ClaudeCodeOutput[type=%s, choices=%d, question=%s]",
            promptType, choices.size(), question != null ? question.substring(0, Math.min(50, question.length())) : "none");
    }
}