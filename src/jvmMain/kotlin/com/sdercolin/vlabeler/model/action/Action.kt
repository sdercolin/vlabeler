package com.sdercolin.vlabeler.model.action

sealed interface Action {
    val displayOrder: Int
    val title: String
}
