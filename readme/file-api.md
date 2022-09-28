# File API documentation

This is the documentation for the custom `File` API provided in `vLabeler`'s scripting environment.

Basically, the `File` class is a JavaScript wrapper around the `File` class in the `java.io` package, representing a
file or a directory on the file system.

### Static methods

`File.fromPath(path: string): File` : create a `File` object from a path string.

`File.fromPathAndChildPath(path: string, childPath: string): File` : create a `File` object from a path string and a
child path string.

### Member methods

`listChildren(): File[]` : list all children of this file, including files and directories (directory).

`listChildDirectories(): File[]` : list all child directories of this file (directory).

`listChildFiles(): File[]` : list all child files of this file (directory).

`exists(): boolean` : check if this file (directory) exists.

`isDirectory(): boolean` : check if this object represents a directory.

`isFile(): boolean` : check if this object represents a file.

`resolve(relativePath: string): File` : resolve a relative path string to a `File` object.

`getName(): string` : get the name of this file (directory).

`getNameWithoutExtension(): string` : get the name of this file without the extension.

`getExtension(): string` : get the extension of this file.

`getPath(): string` : get the path of this file (directory).

`getAbsolutePath(): string` : get the absolute path of this file (directory).

### Source file

If you know Java and Javascript well enough, you can also read the source file for the `File`
class [here](../src/jvmMain/resources/js/file.js)
