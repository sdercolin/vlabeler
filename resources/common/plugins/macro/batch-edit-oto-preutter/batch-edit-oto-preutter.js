let selectedEntryIndexes = params["selector"]
let preutterance = params["preutterance"]
let overlap = params["overlap"]

if (debug) {
    console.log(`Input entries: ${entries.length}`)
    console.log(`Selected entries: ${selectedEntryIndexes.length}`)
}

function tryParseInt(str) {
    if (str === null || str === undefined || str === "") {
        return { success: false, value: null };
    }
    const num = parseInt(str, 10);
    if (isNaN(num)) {
        error({
            en: `Non-numeric characters are entered in the preutterance or overlap`,
            ja: `先行発声またはオーバーラップに数字以外が入力されています`,
            ru: `В поле 'Преу.' или 'Овер.' введены нечисловые символы`
        })
    }
    return { success: true, value: num };
}
let resultPre = tryParseInt(preutterance);
let resultOvl = tryParseInt(overlap);

for (let index of selectedEntryIndexes) {
    let entry = entries[index]
    let edited = Object.assign({}, entry)
    let offset = entry.points[3]
    let preutter = entry.points[1] - offset

    if (resultPre.success) {
        let diff = 0;
        if (preutterance.startsWith("+") || preutterance.startsWith("-")) {
            diff = resultPre.value;
        } else {
            diff = resultPre.value - preutter;
        }
        if (debug) {
            console.log(`diff: ${diff}`)
        }
        edited.points[3] = Math.max(entry.points[3] - diff, 0);
    }

    if (resultOvl.success && resultOvl.value > 0) {
        preutter = entry.points[1] - edited.points[3];
        edited.points[2] = edited.points[3] + (preutter / resultOvl.value);
    }

    edited.start = Math.min(...edited.points, edited.start)

    if (debug) {
        console.log(`Edited: ${JSON.stringify(edited)}`)
    }
    entries[index] = edited
}
