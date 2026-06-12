package com.winasde.apps

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.winasde.apps.data.PlayerPosition
import com.winasde.apps.data.PlayerProfile
import com.winasde.apps.data.SchemeRepository
import com.winasde.apps.data.TeamScheme
import com.winasde.apps.data.TacticalDrawing
import com.winasde.apps.data.defaultScheme
import com.winasde.apps.data.formationPlayers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class SchemeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SchemeRepository(application)

    val schemes: StateFlow<List<TeamScheme>> = repository.schemes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = listOf(defaultScheme())
    )

    val players: StateFlow<List<PlayerProfile>> = repository.players.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun createScheme() {
        val scheme = defaultScheme().copy(
            id = UUID.randomUUID().toString(),
            name = "Схема ${schemes.value.size + 1}"
        )
        save(scheme)
    }

    fun renameScheme(scheme: TeamScheme, name: String) {
        save(scheme.copy(name = name))
    }

    fun updateNotes(scheme: TeamScheme, notes: String) {
        save(scheme.copy(notes = notes))
    }

    fun changeFormation(scheme: TeamScheme, formation: String) {
        save(scheme.copy(formation = formation, players = formationPlayers(formation)))
    }

    fun movePlayer(scheme: TeamScheme, number: Int, x: Float, y: Float) {
        val movedPlayers = scheme.players.map { player ->
            if (player.number == number) {
                player.copy(x = x.coerceIn(0.06f, 0.94f), y = y.coerceIn(0.06f, 0.96f))
            } else {
                player
            }
        }
        save(scheme.copy(players = movedPlayers))
    }

    fun addDrawing(scheme: TeamScheme, drawing: TacticalDrawing) {
        save(scheme.copy(drawings = scheme.drawings + drawing))
    }

    fun clearDrawings(scheme: TeamScheme) {
        save(scheme.copy(drawings = emptyList()))
    }

    fun savePlayer(
        name: String,
        number: Int,
        position: String,
        strongFoot: String,
        age: Int,
        notes: String
    ) {
        val player = PlayerProfile(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            number = number.coerceIn(1, 99),
            position = position.trim().ifBlank { "Игрок" },
            strongFoot = strongFoot.trim().ifBlank { "Правая" },
            age = age.coerceIn(10, 60),
            notes = notes.trim()
        )
        viewModelScope.launch {
            repository.savePlayer(player, players.value)
        }
    }

    fun deletePlayer(id: String) {
        viewModelScope.launch {
            repository.deletePlayer(id, players.value)
        }
    }

    fun deleteScheme(id: String) {
        viewModelScope.launch {
            repository.deleteScheme(id, schemes.value)
        }
    }

    private fun save(scheme: TeamScheme) {
        viewModelScope.launch {
            repository.saveScheme(scheme, schemes.value)
        }
    }
}
