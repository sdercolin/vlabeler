fun Float.toStringTrimmed(): String {
    return this.toString().trim('.', '0')
}
