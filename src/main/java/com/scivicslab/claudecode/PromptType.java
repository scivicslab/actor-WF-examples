package com.scivicslab.claudecode;

public enum PromptType {
    READY,              // Initial prompt, ready for input
    YES_NO,             // Yes/No question (y/n)
    NUMBERED_CHOICE,    // Numbered options (1, 2, 3...)
    TOOL_APPROVAL,      // Tool execution approval
    PROCESSING,         // Claude is processing
    RESPONSE,           // Claude's text response
    ERROR,              // Error message
    UNKNOWN             // Unknown state
}