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
package com.scivicslab.pojoactor.workflow.examples.claudecode;

/**
 * Enumeration of prompt types that can be detected in Claude Code CLI output.
 * <p>
 * This enum categorizes the various states and interaction modes of the
 * Claude Code CLI session, enabling programmatic handling of different
 * prompt scenarios.
 * </p>
 *
 * @author Scivics Lab
 * @version 1.0
 */
public enum PromptType {
    /**
     * Initial prompt, ready for user input.
     */
    READY,

    /**
     * Yes/No question requiring y/n response.
     */
    YES_NO,

    /**
     * Numbered options (1, 2, 3...) requiring selection.
     */
    NUMBERED_CHOICE,

    /**
     * Tool execution approval request.
     */
    TOOL_APPROVAL,

    /**
     * Claude is currently processing the request.
     */
    PROCESSING,

    /**
     * Claude's text response output.
     */
    RESPONSE,

    /**
     * Error message or exception state.
     */
    ERROR,

    /**
     * Unknown or unrecognized state.
     */
    UNKNOWN
}