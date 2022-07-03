import codecs

with codecs.open('dict.txt', 'r', encoding='utf-8') as fp:
    dictLines = fp.readlines()


def split(line):
    (first, second) = line.strip().split(' ', 1)
    return first, second.split()


dict = dict(map(split, dictLines))

ustLines = inputs[0].splitlines()
sample = samples[0]


def get_time_ms(tempo, tick):
    return 1000 * 60.0 / 480.0 / tempo * tick


class Note:
    def __init__(self, pos, length, lyric):
        self.pos = pos
        self.length = length
        self.lyric = lyric


pos = 0
lyric = None
length = None
notes = []

for line in ustLines:
    if line.startswith("Tempo="):
        tempo = float(line.replace("Tempo=", "").replace(",", "."))
    if line.startswith("Length="):
        length = get_time_ms(tempo, int(line.replace("Length=", "")))
    if line.startswith("Lyric="):
        lyric = line.replace("Lyric=", "")
    if line.startswith("[#"):
        if lyric is not None and length is not None:
            note = Note(pos, length, lyric)
            pos += length
            lyric = None
            length = None
            notes.append(note)

entries = []
last = None
for note in notes:
    phonemes = dict.get(note.lyric, [note.lyric])
    # max count of phonemes is 3
    if len(phonemes) == 1:
        if last is not None:
            entries.append(last)
        start = note.pos
        end = note.pos + note.length
        last = Entry(sample=sample, name=phonemes[0], start=start, end=end, points=[], extras=[])
    elif len(phonemes) > 1:
        overlap = 0
        if last is not None:
            overlap = params["overlap"]
            lastLength = last.end - last.start
            if lastLength < overlap * 2:
                overlap = int(lastLength / 2)
            last.end = last.end - overlap
            entries.append(last)
        if len(phonemes) == 2:
            consonantStart = note.pos - overlap
            consonantEnd = note.pos
            vowelStart = consonantEnd
            vowelEnd = note.pos + note.length
            entries.append(Entry(sample=sample, name=phonemes[0], start=consonantStart, end=consonantEnd, points=[], extras=[]))
            last = Entry(sample=sample, name=phonemes[1], start=vowelStart, end=vowelEnd, points=[], extras=[])
        else:
            vowelDelay = params["vowelDelay"]
            if note.length < vowelDelay * 3:
                vowelDelay = int(note.length / 3)
            consonantLength = (overlap + vowelDelay) / 2
            semivowelLength = overlap + vowelDelay - consonantLength
            consonantStart = note.pos - overlap
            consonantEnd = consonantStart + consonantLength
            semivowelStart = consonantEnd
            semivowelEnd = semivowelStart +semivowelLength
            vowelStart = semivowelEnd
            vowelEnd = note.pos + note.length
            entries.append(Entry(sample=sample, name=phonemes[0], start=consonantStart, end=consonantEnd, points=[], extras=[]))
            entries.append(Entry(sample=sample, name=phonemes[1], start=semivowelStart, end=semivowelEnd, points=[], extras=[]))
            last = Entry(sample=sample, name=phonemes[2], start=vowelStart, end=vowelEnd, points=[], extras=[])

if last is not None:
    entries.append(last)

output = entries
