package io.github.oin.titanpocketkeyboard

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmokeTest {

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun packagedClassIsImportable() {
        // Proves INFRA-01: VietnameseTextInput is importable from a packaged test class.
        // Compiles only if the default-package blocker is gone.
        val vti = VietnameseTextInput()
        assertNotNull(vti)
        assertTrue(vti.modifiableChars.contains('a'))
    }

    @Test
    fun mockkStaticLogWorks() {
        // Proves INFRA-02: MockK is on the classpath and functional.
        // Compiles and runs only if io.mockk:mockk is resolved as a testImplementation.
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        assertEquals(0, Log.d("SmokeTest", "hello"))
    }

    @Test
    fun runnerExecutesCoreLogic() {
        // Proves INFRA-03 (partial): core class executes under the JVM test runner.
        // processKey('a') on a fresh instance returns non-null — a bare vowel commits itself.
        assertNotNull(VietnameseTextInput().processKey('a'))
    }
}
