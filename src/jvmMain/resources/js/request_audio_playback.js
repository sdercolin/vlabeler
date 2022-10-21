class AudioFilePlaybackRequest {
    constructor(type, path, offset, duration) {
        this.type = type // currently only "file" is supported
        this.path = path // path to the audio file
        this.offset = offset // starting position of the audio file to play, in milliseconds
        this.duration = duration // duration of the audio file to play, in milliseconds. If not given, the whole file will be played.
    }
}

function requestAudioFilePlayback(path, offset = 0, duration = null) {
    audioPlaybackRequest = new AudioFilePlaybackRequest("file", path, offset, duration)
}
