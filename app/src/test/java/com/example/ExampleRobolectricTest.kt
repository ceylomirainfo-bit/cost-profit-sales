package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.security.AuthManager
import com.example.ui.security.AuthResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Ceyvana Spices", appName)
  }

  @Test
  fun `test AuthManager initial state and login flow`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val authManager = AuthManager(context)

    // Ensure clean state
    authManager.logout()
    assertEquals(false, authManager.isUserLoggedIn())

    // Invalid credentials
    val invalidResult = authManager.authenticate("wrong@email.com", "wrongPass123")
    assertTrue(invalidResult is AuthResult.Error)
    assertEquals(false, authManager.isUserLoggedIn())

    // Empty fields validation
    val emptyResult = authManager.authenticate("", "")
    assertTrue(emptyResult is AuthResult.Error)

    // Valid credentials
    val validResult = authManager.authenticate("ceylomirainfo@gmail.com", "Ceyvana@2026")
    assertTrue(validResult is AuthResult.Success)
    assertEquals(true, authManager.isUserLoggedIn())

    // Lock and Logout
    authManager.lock()
    assertEquals(false, authManager.isUserLoggedIn())

    // PIN unlock
    val pinSuccess = authManager.unlockWithPin("1234")
    assertEquals(true, pinSuccess)
    assertEquals(true, authManager.isUserLoggedIn())

    // Logout
    authManager.logout()
    assertEquals(false, authManager.isUserLoggedIn())
  }
}

