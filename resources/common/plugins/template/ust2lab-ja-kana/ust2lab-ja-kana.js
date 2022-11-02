class Note {
    constructor(pos, length, lyric) {
        this.pos = pos
        this.length = length
        this.lyric = lyric
    }
}

function getTimeMs(tempo, tick) {
    return parseFloat((1000 * 60.0 / 480.0 / tempo * tick).toFixed(5))
}

function log(text) {
    if (debug) {
        console.log(text)
    }
}

function parseLine(line) {
    let sections = line.split(' ')
    let first = sections[0]
    sections.shift()
    return [first, sections]
}

function splitLine(text) {
    return text.split('\n').map(x => x.trim())
}

let ustLines = splitLine(inputs[0])
let sample = samples[0]
let dictLines = splitLine(resources[0]).map(x => parseLine(x))
let dict = Object.assign({}, ...dictLines.map((x) => ({[x[0]]: x[1]})))

let pos = 0
let lyric = null
let length = null
let notes = []

for (const line of ustLines) {
    if (line.startsWith("Tempo=")) {
        tempo = parseFloat(line.replace("Tempo=", "".replace(",", ".")))
    }
    if (line.startsWith("Length=")) {
        length = getTimeMs(tempo, parseInt(line.replace("Length=", "")))
    }
    if (line.startsWith("Lyric=")) {
        lyric = line.replace("Lyric=", "")
    }
    if (line.startsWith("[#")) {
        if (lyric != null && length != null) {
            let note = new Note(pos, length, lyric)
            pos += length
            lyric = null
            length = null
            notes.push(note)
        }
    }
}

let entries = []
let last = null

for (const note of notes) {
    let phonemes = dict[note.lyric] || [note.lyric]
    log("on note: " + JSON.stringify(note) + " phonemes: " + JSON.stringify(phonemes))
    // max count of phonemes is 3
    if (phonemes.length === 1) {
        if (last) {
            entries.push(last)
            log("push last: " + JSON.stringify(last))
        }
        start = note.pos
        end = note.pos + note.length
        last = new Entry(sample, phonemes[0], start, end, [], [])
        log("assign last: " + JSON.stringify(last))
    } else if (phonemes.length > 1) {
        overlap = 0
        if (last) {
            overlap = params["overlap"]
            lastLength = last.end - last.start
            if (lastLength < overlap * 2) {
                overlap = (lastLength / 2).toFixed()
            }
            last.end = last.end - overlap
            entries.push(last)
            log("push edited last: " + JSON.stringify(last))
        }
        if (phonemes.length === 2) {
            consonantStart = note.pos - overlap
            consonantEnd = note.pos
            vowelStart = consonantEnd
            vowelEnd = note.pos + note.length
            created = new Entry(sample, phonemes[0], consonantStart, consonantEnd, [], [])
            entries.push(created)
            log("push new: " + JSON.stringify(created))
            last = new Entry(sample, phonemes[1], vowelStart, vowelEnd, [], [])
            log("assign last: " + JSON.stringify(last))
        } else {
            vowelDelay = params["vowelDelay"]
            if (note.length < vowelDelay * 3) {
                vowelDelay = (note.length / 3).toFixed()
            }
            consonantLength = (overlap + vowelDelay) / 2
            semivowelLength = overlap + vowelDelay - consonantLength
            consonantStart = note.pos - overlap
            consonantEnd = consonantStart + consonantLength
            semivowelStart = consonantEnd
            semivowelEnd = semivowelStart + semivowelLength
            vowelStart = semivowelEnd
            vowelEnd = note.pos + note.length

            created = new Entry(sample, phonemes[0], consonantStart, consonantEnd, [], [])
            entries.push(created)
            log("push new: " + JSON.stringify(created))
            created = new Entry(sample, phonemes[1], semivowelStart, semivowelEnd, [], [])
            entries.push(created)
            log("push new: " + JSON.stringify(created))
            last = new Entry(sample, phonemes[2], vowelStart, vowelEnd, [], [])
            log("assign last: " + JSON.stringify(last))
        }
    }
}

if (last) {
    entries.push(last)
    log("push last: " + JSON.stringify(last))
}
output = entries
