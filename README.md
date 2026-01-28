# actor-WF-examples

Example applications demonstrating [POJO-actor](https://github.com/scivicslab/POJO-actor)'s workflow engine. These examples implement Turing machines based on algorithms from Charles Petzold's *The Annotated Turing* (Wiley, 2008).

This repository accompanies the **POJO-actor Tutorial Part 2** blog series:
- [Part 2-1: Workflow Language Basics](https://scivicslab.com/blog/2025-12-30-TutorialPart2-1)
- [Part 2-2: Creating Workflows](https://scivicslab.com/blog/2025-12-31-TutorialPart2-2)

[![Java Version](https://img.shields.io/badge/java-21+-blue.svg)](https://openjdk.java.net/)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)


## Prerequisites

- **JDK 21** or later
- **Maven 3.x**
- **POJO-actor** installed in local Maven repository


## Quick Start

### Step 1: Install POJO-actor

Clone and install POJO-actor to your local Maven repository:

```bash
git clone https://github.com/scivicslab/POJO-actor
cd POJO-actor
mvn install
```

### Step 2: Build actor-WF-examples

```bash
git clone https://github.com/scivicslab/actor-WF-examples
cd actor-WF-examples
mvn compile
```

### Step 3: Run Examples

```bash
# turing83: outputs alternating 0 1 0 1 0 1...
mvn exec:java -Dexec.mainClass="com.scivicslab.turing.TuringWorkflowApp" -Dexec.args="turing83"

# turing87: outputs irrational number 001011011101111011111...
mvn exec:java -Dexec.mainClass="com.scivicslab.turing.TuringWorkflowApp" -Dexec.args="turing87"
```


## Examples

### turing83 - Binary Representation of 1/3

A Turing machine that computes the binary representation of the rational number 1/3. It writes symbols "0 1 0 1 0 1..." alternately on the tape while cycling through states 1→2→3→4→5→1.

**Output:**
```
Loading workflow from: /code/turing83.yaml
Workflow loaded successfully
Executing workflow...

TAPE    0    value
TAPE    0    value    0 1
TAPE    0    value    0 1 0 1
TAPE    0    value    0 1 0 1 0 1
TAPE    0    value    0 1 0 1 0 1 0 1
...

Workflow finished: Maximum iterations (200) exceeded
```

### turing87 - Irrational Number

A more complex Turing machine that outputs an irrational number: 001011011101111011111...

This example demonstrates **conditional branching** using multiple workflow rows with the same from-state.

**Output:**
```
Loading workflow from: /code/turing87.yaml
Workflow loaded successfully
Executing workflow...

TAPE    0    value
TAPE    0    value    ee0 0 1 0
TAPE    0    value    ee0 0 1 0 1 1 0
TAPE    0    value    ee0 0 1 0 1 1 0 1 1 1 0
TAPE    0    value    ee0 0 1 0 1 1 0 1 1 1 0 1 1 1 1 0

Workflow finished: Maximum iterations (200) exceeded
```


## Project Structure

```
actor-WF-examples/
├── pom.xml
├── src/main/
│   ├── java/com/scivicslab/turing/
│   │   ├── TuringWorkflowApp.java    # Main application
│   │   ├── Turing.java               # Turing machine POJO
│   │   ├── TuringIIAR.java           # Actor wrapper (IIActorRef)
│   │   └── Tape.java                 # Tape implementation
│   └── resources/code/
│       ├── turing83.yaml             # Workflow for 1/3
│       └── turing87.yaml             # Workflow for irrational number
```


## How Workflows Work

actor-WF workflows are defined in YAML with `states` (state transitions) and `actions` (method calls):

```yaml
name: turing83
steps:
- states: ["0", "1"]
  actions:
  - actor: turing
    method: initMachine
- states: ["1", "2"]
  actions:
  - actor: turing
    method: printTape
# ... more steps
```

Each step defines:
- `states: [from-state, to-state]` - State transition
- `actions` - List of actor method calls

The interpreter starts at state `"0"` and transitions through states until reaching `"end"` or hitting the maximum iteration limit.

For detailed explanation of the workflow syntax and semantics, see [Tutorial Part 2-1](https://scivicslab.com/blog/2025-12-30-TutorialPart2-1).


## Creating Your Own Workflows

To create custom workflows, you need to:

1. Create a POJO with the methods you want to call from workflows
2. Create an `IIActorRef` wrapper to expose those methods to the workflow engine
3. Write a YAML workflow file
4. Create an application to run the workflow

See [Tutorial Part 2-2](https://scivicslab.com/blog/2025-12-31-TutorialPart2-2) for step-by-step instructions.


## References

- [POJO-actor](https://github.com/scivicslab/POJO-actor): Lightweight actor model library with workflow engine
- [POJO-actor Documentation](https://scivicslab.com/docs/pojo-actor/introduction): Official documentation
- [POJO-actor Javadoc](https://scivicslab.github.io/POJO-actor/): API reference
- [Tutorial Part 2-1: Workflow Language Basics](https://scivicslab.com/blog/2025-12-30-TutorialPart2-1)
- [Tutorial Part 2-2: Creating Workflows](https://scivicslab.com/blog/2025-12-31-TutorialPart2-2)


## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
