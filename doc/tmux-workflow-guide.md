# Tmux Workflow Guide

## Overview

This guide explains how to use the YAML workflow approach to control tmux sessions with actor-WF.

## Workflow Execution Model

### State Machine Concept

A workflow is a finite state machine where:
- Each row in the YAML matrix defines a state transition
- `states: [current, next]` specifies the transition
- `actions` are executed during the transition
- The interpreter executes one row per cycle

### Workflow Structure

```yaml
name: workflow-name
matrix:
  - states: [0, 1]        # Transition from state 0 to state 1
    actions:              # Actions to execute
      - [actor, action, argument]
  - states: [1, 2]        # Transition from state 1 to state 2
    actions:
      - [actor, action, argument]
  - states: [2, -1]       # Transition to terminal state -1
    actions:
      - [actor, action, argument]
```

**Terminal States:**
- `-1`: Workflow completion
- `any`: Match any current state (catch-all)

## Available Actors and Actions

### session Actor (TmuxSessionIIAR)

Controls tmux session operations.

| Action | Argument | Description |
|--------|----------|-------------|
| `createSession` | `""` | Create a new tmux session |
| `sendCommand` | `"command"` | Send command with Enter key |
| `sendKeys` | `"keys"` | Send raw keys without Enter |
| `capturePane` | `""` | Capture pane output (returns line count) |
| `printPane` | `""` | Print pane output to console |
| `checkPrompt` | `""` | Check if prompt is present |
| `killSession` | `""` | Terminate the tmux session |

### monitor Actor (TmuxMonitorIIAR)

Controls output monitoring.

| Action | Argument | Description |
|--------|----------|-------------|
| `startMonitoring` | `""` | Start background output monitoring |
| `stopMonitoring` | `""` | Stop monitoring |
| `printLatestOutput` | `""` | Print latest captured output |
| `checkMonitoring` | `""` | Check if monitoring is active |

### @ip Actor (InterpreterIIAR)

Built-in interpreter actions.

| Action | Argument | Description |
|--------|----------|-------------|
| `sleep` | `"milliseconds"` | Sleep for specified duration |
| `print` | `"text"` | Print text to console |
| `doNothing` | `"comment"` | No operation (for comments) |

## Example Workflows

### 1. Basic Command Execution (tmux-demo.yaml)

```yaml
---
name: tmux-demo
matrix:
  # State 0->1: Create tmux session
  - states: [0, 1]
    actions:
      - [session, createSession, ""]

  # State 1->2: Start monitoring
  - states: [1, 2]
    actions:
      - [monitor, startMonitoring, ""]

  # State 2->3: Wait for initialization
  - states: [2, 3]
    actions:
      - [@ip, sleep, "2000"]

  # State 3->4: Execute ls command
  - states: [3, 4]
    actions:
      - [session, sendCommand, "ls -la"]

  # State 4->5: Wait for command completion
  - states: [4, 5]
    actions:
      - [@ip, sleep, "2000"]

  # State 5->6: Print output
  - states: [5, 6]
    actions:
      - [session, printPane, ""]

  # State 6->7: Execute echo command
  - states: [6, 7]
    actions:
      - [session, sendCommand, "echo 'Hello from workflow'"]

  # State 7->8: Wait for command completion
  - states: [7, 8]
    actions:
      - [@ip, sleep, "2000"]

  # State 8->9: Print output
  - states: [8, 9]
    actions:
      - [session, printPane, ""]

  # State 9->10: Execute pwd command
  - states: [9, 10]
    actions:
      - [session, sendCommand, "pwd"]

  # State 10->11: Wait for command completion
  - states: [10, 11]
    actions:
      - [@ip, sleep, "2000"]

  # State 11->12: Print output
  - states: [11, 12]
    actions:
      - [session, printPane, ""]

  # State 12->13: Check for prompt
  - states: [12, 13]
    actions:
      - [session, checkPrompt, ""]

  # State 13->14: Stop monitoring
  - states: [13, 14]
    actions:
      - [monitor, stopMonitoring, ""]

  # State 14->-1: Cleanup and terminate
  - states: [14, -1]
    actions:
      - [session, killSession, ""]
```

### 2. Interactive Command Sequence (tmux-interactive.yaml)

```yaml
---
name: tmux-interactive
matrix:
  # Initialize
  - states: [0, 1]
    actions:
      - [session, createSession, ""]
      - [monitor, startMonitoring, ""]

  # Create directory
  - states: [1, 2]
    actions:
      - [session, sendCommand, "mkdir -p /tmp/test-dir"]
      - [@ip, sleep, "1000"]

  # Change to directory
  - states: [2, 3]
    actions:
      - [session, sendCommand, "cd /tmp/test-dir"]
      - [@ip, sleep, "1000"]

  # Create file
  - states: [3, 4]
    actions:
      - [session, sendCommand, "echo 'Test content' > test.txt"]
      - [@ip, sleep, "1000"]

  # Verify file
  - states: [4, 5]
    actions:
      - [session, sendCommand, "cat test.txt"]
      - [@ip, sleep, "1000"]

  # Print result
  - states: [5, 6]
    actions:
      - [session, printPane, ""]

  # Cleanup
  - states: [6, 7]
    actions:
      - [session, sendCommand, "cd ~ && rm -rf /tmp/test-dir"]
      - [@ip, sleep, "1000"]

  # Terminate
  - states: [7, -1]
    actions:
      - [monitor, stopMonitoring, ""]
      - [session, killSession, ""]
```

### 3. Multi-Command with Monitoring (tmux-monitor.yaml)

```yaml
---
name: tmux-monitor
matrix:
  # Initialize
  - states: [0, 1]
    actions:
      - [session, createSession, ""]
      - [monitor, startMonitoring, ""]
      - [@ip, sleep, "2000"]

  # Command 1
  - states: [1, 2]
    actions:
      - [session, sendCommand, "date"]
      - [@ip, sleep, "1000"]
      - [monitor, printLatestOutput, ""]

  # Command 2
  - states: [2, 3]
    actions:
      - [session, sendCommand, "whoami"]
      - [@ip, sleep, "1000"]
      - [monitor, printLatestOutput, ""]

  # Command 3
  - states: [3, 4]
    actions:
      - [session, sendCommand, "hostname"]
      - [@ip, sleep, "1000"]
      - [monitor, printLatestOutput, ""]

  # Command 4
  - states: [4, 5]
    actions:
      - [session, sendCommand, "uname -a"]
      - [@ip, sleep, "1000"]
      - [monitor, printLatestOutput, ""]

  # Terminate
  - states: [5, -1]
    actions:
      - [monitor, stopMonitoring, ""]
      - [session, killSession, ""]
```

## Java Application Structure

### Main Application (TmuxWorkflowApp.java)

The application follows this structure:

```java
public class TmuxWorkflowApp {
    public static void main(String[] args) {
        // 1. Parse command line argument (workflow name)
        String workflowName = args[0];
        String yamlPath = "/code/" + workflowName + ".yaml";

        // 2. Create IIActorSystem
        IIActorSystem system = new IIActorSystem("tmux-workflow-system");

        // 3. Create and register actors
        TmuxSession session = new TmuxSession(sessionName);
        TmuxSessionIIAR sessionActor = new TmuxSessionIIAR("session", session);
        system.addIIActor(sessionActor);

        TmuxMonitor monitor = new TmuxMonitor(session, 1000);
        TmuxMonitorIIAR monitorActor = new TmuxMonitorIIAR("monitor", monitor);
        system.addIIActor(monitorActor);

        // 4. Create and register interpreter
        Interpreter interpreter = new Interpreter.Builder()
            .loggerName("TmuxWorkflow")
            .team(system)
            .build();
        InterpreterIIAR interpreterActor = new InterpreterIIAR("@ip", interpreter, system);
        system.addIIActor(interpreterActor);

        // 5. Load YAML workflow
        interpreterActor.tell(i -> i.readYaml(yamlInput)).get();

        // 6. Execute workflow
        for (int i = 0; i < 50; i++) {
            interpreterActor.tell(interp -> interp.execCode()).get();
        }

        // 7. Cleanup
        system.terminate();
    }
}
```

### Key Components

#### 1. IIActorSystem

Extended actor system that manages both regular actors and IIActorRefs.

```java
IIActorSystem system = new IIActorSystem("system-name");
```

#### 2. IIActorRef Registration

Actors must be registered with the system:

```java
TmuxSessionIIAR sessionActor = new TmuxSessionIIAR("session", session);
system.addIIActor(sessionActor);
```

The first argument (`"session"`) is the actor name used in YAML.

#### 3. Interpreter Setup

The interpreter needs reference to the actor system:

```java
Interpreter interpreter = new Interpreter.Builder()
    .loggerName("LoggerName")
    .team(system)  // Important: links interpreter to actor system
    .build();
```

#### 4. Workflow Loading

Load YAML from resources:

```java
InputStream yamlInput = getClass().getResourceAsStream("/code/workflow.yaml");
interpreterActor.tell(i -> i.readYaml(yamlInput)).get();
```

#### 5. Workflow Execution Loop

Execute workflow state by state:

```java
for (int i = 0; i < maxIterations; i++) {
    interpreterActor.tell(interp -> interp.execCode()).get();
}
```

Each iteration executes one state transition.

## Running Workflows

### Command Line Execution

```bash
# Build the project
mvn clean package -DskipTests

# Run tmux-demo workflow
java -cp target/actor-WF-examples-1.0.0.jar \
    com.scivicslab.terminal.TmuxWorkflowApp tmux-demo

# Run tmux-interactive workflow
java -cp target/actor-WF-examples-1.0.0.jar \
    com.scivicslab.terminal.TmuxWorkflowApp tmux-interactive

# Run tmux-monitor workflow
java -cp target/actor-WF-examples-1.0.0.jar \
    com.scivicslab.terminal.TmuxWorkflowApp tmux-monitor
```

## Workflow Execution Flow

```
Start
  ↓
Load YAML workflow
  ↓
Parse workflow matrix
  ↓
Initialize state = 0
  ↓
┌─────────────────────┐
│ Execution Loop      │
│                     │
│ Find row matching   │
│ current state       │
│         ↓           │
│ Execute all actions │
│ in sequence         │
│         ↓           │
│ Transition to       │
│ next state          │
│         ↓           │
│ Current = Next      │
│         ↓           │
│ If state == -1      │
│   Exit loop         │
│ Else                │
│   Continue loop     │
└─────────────────────┘
  ↓
Cleanup
  ↓
End
```

## Workflow Design Guidelines

### 1. State Numbering

Use sequential state numbers for clarity:
```yaml
- states: [0, 1]
- states: [1, 2]
- states: [2, 3]
```

### 2. Wait After Commands

Always add sleep after sending commands:
```yaml
- states: [1, 2]
  actions:
    - [session, sendCommand, "ls"]
    - [@ip, sleep, "1000"]  # Wait for command execution
```

### 3. Action Sequences

Multiple actions in one state execute in order:
```yaml
- states: [1, 2]
  actions:
    - [session, sendCommand, "pwd"]    # Execute first
    - [@ip, sleep, "1000"]             # Then wait
    - [session, printPane, ""]         # Then print
```

### 4. Cleanup Pattern

Always cleanup resources at the end:
```yaml
- states: [N, -1]
  actions:
    - [monitor, stopMonitoring, ""]
    - [session, killSession, ""]
```

### 5. Error Handling

Use state transitions for error paths:
```yaml
# Normal path
- states: [1, 2]
  actions:
    - [session, sendCommand, "test -f file.txt"]

# Check result (simplified - actual error handling needs condition support)
- states: [2, 3]
  actions:
    - [session, printPane, ""]
```

## Advantages of Workflow Approach

### 1. Declarative

Workflow logic is separated from implementation:
- Easy to read and understand
- Non-programmers can modify
- Version control friendly

### 2. Reusable

Same POJOs and IIActorRefs work across workflows:
- No code changes needed
- Just create new YAML files

### 3. Testable

Workflows can be tested independently:
- No recompilation needed
- Quick iteration

### 4. Maintainable

Changes are localized:
- Modify YAML for workflow changes
- Modify Java only for new actions

## Comparison: Traditional vs Workflow

### Traditional Actor-Based

**Pros:**
- Type-safe
- IDE support
- Complex logic support
- Conditional branches

**Cons:**
- Requires recompilation
- More verbose
- Harder for non-programmers

**Use When:**
- Complex business logic needed
- Dynamic control flow required
- Strong typing important

### YAML Workflow-Based

**Pros:**
- Declarative
- No recompilation
- Easy to modify
- Clear sequence visualization

**Cons:**
- Limited conditionals
- Runtime errors only
- Less IDE support

**Use When:**
- Sequential operations
- Configuration-driven execution
- Non-programmer maintenance
- Rapid prototyping

## Troubleshooting

### Workflow Not Loading

Check:
- YAML file in `src/main/resources/code/`
- File name matches argument
- YAML syntax is valid

### Actions Not Executing

Check:
- Actor name in YAML matches registration name
- Action name is supported by IIActorRef
- Arguments are correct format

### Session Creation Fails

Check:
- tmux is installed (`which tmux`)
- Session name is unique
- No permission issues

### Output Not Captured

Check:
- Sufficient sleep time after commands
- Monitoring is started
- Commands produce output

## Next Steps

1. Try running existing workflows
2. Modify YAML files to experiment
3. Create custom workflows
4. Add new actions to IIActorRef classes
5. Extend with new actor types