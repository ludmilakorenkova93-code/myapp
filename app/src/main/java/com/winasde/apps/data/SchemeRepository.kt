package com.winasde.apps.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.schemeDataStore by preferencesDataStore(name = "football_schemes")

class SchemeRepository(context: Context) {
    private val dataStore = context.applicationContext.schemeDataStore
    private val schemesKey = stringPreferencesKey("schemes_json")
    private val playersKey = stringPreferencesKey("players_json")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val schemes: Flow<List<TeamScheme>> = dataStore.data
        .map { preferences ->
            preferences[schemesKey]
                ?.let { json.decodeFromString<List<TeamScheme>>(it) }
                ?.sortedByDescending { it.updatedAt }
                ?: listOf(defaultScheme())
        }
        .catch { emit(listOf(defaultScheme())) }

    val players: Flow<List<PlayerProfile>> = dataStore.data
        .map { preferences ->
            preferences[playersKey]
                ?.let { json.decodeFromString<List<PlayerProfile>>(it) }
                ?.sortedWith(compareBy<PlayerProfile> { it.number }.thenBy { it.name })
                ?: emptyList()
        }
        .catch { emit(emptyList()) }

    suspend fun saveScheme(scheme: TeamScheme, currentSchemes: List<TeamScheme>) {
        val updatedScheme = scheme.copy(updatedAt = System.currentTimeMillis())
        val nextSchemes = currentSchemes
            .filterNot { it.id == updatedScheme.id }
            .plus(updatedScheme)
            .sortedByDescending { it.updatedAt }

        dataStore.edit { preferences ->
            preferences[schemesKey] = json.encodeToString(nextSchemes)
        }
    }

    suspend fun deleteScheme(id: String, currentSchemes: List<TeamScheme>) {
        val nextSchemes = currentSchemes.filterNot { it.id == id }
        dataStore.edit { preferences ->
            preferences[schemesKey] = json.encodeToString(nextSchemes.ifEmpty { listOf(defaultScheme()) })
        }
    }

    suspend fun savePlayer(player: PlayerProfile, currentPlayers: List<PlayerProfile>) {
        val nextPlayers = currentPlayers
            .filterNot { it.id == player.id }
            .plus(player)
            .sortedWith(compareBy<PlayerProfile> { it.number }.thenBy { it.name })

        dataStore.edit { preferences ->
            preferences[playersKey] = json.encodeToString(nextPlayers)
        }
    }

    suspend fun deletePlayer(id: String, currentPlayers: List<PlayerProfile>) {
        dataStore.edit { preferences ->
            preferences[playersKey] = json.encodeToString(currentPlayers.filterNot { it.id == id })
        }
    }
}
