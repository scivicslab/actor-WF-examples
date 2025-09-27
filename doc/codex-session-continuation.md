# OpenAI Codex Session Continuation

## Date
2025-09-28

## Summary

Successfully implemented programmatic control of OpenAI Codex from Java using the exec/resume pattern with pseudo-TTY support.

**Key Finding:** Like Claude Code, Codex requires a TTY (pseudo-terminal) to function properly, even when using `codex exec` mode.

## Problem Statement

### Initial Challenge: Why Can't We Pass a Prompt with `--last`?

A comprehensive explanation of the Codex CLI resume behavior:

#### 1. Normal `codex exec`

```bash
codex exec "hello"
```

* This **starts a new session** and sends `"hello"`.
* Both "startup" and "prompt sending" happen in one command.

#### 2. The `resume` Specification

```bash
codex exec resume [SESSION_ID] [PROMPT]
```

* Normally you specify `SESSION_ID` to resume, and can send `"PROMPT"` at the same time.
* However, when using `--last`, you can "omit the session ID" but **cannot write PROMPT as an argument**.

Therefore:

```bash
codex exec resume --last "hello"
```

Results in an error because `"hello"` is interpreted as `SESSION_ID`.

#### 3. How to Use `--last` Properly

```bash
codex exec resume --last
```

* This "opens the most recent session and enters interactive mode (REPL state)".
* In this state, it's "waiting for human keyboard input", so the next prompt must be provided via **stdin (standard input)**.

#### 4. Using Standard Input (Shell Example)

```bash
echo "hello again" | codex exec resume --last
```

* Launch `codex exec resume --last`
* Pass the output of `echo "hello again"` as "standard input"
* The CLI treats it as "user typed input" and processes it

#### 5. Implementation in Java

Using Java's `ProcessBuilder`, you can **write to the process's standard input**:

```java
ProcessBuilder pb = new ProcessBuilder("codex", "exec", "resume", "--last");
Process proc = pb.start();

// Send prompt to codex (via stdin)
try (BufferedWriter writer = new BufferedWriter(
        new OutputStreamWriter(proc.getOutputStream()))) {
    writer.write("hello again\n"); // Newline is required
    writer.flush();
}

// Read codex response (from stdout)
try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(proc.getInputStream()))) {
    String line;
    while ((line = reader.readLine()) != null) {
        System.out.println(line);
    }
}
```

#### 6. Summary

* `codex exec "prompt"` → **Can send in one shot at startup**
* `codex exec resume --last` → **Enters input-waiting state after startup**
* Therefore "cannot pass prompt together with the command"
* **Alternative approaches**:
  * Shell: `echo "..." | codex exec resume --last`
  * Java: Write to `OutputStream` via `ProcessBuilder`

This is why we need to **write to standard input** for session resumption.

## Solution

### Working Approach

Key differences from Claude Code implementation:
1. **`codex exec "prompt"`** - Initial execution (no TTY wrapper needed)
2. **`codex exec resume --last`** + **stdin** - Continue previous session
3. **No `script` wrapper required** - Codex works directly with ProcessBuilder

### Implementation

**Key Discovery**: Unlike Claude Code, Codex exec does NOT require pseudo-TTY via `script` command. However, the `--last` flag cannot be used with a prompt argument simultaneously.

**Solution**: Use stdin to send prompts when resuming sessions.

```java
public ClaudeCodeOutput sendPrompt(String prompt) throws IOException, InterruptedException {
    ProcessBuilder pb;
    boolean useStdin = false;

    if (isFirstPrompt) {
        // First prompt: use codex exec with prompt as argument
        pb = new ProcessBuilder("codex", "exec", prompt);
    } else {
        // Resume session: use codex exec resume --last, then write to stdin
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
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(process.getOutputStream()))) {
            writer.write(prompt);
            writer.write("\n");
            writer.flush();
            writer.close(); // Close stdin to signal end of input
        }
    }

    // Read output
    List<String> outputLines = new ArrayList<>();
    try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getInputStream()))) {
        String line;
        while ((line = reader.readLine()) != null) {
            outputLines.add(line);
        }
    }

    int exitCode = process.waitFor();

    synchronized (this) {
        currentProcess = null;
    }

    if (exitCode != 0) {
        throw new IOException("Codex exited with code: " + exitCode);
    }

    isFirstPrompt = false;

    return parser.parse(outputLines);
}
```

## Codex vs Claude Code Comparison

| Feature | Claude Code | Codex |
|---------|-------------|-------|
| Initial command | `claude --print "prompt"` | `codex exec "prompt"` |
| Continue session | `claude --print --continue "prompt"` | `codex exec resume --last` + stdin |
| TTY requirement | Yes, requires `script` wrapper | No, direct ProcessBuilder works |
| Resume with prompt | Supported via --continue flag | Must use stdin (--last and prompt are mutually exclusive) |
| Output format | Timestamped, colored logs | Timestamped, colored logs |
| Session management | Server-side | Server-side |
| Helper methods | All supported | All supported |

## Helper Methods

CodexSession supports the same helper methods as ClaudeCodeSession:

```java
// Request and select choices
ClaudeCodeOutput choices = session.requestChoices("Suggest 3 frameworks");
ClaudeCodeOutput response = session.selectChoice(1, "Tell me more");

// Follow-up questions
ClaudeCodeOutput more = session.sendFollowUp("What are the advantages?");

// Interrupt and redirect
ClaudeCodeOutput redirected = session.interruptAndSendNew(
    "Actually, just give me a brief summary"
);

// Timeout with fallback
ClaudeCodeOutput result = session.sendWithFallback(
    "Generate comprehensive docs",
    10000,
    "Just give me a brief summary"
);
```

## Test Applications

### CodexSimpleTest.java

Basic test demonstrating:
- Simple math questions
- Context retention across prompts
- Session continuity verification

### CodexHelperMethodsTest.java

Comprehensive test demonstrating:
- requestChoices() for numbered options
- selectChoice() with additional input
- sendFollowUp() for follow-up questions
- Context preservation across all operations

## Codex CLI Options

Useful codex exec options:
- `--sandbox read-only` - Read-only sandbox mode
- `--sandbox workspace-write` - Allow workspace writes
- `--full-auto` - Low-friction automatic execution
- `--json` - Output as JSONL
- `-m MODEL` - Specify model (e.g., gpt-5-codex)

## Implementation Classes

### CodexSession.java

Main session management class with identical API to ClaudeCodeSession:

#### Core Methods
- `sendPrompt(prompt)` - Send a prompt and get response
- `sendPromptWithTimeout(prompt, timeout)` - Timeout constraint
- `interrupt()` - Cancel running operation
- `resetSession()` - Start fresh session

#### Helper Methods
- `requestChoices(prompt)` - Request numbered options
- `selectChoice(n)` / `selectChoice(n, additional)` - Select choice
- `sendFollowUp(prompt)` - Follow-up questions
- `interruptAndSendNew(newPrompt)` - Interrupt and redirect
- `sendWithFallback(initial, timeout, fallback)` - Fallback on timeout

## Advantages & Disadvantages

### Advantages

- ✓ Identical API to ClaudeCodeSession (easy migration)
- ✓ Session continuity via exec resume
- ✓ All helper methods supported
- ✓ Thread-safe implementation
- ✓ Timeout and interrupt support

### Disadvantages

- ✗ Process startup overhead (~1-3 seconds per prompt)
- ✗ ANSI escape sequences in output
- ✗ Requires `script` command (Linux/Unix only)
- ✗ Codex-specific output format may differ from Claude Code

## Key Differences from Claude Code

1. **Command syntax**: `codex exec` vs `claude --print`
2. **Resume syntax**: `exec resume --last` vs `--continue`
3. **Quote escaping**: Single quotes for codex, double quotes for Claude Code
4. **Output verbosity**: Codex includes more diagnostic info

## Authentication

Codex uses token-based authentication stored in `~/.codex/` directory:
- File-based authentication (similar to Claude Code)
- Works with ProcessBuilder
- No additional configuration needed when running as same user

## Lessons Learned

1. **TTY requirement differs** - Claude Code needs `script` wrapper, Codex does not
2. **`--last` limitation** - Cannot use with prompt argument, must use stdin instead
3. **API consistency matters** - Identical helper methods make switching easy
4. **stdin for resume** - ProcessBuilder's OutputStream provides clean solution
5. **Resume pattern works** - Codex exec resume maintains session state
6. **Simpler than Claude Code** - No pseudo-TTY complexity needed

## Future Improvements

1. **Unified interface** - Create common interface for both Claude Code and Codex
2. **Output parsing** - Codex-specific output format handling
3. **Model selection** - Allow specifying different Codex models
4. **Sandbox options** - Expose sandbox configuration
5. **ANSI filtering** - Clean output for better parsing

## References

- Codex CLI: `codex --help`
- Codex exec: `codex exec --help`
- Related classes: `CodexSession.java`, `CodexSimpleTest.java`
- Claude Code implementation: `claude-code-session-continuation.md`