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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for analyzing Claude Code CLI terminal output and detecting prompt types.
 * <p>
 * This class analyzes terminal output lines to identify various states and prompts
 * from the Claude Code CLI, including yes/no questions, numbered choices, tool approval
 * requests, processing states, and errors. It uses regular expressions and heuristics
 * to classify the output appropriately.
 * </p>
 *
 * @author Scivics Lab
 * @version 1.0
 */
public class ClaudeCodeParser {

    /**
     * Pattern for detecting numbered choice items (e.g., "1. Option" or "1) Option").
     */
    private static final Pattern NUMBERED_CHOICE_PATTERN = Pattern.compile("^\\s*\\d+[\\.\\)]\\s+(.+)$");

    /**
     * Pattern for detecting yes/no prompts in various formats.
     */
    private static final Pattern YES_NO_PATTERN = Pattern.compile("\\(y/n\\)|\\[y/n\\]|\\(yes/no\\)", Pattern.CASE_INSENSITIVE);

    /**
     * Parses terminal output lines and determines the current state and prompt type.
     * <p>
     * The parsing process examines the last few lines of output and applies various
     * heuristics to identify the current state: ready prompt, numbered choices,
     * yes/no questions, tool approval requests, processing, errors, or general responses.
     * </p>
     *
     * @param lines the terminal output lines to parse
     * @return a ClaudeCodeOutput object containing the parsed state and metadata
     */
    public ClaudeCodeOutput parse(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return new ClaudeCodeOutput(lines, PromptType.UNKNOWN, null, null);
        }

        String lastLine = getLastNonEmptyLine(lines);
        String lastFewLines = getLastNLines(lines, 10);

        if (isReadyPrompt(lastLine)) {
            return new ClaudeCodeOutput(lines, PromptType.READY, null, null);
        }

        List<String> choices = extractNumberedChoices(lastFewLines);
        if (!choices.isEmpty()) {
            String question = extractQuestion(lastFewLines, choices.size());
            return new ClaudeCodeOutput(lines, PromptType.NUMBERED_CHOICE, choices, question);
        }

        if (isYesNoPrompt(lastLine)) {
            return new ClaudeCodeOutput(lines, PromptType.YES_NO, null, lastLine);
        }

        if (isToolApprovalPrompt(lastFewLines)) {
            return new ClaudeCodeOutput(lines, PromptType.TOOL_APPROVAL, null, lastLine);
        }

        if (isProcessing(lastLine)) {
            return new ClaudeCodeOutput(lines, PromptType.PROCESSING, null, null);
        }

        if (isError(lastFewLines)) {
            return new ClaudeCodeOutput(lines, PromptType.ERROR, null, lastLine);
        }

        return new ClaudeCodeOutput(lines, PromptType.RESPONSE, null, null);
    }

    private String getLastNonEmptyLine(List<String> lines) {
        for (int i = lines.size() - 1; i >= 0; i--) {
            String line = lines.get(i).trim();
            if (!line.isEmpty()) {
                return line;
            }
        }
        return "";
    }

    private String getLastNLines(List<String> lines, int n) {
        int start = Math.max(0, lines.size() - n);
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < lines.size(); i++) {
            sb.append(lines.get(i)).append("\n");
        }
        return sb.toString();
    }

    private boolean isReadyPrompt(String line) {
        return line.endsWith(">") || line.endsWith("$") || line.contains("claude>");
    }

    private List<String> extractNumberedChoices(String text) {
        List<String> choices = new ArrayList<>();
        String[] lines = text.split("\n");

        for (String line : lines) {
            Matcher matcher = NUMBERED_CHOICE_PATTERN.matcher(line);
            if (matcher.find()) {
                choices.add(matcher.group(1).trim());
            }
        }

        return choices;
    }

    private String extractQuestion(String text, int choiceCount) {
        String[] lines = text.split("\n");
        StringBuilder question = new StringBuilder();

        boolean inChoices = false;
        int choicesFound = 0;

        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i].trim();

            if (NUMBERED_CHOICE_PATTERN.matcher(line).find()) {
                choicesFound++;
                inChoices = true;
                if (choicesFound >= choiceCount) {
                    break;
                }
            } else if (inChoices && !line.isEmpty()) {
                question.insert(0, line + " ");
                break;
            }
        }

        return question.toString().trim();
    }

    private boolean isYesNoPrompt(String line) {
        return YES_NO_PATTERN.matcher(line).find();
    }

    private boolean isToolApprovalPrompt(String text) {
        String lower = text.toLowerCase();
        return lower.contains("approve") ||
               lower.contains("allow") ||
               lower.contains("proceed") ||
               (lower.contains("tool") && isYesNoPrompt(text));
    }

    private boolean isProcessing(String line) {
        String lower = line.toLowerCase();
        return lower.contains("processing") ||
               lower.contains("thinking") ||
               lower.contains("working") ||
               lower.contains("...");
    }

    private boolean isError(String text) {
        String lower = text.toLowerCase();
        return lower.contains("error") ||
               lower.contains("failed") ||
               lower.contains("exception");
    }
}