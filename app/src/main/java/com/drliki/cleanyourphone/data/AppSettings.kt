package com.drliki.cleanyourphone.data

import android.content.Context
import android.content.SharedPreferences
import com.drliki.cleanyourphone.model.SavedDeviceProfile
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AppSettings(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("cyp_settings", Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        const val DEFAULT_UAD_REPO_URL =
            "https://raw.githubusercontent.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation/main/resources/assets/uad_lists.json"
        const val DEFAULT_TV_REPO_URL =
            "https://raw.githubusercontent.com/dr-liki/cleanyourphone/main/app/src/main/assets/cleanyourtv_lists_seed.json"
        private const val KEY_UAD_REPO_URL = "uad_repo_url"
        private const val KEY_TV_REPO_URL = "tv_repo_url"
        private const val KEY_AUTO_UPDATE = "auto_update_on_launch"
        private const val KEY_AUTO_UPDATE_TV = "auto_update_tv_on_launch"
        private const val KEY_LAST_UPDATE_TIME = "last_update_time"
        private const val KEY_LAST_TV_UPDATE_TIME = "last_tv_update_time"
        private const val KEY_SAVED_PROFILES = "saved_device_profiles"
    }

    var uadRepoUrl: String
        get() = prefs.getString(KEY_UAD_REPO_URL, DEFAULT_UAD_REPO_URL)?.ifBlank { DEFAULT_UAD_REPO_URL }
            ?: DEFAULT_UAD_REPO_URL
        set(value) {
            prefs.edit().putString(KEY_UAD_REPO_URL, value.trim()).apply()
        }

    fun resetRepoUrl() {
        prefs.edit().putString(KEY_UAD_REPO_URL, DEFAULT_UAD_REPO_URL).apply()
    }

    var tvRepoUrl: String
        get() = prefs.getString(KEY_TV_REPO_URL, DEFAULT_TV_REPO_URL)?.ifBlank { DEFAULT_TV_REPO_URL }
            ?: DEFAULT_TV_REPO_URL
        set(value) {
            prefs.edit().putString(KEY_TV_REPO_URL, value.trim()).apply()
        }

    fun resetTvRepoUrl() {
        prefs.edit().putString(KEY_TV_REPO_URL, DEFAULT_TV_REPO_URL).apply()
    }

    var autoUpdateOnLaunch: Boolean
        get() = prefs.getBoolean(KEY_AUTO_UPDATE, false)
        set(value) {
            prefs.edit().putBoolean(KEY_AUTO_UPDATE, value).apply()
        }

    var autoUpdateTvOnLaunch: Boolean
        get() = prefs.getBoolean(KEY_AUTO_UPDATE_TV, false)
        set(value) {
            prefs.edit().putBoolean(KEY_AUTO_UPDATE_TV, value).apply()
        }

    var lastUpdateTime: Long
        get() = prefs.getLong(KEY_LAST_UPDATE_TIME, 0L)
        set(value) {
            prefs.edit().putLong(KEY_LAST_UPDATE_TIME, value).apply()
        }

    var lastTvUpdateTime: Long
        get() = prefs.getLong(KEY_LAST_TV_UPDATE_TIME, 0L)
        set(value) {
            prefs.edit().putLong(KEY_LAST_TV_UPDATE_TIME, value).apply()
        }

    fun getSavedProfiles(): List<SavedDeviceProfile> {
        val raw = prefs.getString(KEY_SAVED_PROFILES, null) ?: return emptyList()
        return runCatching {
            json.decodeFromString<List<SavedDeviceProfile>>(raw)
        }.getOrDefault(emptyList())
    }

    fun saveProfile(profile: SavedDeviceProfile) {
        val current = getSavedProfiles().toMutableList()
        val index = current.indexOfFirst { it.id == profile.id || (it.host == profile.host && it.port == profile.port) }
        if (index >= 0) {
            current[index] = profile
        } else {
            current.add(0, profile)
        }
        val encoded = json.encodeToString(current)
        prefs.edit().putString(KEY_SAVED_PROFILES, encoded).apply()
    }

    fun deleteProfile(id: String) {
        val current = getSavedProfiles().filterNot { it.id == id }
        val encoded = json.encodeToString(current)
        prefs.edit().putString(KEY_SAVED_PROFILES, encoded).apply()
    }

    fun recordDisabledPackages(host: String, port: Int, disabledPackages: List<String>, totalCount: Int) {
        val current = getSavedProfiles().toMutableList()
        val index = current.indexOfFirst { it.host == host && it.port == port }
        if (index >= 0) {
            val existing = current[index]
            val mergedDisabled = (existing.disabledPackages + disabledPackages).distinct()
            current[index] = existing.copy(
                disabledPackages = mergedDisabled,
                totalPackagesCount = if (totalCount > 0) totalCount else existing.totalPackagesCount,
                lastConnected = System.currentTimeMillis()
            )
            prefs.edit().putString(KEY_SAVED_PROFILES, json.encodeToString(current)).apply()
        }
    }
}
