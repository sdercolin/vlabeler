let CommandLine = Java.type('com.sdercolin.vlabeler.util.CommandLine')

function executeCommand(...args) {
    if (debug) {
        let command = args.map(arg => `"${arg}"`).join(' ')
        console.log(`Executing command: ${command}`)
    }
    return CommandLine.execute(args)
}
