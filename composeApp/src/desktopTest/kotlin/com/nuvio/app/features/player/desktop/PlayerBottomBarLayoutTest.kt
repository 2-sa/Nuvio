package com.nuvio.app.features.player.desktop

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlayerBottomBarLayoutTest {
    private fun loadPlayerUiResource(name: String): String {
        val stream = checkNotNull(
            javaClass.classLoader.getResourceAsStream("player-ui/$name"),
        ) { "Missing desktop player UI resource: player-ui/$name" }
        return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    private fun assertInOrder(content: String, tokens: List<String>, context: String) {
        var cursor = -1
        for (token in tokens) {
            val idx = content.indexOf(token)
            assertTrue(idx >= 0, "$context: missing token `$token`")
            assertTrue(
                idx > cursor,
                "$context: expected `$token` after previous token (index $idx <= $cursor)",
            )
            cursor = idx
        }
    }

    @Test
    fun `seek row places position seekWrap and duration button in left-to-right order`() {
        val html = loadPlayerUiResource("controls.html")
        val seekRowStart = html.indexOf("""<div class="seek-row">""")
        val actionRowStart = html.indexOf("""<div class="action-row">""")
        assertTrue(seekRowStart >= 0, "Expected .seek-row container in controls.html")
        assertTrue(actionRowStart > seekRowStart, "Expected .action-row after .seek-row")

        val seekRowHtml = html.substring(seekRowStart, actionRowStart)
        assertInOrder(
            seekRowHtml,
            listOf(
                """class="time-pill time-position" id="position"""",
                """<div class="seek-wrap" id="seekWrap">""",
                """id="seekTooltip"""",
                """id="seek"""",
                """id="chapterMarks"""",
                """<button class="time-pill time-duration" id="duration" type="button"""",
            ),
            "seek-row",
        )
        assertFalse(html.contains("""class="time-row""""), "Legacy .time-row should be removed")
        assertFalse(html.contains("""class="playback-status""""), "Legacy .playback-status should be removed")
        assertFalse(html.contains("""id="timeLabel""""), "Legacy #timeLabel should be removed")
    }

    @Test
    fun `action row splits start and end groups in stremio order`() {
        val html = loadPlayerUiResource("controls.html")
        val startGroupIdx = html.indexOf("""<div class="action-group action-group-start">""")
        val endGroupIdx = html.indexOf("""<div class="action-group action-group-end">""")
        val progressEndIdx = html.indexOf("</section>", endGroupIdx)

        assertTrue(startGroupIdx >= 0, "Expected .action-group-start in controls.html")
        assertTrue(endGroupIdx > startGroupIdx, "Expected .action-group-end after .action-group-start")
        assertTrue(progressEndIdx > endGroupIdx, "Expected closing </section> for .progress")

        val startGroupHtml = html.substring(startGroupIdx, endGroupIdx)
        val endGroupHtml = html.substring(endGroupIdx, progressEndIdx)

        assertInOrder(
            startGroupHtml,
            listOf(
                """id="toggle"""",
                """id="nextEpisodeButton"""",
                """id="volumeControl"""",
                """id="volumeButton"""",
                """id="volumeSlider"""",
            ),
            "action-group-start",
        )

        assertInOrder(
            endGroupHtml,
            listOf(
                """data-command="streamInfo"""",
                """data-command="speed"""",
                """data-command="resize"""",
                """data-command="subtitles"""",
                """data-command="audio"""",
                """data-command="sources"""",
                """data-command="episodes"""",
                """data-command="chapters"""",
            ),
            "action-group-end",
        )
    }

    @Test
    fun `controls css uses stremio-scale desktop icons flat play button visible thumb and popover pointer events`() {
        val css = loadPlayerUiResource("controls.css")

        // Popover-open pointer-events allowlist
        val popoverBlockStart = css.indexOf(".player.popover-open .progress .seek-row")
        assertTrue(popoverBlockStart >= 0, "Expected .player.popover-open pointer-events for .seek-row")
        val popoverBlockEnd = css.indexOf("}", popoverBlockStart)
        val popoverBlock = css.substring(popoverBlockStart, popoverBlockEnd)
        assertTrue(popoverBlock.contains(".action-row"), "Popover pointer-events must include .action-row")
        assertTrue(popoverBlock.contains(".action-group"), "Popover pointer-events must include .action-group")
        assertTrue(popoverBlock.contains("pointer-events: auto"), "Popover rule must restore pointer-events: auto")

        // Visible circular scrubber thumb
        val thumbStart = css.indexOf(".scrub::-webkit-slider-thumb")
        assertTrue(thumbStart >= 0, "Expected .scrub::-webkit-slider-thumb rule")
        val thumbBlock = css.substring(thumbStart, css.indexOf("}", thumbStart))
        assertTrue(thumbBlock.contains("width: 16px"), "Scrub thumb should be 16px wide")
        assertTrue(thumbBlock.contains("height: 16px"), "Scrub thumb should be 16px tall")
        assertTrue(thumbBlock.contains("opacity: 1"), "Scrub thumb must be visible (opacity: 1)")

        // Desktop large icon dimensions & flat minimalist playback button
        val desktopMediaStart = css.indexOf("@media (min-width: 761px)")
        assertTrue(desktopMediaStart >= 0, "Expected @media (min-width: 761px) desktop rules")
        val desktopMediaBlock = css.substring(desktopMediaStart, css.indexOf(".modal-layer", desktopMediaStart))
        assertTrue(desktopMediaBlock.contains("width: 28px;"), "Desktop action icons should be 28px wide")
        assertTrue(desktopMediaBlock.contains("height: 28px;"), "Desktop action icons should be 28px tall")
        assertTrue(desktopMediaBlock.contains("width: 30px;"), "Desktop playback icon should be 30px wide")
        assertTrue(desktopMediaBlock.contains("height: 30px;"), "Desktop playback icon should be 30px tall")

        val playbackRuleIdx = desktopMediaBlock.indexOf(".action.playback-action,")
        assertTrue(playbackRuleIdx >= 0, "Expected desktop .action.playback-action rule")
        val playbackRuleBlock = desktopMediaBlock.substring(playbackRuleIdx, desktopMediaBlock.indexOf("}", playbackRuleIdx))
        assertTrue(
            playbackRuleBlock.contains("background: transparent;"),
            "Play button must be flat (background: transparent)",
        )
        assertFalse(
            playbackRuleBlock.contains("var(--theme-accent)"),
            "Play button must not use filled accent background",
        )
    }

    @Test
    fun `controls js replaces action-pill queries and wires duration remaining-time toggle`() {
        val js = loadPlayerUiResource("controls.js")

        assertFalse(js.contains(".action-pill"), "controls.js must have zero stale .action-pill references")
        assertTrue(
            js.contains("""document.querySelectorAll(".action-row .action")"""),
            "visibleActionButtons must query .action-row .action",
        )
        assertTrue(
            js.contains("""document.querySelectorAll(".action-row .action.focused")"""),
            "syncActionFocusState must query .action-row .action.focused",
        )
        assertTrue(
            js.contains("""durationLabel.addEventListener("click""""),
            "durationLabel must wire click handler for remaining-time toggle",
        )
        assertTrue(
            js.contains("timeLabelShowRemaining ? `-\${formatTime(remainingTimeMs)}` : formatTime(durationMs)"),
            "setProgress must toggle durationLabel between remaining and total time",
        )
        assertTrue(
            js.contains(".seek-row, .action-row, .action-group"),
            "isControlsSurfaceEvent must treat .seek-row, .action-row, and .action-group as controls surfaces",
        )
    }

    @Test
    fun `header places back chevron and title on start and keeps fullscreen clean on end`() {
        val html = loadPlayerUiResource("controls.html")
        val css = loadPlayerUiResource("controls.css")

        val headerStartIdx = html.indexOf("""<header class="header">""")
        val headerEndIdx = html.indexOf("</header>", headerStartIdx)
        assertTrue(headerStartIdx >= 0, "Expected <header class=\"header\"> in controls.html")
        assertTrue(headerEndIdx > headerStartIdx, "Expected closing </header> in controls.html")

        val headerHtml = html.substring(headerStartIdx, headerEndIdx)
        val headerActionsIdx = headerHtml.indexOf("""<nav class="header-actions"""")
        assertTrue(headerActionsIdx >= 0, "Expected <nav class=\"header-actions\"> inside <header>")

        val headerStartHtml = headerHtml.substring(0, headerActionsIdx)
        val headerActionsHtml = headerHtml.substring(headerActionsIdx)

        assertInOrder(
            headerStartHtml,
            listOf(
                """<div class="header-start">""",
                """id="backButton"""",
                """data-command="back"""",
                """id="headerTitle"""",
            ),
            "header-start",
        )

        assertTrue(headerActionsHtml.contains("""id="fullscreenButton""""), "header-actions must contain #fullscreenButton")
        assertFalse(headerActionsHtml.contains("""id="pipButton""""), "header-actions must not contain #pipButton")
        assertFalse(headerActionsHtml.contains("""id="partyButton""""), "header-actions must not contain #partyButton")
        assertFalse(headerActionsHtml.contains("""id="videoSettingsButton""""), "header-actions must not contain #videoSettingsButton")
        assertFalse(headerActionsHtml.contains("""id="submitIntroButton""""), "header-actions must not contain #submitIntroButton")
        assertFalse(headerActionsHtml.contains("#icon-close"), "header-actions must not contain #icon-close")

        val endGroupIdx = html.indexOf("""<div class="action-group action-group-end">""")
        val progressEndIdx = html.indexOf("</section>", endGroupIdx)
        assertTrue(endGroupIdx >= 0 && progressEndIdx > endGroupIdx, "Expected .action-group-end container")
        val endGroupHtml = html.substring(endGroupIdx, progressEndIdx)

        assertTrue(endGroupHtml.contains("""id="partyButton""""), "action-group-end must contain #partyButton")
        assertTrue(endGroupHtml.contains("""id="pipButton""""), "action-group-end must contain #pipButton")
        assertTrue(endGroupHtml.contains("""id="videoSettingsButton""""), "action-group-end must contain #videoSettingsButton")
        assertTrue(endGroupHtml.contains("""id="submitIntroButton""""), "action-group-end must contain #submitIntroButton")

        val headerCssIdx = css.indexOf(".header {")
        assertTrue(headerCssIdx >= 0, "Expected .header CSS rule in controls.css")
        val headerCssBlock = css.substring(headerCssIdx, css.indexOf("}", headerCssIdx))
        assertTrue(
            headerCssBlock.contains("justify-content: space-between"),
            ".header CSS rule must use justify-content: space-between",
        )

        val backButtonIdx = css.indexOf(".header-back-button {", css.indexOf(".header-button svg"))
        assertTrue(backButtonIdx >= 0, "Expected .header-back-button rule after .header-button")
        val backButtonBlock = css.substring(backButtonIdx, css.indexOf("}", backButtonIdx))
        assertTrue(backButtonBlock.contains("width: 48px"), "Back button must have 48px hitbox width")
        assertTrue(backButtonBlock.contains("height: 48px"), "Back button must have 48px hitbox height")
        assertTrue(backButtonBlock.contains("border-radius: 10px"), "Back button must use rounded-square radius")
        assertTrue(backButtonBlock.contains("background: transparent"), "Back button must have transparent default background")
    }
}
