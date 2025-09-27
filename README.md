# actor-WF-examples

Example applications demonstrating [actor-WF](https://github.com/scivicslab/actor-WF), a data-driven workflow interpreter built on [POJO-actor](https://github.com/scivicslab/POJO-actor). These examples show both traditional actor-based programming and YAML-based workflow execution.

[![Java Version](https://img.shields.io/badge/java-21+-blue.svg)](https://openjdk.java.net/)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

## Overview

This repository contains Turing machine implementations that demonstrate:

- **Actor-based programming**: Direct actor invocation using POJO-actor's `ActorRef`
- **YAML-based workflows**: Declarative workflow definitions executed by the actor-WF interpreter
- **IIActorRef pattern**: Wrapping actors for dynamic method invocation by name

## Examples

### Turing Machine Implementations

The examples implement Turing machines that write patterns to an infinite tape. Two algorithms are provided:

1. **Turing123**: Writes alternating `0 1` pattern (`0 1 0 1 0 1...`)
2. **Turing134**: Writes a more complex pattern with state-based logic

Each algorithm is available in two forms:

- **Traditional actor-based** (`Turing123App`, `Turing134App`): Java code directly invoking actor methods
- **YAML workflow-based** (`TuringWorkflowApp`): Declarative workflow definitions in YAML

### Core Components

- **`Turing`**: The Turing machine implementation with tape operations
  - `initMachine()`: Initialize the machine state
  - `put(String)`: Write a value to the current tape position
  - `move(String)`: Move the tape head left ("L") or right ("R")
  - `printTape()`: Display the current tape contents
  - `getCurrentValue()`: Read the current tape position
  - `matchCurrentValue(String)`: Test the current value

- **`TuringIIAR`**: Interpreter-interfaced actor reference for Turing
  - Wraps `Turing` for dynamic action invocation
  - Implements `callByActionName()` to bridge YAML workflows to Java methods

- **`TuringWorkflowApp`**: Main application for executing YAML workflows
  - Loads workflow definitions from resources
  - Creates and registers actors with the interpreter
  - Executes workflow state transitions

## Quick Start

### Prerequisites

- Java 21 or higher
- Maven 3.6+

### Building

```bash
mvn clean package -DskipTests
```

This creates a JAR with all dependencies: `target/actor-WF-examples-1.0.0.jar`

### Running Examples

#### Traditional Actor-Based Examples

```bash
# Run Turing123 algorithm
java -cp target/actor-WF-examples-1.0.0.jar com.scivicslab.turing.Turing123App

# Run Turing134 algorithm
java -cp target/actor-WF-examples-1.0.0.jar com.scivicslab.turing.Turing134App
```

#### YAML Workflow Examples

```bash
# Run turing123 workflow
java -cp target/actor-WF-examples-1.0.0.jar com.scivicslab.turing.TuringWorkflowApp turing123

# Run turing134 workflow
java -cp target/actor-WF-examples-1.0.0.jar com.scivicslab.turing.TuringWorkflowApp turing134
```

### Example Output

```
Loading workflow from: /code/turing123.yaml
Workflow loaded successfully
Executing workflow...

TAPE	0	value
TAPE	0	value	0 1
TAPE	0	value	0 1 0 1
TAPE	0	value	0 1 0 1 0 1
TAPE	0	value	0 1 0 1 0 1 0 1
...
```

## Workflow Structure

Workflows are defined in YAML under `src/main/resources/code/`:

### turing123.yaml

```yaml
name: turing123
matrix:
  - states: [0, 1]
    actions:
      - [turing, initMachine, ""]
  - states: [1, 2]
    actions:
      - [turing, printTape, ""]
  - states: [2, 3]
    actions:
      - [turing, put, "0"]
      - [turing, move, "R"]
  - states: [3, 4]
    actions:
      - [turing, move, "R"]
  - states: [4, 5]
    actions:
      - [turing, put, "1"]
      - [turing, move, "R"]
  - states: [5, 1]
    actions:
      - [turing, move, "R"]
```

Each workflow row defines:
- **states**: `[currentState, nextState]` - State transition
- **actions**: `[actorName, actionName, argument]` - Actions to execute

## Understanding the IIActorRef Pattern

The `TuringIIAR` class demonstrates how to wrap a POJO for workflow execution:

```java
public class TuringIIAR extends IIActorRef<Turing> {
    public TuringIIAR(String actorName, Turing turing, IIActorSystem system) {
        super(actorName, turing, system);
    }

    @Override
    public ActionResult callByActionName(String actionName, String args) {
        switch (actionName) {
            case "initMachine":
                this.tell(t -> t.initMachine()).get();
                return new ActionResult(true, "Machine initialized");

            case "put":
                this.tell(t -> t.put(args)).get();
                return new ActionResult(true, "Put " + args);

            case "move":
                this.tell(t -> t.move(args)).get();
                return new ActionResult(true, "Moved " + args);

            // ... more cases
        }
    }
}
```

This pattern:
1. Extends `IIActorRef<T>` where `T` is your actor class
2. Implements `callByActionName()` to map action strings to method calls
3. Uses `tell()` or `ask()` for actor method invocation
4. Returns `ActionResult` to indicate success/failure

## Architecture

```
TuringWorkflowApp
    ├── IIActorSystem (manages actors)
    │   ├── TuringIIAR (wraps Turing machine)
    │   └── InterpreterIIAR (wraps Interpreter)
    │
    └── Workflow (YAML definition)
        └── Matrix of [states] → [actions]
```

The workflow interpreter:
1. Loads YAML workflow definition
2. Starts at state 0
3. For each row matching current state:
   - Executes all actions in sequence
   - Transitions to next state
4. Continues until reaching terminal state

## Comparison: Traditional vs. Workflow Approach

### Traditional Actor-Based (Turing123App.java)

**Pros:**
- Type-safe method invocation
- IDE support (autocomplete, refactoring)
- Compile-time error checking

**Cons:**
- Workflow logic embedded in code
- Changes require recompilation
- Less suitable for runtime configuration

### YAML Workflow-Based (TuringWorkflowApp + turing123.yaml)

**Pros:**
- Workflow logic separated from implementation
- No recompilation for workflow changes
- Easy to version and compare workflows
- Suitable for runtime configuration
- Non-programmers can modify workflows

**Cons:**
- No compile-time workflow validation
- Requires IIActorRef wrapper implementation
- Runtime overhead for dynamic invocation

## Creating Your Own Workflow Actors

1. **Define your actor class**:
```java
public class MyActor {
    public void doSomething(String arg) {
        // Implementation
    }
}
```

2. **Create IIActorRef wrapper**:
```java
public class MyActorIIAR extends IIActorRef<MyActor> {
    public MyActorIIAR(String name, MyActor actor, IIActorSystem system) {
        super(name, actor, system);
    }

    @Override
    public ActionResult callByActionName(String actionName, String args) {
        switch (actionName) {
            case "doSomething":
                this.tell(a -> a.doSomething(args)).get();
                return new ActionResult(true, "Done");
            default:
                return new ActionResult(false, "Unknown: " + actionName);
        }
    }
}
```

3. **Register in workflow app**:
```java
IIActorSystem system = new IIActorSystem("mySystem");
MyActorIIAR myActor = new MyActorIIAR("myactor", new MyActor(), system);
system.addIIActor(myActor);
```

4. **Define YAML workflow**:
```yaml
name: myWorkflow
matrix:
  - states: [0, 1]
    actions:
      - [myactor, doSomething, "hello"]
```

## Dependencies

- **POJO-actor 1.0.0**: Lightweight actor model framework
- **actor-WF 1.0.0**: Workflow interpreter
- **SnakeYAML 1.28**: YAML parsing

## Requirements

- Java 21 or higher (for virtual threads)
- Maven 3.6+

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

These examples demonstrate the actor-WF workflow system, which builds upon:
- [POJO-actor](https://github.com/scivicslab/POJO-actor): Lightweight actor model framework
- [actor-WF](https://github.com/scivicslab/actor-WF): Data-driven workflow interpreter