# CommandLine API documentation

This is the documentation for the custom `CommandLine` API provided in `vLabeler`'s scripting environment.

### Global functions

`executeCommand(...args: string[]): string` : execute a command in the command line, wait for it to finish, and return
the output as a string. The args will be automatically wrapped by `""`.

e.g.

```javascript
executeCommand("ls", "-l")
```

### Source file

See the [JavaScript source code](../src/jvmMain/resources/js/env.js)
and [Kotlin source code](../src/jvmMain/kotlin/com/sdercolin/vlabeler/util/CommandLine.kt) for more details.
