package com.scivicslab.claudecode;

import java.util.ArrayList;
import java.util.List;

public class ClaudeCodeOutput {

    private final List<String> lines;
    private final PromptType promptType;
    private final List<String> choices;
    private final String question;
    private final long timestamp;

    public ClaudeCodeOutput(List<String> lines, PromptType promptType,
                           List<String> choices, String question) {
        this.lines = new ArrayList<>(lines);
        this.promptType = promptType;
        this.choices = choices != null ? new ArrayList<>(choices) : new ArrayList<>();
        this.question = question;
        this.timestamp = System.currentTimeMillis();
    }

    public List<String> getLines() {
        return new ArrayList<>(lines);
    }

    public PromptType getPromptType() {
        return promptType;
    }

    public List<String> getChoices() {
        return new ArrayList<>(choices);
    }

    public String getQuestion() {
        return question;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean hasChoices() {
        return !choices.isEmpty();
    }

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