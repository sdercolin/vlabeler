# Launch Arguments

You can pass the following launch arguments to the application:

### Open Project

- `--open <path>`: **Required.** Open the given project file

### Open or Create Project

- `--open-or-create <path>` **Required.** Open the given project file if it exists, otherwise create a new project
  in the given path
- `--sample-dir <path>`: Set the sample directory for the new project. If not set, the location of the project
  file is used
- `--cache-dir <path>`: Set the cache directory for the new project. **Please ensure the directory is only used by this
  project.** If not set, the default directory is used (`<project_location>/<project_name>.lbp.caches/`).
- `--input <path>`: Set the input file for the new project. If not set, the default input file defined by the
  selected labeler is used
- `--labeler <name>`: Set the labeler by labeler's `name` for the new project
- `--encoding <encoding>`: Set the encoding of the input file for the new project. If not set, the detected
  encoding is used
- `--auto-export <bool>`: Set whether to automatically export the project file to the input file when saved
