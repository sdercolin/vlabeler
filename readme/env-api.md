# Env API documentation

This is the documentation for the custom `Env` API provided in `vLabeler`'s scripting environment.

### Static methods

`Env.getSystemProperty(name: string): string` : get the value of a JVM system property.

`Env.getOsName(): string` : get the name of the operating system (JVM property `os.name`).

`Env.getOsVersion(): string` : get the version of the operating system (JVM property `os.version`).

`Env.getOsArch(): string` : get the architecture of the operating system (JVM property `os.arch`).

`Env.isWindows(): boolean` : check if the operating system is Windows.

`Env.isMac(): boolean` : check if the operating system is macOS.

`Env.isLinux(): boolean` : check if the operating system is Linux.

### Source file

See the source file of the `Env` class [here](../src/jvmMain/resources/js/env.js)
