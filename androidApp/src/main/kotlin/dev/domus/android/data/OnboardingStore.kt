package dev.domus.android.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.onboardingDataStore by preferencesDataStore(name = "domus_onboarding")
private val HAS_SEEN_ONBOARDING_KEY = booleanPreferencesKey("has_seen_onboarding")

/** Tracks whether the first-run onboarding has already been shown. */
class OnboardingStore(private val context: Context) {
    suspend fun hasSeenOnboarding(): Boolean =
        context.onboardingDataStore.data.first()[HAS_SEEN_ONBOARDING_KEY] ?: false

    suspend fun markSeen() {
        context.onboardingDataStore.edit { it[HAS_SEEN_ONBOARDING_KEY] = true }
    }
}
