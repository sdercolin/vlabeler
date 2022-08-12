@file:OptIn(ExperimentalComposeUiApi::class, ExperimentalSerializationApi::class)

package com.sdercolin.vlabeler.model.key

import androidx.compose.ui.ExperimentalComposeUiApi
import com.sdercolin.vlabeler.env.isMacOS
import kotlinx.serialization.ExperimentalSerializationApi

typealias ActualKey = androidx.compose.ui.input.key.Key

enum class Key(
    val displayedName: String,
    val actualKeys: List<ActualKey>,
    val isMainKey: Boolean = true
) {
    Ctrl(
        if (isMacOS) {
            "⌘"
        } else {
            "Ctrl"
        },
        if (isMacOS) {
            listOf(ActualKey.MetaLeft, ActualKey.MetaRight)
        } else {
            listOf(ActualKey.CtrlLeft, ActualKey.CtrlRight)
        },
        isMainKey = false
    ),
    Shift(
        if (isMacOS) {
            "⇧"
        } else {
            "Shift"
        },
        listOf(ActualKey.ShiftLeft, ActualKey.ShiftRight),
        isMainKey = false
    ),
    Alt(
        if (isMacOS) {
            "⌥"
        } else {
            "Alt"
        },
        listOf(ActualKey.AltLeft, ActualKey.AltRight),
        isMainKey = false
    ),
    Windows(
        if (isMacOS) {
            "⌃"
        } else {
            "Win"
        },
        if (isMacOS) {
            listOf(ActualKey.CtrlLeft, ActualKey.CtrlRight)
        } else {
            listOf(ActualKey.MetaLeft, ActualKey.MetaRight)
        },
        isMainKey = false
    ),
    Space("Space", listOf(ActualKey.Spacebar)),
    Enter("Enter", listOf(ActualKey.Enter)),
    Backspace(
        if (isMacOS) {
            "⌫"
        } else {
            "Backspace"
        },
        listOf(ActualKey.Backspace)
    ),
    Delete(
        if (isMacOS) {
            "⌦"
        } else {
            "Delete"
        },
        listOf(ActualKey.Delete)
    ),
    Escape(
        if (isMacOS) {
            "⎋"
        } else {
            "Escape"
        },
        listOf(ActualKey.Escape)
    ),
    Up(
        if (isMacOS) {
            "▲"
        } else {
            "Up"
        },
        listOf(ActualKey.DirectionUp)
    ),
    Down(
        if (isMacOS) {
            "▼"
        } else {
            "Down"
        },
        listOf(ActualKey.DirectionDown)
    ),
    Left(
        if (isMacOS) {
            "◄"
        } else {
            "Left"
        },
        listOf(ActualKey.DirectionLeft)
    ),
    Right(
        if (isMacOS) {
            "►"
        } else {
            "Right"
        },
        listOf(ActualKey.DirectionRight)
    ),
    Home("Home", listOf(ActualKey.MoveHome)),
    End("End", listOf(ActualKey.MoveEnd)),
    PageUp("PageUp", listOf(ActualKey.PageUp)),
    PageDown("PageDown", listOf(ActualKey.PageDown)),
    Zero("0", listOf(ActualKey.Zero)),
    One("1", listOf(ActualKey.One)),
    Two("2", listOf(ActualKey.Two)),
    Three("3", listOf(ActualKey.Three)),
    Four("4", listOf(ActualKey.Four)),
    Five("5", listOf(ActualKey.Five)),
    Six("6", listOf(ActualKey.Six)),
    Seven("7", listOf(ActualKey.Seven)),
    Eight("8", listOf(ActualKey.Eight)),
    Nine("9", listOf(ActualKey.Nine)),
    A("A", listOf(ActualKey.A)),
    B("B", listOf(ActualKey.B)),
    C("C", listOf(ActualKey.C)),
    D("D", listOf(ActualKey.D)),
    E("E", listOf(ActualKey.E)),
    F("F", listOf(ActualKey.F)),
    G("G", listOf(ActualKey.G)),
    H("H", listOf(ActualKey.H)),
    I("I", listOf(ActualKey.I)),
    J("J", listOf(ActualKey.J)),
    K("K", listOf(ActualKey.K)),
    L("L", listOf(ActualKey.L)),
    M("M", listOf(ActualKey.M)),
    N("N", listOf(ActualKey.N)),
    O("O", listOf(ActualKey.O)),
    P("P", listOf(ActualKey.P)),
    Q("Q", listOf(ActualKey.Q)),
    R("R", listOf(ActualKey.R)),
    S("S", listOf(ActualKey.S)),
    T("T", listOf(ActualKey.T)),
    U("U", listOf(ActualKey.U)),
    V("V", listOf(ActualKey.V)),
    W("W", listOf(ActualKey.W)),
    X("X", listOf(ActualKey.X)),
    Y("Y", listOf(ActualKey.Y)),
    Z("Z", listOf(ActualKey.Z)),
    Comma(",", listOf(ActualKey.Comma)),
    Period(".", listOf(ActualKey.Period)),
    Slash("/", listOf(ActualKey.Slash)),
    Minus("-", listOf(ActualKey.Minus)),
    Equals("=", listOf(ActualKey.Equals)),
    NumPadAdd("NumPad+", listOf(ActualKey.NumPadAdd)),
    NumPadSubtract("NumPad-", listOf(ActualKey.NumPadSubtract)),
    NumPadMultiply("NumPad*", listOf(ActualKey.NumPadMultiply)),
    NumPadDivide("NumPad/", listOf(ActualKey.NumPadDivide)),
    NumPadEquals("NumPad=", listOf(ActualKey.NumPadEquals)),
    NumPad0("NumPad0", listOf(ActualKey.NumPad0)),
    NumPad1("NumPad1", listOf(ActualKey.NumPad1)),
    NumPad2("NumPad2", listOf(ActualKey.NumPad2)),
    NumPad3("NumPad3", listOf(ActualKey.NumPad3)),
    NumPad4("NumPad4", listOf(ActualKey.NumPad4)),
    NumPad5("NumPad5", listOf(ActualKey.NumPad5)),
    NumPad6("NumPad6", listOf(ActualKey.NumPad6)),
    NumPad7("NumPad7", listOf(ActualKey.NumPad7)),
    NumPad8("NumPad8", listOf(ActualKey.NumPad8)),
    NumPad9("NumPad9", listOf(ActualKey.NumPad9)),
    NumPadEnter("NumPadEnter", listOf(ActualKey.NumPadEnter)),
    BracketLeft("[", listOf(ActualKey.LeftBracket)),
    BracketRight("]", listOf(ActualKey.RightBracket)),
    Backslash("\\", listOf(ActualKey.Backslash)),
    Semicolon(";", listOf(ActualKey.Semicolon)),
    Apostrophe("'", listOf(ActualKey.Apostrophe)),
    Grave("`", listOf(ActualKey.Grave));

    fun isKey(actualKey: ActualKey) = actualKeys.contains(actualKey)

    companion object {
        fun fromActualKey(actualKey: ActualKey): Key? = values().find { it.isKey(actualKey) }
    }
}
