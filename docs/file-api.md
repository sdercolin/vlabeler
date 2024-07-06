# File API documentation

This is the documentation for the custom `File` API provided in `vLabeler`'s scripting environment.

Basically, the `File` class is a JavaScript wrapper around the `File` class in the `java.io` package, representing a
file or a directory on the file system.

### Static methods

`File.fromPath(path: string): File` : create a `File` object from a path string.

`File.fromPathAndChildPath(path: string, childPath: string): File` : create a `File` object from a path string and a
child path string.

### Member methods

`getParentFile(): File | null` : get the parent directory of this file (directory) or `null` if this file is the root.

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

`readText(encoding = 'UTF-8'): string` : read the content of this file as a string using the specified encoding.

`readLines(encoding = 'UTF-8'): string[]` : read the content of this file as an array of lines using the specified
encoding.

`write(text: string, encoding = 'UTF-8'): void` : write the input string to this file using the specified encoding.

`mkdir(): boolean` : create this directory, return `true` if the directory is created successfully.

`mkdirs(): boolean` : create this directory and all parent directories, return `true` if the directory is created
successfully.

`delete(): boolean` : delete this file or empty directory, return `true` if the file (directory) is deleted
successfully.

`deleteRecursively(): void` : delete this file or directory recursively

### Related global functions

`getNameWithoutExtension(fileName: string): string` : get the name without extension of the input string as a file name.

`getExtension(fileName: string): string` : get the extension of the input string as a file name.

`getSafeFileName(fileName: string): string` : get a safe file name from the input string.

### Source file

If you know Java and Javascript well enough, you can also read the source file for the `File`
class [here](../src/jvmMain/resources/js/file.js)
