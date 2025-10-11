# Claude Code Control - Actor Structure

## Overview

This document explains the actor-based architecture for controlling Claude Code through tmux sessions.

## Actor System Architecture

### ClaudeCodeSimpleTest (Simplest Example)

```
ActorSystem ("claude-simple-system")
  └─ ActorRef<ClaudeCodeSession> ("session")
      └─ ClaudeCodeSession POJO
          └─ TmuxSession (terminal control)
```

**Actor Count: 1**

This is the minimal configuration with just a session actor.

### ClaudeCodeApp (Basic Example)

```
ActorSystem ("claude-code-system")
  ├─ ActorRef<ClaudeCodeSession> ("session")
  │   └─ ClaudeCodeSession POJO
  │       └─ TmuxSession
  │
  └─ ActorRef<ClaudeCodeMonitor> ("monitor")
      └─ ClaudeCodeMonitor POJO
          └─ References ClaudeCodeSession
```

**Actor Count: 2**

- **session actor**: Controls Claude Code (send prompts, send keys)
- **monitor actor**: Monitors output in background thread

### ClaudeCodeWorkflowApp (Workflow Mode)

```
IIActorSystem ("claude-code-workflow-system")
  ├─ ClaudeCodeSessionIIAR ("session")
  │   └─ ClaudeCodeSession POJO
  │       └─ TmuxSession
  │
  ├─ ClaudeCodeMonitorIIAR ("monitor")
  │   └─ ClaudeCodeMonitor POJO
  │
  └─ InterpreterIIAR ("@ip")
      └─ Interpreter POJO
          └─ Executes YAML workflow
```

**Actor Count: 3 IIActorRefs**

- **session actor**: Claude Code control (workflow-enabled)
- **monitor actor**: Output monitoring (workflow-enabled)
- **@ip actor**: Workflow interpreter (sleep, print, etc.)

## Component Layer Structure

### Layer 1: POJO (Plain Old Java Objects)

Core business logic without actor-specific code.

#### TmuxSession
- Creates/manages tmux sessions
- Sends commands to tmux
- Captures pane output
- Low-level terminal control

#### ClaudeCodeSession
- Wraps TmuxSession
- Provides Claude Code-specific operations
- Methods:
  - `createSession()` - Create tmux session
  - `startClaudeCode()` - Launch claude command
  - `sendPrompt(String)` - Send user message
  - `sendChoice(int)` - Select numbered option
  - `sendYes()` / `sendNo()` - Answer y/n questions
  - `sendCtrlC()` / `sendCtrlD()` - Send control keys
  - `captureAndParse()` - Get parsed output
  - `killSession()` - Terminate session

#### ClaudeCodeParser
- Parses terminal output
- Detects prompt types:
  - READY - Ready for input
  - YES_NO - y/n question
  - NUMBERED_CHOICE - Numbered options
  - TOOL_APPROVAL - Tool execution approval
  - PROCESSING - Claude thinking
  - RESPONSE - Text response
  - ERROR - Error message
  - UNKNOWN - Unrecognized state
- Extracts choices and questions

#### ClaudeCodeMonitor
- Background monitoring in virtual thread
- Polls output at configurable interval (default: 1-2 seconds)
- Detects output changes
- Supports callback on change
- Thread-safe with AtomicBoolean

#### ClaudeCodeOutput
- Data class for parsed output
- Contains:
  - List of output lines
  - PromptType
  - List of choices (if any)
  - Question text (if any)
  - Timestamp

### Layer 2: Actor (Concurrency Control)

Wraps POJOs in ActorRef for thread-safe concurrent access.

#### ActorRef<ClaudeCodeSession>
- Mailbox-based message processing
- Sequential execution of messages
- tell() - Fire and forget
- ask() - Request-reply pattern

#### ActorRef<ClaudeCodeMonitor>
- Controls monitoring lifecycle
- Accesses latest output
- Thread-safe state queries

**Benefits:**
- No manual locking needed
- Isolated failure handling
- Message ordering guarantees
- Safe shared state access

### Layer 3: Workflow Integration (IIActorRef)

Enables YAML-based declarative control.

#### ClaudeCodeSessionIIAR
- Implements `callByActionName(String, String)`
- Maps action strings to methods:
  - "createSession" → createSession()
  - "startClaudeCode" → startClaudeCode()
  - "sendPrompt" → sendPrompt(args)
  - "sendChoice" → sendChoice(Integer.parseInt(args))
  - "sendYes" → sendYes()
  - "sendNo" → sendNo()
  - "captureAndParse" → captureAndParse()
  - "printOutput" → captureOutput() + print
  - "killSession" → killSession()

#### ClaudeCodeMonitorIIAR
- Maps monitoring actions:
  - "startMonitoring" → startMonitoring()
  - "stopMonitoring" → stopMonitoring()
  - "printLatestOutput" → printLatestOutput()
  - "checkMonitoring" → isMonitoring()
  - "getPromptType" → getLatestOutput().getPromptType()

#### InterpreterIIAR
- Built-in workflow utilities:
  - "sleep" → Thread.sleep(millis)
  - "print" → System.out.println(text)
  - "doNothing" → no-op (comments)

## Data Flow

### Simple Test Flow (ClaudeCodeSimpleTest)

```
Main Thread
    ↓
ActorSystem.actorOf("session", ClaudeCodeSession)
    ↓
sessionRef.tell(s -> s.createSession())
    ↓
Session Actor Mailbox
    ↓
ClaudeCodeSession.createSession()
    ↓
TmuxSession.createSession()
    ↓
ProcessBuilder executes: tmux new-session -d -s <name>
    ↓
sessionRef.tell(s -> s.startClaudeCode())
    ↓
ClaudeCodeSession.startClaudeCode()
    ↓
TmuxSession.sendCommand("claude")
    ↓
ProcessBuilder executes: tmux send-keys -t <name> "claude" "Enter"
    ↓
sessionRef.tell(s -> s.sendPrompt("Hello"))
    ↓
ClaudeCodeSession.sendPrompt("Hello")
    ↓
TmuxSession.sendCommand("Hello")
    ↓
Thread.sleep(wait for response)
    ↓
sessionRef.ask(s -> s.captureOutput())
    ↓
TmuxSession.capturePane()
    ↓
ProcessBuilder executes: tmux capture-pane -t <name> -p
    ↓
Returns List<String> to caller
```

### Monitored Flow (ClaudeCodeApp)

```
Main Thread
    ↓
Create ActorSystem
    ↓
sessionRef = actorOf("session", ClaudeCodeSession)
monitorRef = actorOf("monitor", ClaudeCodeMonitor)
    ↓
sessionRef.tell(create & start)
    ↓
monitorRef.tell(m -> m.startMonitoring())
    ↓
Monitor Actor spawns Virtual Thread
    ↓
┌─── Monitor Loop (background) ───┐
│ while (monitoring) {             │
│   output = session.captureAndParse() │
│   if (changed) {                 │
│     latestOutput = output        │
│     callback?.accept(output)     │
│   }                              │
│   Thread.sleep(intervalMs)       │
│ }                                │
└──────────────────────────────────┘
    ↓
Main Thread can:
- sessionRef.tell(send prompts)
- monitorRef.ask(get latest output)
- monitorRef.tell(print output)
```

### Workflow Flow (ClaudeCodeWorkflowApp)

```
Main Thread
    ↓
Create IIActorSystem
    ↓
sessionActor = new ClaudeCodeSessionIIAR("session", session)
monitorActor = new ClaudeCodeMonitorIIAR("monitor", monitor)
interpreterActor = new InterpreterIIAR("@ip", interpreter, system)
    ↓
system.addIIActor(sessionActor)
system.addIIActor(monitorActor)
system.addIIActor(interpreterActor)
    ↓
interpreterActor.tell(i -> i.readYaml(yamlInput))
    ↓
Interpreter parses YAML workflow
    ↓
for each iteration:
  interpreterActor.tell(i -> i.execCode())
      ↓
  Interpreter gets current state
      ↓
  Finds matching workflow row: states: [current, next]
      ↓
  For each action: [actorName, actionName, args]
      ↓
  system.getIIActor(actorName)
      ↓
  actor.callByActionName(actionName, args)
      ↓
  Execute corresponding POJO method
      ↓
  Returns ActionResult(success, message)
      ↓
  Transition to next state
```

## State Management

### Session State

```
ClaudeCodeSession
    ├─ sessionName: String (immutable)
    ├─ claudeCodeStarted: boolean (volatile)
    └─ tmuxSession: TmuxSession
        ├─ sessionName: String
        └─ active: boolean
```

**Access Pattern:**
- All state modifications go through ActorRef mailbox
- Sequential processing ensures no race conditions
- No manual synchronization needed

### Monitor State

```
ClaudeCodeMonitor
    ├─ monitoring: AtomicBoolean (thread-safe)
    ├─ monitorThread: Thread
    ├─ latestOutput: ClaudeCodeOutput (volatile)
    ├─ intervalMs: int (immutable)
    └─ onOutputChange: Consumer<ClaudeCodeOutput>
```

**Concurrency:**
- `monitoring` uses AtomicBoolean for visibility
- `monitorThread` runs independently
- `latestOutput` accessed from both threads (read-only after set)
- ActorRef mailbox protects state queries

## Message Passing Patterns

### Tell Pattern (Fire-and-forget)

```java
sessionRef.tell(s -> s.sendPrompt("Hello")).get();
```

- Asynchronous message send
- Returns CompletableFuture
- `.get()` blocks until completion
- Used for commands without return values

### Ask Pattern (Request-reply)

```java
List<String> output = sessionRef.ask(s -> s.captureOutput()).get();
```

- Sends message and waits for reply
- Returns CompletableFuture<T>
- `.get()` blocks and returns result
- Used for queries with return values

### Workflow Pattern (String-based invocation)

```java
ActionResult result = actor.callByActionName("sendPrompt", "Hello");
```

- Dynamic method invocation by name
- String-based arguments
- Returns ActionResult(success, message)
- Enables YAML-based workflows

## Actor Lifecycle

### Session Actor

1. **Creation**: `system.actorOf("session", new ClaudeCodeSession(name))`
2. **Initialization**: Actor mailbox created, message loop started
3. **Operation**: Processes messages sequentially from mailbox
4. **Termination**: `system.terminate()` stops all actors

### Monitor Actor

1. **Creation**: `system.actorOf("monitor", new ClaudeCodeMonitor(...))`
2. **Initialization**: Actor mailbox created
3. **Start Monitoring**: `tell(m -> m.startMonitoring())` spawns background thread
4. **Operation**: Background thread polls, actor handles state queries
5. **Stop Monitoring**: `tell(m -> m.stopMonitoring())` stops background thread
6. **Termination**: `system.terminate()` stops actor

## Thread Model

### ActorSystem Threads

- Each actor has a virtual thread for message processing
- Messages processed sequentially per actor
- Different actors can run concurrently

### Monitor Background Thread

- Additional virtual thread for polling
- Runs independently from actor message loop
- Coordinates with actor through shared state

### Total Thread Count

- **ClaudeCodeSimpleTest**: 1 actor thread
- **ClaudeCodeApp**: 2 actor threads + 1 monitor thread = 3 threads
- **ClaudeCodeWorkflowApp**: 3 actor threads + 1 monitor thread = 4 threads

All use virtual threads (lightweight, Project Loom).

## Error Handling

### POJO Level

```java
public void sendPrompt(String prompt) throws IOException, InterruptedException {
    // Can throw checked exceptions
}
```

### Actor Level

```java
sessionRef.tell(s -> {
    try {
        s.sendPrompt(prompt);
    } catch (Exception e) {
        throw new RuntimeException(e);  // Wrap checked exceptions
    }
}).get();
```

### Workflow Level

```java
public ActionResult callByActionName(String actionName, String args) {
    try {
        // ... execute action
        return new ActionResult(true, "Success");
    } catch (Exception e) {
        return new ActionResult(false, "Error: " + e.getMessage());
    }
}
```

## Shared State Pattern

Both `sessionRef` and `monitorRef` reference the same `ClaudeCodeSession` instance:

```java
ClaudeCodeSession session = new ClaudeCodeSession(name);
ActorRef<ClaudeCodeSession> sessionRef = system.actorOf("session", session);
ActorRef<ClaudeCodeMonitor> monitorRef = system.actorOf("monitor",
    new ClaudeCodeMonitor(session, 1000));  // Same instance
```

**Safety:**
- Both actors access via mailboxes (sequential)
- No concurrent modifications
- Monitor only reads state
- Session modifies state

This is the **Monitor Pattern** - one actor writes, another reads.

## Summary

| Component | Type | Count | Purpose |
|-----------|------|-------|---------|
| ClaudeCodeSession | POJO | 1 | Claude Code control |
| ClaudeCodeMonitor | POJO | 0-1 | Background monitoring |
| TmuxSession | POJO | 1 | Terminal control |
| ClaudeCodeParser | POJO | 1 | Output parsing |
| ActorRef<ClaudeCodeSession> | Actor | 1 | Concurrent session control |
| ActorRef<ClaudeCodeMonitor> | Actor | 0-1 | Concurrent monitor control |
| ClaudeCodeSessionIIAR | IIActorRef | 0-1 | Workflow integration |
| ClaudeCodeMonitorIIAR | IIActorRef | 0-1 | Workflow integration |
| InterpreterIIAR | IIActorRef | 0-1 | Workflow execution |

**Total Actors:**
- Simple: 1 actor
- With monitoring: 2 actors
- Workflow mode: 3 actors