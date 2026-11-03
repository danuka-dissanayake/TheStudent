package com.example

import com.example.data.local.AssistantHandshake
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLDecoder

/** The SSO handshake must be deterministic, single-use-shaped and PKCE-correct. */
class AssistantHandshakeTest {

    @Test
    fun `verifier and state are high entropy and distinct`() {
        val verifier = AssistantHandshake.newVerifier()
        val state = AssistantHandshake.newState()
        assertEquals(48, verifier.length)
        assertEquals(32, state.length)
        assertNotEquals(verifier, AssistantHandshake.newVerifier())
        assertNotEquals(state, AssistantHandshake.newState())
        assertTrue(verifier.all { it.isLetterOrDigit() || it == '-' || it == '.' || it == '_' || it == '~' })
        assertTrue(state.all { it.isLetterOrDigit() || it == '-' || it == '.' || it == '_' || it == '~' })
    }

    @Test
    fun `challenge is unpadded base64url sha256 of the verifier - RFC 7636 vector`() {
        // RFC 7636 Appendix B known-answer test.
        val verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
        assertEquals(
            "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
            AssistantHandshake.challengeFor(verifier)
        )
    }

    @Test
    fun `challenge never contains padding or url-unsafe characters`() {
        repeat(25) {
            val challenge = AssistantHandshake.challengeFor(AssistantHandshake.newVerifier())
            assertEquals(43, challenge.length)
            assertFalse(challenge.contains("="))
            assertFalse(challenge.contains("+"))
            assertFalse(challenge.contains("/"))
        }
    }

    @Test
    fun `form body carries the code but no token is placed in a url`() {
        val body = String(
            AssistantHandshake.formBody(
                code = "a1b2c3",
                verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk",
                state = "state_1234567890abcd",
                darkTheme = true
            ),
            Charsets.UTF_8
        )
        val params = body.split("&").associate {
            val (key, value) = it.split("=", limit = 2)
            key to URLDecoder.decode(value, "UTF-8")
        }
        assertEquals("a1b2c3", params["code"])
        assertEquals("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk", params["code_verifier"])
        assertEquals("state_1234567890abcd", params["state"])
        assertEquals("android", params["app"])
        assertEquals("dark", params["theme"])
        // No Bearer token, password or email is ever part of the delivery.
        assertFalse(body.contains("Bearer"))
        assertFalse(body.contains("password"))
        assertFalse(body.contains("@"))
    }

    @Test
    fun `only allow-listed bridge events are accepted`() {
        assertTrue(AssistantHandshake.isAllowedEvent("SESSION_EXPIRED"))
        assertTrue(AssistantHandshake.isAllowedEvent("SESSION_READY"))
        assertTrue(AssistantHandshake.isAllowedEvent("NEW_CHAT"))
        assertTrue(AssistantHandshake.isAllowedEvent(AssistantHandshake.EVENT_CHAT_HISTORY))
        // App -> web events are never accepted as incoming messages.
        assertFalse(AssistantHandshake.isAllowedEvent(AssistantHandshake.EVENT_REQUEST_HISTORY))
        assertFalse(AssistantHandshake.isAllowedEvent(AssistantHandshake.EVENT_OPEN_CHAT))
        assertFalse(AssistantHandshake.isAllowedEvent("GET_TOKEN"))
        assertFalse(AssistantHandshake.isAllowedEvent("SET_SESSION"))
        assertFalse(AssistantHandshake.isAllowedEvent(""))
        assertFalse(AssistantHandshake.isAllowedEvent(null))
    }

    @Test
    fun `chat history events carry no credential names`() {
        assertEquals("CHAT_HISTORY", AssistantHandshake.EVENT_CHAT_HISTORY)
        assertEquals("REQUEST_HISTORY", AssistantHandshake.EVENT_REQUEST_HISTORY)
        assertEquals("OPEN_CHAT", AssistantHandshake.EVENT_OPEN_CHAT)
        assertTrue(AssistantHandshake.ALLOWED_EVENTS.none { it.contains("TOKEN") })
        assertTrue(AssistantHandshake.ALLOWED_EVENTS.none { it.contains("PASSWORD") })
    }

    @Test
    fun `the bridge object name is fixed and credential events do not exist`() {
        assertEquals("TheStudentBridge", AssistantHandshake.BRIDGE_OBJECT)
        assertTrue(AssistantHandshake.ALLOWED_EVENTS.none { it.contains("TOKEN") })
        assertTrue(AssistantHandshake.ALLOWED_EVENTS.none { it.contains("PASSWORD") })
        assertEquals(4_096, AssistantHandshake.MAX_BRIDGE_MESSAGE)
    }
}
