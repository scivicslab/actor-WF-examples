# Claude Code Control - Usage Guide

## Prerequisites

1. **Java 21 or higher** - Required for virtual threads
2. **Maven 3.6+** - For building the project
3. **tmux** - Terminal multiplexer
   ```bash
   # Check if installed
   which tmux

   # Install on Ubuntu/Debian
   sudo apt-get install tmux
   ```
4. **Claude Code CLI** - Anthropic's Claude Code tool
   ```bash
   # Check if installed
   which claude

   # Install from: https://docs.claude.ai/en/docs/claude-code
   ```

## Building the Project

```bash
cd actor-WF-examples
mvn clean package -DskipTests
```

This creates: `target/actor-WF-examples-1.0.0.jar`

## Available Programs

### 1. ClaudeCodeSimpleTest (Simplest)

**Purpose:** Minimal example with 3 prompt exchanges

**Actors:** 1 (session only)

**Usage:**
```bash
java -cp target/actor-WF-examples-1.0.0.jar \
    com.scivicslab.claudecode.ClaudeCodeSimpleTest
```

**What it does:**
1. Creates tmux session
2. Starts Claude Code
3. Sends 3 prompts with 15-second waits
4. Captures and prints last 15 lines after each
5. Exits and cleans up

**Best for:** Testing basic functionality, debugging

### 2. ClaudeCodeApp (Basic with Monitoring)

**Purpose:** Demonstrates monitoring pattern

**Actors:** 2 (session + monitor)

**Usage:**
```bash
java -cp target/actor-WF-examples-1.0.0.jar \
    com.scivicslab.claudecode.ClaudeCodeApp
```

**What it does:**
1. Creates session and starts monitoring
2. Sends one prompt
3. Waits 10 seconds
4. Checks if waiting for input
5. Displays prompt type and choices if any

**Best for:** Understanding the monitor pattern

### 3. ClaudeCodeConversationApp (Full Conversation)

**Purpose:** Multi-turn conversation with detailed output

**Actors:** 2 (session + monitor)

**Usage:**
```bash
java -cp target/actor-WF-examples-1.0.0.jar \
    com.scivicslab.claudecode.ClaudeCodeConversationApp
```

**What it does:**
1. Creates session with monitoring
2. Sends 3 prompts with 30-second waits
3. Captures full output after each turn
4. Displays conversation summary

**Best for:** Testing multi-turn conversations

### 4. ClaudeCodeWorkflowApp (YAML-based)

**Purpose:** Declarative workflow execution

**Actors:** 3 (session + monitor + interpreter)

**Usage:**
```bash
java -cp target/actor-WF-examples-1.0.0.jar \
    com.scivicslab.claudecode.ClaudeCodeWorkflowApp claude-code-basic
```

**Workflow file:** `src/main/resources/code/claude-code-basic.yaml`

**What it does:**
Executes YAML-defined workflow:
1. Create session
2. Start Claude Code
3. Start monitoring
4. Wait 5 seconds
5. Print initial state
6. Send prompt
7. Wait 10 seconds
8. Print response
9. Cleanup

**Best for:** Automated sequences, CI/CD integration

## Programming Guide

### Basic Pattern (1 Actor)

```java
import com.scivicslab.pojoactor.ActorSystem;
import com.scivicslab.pojoactor.ActorRef;
import com.scivicslab.claudecode.ClaudeCodeSession;

public class MyClaudeApp {
    public static void main(String[] args) throws Exception {
        // Create actor system
        ActorSystem system = new ActorSystem.Builder("my-system").build();

        // Create session
        String sessionName = "my-session-" + System.currentTimeMillis();
        ClaudeCodeSession session = new ClaudeCodeSession(sessionName);

        // Wrap in actor
        ActorRef<ClaudeCodeSession> sessionRef =
            system.actorOf("session", session);

        try {
            // Initialize
            sessionRef.tell(s -> {
                s.createSession();
                s.startClaudeCode();
            }).get();

            Thread.sleep(3000);  // Wait for startup

            // Send prompt
            sessionRef.tell(s -> s.sendPrompt("Hello!")).get();

            Thread.sleep(15000);  // Wait for response

            // Get output
            var output = sessionRef.ask(s -> s.captureOutput()).get();
            for (String line : output) {
                System.out.println(line);
            }

            // Cleanup
            sessionRef.tell(s -> {
                s.sendCtrlD();
                Thread.sleep(1000);
                s.killSession();
            }).get();

        } finally {
            system.terminate();
        }
    }
}
```

### With Monitoring (2 Actors)

```java
import com.scivicslab.claudecode.ClaudeCodeMonitor;

// ... create system and session ...

ActorRef<ClaudeCodeSession> sessionRef = system.actorOf("session", session);
ActorRef<ClaudeCodeMonitor> monitorRef = system.actorOf("monitor",
    new ClaudeCodeMonitor(session, 2000));  // Poll every 2 seconds

// Start monitoring
monitorRef.tell(m -> m.startMonitoring()).get();

// Send prompt
sessionRef.tell(s -> s.sendPrompt("Hello")).get();

Thread.sleep(15000);

// Check latest output
ClaudeCodeOutput output = monitorRef.ask(m -> m.getLatestOutput()).get();
System.out.println("Prompt type: " + output.getPromptType());

if (output.hasChoices()) {
    System.out.println("Choices:");
    for (int i = 0; i < output.getChoices().size(); i++) {
        System.out.println((i + 1) + ". " + output.getChoices().get(i));
    }
    // Select first choice
    sessionRef.tell(s -> s.sendChoice(1)).get();
}

// Stop monitoring
monitorRef.tell(m -> m.stopMonitoring()).get();
```

### Workflow Pattern (3 Actors)

**Java Code:**

```java
import com.scivicslab.actorwf.*;

IIActorSystem system = new IIActorSystem("workflow-system");

// Create and register actors
ClaudeCodeSession session = new ClaudeCodeSession(sessionName);
ClaudeCodeSessionIIAR sessionActor =
    new ClaudeCodeSessionIIAR("session", session);
system.addIIActor(sessionActor);

ClaudeCodeMonitor monitor = new ClaudeCodeMonitor(session, 1000);
ClaudeCodeMonitorIIAR monitorActor =
    new ClaudeCodeMonitorIIAR("monitor", monitor);
system.addIIActor(monitorActor);

Interpreter interpreter = new Interpreter.Builder()
    .loggerName("MyWorkflow")
    .team(system)
    .build();
InterpreterIIAR interpreterActor =
    new InterpreterIIAR("@ip", interpreter, system);
system.addIIActor(interpreterActor);

// Load workflow
InputStream yaml = getClass().getResourceAsStream("/code/my-workflow.yaml");
interpreterActor.tell(i -> i.readYaml(yaml)).get();

// Execute workflow (max 50 iterations)
for (int i = 0; i < 50; i++) {
    interpreterActor.tell(interp -> interp.execCode()).get();
}

system.terminate();
```

**YAML Workflow:**

```yaml
---
name: my-workflow
matrix:
  - states: [0, 1]
    actions:
      - - session
        - createSession
        - ""
  - states: [1, 2]
    actions:
      - - session
        - startClaudeCode
        - ""
  - states: [2, 3]
    actions:
      - - "@ip"
        - sleep
        - "3000"
  - states: [3, 4]
    actions:
      - - session
        - sendPrompt
        - "What is 2 + 2?"
  - states: [4, 5]
    actions:
      - - "@ip"
        - sleep
        - "15000"
  - states: [5, 6]
    actions:
      - - session
        - printOutput
        - ""
  - states: [6, -1]
    actions:
      - - session
        - killSession
        - ""
```

## Available Actions

### Session Actions

| Action | Args | Description | Example |
|--------|------|-------------|---------|
| `createSession` | `""` | Create tmux session | `sessionRef.tell(s -> s.createSession())` |
| `startClaudeCode` | `""` | Launch claude | `sessionRef.tell(s -> s.startClaudeCode())` |
| `sendPrompt` | `"text"` | Send user message | `sessionRef.tell(s -> s.sendPrompt("Hello"))` |
| `sendChoice` | `1-9` | Select option | `sessionRef.tell(s -> s.sendChoice(1))` |
| `sendYes` | `""` | Send 'y' | `sessionRef.tell(s -> s.sendYes())` |
| `sendNo` | `""` | Send 'n' | `sessionRef.tell(s -> s.sendNo())` |
| `sendEnter` | `""` | Press Enter | `sessionRef.tell(s -> s.sendEnter())` |
| `sendCtrlC` | `""` | Send Ctrl-C | `sessionRef.tell(s -> s.sendCtrlC())` |
| `sendCtrlD` | `""` | Send Ctrl-D (exit) | `sessionRef.tell(s -> s.sendCtrlD())` |
| `captureOutput` | `""` | Get output lines | `sessionRef.ask(s -> s.captureOutput())` |
| `captureAndParse` | `""` | Get parsed output | `sessionRef.ask(s -> s.captureAndParse())` |
| `killSession` | `""` | Terminate session | `sessionRef.tell(s -> s.killSession())` |

### Monitor Actions

| Action | Args | Description | Example |
|--------|------|-------------|---------|
| `startMonitoring` | `""` | Start background polling | `monitorRef.tell(m -> m.startMonitoring())` |
| `stopMonitoring` | `""` | Stop polling | `monitorRef.tell(m -> m.stopMonitoring())` |
| `getLatestOutput` | `""` | Get latest parsed output | `monitorRef.ask(m -> m.getLatestOutput())` |
| `printLatestOutput` | `""` | Print latest to console | `monitorRef.tell(m -> m.printLatestOutput())` |
| `isMonitoring` | `""` | Check if monitoring | `monitorRef.ask(m -> m.isMonitoring())` |

### Workflow-Only Actions (@ip actor)

| Action | Args | Description |
|--------|------|-------------|
| `sleep` | `"millis"` | Sleep for N milliseconds |
| `print` | `"text"` | Print to console |
| `doNothing` | `"comment"` | No operation (for comments) |

## Timing Guidelines

### Startup Timing

```java
// After starting Claude Code, wait for initialization
sessionRef.tell(s -> s.startClaudeCode()).get();
Thread.sleep(3000);  // 3-5 seconds recommended
```

### Prompt Response Timing

```java
// After sending prompt, wait for Claude to respond
sessionRef.tell(s -> s.sendPrompt("...")).get();
Thread.sleep(15000);  // 15-30 seconds recommended

// For complex tasks, longer waits may be needed
Thread.sleep(60000);  // Up to 60 seconds
```

### Monitoring Interval

```java
// Faster polling = more CPU, faster detection
new ClaudeCodeMonitor(session, 1000);  // 1 second (responsive)
new ClaudeCodeMonitor(session, 2000);  // 2 seconds (balanced)
new ClaudeCodeMonitor(session, 5000);  // 5 seconds (light)
```

## Handling Different Prompt Types

### Example: Auto-respond to prompts

```java
ClaudeCodeOutput output = sessionRef.ask(s -> s.captureAndParse()).get();

switch (output.getPromptType()) {
    case READY:
        // Ready for next prompt
        sessionRef.tell(s -> s.sendPrompt("Next question")).get();
        break;

    case YES_NO:
        // Automatically approve
        sessionRef.tell(s -> s.sendYes()).get();
        break;

    case NUMBERED_CHOICE:
        // Select first option
        sessionRef.tell(s -> s.sendChoice(1)).get();
        break;

    case TOOL_APPROVAL:
        // Approve tool execution
        sessionRef.tell(s -> s.sendYes()).get();
        break;

    case PROCESSING:
        // Wait longer
        Thread.sleep(5000);
        break;

    case ERROR:
        System.err.println("Error detected!");
        break;

    case RESPONSE:
        // Claude is responding, wait for completion
        Thread.sleep(2000);
        break;
}
```

## Best Practices

### 1. Use Unique Session Names

```java
String sessionName = "my-app-" + System.currentTimeMillis();
```

Prevents conflicts with existing sessions.

### 2. Always Clean Up

```java
try {
    // ... your code ...
} finally {
    sessionRef.tell(s -> {
        try {
            s.sendCtrlD();
            Thread.sleep(1000);
            s.killSession();
        } catch (Exception e) {
            // Log error
        }
    }).get();
    system.terminate();
}
```

### 3. Use Monitoring for Long Operations

```java
// Start monitoring before long-running operations
monitorRef.tell(m -> m.startMonitoring()).get();

sessionRef.tell(s -> s.sendPrompt("Complex task...")).get();

// Poll periodically
for (int i = 0; i < 30; i++) {
    Thread.sleep(2000);
    ClaudeCodeOutput output = monitorRef.ask(m -> m.getLatestOutput()).get();
    if (output.getPromptType() == PromptType.READY) {
        break;  // Done
    }
}
```

### 4. Handle Exceptions Properly

```java
sessionRef.tell(s -> {
    try {
        s.sendPrompt(userInput);
    } catch (IOException e) {
        throw new RuntimeException("IO error", e);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException("Interrupted", e);
    }
}).get();
```

### 5. Log Important Events

```java
System.out.println("Sending prompt: " + prompt);
sessionRef.tell(s -> s.sendPrompt(prompt)).get();
System.out.println("Waiting for response...");
Thread.sleep(15000);
System.out.println("Capturing output...");
```

## Troubleshooting

### Claude Code Doesn't Start

**Symptom:** No welcome message appears

**Solutions:**
1. Check `claude` is in PATH: `which claude`
2. Verify authentication: `claude --version`
3. Increase startup wait time to 5 seconds

### No Response to Prompts

**Symptom:** Prompts appear but no answers

**Solutions:**
1. Check network connection
2. Verify API authentication
3. Increase wait time to 30-60 seconds
4. Check Claude Code status manually: `claude`

### Tmux Session Conflicts

**Symptom:** "session already exists" error

**Solutions:**
1. Use unique session names with timestamp
2. Kill existing sessions: `tmux kill-session -t <name>`
3. List sessions: `tmux list-sessions`

### Output Not Captured

**Symptom:** Empty or partial output

**Solutions:**
1. Increase sleep time after operations
2. Check tmux session is active
3. Verify capture timing

### Monitor Not Detecting Changes

**Symptom:** No output updates

**Solutions:**
1. Verify monitoring is started
2. Check polling interval (not too long)
3. Ensure session is active

## Advanced Usage

### Custom Callback on Output Change

```java
ClaudeCodeMonitor monitor = new ClaudeCodeMonitor(session, 2000);

monitor.setOnOutputChange(output -> {
    System.out.println("Output changed: " + output.getPromptType());

    if (output.isWaitingForInput()) {
        System.out.println("ACTION REQUIRED!");
        // Handle automatically or alert user
    }
});

ActorRef<ClaudeCodeMonitor> monitorRef = system.actorOf("monitor", monitor);
monitorRef.tell(m -> m.startMonitoring()).get();
```

### Conditional Workflow Logic

While YAML workflows are sequential, you can implement conditional logic in Java:

```java
for (int turn = 0; turn < 10; turn++) {
    sessionRef.tell(s -> s.sendPrompt("Question " + turn)).get();
    Thread.sleep(15000);

    ClaudeCodeOutput output = sessionRef.ask(s -> s.captureAndParse()).get();

    if (output.getPromptType() == PromptType.ERROR) {
        System.err.println("Error detected, stopping");
        break;
    }

    if (output.hasChoices()) {
        // Select based on some logic
        int choice = selectBestChoice(output.getChoices());
        sessionRef.tell(s -> s.sendChoice(choice)).get();
    }
}
```

## Examples Repository

All examples are in: `src/main/java/com/scivicslab/claudecode/`

- **ClaudeCodeSimpleTest.java** - Basic 3-turn test
- **ClaudeCodeApp.java** - With monitoring
- **ClaudeCodeConversationApp.java** - Full conversation
- **ClaudeCodeWorkflowApp.java** - YAML workflow

Workflows are in: `src/main/resources/code/`

- **claude-code-basic.yaml** - Basic workflow example