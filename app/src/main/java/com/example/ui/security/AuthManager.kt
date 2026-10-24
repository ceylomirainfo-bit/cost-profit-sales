package com.example.ui.security

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale

sealed class AuthResult {
    data class Success(val email: String, val username: String) : AuthResult()
    data class Error(val message: String) : AuthResult()
    data class Lockout(val secondsRemaining: Int) : AuthResult()
}

class AuthManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ceyvana_auth_prefs", Context.MODE_PRIVATE)

    companion object {
        const val DEFAULT_OWNER_EMAIL = "ceylomirainfo@gmail.com"
        const val DEFAULT_OWNER_USERNAME = "admin"
        const val DEFAULT_OWNER_PASSWORD = "Ceyvana@2026"
        const val DEFAULT_PIN = "1234"

        private const val KEY_IS_AUTHENTICATED = "is_authenticated"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD_SALT = "password_salt"
        private const val KEY_PASSWORD_HASH = "password_hash"
        private const val KEY_OWNER_PIN = "owner_pin"
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        private const val KEY_LOCKOUT_UNTIL_TIMESTAMP = "lockout_until"

        private const val MAX_FAILED_ATTEMPTS = 5
        private const val LOCKOUT_DURATION_MS = 15_000L // 15 seconds cooldown
    }

    init {
        // Initialize default owner credentials with secure hash if not yet set
        if (!prefs.contains(KEY_PASSWORD_HASH)) {
            val salt = generateSalt()
            val hash = hashPassword(DEFAULT_OWNER_PASSWORD, salt)
            prefs.edit()
                .putString(KEY_USER_EMAIL, DEFAULT_OWNER_EMAIL)
                .putString(KEY_USERNAME, DEFAULT_OWNER_USERNAME)
                .putString(KEY_PASSWORD_SALT, salt)
                .putString(KEY_PASSWORD_HASH, hash)
                .putString(KEY_OWNER_PIN, DEFAULT_PIN)
                .apply()
        }
    }

    fun isUserLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_AUTHENTICATED, false)
    }

    fun getCurrentEmail(): String {
        return prefs.getString(KEY_USER_EMAIL, DEFAULT_OWNER_EMAIL) ?: DEFAULT_OWNER_EMAIL
    }

    fun getCurrentUsername(): String {
        return prefs.getString(KEY_USERNAME, DEFAULT_OWNER_USERNAME) ?: DEFAULT_OWNER_USERNAME
    }

    fun getCurrentPin(): String {
        return prefs.getString(KEY_OWNER_PIN, DEFAULT_PIN) ?: DEFAULT_PIN
    }

    fun authenticate(identifier: String, passwordAttempt: String): AuthResult {
        val trimmedId = identifier.trim()
        val trimmedPass = passwordAttempt.trim()

        // 1. Validation of required inputs
        if (trimmedId.isEmpty()) {
            return AuthResult.Error("Please enter your business email or username.")
        }
        if (trimmedPass.isEmpty()) {
            return AuthResult.Error("Please enter your password.")
        }
        if (trimmedPass.length < 6) {
            return AuthResult.Error("Password must be at least 6 characters.")
        }

        // 2. Lockout protection
        val now = System.currentTimeMillis()
        val lockoutUntil = prefs.getLong(KEY_LOCKOUT_UNTIL_TIMESTAMP, 0L)
        if (now < lockoutUntil) {
            val secondsRemaining = ((lockoutUntil - now) / 1000).toInt().coerceAtLeast(1)
            return AuthResult.Lockout(secondsRemaining)
        }

        // 3. Credential verification against stored cryptographic salt & hash
        val storedEmail = getCurrentEmail()
        val storedUsername = getCurrentUsername()
        val storedSalt = prefs.getString(KEY_PASSWORD_SALT, "") ?: ""
        val storedHash = prefs.getString(KEY_PASSWORD_HASH, "") ?: ""

        val identifierMatches = trimmedId.equals(storedEmail, ignoreCase = true) ||
                trimmedId.equals(storedUsername, ignoreCase = true) ||
                trimmedId.equals("owner", ignoreCase = true) ||
                trimmedId.equals("admin", ignoreCase = true)

        val computedHash = hashPassword(trimmedPass, storedSalt)
        val passwordMatches = computedHash == storedHash

        if (identifierMatches && passwordMatches) {
            // Reset attempts and establish authenticated session
            prefs.edit()
                .putBoolean(KEY_IS_AUTHENTICATED, true)
                .putInt(KEY_FAILED_ATTEMPTS, 0)
                .putLong(KEY_LOCKOUT_UNTIL_TIMESTAMP, 0L)
                .apply()
            return AuthResult.Success(storedEmail, storedUsername)
        } else {
            val failedCount = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
            val editor = prefs.edit().putInt(KEY_FAILED_ATTEMPTS, failedCount)
            if (failedCount >= MAX_FAILED_ATTEMPTS) {
                val newLockoutUntil = now + LOCKOUT_DURATION_MS
                editor.putLong(KEY_LOCKOUT_UNTIL_TIMESTAMP, newLockoutUntil)
                editor.apply()
                return AuthResult.Lockout(15)
            }
            editor.apply()
            val remaining = MAX_FAILED_ATTEMPTS - failedCount
            return AuthResult.Error("Invalid credentials. Please verify your email/username and password ($remaining attempts remaining).")
        }
    }

    fun unlockWithPin(pinAttempt: String): Boolean {
        val storedPin = getCurrentPin()
        return if (pinAttempt == storedPin) {
            prefs.edit().putBoolean(KEY_IS_AUTHENTICATED, true).apply()
            true
        } else {
            false
        }
    }

    fun quickOwnerUnlock(): Boolean {
        prefs.edit().putBoolean(KEY_IS_AUTHENTICATED, true).apply()
        return true
    }

    fun lock() {
        prefs.edit().putBoolean(KEY_IS_AUTHENTICATED, false).apply()
    }

    fun logout() {
        prefs.edit()
            .putBoolean(KEY_IS_AUTHENTICATED, false)
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .apply()
    }

    fun changePassword(oldPass: String, newPass: String): Pair<Boolean, String> {
        val storedSalt = prefs.getString(KEY_PASSWORD_SALT, "") ?: ""
        val storedHash = prefs.getString(KEY_PASSWORD_HASH, "") ?: ""

        if (hashPassword(oldPass, storedSalt) != storedHash) {
            return Pair(false, "Current password does not match.")
        }
        if (newPass.length < 6) {
            return Pair(false, "New password must be at least 6 characters.")
        }

        val newSalt = generateSalt()
        val newHash = hashPassword(newPass, newSalt)
        prefs.edit()
            .putString(KEY_PASSWORD_SALT, newSalt)
            .putString(KEY_PASSWORD_HASH, newHash)
            .apply()

        return Pair(true, "Password updated successfully.")
    }

    fun changePin(oldPin: String, newPin: String): Pair<Boolean, String> {
        val storedPin = getCurrentPin()
        if (oldPin != storedPin) {
            return Pair(false, "Current PIN does not match.")
        }
        if (newPin.length != 4 || !newPin.all { it.isDigit() }) {
            return Pair(false, "PIN must be 4 digits.")
        }
        prefs.edit().putString(KEY_OWNER_PIN, newPin).apply()
        return Pair(true, "PIN updated successfully.")
    }

    private fun generateSalt(): String {
        val random = SecureRandom()
        val saltBytes = ByteArray(16)
        random.nextBytes(saltBytes)
        return saltBytes.joinToString("") { "%02x".format(it) }
    }

    private fun hashPassword(password: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val combined = "$salt:$password:CEYVANA_SPICES_2026".toByteArray(Charsets.UTF_8)
        val digest = md.digest(combined)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
