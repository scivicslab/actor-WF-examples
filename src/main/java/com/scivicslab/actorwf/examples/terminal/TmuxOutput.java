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

import java.util.List;

/**
 * Represents the captured output from a tmux pane at a specific point in time.
 * This immutable class contains the text lines from the terminal, prompt detection status,
 * and a timestamp indicating when the output was captured.
 */
public class TmuxOutput {

    /** The list of text lines captured from the tmux pane */
    private final List<String> lines;

    /** Indicates whether a shell prompt was detected in the output */
    private final boolean hasPrompt;

    /** The timestamp (in milliseconds since epoch) when this output was captured */
    private final long timestamp;

    /**
     * Creates a new TmuxOutput instance with the specified lines and prompt status.
     * The timestamp is automatically set to the current time.
     *
     * @param lines the list of text lines from the tmux pane
     * @param hasPrompt whether a shell prompt was detected
     */
    public TmuxOutput(List<String> lines, boolean hasPrompt) {
        this.lines = lines;
        this.hasPrompt = hasPrompt;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Gets the list of text lines captured from the tmux pane.
     *
     * @return the list of captured lines
     */
    public List<String> getLines() {
        return lines;
    }

    /**
     * Checks whether a shell prompt was detected in the captured output.
     *
     * @return true if a prompt was detected, false otherwise
     */
    public boolean hasPrompt() {
        return hasPrompt;
    }

    /**
     * Gets the timestamp when this output was captured.
     *
     * @return the timestamp in milliseconds since epoch
     */
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return String.format("TmuxOutput[lines=%d, hasPrompt=%b, timestamp=%d]",
            lines.size(), hasPrompt, timestamp);
    }
}