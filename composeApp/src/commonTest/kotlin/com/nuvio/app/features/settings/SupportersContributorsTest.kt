package com.nuvio.app.features.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SupportersContributorsTest {

    @Test
    fun `mergeContributorsWithForkOwner includes 2-sa first when list is empty`() {
        val merged = mergeContributorsWithForkOwner(emptyList())

        assertEquals(1, merged.size)
        val owner = merged.first()
        assertEquals("2-sa", owner.login)
        assertEquals("https://github.com/2-sa", owner.profileUrl)
        assertTrue(!owner.avatarUrl.isNullOrBlank())
    }

    @Test
    fun `parseContributorsPayload parses GitHub array and filters bots while pinning 2-sa first`() {
        val payload = """
            [
              {
                "login": "tapframe",
                "avatar_url": "https://avatars.githubusercontent.com/u/85391825?v=4",
                "html_url": "https://github.com/tapframe",
                "contributions": 2676,
                "type": "User"
              },
              {
                "login": "github-actions[bot]",
                "avatar_url": "https://avatars.githubusercontent.com/in/15368?v=4",
                "html_url": "https://github.com/apps/github-actions",
                "contributions": 42,
                "type": "Bot"
              },
              {
                "login": "2-sa",
                "avatar_url": "https://avatars.githubusercontent.com/u/46811082?v=4",
                "html_url": "https://github.com/2-sa",
                "contributions": 74,
                "type": "User"
              }
            ]
        """.trimIndent()

        val parsed = parseContributorsPayload(payload)
        val merged = mergeContributorsWithForkOwner(parsed)

        assertEquals(listOf("2-sa", "tapframe"), merged.map { it.login })
    }
}
