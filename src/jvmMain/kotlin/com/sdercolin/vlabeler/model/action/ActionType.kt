package com.sdercolin.vlabeler.model.action

enum class ActionType(val requiresCompleteKeySet: Boolean) {
    Key(true),
    MouseClick(false)
}
