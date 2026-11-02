package com.example

import com.example.data.local.WebAssistantConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The URL rules are what keep the AI assistant feeling like part of the app. */
class WebAssistantConfigTest {

    @Test
    fun `only https base urls are accepted`() {
        assertEquals(
            "https://chat.example.com/",
            WebAssistantConfig.normaliseBaseUrl("https://chat.example.com")
        )
        assertEquals(
            "https://chat.example.com/ai/",
            WebAssistantConfig.normaliseBaseUrl("https://chat.example.com/ai/")
        )
        assertEquals(
            WebAssistantConfig.DEFAULT_BASE_URL,
            WebAssistantConfig.normaliseBaseUrl("http://chat.example.com")
        )
        assertEquals(
            WebAssistantConfig.DEFAULT_BASE_URL,
            WebAssistantConfig.normaliseBaseUrl("not a url")
        )
        assertEquals(
            WebAssistantConfig.DEFAULT_BASE_URL,
            WebAssistantConfig.normaliseBaseUrl(null)
        )
    }

    @Test
    fun `a query string on a configured base url is discarded`() {
        assertEquals(
            "https://chat.example.com/",
            WebAssistantConfig.normaliseBaseUrl("https://chat.example.com/?old=1")
        )
    }

    @Test
    fun `the page url carries embed mode, theme and an encoded question`() {
        val url = WebAssistantConfig.pageUrl(
            "https://chat.example.com",
            darkTheme = true,
            initialPrompt = "Explain key questions & marks"
        )
        assertTrue(url.startsWith("https://chat.example.com/?"))
        assertTrue(url.contains("app=android"))
        assertTrue(url.contains("theme=dark"))
        assertTrue(url.contains("q=Explain"))
        assertFalse(url.contains(" & "))
        assertFalse(
            WebAssistantConfig.pageUrl(
                "https://chat.example.com",
                darkTheme = false
            ).contains("q=")
        )
    }

    @Test
    fun `navigation stays on the configured host over https`() {
        val base = "https://chat.example.com/ai/"
        assertTrue(
            WebAssistantConfig.isAllowedUrl(
                "https://chat.example.com/terms",
                base
            )
        )
        assertFalse(
            WebAssistantConfig.isAllowedUrl(
                "https://evil.example.com/terms",
                base
            )
        )
        assertFalse(
            WebAssistantConfig.isAllowedUrl(
                "http://chat.example.com/terms",
                base
            )
        )
        assertFalse(
            WebAssistantConfig.isAllowedUrl(
                "https://sub.chat.example.com/terms",
                base
            )
        )
        assertFalse(WebAssistantConfig.isAllowedUrl(null, base))
        assertFalse(WebAssistantConfig.isAllowedUrl("garbage", base))
    }

    @Test
    fun `identity script escapes names and never closes its own script`() {
        val script = WebAssistantConfig.identityScript(
            name = "A</script> \"Bro\"",
            userId = "42",
            darkTheme = false
        )
        assertTrue(script.contains("THESTUDENT_USER"))
        assertTrue(script.contains("__THESTUDENT_USER__"))
        assertTrue(script.contains("postMessage"))
        assertFalse(script.contains("</script>"))
        assertTrue(script.contains("\\u003c/script\\u003e"))
        assertTrue(script.contains("\\\"Bro\\\""))
        assertTrue(script.contains("\"42\""))
    }
}
