package com.example.myapplication.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.myapplication.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = Constants.DATASTORE_NAME)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val USER_ROLE = stringPreferencesKey("user_role")
        val USER_NAME = stringPreferencesKey("user_name")
    }

    val userRole: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.USER_ROLE]
    }

    val userName: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.USER_NAME]
    }

    suspend fun saveUserSession(role: String, username: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_ROLE] = role
            preferences[PreferencesKeys.USER_NAME] = username
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.USER_ROLE)
            preferences.remove(PreferencesKeys.USER_NAME)
        }
    }
}
