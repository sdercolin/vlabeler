let input = params["input"]
let mark = params["mark"]
let prefixMapLines = params["prefixMap"]
let prefixMap = undefined
let allModules = params["allModules"]

function convertMidiKeyToInt(key) {
    if (!key) return undefined
    let letter = "";
    let number = 0;
    let isSharp = false;

    if (key.indexOf('#') !== -1) {
        const parts = key.split('#');
        isSharp = true;
        letter = parts[0];
        number = parseInt(parts[1], 10);
    } else {
        const parts = key.match(/([A-G])([0-9])/);
        letter = parts[1];
        number = parseInt(parts[2], 10);
    }

    let letterInt = 0;
    switch (letter) {
        case 'C':
            letterInt = 0;
            break;
        case 'D':
            letterInt = 2;
            break;
        case 'E':
            letterInt = 4;
            break;
        case 'F':
            letterInt = 5;
            break;
        case 'G':
            letterInt = 7;
            break;
        case 'A':
            letterInt = 9;
            break;
        case 'B':
            letterInt = 11;
    }

    if (isSharp) {
        letterInt += 1;
    }

    return letterInt + (number + 1) * 12;
}

if (prefixMapLines) {
    prefixMap = prefixMapLines.split(/\r?\n/).map(line => line.split(/[\t\s]+/).filter(s => s !== "")).reduce((map, [key, text]) => {
        map[convertMidiKeyToInt(key)] = text;
        return map
    }, {});
}

if (debug) {
    if (prefixMap) {
        for (let [prefix, mark] of Object.entries(prefixMap)) {
            console.log(`${prefix} -> ${mark}`)
        }
    }
}

let usedLyrics = new Set()

let lyric = null
let key = null

input.split("\n").map(line => line.trim()).filter(line => line !== "").forEach(line => {
    if (line.startsWith("Lyric=")) {
        lyric = line.slice(6)
    } else if (line.startsWith("NoteNum=")) {
        key = line.slice(8)
    } else if (line.startsWith("[#")) {
        if (lyric && key) {
            if (prefixMap) {
                let suffix = prefixMap[key] || ""
                usedLyrics.add(`${lyric}${suffix}`)
            }
            usedLyrics.add(`${lyric}`)
        }
        lyric = null
        key = null
    }
});

if (debug) {
    usedLyrics.forEach(lyric => console.log(lyric))
}

for (let moduleIndex in modules) {
    let module = modules[moduleIndex]
    if (allModules || moduleIndex == currentModuleIndex) {
        module.entries.forEach(entry => {
            if (usedLyrics.has(entry.name)) {
                entry.notes.tag += mark
            }
        });
    }
}
