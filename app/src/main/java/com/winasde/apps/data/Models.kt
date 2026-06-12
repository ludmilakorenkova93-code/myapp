package com.winasde.apps.data

import kotlinx.serialization.Serializable

@Serializable
data class TeamScheme(
    val id: String,
    val name: String,
    val formation: String,
    val notes: String,
    val players: List<PlayerPosition>,
    val drawings: List<TacticalDrawing> = emptyList(),
    val updatedAt: Long
)

@Serializable
data class PlayerPosition(
    val number: Int,
    val role: String,
    val x: Float,
    val y: Float
)

@Serializable
data class PlayerProfile(
    val id: String,
    val name: String,
    val number: Int,
    val position: String,
    val strongFoot: String,
    val age: Int,
    val notes: String = ""
)

@Serializable
data class TacticalDrawing(
    val id: String,
    val type: DrawingType,
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val label: String = ""
)

@Serializable
enum class DrawingType {
    Arrow,
    Zone,
    Marker
}

data class SchemeAnalysis(
    val attackingPlayers: Int,
    val midfieldPlayers: Int,
    val defensivePlayers: Int,
    val leftSidePlayers: Int,
    val rightSidePlayers: Int,
    val balanceText: String,
    val pressureText: String
)

fun TeamScheme.analyze(): SchemeAnalysis {
    val outfieldPlayers = players.filterNot { it.role == "GK" }
    val attackingPlayers = outfieldPlayers.count { it.y < 0.36f }
    val midfieldPlayers = outfieldPlayers.count { it.y in 0.36f..0.68f }
    val defensivePlayers = outfieldPlayers.count { it.y > 0.68f }
    val leftSidePlayers = outfieldPlayers.count { it.x < 0.42f }
    val rightSidePlayers = outfieldPlayers.count { it.x > 0.58f }
    val sideDelta = kotlin.math.abs(leftSidePlayers - rightSidePlayers)

    val balanceText = when {
        sideDelta <= 1 -> "Фланги сбалансированы"
        leftSidePlayers > rightSidePlayers -> "Перегрузка слева"
        else -> "Перегрузка справа"
    }

    val pressureText = when {
        attackingPlayers >= 4 -> "Высокий прессинг и агрессивная атака"
        defensivePlayers >= 5 -> "Глубокий блок, осторожная структура"
        midfieldPlayers >= 4 -> "Сильный контроль центра поля"
        else -> "Сбалансированная структура без явного перекоса"
    }

    return SchemeAnalysis(
        attackingPlayers = attackingPlayers,
        midfieldPlayers = midfieldPlayers,
        defensivePlayers = defensivePlayers,
        leftSidePlayers = leftSidePlayers,
        rightSidePlayers = rightSidePlayers,
        balanceText = balanceText,
        pressureText = pressureText
    )
}

fun defaultScheme(): TeamScheme = TeamScheme(
    id = "default",
    name = "Новая схема",
    formation = "4-3-3",
    notes = "План на матч, сильные стороны соперника, задачи по прессингу.",
    players = formationPlayers("4-3-3"),
    updatedAt = System.currentTimeMillis()
)

fun formationPlayers(formation: String): List<PlayerPosition> {
    val lines = when (formation) {
        "4-4-2" -> listOf(
            0.86f to listOf(0.18f, 0.38f, 0.62f, 0.82f),
            0.56f to listOf(0.16f, 0.38f, 0.62f, 0.84f),
            0.25f to listOf(0.38f, 0.62f)
        )
        "3-5-2" -> listOf(
            0.84f to listOf(0.24f, 0.50f, 0.76f),
            0.55f to listOf(0.12f, 0.32f, 0.50f, 0.68f, 0.88f),
            0.25f to listOf(0.38f, 0.62f)
        )
        "4-2-3-1" -> listOf(
            0.86f to listOf(0.18f, 0.38f, 0.62f, 0.82f),
            0.64f to listOf(0.38f, 0.62f),
            0.42f to listOf(0.20f, 0.50f, 0.80f),
            0.22f to listOf(0.50f)
        )
        else -> listOf(
            0.86f to listOf(0.18f, 0.38f, 0.62f, 0.82f),
            0.57f to listOf(0.28f, 0.50f, 0.72f),
            0.25f to listOf(0.18f, 0.50f, 0.82f)
        )
    }

    val players = mutableListOf(PlayerPosition(1, "GK", 0.50f, 0.96f))
    var number = 2
    lines.forEachIndexed { lineIndex, line ->
        val role = when (lineIndex) {
            0 -> "DEF"
            lines.lastIndex -> "ATT"
            else -> "MID"
        }
        line.second.forEach { x ->
            players += PlayerPosition(number = number, role = role, x = x, y = line.first)
            number += 1
        }
    }
    return players
}
