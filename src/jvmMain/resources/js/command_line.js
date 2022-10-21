let CommandLine = Java.type('com.sdercolin.vlabeler.util.CommandLine')

function executeCommand(...args) {
    return CommandLine.execute(args)
}
