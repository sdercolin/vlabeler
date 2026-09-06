let map = {
    "っ": ["cl"],
    "ッ": ["cl"],
    "ー": ["-"],
    "ん": ["N"],
    "ン": ["N"]
}

if (typeof resources !== "undefined" && resources) {
    for (let r = 0; r < resources.length; r++) {
        let lines = resources[r].split(/\r?\n/)
        for (let l = 0; l < lines.length; l++) {
            let line = lines[l].trim()
            if (!line || line.startsWith("#") || line.startsWith(";;;")) continue
            let parts = line.split(/\s+/)
            if (parts.length >= 2) {
                map[parts[0]] = parts.slice(1)
            }
        }
    }
}

let out = []
let s = input.trim()
let i = 0

while (i < s.length) {
    let r = s.length - i
    if (r >= 4) {
        let k4 = s.substring(i, i + 4).toLowerCase()
        if (map[k4]) {
            out.push(...map[k4])
            i += 4
            continue
        }
    }
    if (r >= 3) {
        let k3 = s.substring(i, i + 3).toLowerCase()
        if (map[k3]) {
            out.push(...map[k3])
            i += 3
            continue
        }
    }
    if (r >= 2) {
        let k2 = s.substring(i, i + 2)
        if (map[k2]) {
            out.push(...map[k2])
            i += 2
            continue
        }
        let k2low = k2.toLowerCase()
        if (map[k2low]) {
            out.push(...map[k2low])
            i += 2
            continue
        }
    }
    let one = s.substring(i, i + 1)
    if (map[one]) {
        out.push(...map[one])
        i++
        continue
    }
    let onelow = one.toLowerCase()
    if (map[onelow]) {
        out.push(...map[onelow])
        i++
        continue
    }
    if (one.trim().length > 0 && one !== "-" && one !== "_") {
        out.push(onelow)
    }
    i++
}

let output = out
