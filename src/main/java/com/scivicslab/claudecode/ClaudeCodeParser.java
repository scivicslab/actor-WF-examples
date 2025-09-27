package com.scivicslab.claudecode;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClaudeCodeParser {

    private static final Pattern NUMBERED_CHOICE_PATTERN = Pattern.compile("^\\s*\\d+[\\.\\)]\\s+(.+)$");
    private static final Pattern YES_NO_PATTERN = Pattern.compile("\\(y/n\\)|\\[y/n\\]|\\(yes/no\\)", Pattern.CASE_INSENSITIVE);

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