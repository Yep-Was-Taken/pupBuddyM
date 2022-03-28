package com.example.pupbuddym.dto

class House(
    var houseId: String,
    var humans: Map<Int, Human>,
    var dogs: Map<Int, Dog>,
    var chores: Map<Int, Chore>
) {
}