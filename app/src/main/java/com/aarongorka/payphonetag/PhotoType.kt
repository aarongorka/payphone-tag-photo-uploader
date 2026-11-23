package com.aarongorka.payphonetag

enum class PhotoType(val value: String, val label: String) {
    FRONT("front", "Front"),
    SIDE("side", "Side"),
    STREET("street", "Street"),
    BOARD("board", "Board"),
    CASE("case", "Case"),
    LIVERY("livery", "Livery"),
    GHOST("ghost", "Ghost"),
    OTHER("other", "Other"),
}
