package io.github.ghsssss.ithomeadkill.hook

import io.github.ghsssss.ithomeadkill.settings.ModuleSettings
import io.github.ghsssss.ithomeadkill.settings.Toggle
import org.junit.Assert.*
import org.junit.Test

class CardRulesTest {
    @Test fun disablingWebPromosRestoresConfirmedWebCardsWhileAdsStayEnabled() {
        val defaults = ModuleSettings.defaults("11111111-1111-4111-8111-111111111111")
        val settings = defaults.copy(toggles = defaults.toggles + (Toggle.WEB_PROMOS to false))

        assertFalse(CardRules.shouldHide(
            settings = settings,
            hasAdSignature = false,
            hasWebContainer = true,
            markers = listOf("tv_small_tags" to "广告"),
        ))
    }
    @Test fun confirmedWebPromotionUsesOnlyItsOwnToggle() {
        for (ads in listOf(false, true)) for (webPromos in listOf(false, true)) {
            for (adSignature in listOf(false, true)) {
                assertEquals("ads=$ads webPromos=$webPromos adSignature=$adSignature", webPromos,
                    CardRules.shouldHide(
                        settings(Toggle.AD_CARDS to ads, Toggle.WEB_PROMOS to webPromos),
                        adSignature, true, listOf("tv_small_tags" to "广告"),
                    ))
            }
        }
    }

    @Test fun nativeAdvertisementsRemainIndependentOfWebPromotionToggle() {
        for (ads in listOf(false, true)) for (webPromos in listOf(false, true)) {
            val settings = settings(Toggle.AD_CARDS to ads, Toggle.WEB_PROMOS to webPromos)
            assertEquals(ads, CardRules.shouldHide(settings, false, false, listOf("tv_small_tags" to "广告")))
            assertEquals(ads, CardRules.shouldHide(settings, true, false, emptyList()))
        }
    }

    @Test fun pinnedCardsUseOnlyPinnedToggle() {
        for (ads in listOf(false, true)) for (webPromos in listOf(false, true)) {
            for (pinned in listOf(false, true)) {
                val settings = settings(Toggle.AD_CARDS to ads, Toggle.WEB_PROMOS to webPromos,
                    Toggle.PINNED_CARDS to pinned)
                assertEquals(pinned, CardRules.shouldHide(settings, false, false, listOf("tv_small_tags" to "置顶")))
            }
        }
    }

    @Test fun unknownWebCardsAreNotHiddenWithoutRecognizedSignals() {
        for (ads in listOf(false, true)) for (webPromos in listOf(false, true)) {
            val settings = settings(Toggle.AD_CARDS to ads, Toggle.WEB_PROMOS to webPromos)
            assertFalse(CardRules.shouldHide(settings, false, true, emptyList()))
            assertFalse(CardRules.shouldHide(settings, false, true, listOf("tv_title" to "广告行业新闻")))
        }
    }

    @Test fun disabledMasterPassesRecognizedCardsThrough() {
        val settings = settings(Toggle.MASTER to false)
        assertFalse(CardRules.shouldHide(settings, true, true, listOf("tv_small_tags" to "广告")))
        assertFalse(CardRules.shouldHide(settings, true, false, emptyList()))
        assertFalse(CardRules.shouldHide(settings, false, false, listOf("tv_small_tags" to "广告")))
        assertFalse(CardRules.shouldHide(settings, false, false, listOf("tv_small_tags" to "置顶")))
    }

    @Test fun absentSettingsPassRecognizedCardsThrough() {
        assertFalse(CardRules.shouldHide(null, true, true, listOf("tv_small_tags" to "广告")))
        assertFalse(CardRules.shouldHide(null, true, false, emptyList()))
        assertFalse(CardRules.shouldHide(null, false, false, listOf("tv_small_tags" to "广告")))
        assertFalse(CardRules.shouldHide(null, false, false, listOf("tv_small_tags" to "置顶")))
    }

    private fun settings(vararg toggles: Pair<Toggle, Boolean>): ModuleSettings {
        val defaults = ModuleSettings.defaults("11111111-1111-4111-8111-111111111111")
        return defaults.copy(toggles = defaults.toggles + toggles.toMap())
    }

    @Test fun advertisementOnlyMatchesItsBadge() {
        assertEquals(CardKind.AD, CardRules.classify(listOf("tv_small_tags" to "广告")))
        assertNull(CardRules.classify(listOf("tv_title" to "广告")))
        assertNull(CardRules.classify(listOf("body" to "广告")))
        assertNull(CardRules.classify(listOf("tv_small_tags" to "广告公司的新产品")))
    }

    @Test fun normalTitleCannotRestoreACardWithAnAdvertisementBadge() {
        assertEquals(CardKind.AD, CardRules.classify(listOf(
            "tv_title" to "普通新闻标题", "tv_small_tags" to "广告"
        )))
        assertEquals(CardKind.AD, CardRules.classify(listOf(
            "tv_small_tags" to "广告", "tv_title" to "普通新闻标题"
        )))
    }

    @Test fun pinnedCardsRemainIndependentAndCalendarIsPassThrough() {
        assertEquals(CardKind.PINNED, CardRules.classify(listOf("tv_small_tags" to "置顶")))
        assertNull(CardRules.classify(listOf("tv_title" to "事件日历")))
        assertNull(CardRules.classify(listOf("tv_title" to "事件日历功能更新")))
        assertNull(CardRules.classify(listOf("tv_small_tags" to "事件日历")))
    }

    @Test fun unknownWebCardsRemainVisible() {
        assertTrue(CardRules.isConfirmedWebPromo(true, listOf("tv_small_tags" to "广告")))
        assertFalse(CardRules.isConfirmedWebPromo(true, emptyList()))
        assertFalse(CardRules.isConfirmedWebPromo(true, listOf("tv_title" to "广告行业新闻")))
        assertFalse(CardRules.isConfirmedWebPromo(false, listOf("tv_small_tags" to "广告")))
    }

    @Test fun recycledNewsCardWithNoMarkerIsNormal() {
        assertNull(CardRules.classify(listOf("tv_title" to "正常文章", "tv_small_tags" to "18:20")))
        assertNull(CardRules.classify(emptyList()))
    }
}
