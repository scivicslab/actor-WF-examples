# Tmux Terminal Control Architecture

## Overview

This document describes the architecture of the tmux terminal control implementation using POJO-actor and actor-WF frameworks.

## Architecture Layers

### 1. POJO Layer (Plain Old Java Objects)

POJOs are simple Java classes that contain the core business logic without any actor-specific code.

#### TmuxSession

The core class for tmux session control.

**Responsibilities:**
- Create and manage tmux sessions
- Send commands to tmux sessions
- Capture output from tmux panes
- Detect prompts in the output
- Kill tmux sessions

**Key Methods:**
```java
void createSession()                    // Create a new tmux session
void sendCommand(String command)        // Send a command with Enter
void sendKeys(String keys)              // Send raw keys without Enter
List<String> capturePane()              // Capture current pane contents
String getLastLine()                    // Get the last non-empty line
boolean hasPrompt()                     // Check if prompt is present
void killSession()                      // Terminate the session
```

**Implementation Details:**
- Uses `ProcessBuilder` to execute tmux commands
- Session name is specified at construction time
- Each operation waits for tmux command completion

#### TmuxMonitor

Background monitoring of tmux output.

**Responsibilities:**
- Periodically capture tmux pane output
- Run monitoring in a background thread
- Store latest output for retrieval
- Control monitoring lifecycle

**Key Methods:**
```java
void startMonitoring()                  // Start background monitoring
void stopMonitoring()                   // Stop monitoring
TmuxOutput getLatestOutput()            // Get latest captured output
boolean isMonitoring()                  // Check monitoring status
void printLatestOutput()                // Print latest output to console
```

**Implementation Details:**
- Uses virtual threads (`Thread.ofVirtual()`)
- Monitoring interval is configurable (default: 1000ms)
- Holds reference to `TmuxSession` instance
- Thread-safe using `AtomicBoolean`

#### TmuxOutput

Data class holding captured output.

**Fields:**
- `List<String> lines` - Captured output lines
- `boolean hasPrompt` - Whether prompt is detected
- `long timestamp` - Capture timestamp

### 2. Actor Layer

Actors wrap POJOs to enable concurrent, message-passing based communication.

#### Actor System Structure

```
ActorSystem ("tmux-system")
  ├─ ActorRef<TmuxSession> ("session")
  │   └─ TmuxSession POJO
  │       - Executes tmux commands
  │       - Returns results synchronously
  │
  └─ ActorRef<TmuxMonitor> ("monitor")
      └─ TmuxMonitor POJO
          - Monitors output in background
          - References same TmuxSession instance
```

**Actor Count:** 2 actors

**Communication Pattern:**
```java
// Tell pattern (fire-and-forget)
sessionRef.tell(s -> s.sendCommand("ls -la")).get();

// Ask pattern (request-reply)
boolean hasPrompt = sessionRef.ask(s -> s.hasPrompt()).get();
```

**Concurrency Benefits:**
- Multiple threads can safely send messages to actors
- Actor mailbox serializes message processing
- No explicit locking needed in POJO code
- Isolated failure handling per actor

### 3. Workflow Integration Layer (IIActorRef)

IIActorRef wrappers enable YAML workflow integration by providing string-based action invocation.

#### TmuxSessionIIAR

Wraps `TmuxSession` for workflow interpreter.

**Supported Actions:**
- `createSession` - Create tmux session
- `sendCommand` - Send command with Enter
- `sendKeys` - Send raw keys
- `capturePane` - Capture output
- `printPane` - Print output to console
- `checkPrompt` - Check for prompt
- `killSession` - Terminate session

**Example Workflow Usage:**
```yaml
- states: [0, 1]
  actions:
    - [session, createSession, ""]
- states: [1, 2]
  actions:
    - [session, sendCommand, "ls -la"]
```

#### TmuxMonitorIIAR

Wraps `TmuxMonitor` for workflow interpreter.

**Supported Actions:**
- `startMonitoring` - Start background monitoring
- `stopMonitoring` - Stop monitoring
- `printLatestOutput` - Print latest output
- `checkMonitoring` - Check monitoring status

### 4. Workflow System Structure (IIActorSystem)

When using YAML workflows, the actor system extends to include the interpreter:

```
IIActorSystem ("tmux-workflow-system")
  ├─ TmuxSessionIIAR ("session")
  │   └─ TmuxSession POJO
  │
  ├─ TmuxMonitorIIAR ("monitor")
  │   └─ TmuxMonitor POJO
  │
  └─ InterpreterIIAR ("@ip")
      └─ Interpreter POJO
          - Reads YAML workflow definition
          - Executes workflow state transitions
          - Invokes actions via IIActorRef
```

**Actor Count:** 3 IIActorRefs (workflow mode)

## Execution Flow

### Traditional Actor-Based Approach

```
TmuxApp.main()
    ↓
Create ActorSystem
    ↓
Create TmuxSession POJO
    ↓
Wrap in ActorRef<TmuxSession>
    ↓
Create TmuxMonitor POJO
    ↓
Wrap in ActorRef<TmuxMonitor>
    ↓
Send messages via tell/ask
    ↓
Actor processes messages sequentially
    ↓
POJO methods execute
    ↓
Results returned to caller
    ↓
Terminate ActorSystem
```

### YAML Workflow Approach

```
TmuxWorkflowApp.main()
    ↓
Create IIActorSystem
    ↓
Create POJOs and wrap in IIActorRef
    ↓
Register IIActorRefs with system
    ↓
Create Interpreter
    ↓
Load YAML workflow
    ↓
Execute workflow loop
    ↓
Interpreter calls callByActionName()
    ↓
IIActorRef translates to POJO method
    ↓
Results returned as ActionResult
    ↓
Workflow transitions to next state
    ↓
Terminate IIActorSystem
```

## Key Design Principles

### 1. Separation of Concerns

- **POJO**: Business logic only
- **ActorRef**: Concurrency control
- **IIActorRef**: Workflow integration
- **Interpreter**: Workflow execution

### 2. Shared State Management

`TmuxMonitor` and the main application both reference the same `TmuxSession` instance:

```java
TmuxSession session = new TmuxSession(sessionName);
ActorRef<TmuxSession> sessionRef = system.actorOf("session", session);
ActorRef<TmuxMonitor> monitorRef = system.actorOf("monitor",
    new TmuxMonitor(session, 1000));  // Same instance
```

This is safe because:
- All access goes through actor mailboxes
- Messages are processed sequentially
- No concurrent access to POJO internals

### 3. Monitoring Pattern

The monitoring pattern separates:
- **Control operations** (`sessionRef`): Sending commands
- **Observation operations** (`monitorRef`): Reading output

This allows:
- Independent lifecycle management
- Flexible monitoring intervals
- Non-blocking output observation

## Example Usage

### Traditional Actor-Based

```java
ActorSystem system = new ActorSystem.Builder("tmux-system").build();

TmuxSession session = new TmuxSession("my-session");
ActorRef<TmuxSession> sessionRef = system.actorOf("session", session);
ActorRef<TmuxMonitor> monitorRef = system.actorOf("monitor",
    new TmuxMonitor(session, 1000));

// Create session
sessionRef.tell(s -> s.createSession()).get();

// Start monitoring
monitorRef.tell(m -> m.startMonitoring()).get();

// Send command
sessionRef.tell(s -> s.sendCommand("ls -la")).get();

// Wait and print output
Thread.sleep(2000);
monitorRef.tell(m -> m.printLatestOutput()).get();

// Cleanup
monitorRef.tell(m -> m.stopMonitoring()).get();
sessionRef.tell(s -> s.killSession()).get();
system.terminate();
```

### YAML Workflow-Based

**YAML Definition (tmux-demo.yaml):**
```yaml
name: tmux-demo
matrix:
  - states: [0, 1]
    actions:
      - [session, createSession, ""]
  - states: [1, 2]
    actions:
      - [monitor, startMonitoring, ""]
  - states: [2, 3]
    actions:
      - [session, sendCommand, "ls -la"]
  - states: [3, 4]
    actions:
      - [@ip, sleep, "2000"]
  - states: [4, 5]
    actions:
      - [session, printPane, ""]
```

**Java Code:**
```java
IIActorSystem system = new IIActorSystem("tmux-workflow-system");

TmuxSession session = new TmuxSession("workflow-session");
TmuxSessionIIAR sessionActor = new TmuxSessionIIAR("session", session);
system.addIIActor(sessionActor);

TmuxMonitor monitor = new TmuxMonitor(session, 1000);
TmuxMonitorIIAR monitorActor = new TmuxMonitorIIAR("monitor", monitor);
system.addIIActor(monitorActor);

Interpreter interpreter = new Interpreter.Builder()
    .loggerName("TmuxWorkflow")
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

## Benefits of This Architecture

### Code Reusability

The same POJOs (`TmuxSession`, `TmuxMonitor`) are used in both:
- Traditional actor-based applications
- YAML workflow-based applications

### Testability

POJOs can be tested independently:
```java
TmuxSession session = new TmuxSession("test-session");
session.createSession();
session.sendCommand("echo test");
List<String> output = session.capturePane();
// Assert on output
session.killSession();
```

### Flexibility

Choose the appropriate approach:
- **Traditional actors**: Complex logic, programmatic control
- **YAML workflows**: Declarative sequences, easy modification

### Safety

Actor model provides:
- Thread-safe concurrent access
- Isolated failure handling
- Message ordering guarantees
- No shared mutable state concerns