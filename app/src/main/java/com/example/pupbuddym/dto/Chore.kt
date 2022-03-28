package com.example.pupbuddym.dto

class Chore(
    var choreId: String,
    var choreName: String = "",
    var choreComplete: Boolean = false,
    var choreStart: String = "",
    var choreEnd: String = "",
    var dogId: String,
    var humanId: String
) {
}