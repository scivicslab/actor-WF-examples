## Claude Code Control Implementation

### Overview

This implementation provides automated control of Claude Code CLI through tmux sessions, enabling:
- Starting Claude Code programmatically
- Sending prompts and commands
- Parsing responses and detecting prompt types
- Handling user choices and confirmations
- Real-time monitoring of output

### Architecture

```
ClaudeCodeSession (POJO)
  ├─ Uses TmuxSession for terminal control
  ├─ Manages Claude Code lifecycle
  └─ Provides high-level commands

ClaudeCodeParser (POJO)
  ├─ Parses terminal output
  ├─ Detects prompt types
  └─ Extracts choices and questions

ClaudeCodeMonitor (POJO)
  ├─ Real-time output monitoring
  ├─ Change detection
  └─ Callback support

ClaudeCodeOutput (Data Class)
  ├─ Parsed output with metadata
  ├─ Prompt type information
  └─ Available choices
```

### Core Components

#### PromptType Enum

Defines different types of prompts from Claude Code:

```java
public enum PromptType {
    READY,              // Ready for input
    YES_NO,             // Yes/No question
    NUMBERED_CHOICE,    // Numbered options
    TOOL_APPROVAL,      // Tool execution approval
    PROCESSING,         // Processing/thinking
    RESPONSE,           // Text response
    ERROR,              // Error message
    UNKNOWN             // Unknown state
}
```

#### ClaudeCodeSession

Main class for controlling Claude Code:

**Key Methods:**
- `createSession()` - Create tmux session
- `startClaudeCode()` - Launch Claude Code
- `sendPrompt(String)` - Send user message
- `sendChoice(int)` - Select numbered option
- `sendYes()` / `sendNo()` - Answer y/n questions
- `sendCtrlC()` / `sendCtrlD()` - Send control characters
- `captureAndParse()` - Capture and parse output
- `killSession()` - Terminate session

#### ClaudeCodeParser

Parses terminal output to detect prompt types:

**Detection Logic:**
1. **Numbered Choices**: Detects lines like "1. Option A", "2. Option B"
2. **Yes/No Questions**: Detects "(y/n)", "[y/n]", "(yes/no)"
3. **Tool Approval**: Detects "approve", "allow", "proceed" keywords
4. **Ready Prompt**: Detects command prompt characters (>, $, claude>)
5. **Processing**: Detects "processing", "thinking", "..."
6. **Errors**: Detects "error", "failed", "exception"

#### ClaudeCodeMonitor

Background monitoring with change detection:

**Features:**
- Configurable polling interval
- Callback on output changes
- Thread-safe implementation
- Virtual thread usage

### Usage Examples

#### Traditional Actor-Based Approach

```java
ActorSystem system = new ActorSystem.Builder("claude-code-system").build();

ClaudeCodeSession session = new ClaudeCodeSession("my-session");
ActorRef<ClaudeCodeSession> sessionRef = system.actorOf("session", session);
ActorRef<ClaudeCodeMonitor> monitorRef = system.actorOf("monitor",
    new ClaudeCodeMonitor(session, 1000));

// Create session and start Claude Code
sessionRef.tell(s -> s.createSession()).get();
sessionRef.tell(s -> s.startClaudeCode()).get();

// Start monitoring
monitorRef.tell(m -> m.startMonitoring()).get();

// Send prompt
sessionRef.tell(s -> s.sendPrompt("Hello, can you help me?")).get();

// Wait for response
Thread.sleep(10000);

// Check output
ClaudeCodeOutput output = monitorRef.ask(m -> m.getLatestOutput()).get();
if (output.isWaitingForInput()) {
    if (output.getPromptType() == PromptType.YES_NO) {
        sessionRef.tell(s -> s.sendYes()).get();
    } else if (output.getPromptType() == PromptType.NUMBERED_CHOICE) {
        sessionRef.tell(s -> s.sendChoice(1)).get();
    }
}

// Cleanup
monitorRef.tell(m -> m.stopMonitoring()).get();
sessionRef.tell(s -> s.sendCtrlD()).get();
sessionRef.tell(s -> s.killSession()).get();
system.terminate();
```

#### YAML Workflow Approach

**Workflow Definition (claude-code-basic.yaml):**

```yaml
name: claude-code-basic
matrix:
  - states: [0, 1]
    actions:
      - [session, createSession, ""]
  - states: [1, 2]
    actions:
      - [session, startClaudeCode, ""]
  - states: [2, 3]
    actions:
      - [monitor, startMonitoring, ""]
  - states: [3, 4]
    actions:
      - ["@ip", sleep, "5000"]
  - states: [4, 5]
    actions:
      - [session, sendPrompt, "Hello, can you help me?"]
  - states: [5, 6]
    actions:
      - ["@ip", sleep, "10000"]
  - states: [6, 7]
    actions:
      - [monitor, printLatestOutput, ""]
  - states: [7, -1]
    actions:
      - [monitor, stopMonitoring, ""]
      - [session, killSession, ""]
```

**Java Application:**

```java
IIActorSystem system = new IIActorSystem("claude-code-workflow-system");

ClaudeCodeSession session = new ClaudeCodeSession(sessionName);
ClaudeCodeSessionIIAR sessionActor = new ClaudeCodeSessionIIAR("session", session);
system.addIIActor(sessionActor);

ClaudeCodeMonitor monitor = new ClaudeCodeMonitor(session, 1000);
ClaudeCodeMonitorIIAR monitorActor = new ClaudeCodeMonitorIIAR("monitor", monitor);
system.addIIActor(monitorActor);

Interpreter interpreter = new Interpreter.Builder()
    .loggerName("ClaudeCodeWorkflow")
    .team(system)
    .build();
InterpreterIIAR interpreterActor = new InterpreterIIAR("@ip", interpreter, system);
system.addIIActor(interpreterActor);

// Load and execute workflow
interpreterActor.tell(i -> i.readYaml(yamlInput)).get();
for (int i = 0; i < 50; i++) {
    interpreterActor.tell(interp -> interp.execCode()).get();
}

system.terminate();
```

### Available Actions (Workflow Mode)

#### session Actor (ClaudeCodeSessionIIAR)

| Action | Argument | Description |
|--------|----------|-------------|
| `createSession` | `""` | Create tmux session |
| `startClaudeCode` | `""` | Launch Claude Code |
| `sendPrompt` | `"prompt text"` | Send user message |
| `sendChoice` | `"1"` | Select numbered option |
| `sendYes` | `""` | Send 'y' response |
| `sendNo` | `""` | Send 'n' response |
| `sendEnter` | `""` | Send Enter key |
| `sendCtrlC` | `""` | Send Ctrl-C |
| `captureAndParse` | `""` | Capture and parse output |
| `printOutput` | `""` | Print captured output |
| `killSession` | `""` | Terminate session |

#### monitor Actor (ClaudeCodeMonitorIIAR)

| Action | Argument | Description |
|--------|----------|-------------|
| `startMonitoring` | `""` | Start background monitoring |
| `stopMonitoring` | `""` | Stop monitoring |
| `printLatestOutput` | `""` | Print latest captured output |
| `checkMonitoring` | `""` | Check monitoring status |
| `getPromptType` | `""` | Get current prompt type |

### Running Examples

```bash
# Build the project
mvn clean package -DskipTests

# Run traditional actor-based example
java -cp target/actor-WF-examples-1.0.0.jar \
    com.scivicslab.claudecode.ClaudeCodeApp

# Run YAML workflow example
java -cp target/actor-WF-examples-1.0.0.jar \
    com.scivicslab.claudecode.ClaudeCodeWorkflowApp claude-code-basic
```

### Design Patterns

#### 1. Parser Pattern

The `ClaudeCodeParser` uses pattern matching to identify different prompt types:
- Regex patterns for numbered choices
- Keyword detection for questions and approvals
- State inference from output patterns

#### 2. Monitor Pattern

The `ClaudeCodeMonitor` implements the Observer pattern:
- Continuous polling in background thread
- Change detection
- Callback notification
- Thread-safe state management

#### 3. Layered Architecture

Three layers of abstraction:
1. **POJO Layer**: Core logic (ClaudeCodeSession, Parser, Monitor)
2. **Actor Layer**: Concurrency control (ActorRef wrapping)
3. **Workflow Layer**: Declarative execution (IIActorRef, YAML)

### Handling Different Prompt Types

#### Yes/No Questions

```java
if (output.getPromptType() == PromptType.YES_NO) {
    // Automatically approve
    session.sendYes();
    // Or deny
    session.sendNo();
}
```

#### Numbered Choices

```java
if (output.getPromptType() == PromptType.NUMBERED_CHOICE) {
    List<String> choices = output.getChoices();
    System.out.println("Available options:");
    for (int i = 0; i < choices.size(); i++) {
        System.out.println((i + 1) + ". " + choices.get(i));
    }

    // Select first option
    session.sendChoice(1);
}
```

#### Tool Approval

```java
if (output.getPromptType() == PromptType.TOOL_APPROVAL) {
    String question = output.getQuestion();
    System.out.println("Tool approval requested: " + question);

    // Approve tool execution
    session.sendYes();
}
```

### Advanced Usage

#### Custom Callback

```java
ClaudeCodeMonitor monitor = new ClaudeCodeMonitor(session, 1000);

monitor.setOnOutputChange(output -> {
    System.out.println("Output changed: " + output.getPromptType());

    if (output.isWaitingForInput()) {
        System.out.println("Claude Code is waiting for input");
        // Handle automatically or alert user
    }
});

monitor.startMonitoring();
```

#### Conditional Logic

```java
ClaudeCodeOutput output = session.captureAndParse();

switch (output.getPromptType()) {
    case READY:
        session.sendPrompt("Next command");
        break;
    case YES_NO:
        session.sendYes();
        break;
    case NUMBERED_CHOICE:
        session.sendChoice(1);
        break;
    case TOOL_APPROVAL:
        session.sendYes();
        break;
    case PROCESSING:
        // Wait for completion
        Thread.sleep(2000);
        break;
    case ERROR:
        System.err.println("Error detected");
        break;
}
```

### Limitations and Considerations

1. **Timing Sensitivity**
   - Need appropriate delays after commands
   - Claude Code processing time varies
   - Network latency affects response time

2. **Output Parsing**
   - Parser heuristics may need tuning
   - Output format changes could break parsing
   - Edge cases in prompt detection

3. **Terminal Emulation**
   - Requires tmux installation
   - Limited to terminal-based interaction
   - No access to GUI elements

4. **Error Handling**
   - Network errors not detected
   - Claude Code crashes need external handling
   - Timeout mechanisms recommended

### Future Enhancements

1. **Smarter Parsing**
   - Machine learning for prompt classification
   - Context-aware response selection
   - Multi-turn conversation tracking

2. **Error Recovery**
   - Automatic retry logic
   - Graceful degradation
   - Session recovery

3. **Advanced Features**
   - File upload handling
   - Multi-tool workflows
   - Parallel session management

4. **Integration**
   - REST API wrapper
   - Web UI frontend
   - CI/CD pipeline integration

### Troubleshooting

#### Claude Code Not Starting

- Check `claude` command is in PATH
- Verify Claude Code is installed
- Check authentication status

#### Prompt Not Detected

- Increase sleep duration
- Check terminal output format
- Adjust parser patterns

#### Session Hangs

- Send Ctrl-C to interrupt
- Kill session and restart
- Check tmux session list

#### Output Not Captured

- Verify tmux session is active
- Check capture timing
- Increase monitoring interval

## Conclusion

This implementation provides a foundation for automated Claude Code control, enabling:
- Programmatic interaction with Claude Code
- Integration into workflows and pipelines
- Automated testing and validation
- Custom tooling and extensions

The actor-based design ensures thread-safe operation, while the workflow layer enables declarative automation.