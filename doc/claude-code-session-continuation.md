# Claude Code Session Continuation Research

## Date
2025-09-28

## Summary

Successfully implemented programmatic control of Claude Code from Java by combining:
1. `--print --continue` options for session continuity
2. `script` command for pseudo-TTY provision

**Key Finding:** Claude Code requires a TTY (pseudo-terminal) to function properly, even when using `--print` mode.

## Problem Statement

When attempting to programmatically control Claude Code from Java, the following issues were encountered:

### 1. Interactive Mode Issues

**Approach:** Launch `claude` using `ProcessBuilder` and connect via stdin/stdout pipes

**Problem:**
- No response received from Claude Code process
- REPL requires TTY (pseudo-terminal) to function properly
- Input sent via standard input pipes is not recognized by the CLI
- Process hangs indefinitely waiting for terminal interaction

### 2. `--print` Option Limitation

**Approach:** Use `claude --print "prompt"` for one-shot execution

**Problem:**
- Each invocation starts a new session
- Previous conversation context is lost
- Cannot maintain multi-turn conversations
- Not suitable for interactive workflows

### 3. TTY Requirement Issue

**Critical Discovery:** Even with `--print` mode, Claude Code still requires a TTY environment.

**Problem:**
- Direct `ProcessBuilder` invocation hangs without TTY
- Using `bash -c` wrapper does not provide TTY
- Process waits indefinitely for terminal interaction

## Solution

### Final Working Approach

Combine three elements:
1. **`--print` option** - Non-interactive output mode
2. **`--continue` option** - Session continuity
3. **`script` command** - Pseudo-TTY provision

### Implementation

#### Basic Implementation

```java
public ClaudeCodeOutput sendPrompt(String prompt) throws IOException, InterruptedException {
    StringBuilder commandStr = new StringBuilder("claude --print");

    if (!isFirstPrompt) {
        commandStr.append(" --continue");
    }

    commandStr.append(" \"").append(prompt.replace("\"", "\\\"")).append("\"");

    // Critical: Use 'script' to provide pseudo-TTY
    ProcessBuilder pb = new ProcessBuilder("script", "-q", "-c", commandStr.toString(), "/dev/null");
    pb.redirectErrorStream(true);
    Process process = pb.start();

    synchronized (this) {
        currentProcess = process;  // Track for interrupt support
    }

    // Read response
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
        throw new IOException("Claude Code exited with code: " + exitCode);
    }

    isFirstPrompt = false;

    return parser.parse(outputLines);
}
```

#### Interrupt Support

```java
/**
 * Interrupts the currently running Claude Code process.
 * Simulates user pressing Escape key to cancel operation.
 */
public synchronized boolean interrupt() {
    if (currentProcess != null && currentProcess.isAlive()) {
        currentProcess.destroy();
        try {
            if (!currentProcess.waitFor(5, TimeUnit.SECONDS)) {
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
```

#### Timeout Support

```java
/**
 * Sends a prompt with timeout constraint.
 * Automatically interrupts if execution exceeds timeout.
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

    promptThread.start();
    promptThread.join(timeoutMillis);

    if (promptThread.isAlive()) {
        interrupt();
        promptThread.join(5000);
        throw new IOException("Prompt execution timed out after " + timeoutMillis + "ms");
    }

    if (ioException[0] != null) throw ioException[0];
    if (interruptedException[0] != null) throw interruptedException[0];

    return result[0];
}
```

### Test Results

```bash
=== Test 1: Simple math question ===
Sending: What is 2 + 2?
--- Output ---
Type: RESPONSE
Lines: 2
4

=== Test 2: Another math question ===
Sending: What is 5 * 3?
--- Output ---
Type: RESPONSE
Lines: 2
15

=== Test 3: Context check ===
Sending: What was my first question?
--- Output ---
Type: RESPONSE
Lines: 2
Your first question was "What is 2 + 2?"

=== Test completed ===
Successfully sent 3 prompts and captured responses
```

**Result:** Complete success! Session continuity confirmed in Test 3.

## Key Components

### The `script` Command

The `script` command is a Linux utility that creates a pseudo-TTY:

```bash
script -q -c "command" /dev/null
```

**Options:**
- `-q` : Quiet mode (suppress script start/done messages)
- `-c "command"` : Run command in pseudo-TTY
- `/dev/null` : Discard typescript file

**Why it works:**
- Creates a pseudo-terminal (PTY) master/slave pair
- Command runs in TTY environment
- Allows Claude Code to detect interactive terminal
- Output captured through standard streams

### Session Continuity Pattern

```bash
# First invocation (creates new session)
script -q -c "claude --print \"prompt1\"" /dev/null

# Subsequent invocations (continue session)
script -q -c "claude --print --continue \"prompt2\"" /dev/null
script -q -c "claude --print --continue \"prompt3\"" /dev/null
```

## Advantages & Disadvantages

### Advantages

- ✓ Session continuity maintained across invocations
- ✓ Synchronous response retrieval for each prompt
- ✓ No complex terminal emulation libraries needed
- ✓ Works with standard Java `ProcessBuilder`
- ✓ Portable solution using standard Linux utilities

### Disadvantages

- ✗ Process startup overhead for each prompt (~1-3 seconds)
- ✗ ANSI escape sequences in output (can be filtered)
- ✗ Requires `script` command (Linux/Unix only)
- ✗ May not be suitable for high-frequency requests

### Output Cleaning

Output may contain ANSI escape sequences like `[?25h`:

```java
// Simple filter to remove ANSI escape sequences
String cleaned = line.replaceAll("\\x1B\\[[0-9;]*[a-zA-Z]", "");
```

## Alternative Approaches (Not Required)

Since we found a working solution, these alternatives were not pursued:

### 1. PTY Libraries (Not needed)

Java libraries that could provide pseudo-terminal:
- **pty4j** - Java PTY implementation
- **JNI-based solutions** - Native PTY access

**Not implemented because:** `script` command provides simpler solution

### 2. Expect-style Libraries (Not needed)

Libraries for interactive CLI control:
- **expect4j** - Java port of expect
- **ExpectIt** - Modern Java expect library

**Not implemented because:** `--print --continue` pattern is sufficient

## Additional Findings

### Authentication Mechanism

Claude Code uses token-based authentication stored in `~/.claude/.credentials.json`:

- File-based authentication (not environment variables)
- Works correctly with `ProcessBuilder` launched processes
- No additional configuration needed when running as same user
- Token persists across multiple invocations

### PATH Environment

When using `ProcessBuilder`, ensure `claude` command is accessible:

- Use `bash -c` if PATH environment needed
- Or use full path to claude binary
- `script` command respects user's shell environment

## Failed Approaches (For Reference)

### 1. Tmux-based Control

**Approach:**
- Launch claude inside tmux session
- Use `tmux capture-pane` to retrieve output
- Send input via `tmux send-keys`

**Issue:** Claude Code did not respond even within tmux (TTY problem persists)

### 2. ProcessBuilder Interactive Mode

**Approach:**
- Launch with `ProcessBuilder("claude")`
- Connect stdin/stdout/stderr pipes
- Write to `OutputStream`, read from `InputStream`

**Issue:** No TTY available, input not recognized by CLI, process hangs

### 3. Bash Wrapper Only

**Approach:**
- Use `bash -c "claude --print 'prompt'"`

**Issue:** Still no TTY, process hangs without `script` wrapper

### 4. `--print` Only (Without TTY)

**Approach:**
- Direct `ProcessBuilder("claude", "--print", prompt)`

**Issue:** Process hangs waiting for TTY, even though `--print` suggests non-interactive mode

## Implementation Classes

### ClaudeCodeSession.java

Main session management class implementing the `script` wrapper pattern with helper methods:

#### Core Methods

```java
public class ClaudeCodeSession {
    private boolean isFirstPrompt = true;

    public ClaudeCodeOutput sendPrompt(String prompt) {
        // Uses: script -q -c "claude --print [--continue] \"prompt\"" /dev/null
        // Returns: Parsed output with conversation context preserved
    }

    public ClaudeCodeOutput sendPromptWithTimeout(String prompt, long timeoutMillis) {
        // Sends prompt with timeout constraint
        // Throws IOException if timeout exceeded
    }

    public boolean interrupt() {
        // Interrupts currently running process
        // Simulates pressing Escape key
    }

    public void resetSession() {
        // Forces next prompt to start new session
        // Clears conversation history
    }
}
```

#### Helper Methods for Common Patterns

```java
// 1. Request and select choices
public ClaudeCodeOutput requestChoices(String prompt) {
    // Convenience method for requesting numbered options
}

public ClaudeCodeOutput selectChoice(int choiceNumber) {
    // Sends: "I choose option N"
}

public ClaudeCodeOutput selectChoice(int choiceNumber, String additionalInput) {
    // Sends: "I choose option N. <additionalInput>"
}

// 2. Follow-up questions
public ClaudeCodeOutput sendFollowUp(String followUpPrompt) {
    // Convenience alias for sendPrompt() for clarity
}

// 3. Interrupt and redirect
public ClaudeCodeOutput interruptAndSendNew(String newPrompt) {
    // Interrupts current operation
    // Waits for cleanup
    // Sends new prompt
}

// 4. Timeout with fallback
public ClaudeCodeOutput sendWithFallback(
    String initialPrompt,
    long timeoutMillis,
    String fallbackPrompt
) {
    // Tries initial prompt with timeout
    // On timeout, automatically sends fallback
}
```

**Usage Example:**
```java
ClaudeCodeSession session = new ClaudeCodeSession("my-session");

// Request choices
ClaudeCodeOutput choices = session.requestChoices(
    "Suggest 3 web frameworks as numbered options"
);

// Select with additional question
ClaudeCodeOutput response = session.selectChoice(2, "What are the pros and cons?");

// Follow-up
ClaudeCodeOutput companies = session.sendFollowUp(
    "Which companies use this in production?"
);

// Interrupt and redirect
ClaudeCodeOutput redirected = session.interruptAndSendNew(
    "Actually, just give me a brief summary instead"
);

// Timeout with fallback
ClaudeCodeOutput result = session.sendWithFallback(
    "Generate comprehensive documentation",
    10000,
    "Just give me a brief summary"
);
```

### ClaudeCodeSimpleTest.java

Test demonstrating multi-turn conversation:
- Test 1: Basic arithmetic
- Test 2: Another calculation
- Test 3: Context check (proves session continuity)

### ClaudeCodeInteractiveTest.java

Advanced test demonstrating interactive choice handling:
- Test 1: Request numbered options (programming language recommendation)
- Test 2: Make a selection and get detailed information
- Test 3: Verify context retention of the selection
- Test 4: Handle yes/no question scenarios

**Example Output:**
```bash
=== Test 1: Request programming language recommendation ===
Sending: I'm starting a new web application project...

--- Output ---
Type: NUMBERED_CHOICE
Lines: 6
Choices detected: 3

Content:
1. **JavaScript/TypeScript** - Runs on both frontend...
2. **Python** - Backend language known for simplicity...
3. **Go** - Backend language designed by Google...

*** Detected numbered choices! ***
Available choices:
  1. **JavaScript/TypeScript** - ...
  2. **Python** - ...
  3. **Go** - ...

=== Test 2: Make a selection ===
Sending: I'll go with option 2. Can you tell me more...

--- Output ---
Type: RESPONSE
Content:
Python is a good choice for web development because:
**Easy to learn and read** - ...
**Mature frameworks** - ...

=== Test 3: Verify context retention ===
Sending: What programming language did I choose?

--- Output ---
Type: RESPONSE
Content:
Python (option 2).
```

**Key Features Demonstrated:**
- `PromptType.NUMBERED_CHOICE` detection via `ClaudeCodeParser`
- Extracting numbered choices from Claude's response
- Session continuity across choice selection
- Context retention verification

### ClaudeCodeInterruptTest.java

Advanced test demonstrating interrupt and resume functionality:
- Test 1: Establish initial context (name and project)
- Test 2: Send prompt with timeout constraint (30 seconds)
- Test 3: Manual interrupt of a long-running prompt
- Test 4: Resume with new prompt after interruption
- Test 5: Verify original context is preserved

**Example Output:**
```bash
=== Test 1: Establish initial context ===
Sending: My name is Alice and I'm working on a machine learning project.

--- Output ---
Hello Alice! I'm here to help you with your machine learning project...

=== Test 2: Send prompt with timeout ===
Sending with 30-second timeout: Write a detailed explanation of neural networks.

--- Output (completed within timeout) ---
A neural network is a computational model inspired by biological neurons...
(Full detailed explanation provided)

=== Test 3: Manual interrupt demonstration ===
Starting a potentially long task...
Waiting 3 seconds then interrupting...
Sending long prompt: Generate a comprehensive tutorial on quantum computing...
*** Sending interrupt (simulating Escape key) ***
Interrupt result: Process interrupted

=== Test 4: Continue after interrupt ===
Sending: Instead, just give me a brief one-sentence summary of quantum computing.

--- Output ---
Quantum computing uses quantum mechanical properties like superposition...

=== Test 5: Verify initial context is preserved ===
Sending: What is my name and what am I working on?

--- Output ---
Your name is Alice and you're working on a machine learning project.
```

**Key Features Demonstrated:**
- **Timeout handling**: `sendPromptWithTimeout()` method with configurable timeout
- **Manual interrupt**: `interrupt()` method to cancel running prompts
- **Session preservation**: Context maintained across interruptions
- **Graceful recovery**: Ability to continue conversation after interrupt
- **Thread safety**: Synchronized access to current process

**Use Cases:**
- Cancelling long-running code generation tasks
- Implementing user-triggered interrupts (Escape key simulation)
- Setting timeout constraints for production systems
- Recovering from hung or slow responses

### ClaudeCodeHelperMethodsTest.java

Comprehensive test demonstrating all helper methods:
- Test 1: Request choices using `requestChoices()`
- Test 2: Select choice with additional input using `selectChoice(2, "question")`
- Test 3: Send follow-up using `sendFollowUp()`
- Test 4: Interrupt and redirect using `interruptAndSendNew()`
- Test 5: Fallback behavior using `sendWithFallback()`
- Test 6: Verify context retention across all operations

**Example Output:**
```bash
=== Test 1: Request choices ===
Sending: I'm building a web application. Suggest 3 backend frameworks...

--- Choices received ---
Type: NUMBERED_CHOICE
1. **Spring Boot** (Java) - Enterprise-grade...
2. **Express.js** (Node.js) - Minimal, flexible...
3. **Django** (Python) - Batteries-included...

=== Test 2: Select choice with additional input ===
Selecting: option 2 with additional question

--- Selection response ---
**Advantages:**
- Minimal and unopinionated - flexible architecture choices
- Fast development with JavaScript/TypeScript across full stack
...

=== Test 3: Send follow-up question ===
Sending follow-up: Which companies use this framework in production?

--- Follow-up response ---
**Major companies using Express.js:**
- Netflix, Uber, PayPal, NASA, IBM, Twitter...

=== Test 4: Interrupt and redirect ===
Starting a long request...
*** User changes mind - interrupting and redirecting ***

--- Redirected response ---
Microservices are an architectural style where an application is built
as a collection of small, independent services...

=== Test 6: Verify context retention ===
Sending: What was the web application framework I chose earlier?

--- Context check response ---
Express.js
```

**Key Features Demonstrated:**
- **requestChoices()** - Semantic method for requesting options
- **selectChoice(n, "additional")** - Clean choice selection API
- **sendFollowUp()** - Clear intent for follow-up questions
- **interruptAndSendNew()** - One-step interrupt and redirect
- **sendWithFallback()** - Automatic fallback on timeout
- **Context preservation** - All operations maintain conversation history

## Lessons Learned

1. **TTY is critical** - Even "non-interactive" modes may require TTY
2. **`script` is powerful** - Simple pseudo-TTY solution
3. **`--continue` works** - Session state preserved across invocations
4. **Test context retention** - Always verify session continuity explicitly
5. **Interrupt support** - Process can be safely interrupted and session resumed
6. **Thread safety matters** - Synchronized access required for concurrent operations
7. **Timeout handling** - Essential for production systems to prevent hangs

## Future Improvements

1. **ANSI escape sequence filtering** - Clean output for better parsing
2. **Error handling** - Handle `script` command not found, permission errors
3. **Performance optimization** - Consider connection pooling or session reuse
4. **Cross-platform support** - Windows equivalent (ConPTY or similar)
5. ~~**Timeout handling** - Add configurable timeouts for long-running prompts~~ ✓ **Implemented**
6. ~~**Interrupt support** - Allow cancelling long-running operations~~ ✓ **Implemented**

## References

- Claude Code CLI documentation: https://docs.claude.ai/en/docs/claude-code
- Claude Code help: `claude --help`
- `script` command: `man script`
- Related classes: `ClaudeCodeSession.java`, `ClaudeCodeSimpleTest.java`
- Linux PTY documentation: `man pty`