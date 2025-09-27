package com.scivicslab.terminal;

import java.util.List;

public class TmuxOutput {

    private final List<String> lines;
    private final boolean hasPrompt;
    private final long timestamp;

    public TmuxOutput(List<String> lines, boolean hasPrompt) {
        this.lines = lines;
        this.hasPrompt = hasPrompt;
        this.timestamp = System.currentTimeMillis();
    }

    public List<String> getLines() {
        return lines;
    }

    public boolean hasPrompt() {
        return hasPrompt;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return String.format("TmuxOutput[lines=%d, hasPrompt=%b, timestamp=%d]",
            lines.size(), hasPrompt, timestamp);
    }
}