function lookupDict(dictText, word) {
    if (!dictText) return null
    let escaped = word.toUpperCase().replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    let re = new RegExp("^" + escaped + "\\s+(.+)$", "m")
    let match = dictText.match(re)
    if (match) {
        return match[1].trim().split(/\s+/).map(p => p.replace(/\d+/g, "").toLowerCase())
    }
    return null
}

const enGraphemes = ["", "", "", "", "'", "-", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"]
const enPhonemes = ["", "", "", "", "aa", "ae", "ah", "ao", "aw", "ay", "b", "ch", "d", "dh", "eh", "er", "ey", "f", "g", "hh", "ih", "iy", "jh", "k", "l", "m", "n", "ng", "ow", "oy", "p", "r", "s", "sh", "t", "th", "uh", "uw", "v", "w", "y", "z", "zh"]

function predictOnnx(word) {
    try {
        if (typeof g2p !== "undefined") {
            let pred = g2p.predict("g2p.onnx", enGraphemes, enPhonemes, word)
            if (pred != null && pred.size() > 0) {
                let res = []
                for (let i = 0; i < pred.size(); i++) {
                    res.push(pred.get(i))
                }
                return res
            }
        }
    } catch (e) {}
    return null
}

function ruleBasedEnglish(word) {
    let c = word.toLowerCase().trim()
    let o = []
    let i = 0
    while (i < c.length) {
        if (i + 1 < c.length) {
            let t = c.substring(i, i + 2)
            let m = null
            switch (t) {
                case "th": m = "th"; break
                case "sh": m = "sh"; break
                case "ch": m = "ch"; break
                case "ph": m = "f"; break
                case "wh": m = "w"; break
                case "ng": m = "ng"; break
                case "ck": m = "k"; break
                case "ee":
                case "ea": m = "iy"; break
                case "oo": m = "uw"; break
                case "ou":
                case "ow": m = "aw"; break
                case "ai":
                case "ay": m = "ey"; break
                case "oi":
                case "oy": m = "oy"; break
            }
            if (m !== null) {
                o.push(m)
                i += 2
                continue
            }
        }
        let ch = c.charAt(i)
        switch (ch) {
            case 'a': o.push("ae"); break
            case 'e': o.push("eh"); break
            case 'i': o.push("ih"); break
            case 'o': o.push("aa"); break
            case 'u': o.push("ah"); break
            case 'y': o.push("iy"); break
            case 'b': o.push("b"); break
            case 'c': o.push("k"); break
            case 'd': o.push("d"); break
            case 'f': o.push("f"); break
            case 'g': o.push("g"); break
            case 'h': o.push("hh"); break
            case 'j': o.push("jh"); break
            case 'k': o.push("k"); break
            case 'l': o.push("l"); break
            case 'm': o.push("m"); break
            case 'n': o.push("n"); break
            case 'p': o.push("p"); break
            case 'q': o.push("k"); o.push("w"); break
            case 'r': o.push("r"); break
            case 's': o.push("s"); break
            case 't': o.push("t"); break
            case 'v': o.push("v"); break
            case 'w': o.push("w"); break
            case 'x': o.push("k"); o.push("s"); break
            case 'z': o.push("z"); break
            default:
                if (/[a-zA-Z]/.test(ch)) o.push(ch)
                break
        }
        i++
    }
    return o
}

let dictText = (typeof resources !== "undefined" && resources.length > 0) ? resources[0] : null
let words = input.trim().split(/\s+/).filter(w => w.length > 0)
let out = []

for (let w of words) {
    let clean = w.toLowerCase().replace(/[^a-zA-Z]/g, "")
    if (!clean) continue
    let phones = lookupDict(dictText, clean)
    if (!phones || phones.length === 0) {
        phones = predictOnnx(clean)
    }
    if (!phones || phones.length === 0) {
        phones = ruleBasedEnglish(clean)
    }
    if (phones && phones.length > 0) {
        out.push(...phones)
    }
}

let output = out
