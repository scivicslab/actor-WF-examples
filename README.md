# actor-WF-examples

Example applications demonstrating [POJO-actor](https://github.com/scivicslab/POJO-actor)'s workflow engine. These examples implement Turing machines based on algorithms from Charles Petzold's *The Annotated Turing* (Wiley, 2008).

[![Java Version](https://img.shields.io/badge/java-21+-blue.svg)](https://openjdk.java.net/)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)


## Building

```bash
git clone https://github.com/scivicslab/actor-WF-examples
cd actor-WF-examples
mvn compile
```


## Running

```bash
# Turing87: outputs irrational number 001011011101111011111...
mvn exec:java -Dexec.mainClass="com.scivicslab.turing.TuringWorkflowApp" -Dexec.args="turing87"

# Turing83: outputs alternating 0 1 0 1 0 1...
mvn exec:java -Dexec.mainClass="com.scivicslab.turing.TuringWorkflowApp" -Dexec.args="turing83"
```

**Output (turing87):**

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


## References

- [POJO-actor](https://github.com/scivicslab/POJO-actor): Lightweight actor model library with workflow engine
- [POJO-actor Documentation](https://scivicslab.com/docs/pojo-actor/introduction): Official documentation
- [POJO-actor Javadoc](https://scivicslab.github.io/POJO-actor/): API reference


## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
