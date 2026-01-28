# actor-WF-examples

Example applications demonstrating [POJO-actor](https://github.com/scivicslab/POJO-actor)'s workflow engine. These examples implement Turing machines based on algorithms from Charles Petzold's *The Annotated Turing* (Wiley, 2008).

This repository accompanies the **POJO-actor Tutorial Part 2** blog series:
- [Part 2-1: Workflow Language Basics](https://scivicslab.com/blog/2025-12-30-TutorialPart2-1)
- [Part 2-2: Creating Workflows](https://scivicslab.com/blog/2025-12-31-TutorialPart2-2)
- [Part 2-3: @Action Annotation](https://scivicslab.com/blog/2026-01-27-TutorialPart2-3)

[![Java Version](https://img.shields.io/badge/java-21+-blue.svg)](https://openjdk.java.net/)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)


## Prerequisites

- **JDK 21** or later
- **Maven 3.x**
- **POJO-actor** installed in local Maven repository (required for v2.14.0-SNAPSHOT)


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
# Part 2-1/2-2: callByActionName() version
mvn exec:java -Dexec.mainClass="com.scivicslab.turing.TuringWorkflowApp" -Dexec.args="turing83"
mvn exec:java -Dexec.mainClass="com.scivicslab.turing.TuringWorkflowApp" -Dexec.args="turing87"

# Part 2-3: @Action annotation version
mvn exec:java -Dexec.mainClass="com.scivicslab.turing.TuringWorkflowApp" -Dexec.args="turing87-array --action"
```


## Examples

### turing83 - Binary Representation of 1/3 (Part 2-1)

A Turing machine that computes the binary representation of the rational number 1/3. It writes symbols "0 1 0 1 0 1..." alternately on the tape while cycling through states 1→2→3→4→5→1.

**Output:**
```
TAPE    0    value
TAPE    0    value    0 1
TAPE    0    value    0 1 0 1
TAPE    0    value    0 1 0 1 0 1
...
```

### turing87 - Irrational Number (Part 2-1/2-2)

A more complex Turing machine that outputs an irrational number: 001011011101111011111...

This example demonstrates **conditional branching** using multiple workflow rows with the same from-state.

**Output:**
```
TAPE    0    value
TAPE    0    value    ee0 0 1 0
TAPE    0    value    ee0 0 1 0 1 1 0
TAPE    0    value    ee0 0 1 0 1 1 0 1 1 1 0
TAPE    0    value    ee0 0 1 0 1 1 0 1 1 1 0 1 1 1 1 0
...
```

### turing87-array - @Action Version (Part 2-3)

The same turing87 workflow, but using `TuringActionIIAR` which demonstrates the `@Action` annotation approach introduced in POJO-actor v2.14.

Run with the `--action` flag:
```bash
mvn exec:java -Dexec.mainClass="com.scivicslab.turing.TuringWorkflowApp" -Dexec.args="turing87-array --action"
```


## Project Structure

```
actor-WF-examples/
├── pom.xml
├── src/main/
│   ├── java/com/scivicslab/turing/
│   │   ├── TuringWorkflowApp.java    # Main application
│   │   ├── Turing.java               # Turing machine POJO
│   │   ├── TuringIIAR.java           # Part 2-2: callByActionName() version
│   │   ├── TuringActionIIAR.java     # Part 2-3: @Action annotation version
│   │   └── Tape.java                 # Tape implementation
│   └── resources/code/
│       ├── turing83.yaml             # Part 2-1: Workflow for 1/3
│       ├── turing87.yaml             # Part 2-1/2-2: Workflow for irrational number
│       └── turing87-array.yaml       # Part 2-3: Same workflow for @Action version
```


## Two Approaches to IIActorRef

### Part 2-2: Override `callByActionName()` (TuringIIAR)

```java
@Override
public ActionResult callByActionName(String actionName, String args) {
    switch (actionName) {
        case "initMachine":
            this.tell(t -> t.initMachine()).get();
            return new ActionResult(true, "initialized");
        case "put":
            String value = parseFirstArg(args);
            this.tell(t -> t.put(value)).get();
            return new ActionResult(true, "put " + value);
        // ... more cases
    }
}
```

### Part 2-3: Use `@Action` Annotation (TuringActionIIAR)

```java
@Action("initMachine")
public ActionResult initMachine(String args) {
    this.object.initMachine();
    return new ActionResult(true, "Machine initialized");
}

@Action("put")
public ActionResult put(String args) {
    String value = getFirst(args);
    this.object.put(value);
    return new ActionResult(true, "Put " + value);
}
// No need to override callByActionName()!
```

The `@Action` annotation approach is cleaner and easier to maintain.


## References

- [POJO-actor](https://github.com/scivicslab/POJO-actor): Lightweight actor model library with workflow engine
- [POJO-actor Documentation](https://scivicslab.com/docs/pojo-actor/introduction): Official documentation
- [POJO-actor Javadoc](https://scivicslab.github.io/POJO-actor/): API reference
- [Tutorial Part 2-1: Workflow Language Basics](https://scivicslab.com/blog/2025-12-30-TutorialPart2-1)
- [Tutorial Part 2-2: Creating Workflows](https://scivicslab.com/blog/2025-12-31-TutorialPart2-2)
- [Tutorial Part 2-3: @Action Annotation](https://scivicslab.com/blog/2026-01-27-TutorialPart2-3)


## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
