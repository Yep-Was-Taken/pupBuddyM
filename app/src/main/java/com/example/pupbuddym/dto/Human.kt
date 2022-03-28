package com.example.pupbuddym.dto

data class Human(
    var humanId: String,
    var humanName: String = "",
    var careRole: String = "",
    var houseId: String
) {
}