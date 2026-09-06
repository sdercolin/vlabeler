function single(s) {
    switch (s) {
        case "bb": return "bj"
        case "vv": return "vj"
        case "gg": return "gj"
        case "dd": return "dj"
        case "zz": return "zj"
        case "kk": return "kj"
        case "ll": return "lj"
        case "mm": return "mj"
        case "nn": return "nj"
        case "pp": return "pj"
        case "rr": return "rj"
        case "ss": return "sj"
        case "tt": return "tj"
        case "ff": return "fj"
        case "hh": return "hj"
        case "sch": return "shj"
        case "a": return "A"
        case "aa": return "a"
        case "i": return "E"
        case "ii": return "i"
        case "y": return "Y"
        case "yy": return "y"
        case "ee": return "e"
        case "oo": return "o"
        case "uu": return "u"
        default: return s
    }
}

function mapPhones(phones) {
    let m = phones.map(single)
    let r = []
    for (let ph of m) {
        let prev = r.length > 0 ? r[r.length - 1] : null
        let soft = prev !== null && prev.endsWith("j") && prev.length > 1
        switch (ph) {
            case "je": if (soft) r.push("e"); else { r.push("j"); r.push("e"); } break
            case "ja": if (soft) r.push("a"); else { r.push("j"); r.push("a"); } break
            case "jo": if (soft) r.push("o"); else { r.push("j"); r.push("o"); } break
            case "ju": if (soft) r.push("u"); else { r.push("j"); r.push("u"); } break
            case "ay": r.push("a"); r.push("j"); break
            case "uj": r.push("u"); r.push("j"); break
            default: r.push(ph); break
        }
    }
    return r
}

const hard = {
    'б': "b", 'в': "v", 'г': "g", 'д': "d", 'ж': "zh", 'з': "z", 'й': "j", 'к': "k",
    'л': "l", 'м': "m", 'н': "n", 'п': "p", 'р': "r", 'с': "s", 'т': "t", 'ф': "f",
    'х': "h", 'ц': "c", 'ч': "ch", 'ш': "sh", 'щ': "shj"
}
const softMap = {
    'б': "bj", 'в': "vj", 'г': "gj", 'д': "dj", 'ж': "zhj", 'з': "zj", 'й': "j", 'к': "kj",
    'л': "lj", 'м': "mj", 'н': "nj", 'п': "pj", 'р': "rj", 'с': "sj", 'т': "tj", 'ф': "fj",
    'х': "hj", 'ц': "cj", 'ч': "ch", 'ш': "shj", 'щ': "shj"
}
const pal = new Set(['е', 'ё', 'и', 'ю', 'я', 'ь'])

function ruleBasedRu(text) {
    let r = []
    let s = text.trim()
    let i = 0
    while (i < s.length) {
        let raw = s.charAt(i)
        let c = raw.toLowerCase()
        if (/\s/.test(raw) || raw === '-' || raw === '_') { i++; continue }
        if (hard[c] !== undefined) {
            let nxt = (i + 1 < s.length) ? s.charAt(i + 1).toLowerCase() : null
            let palatal = nxt !== null && pal.has(nxt)
            r.push(palatal ? (softMap[c] || hard[c]) : hard[c])
            if (nxt === 'ь' || nxt === 'ъ') { i += 2; continue }
            i++
            continue
        }
        let prev = (i > 0) ? s.charAt(i - 1).toLowerCase() : null
        let after = prev !== null && hard[prev] !== undefined
        switch (c) {
            case 'а': r.push("a"); break
            case 'о': r.push("o"); break
            case 'у': r.push("u"); break
            case 'э': r.push("e"); break
            case 'ы': r.push("y"); break
            case 'и': r.push("i"); break
            case 'я': if (after) r.push("a"); else { r.push("j"); r.push("a"); } break
            case 'е': if (after) r.push("e"); else { r.push("j"); r.push("e"); } break
            case 'ё': if (after) r.push("o"); else { r.push("j"); r.push("o"); } break
            case 'ю': if (after) r.push("u"); else { r.push("j"); r.push("u"); } break
            case 'ь':
            case 'ъ': break
            default:
                if (/[\u0400-\u04FFa-zA-Z]/.test(raw)) r.push(raw.toLowerCase())
                break
        }
        i++
    }
    return r
}

function lookupDict(dictText, word) {
    if (!dictText) return null
    let escaped = word.toLowerCase().replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    let re = new RegExp("^" + escaped + "\\s+(.+)$", "m")
    let match = dictText.match(re)
    if (match) {
        let rawPhones = match[1].trim().split(/\s+/)
        return mapPhones(rawPhones)
    }
    return null
}

const ruGraphemes = ["", "", "", "", "-", "а", "б", "в", "г", "д", "е", "ж", "з", "и", "й", "к", "л", "м", "н", "о", "п", "р", "с", "т", "у", "ф", "х", "ц", "ч", "ш", "щ", "ъ", "ы", "ь", "э", "ю", "я", "ё"]
const ruPhonemes = ["", "", "", "", "a", "aa", "ay", "b", "bb", "c", "ch", "d", "dd", "ee", "f", "ff", "g", "gg", "h", "hh", "i", "ii", "j", "ja", "je", "jo", "ju", "k", "kk", "l", "ll", "m", "mm", "n", "nn", "oo", "p", "pp", "r", "rr", "s", "sch", "sh", "ss", "t", "tt", "u", "uj", "uu", "v", "vv", "y", "yy", "z", "zh", "zz"]

function predictOnnx(word) {
    try {
        if (typeof g2p !== "undefined") {
            let pred = g2p.predict("g2p.onnx", ruGraphemes, ruPhonemes, word)
            if (pred != null && pred.size() > 0) {
                let res = []
                for (let i = 0; i < pred.size(); i++) {
                    res.push(pred.get(i))
                }
                return mapPhones(res)
            }
        }
    } catch (e) {}
    return null
}

let dictText = (typeof resources !== "undefined" && resources.length > 0) ? resources[0] : null
let words = input.trim().split(/\s+/).filter(w => w.length > 0)
let out = []

for (let w of words) {
    let clean = w.toLowerCase().replace(/[^\u0400-\u04FF]/g, "")
    if (!clean) continue
    let phones = lookupDict(dictText, clean)
    if (!phones || phones.length === 0) {
        phones = predictOnnx(clean)
    }
    if (!phones || phones.length === 0) {
        phones = ruleBasedRu(w)
    }
    if (phones && phones.length > 0) {
        out.push(...phones)
    }
}

let output = out
