package io.github.ghsssss.ithomeadkill.hook

import io.github.ghsssss.ithomeadkill.settings.ModuleSettings
import io.github.ghsssss.ithomeadkill.settings.Toggle

enum class CardKind { AD, PINNED }

object CardRules {
    fun shouldHide(
        settings: ModuleSettings?,
        hasAdSignature: Boolean,
        hasWebContainer: Boolean,
        markers: List<Pair<String, String>>,
    ): Boolean {
        fun enabled(toggle: Toggle) = settings?.enabled(toggle) == true
        // Confirmed web promotions belong only to their own toggle, even when
        // their ad badge also matches the generic advertisement classifier.
        if (isConfirmedWebPromo(hasWebContainer, markers)) return enabled(Toggle.WEB_PROMOS)
        return (hasAdSignature && enabled(Toggle.AD_CARDS)) || when (classify(markers)) {
            CardKind.AD -> enabled(Toggle.AD_CARDS)
            CardKind.PINNED -> enabled(Toggle.PINNED_CARDS)
            null -> false
        }
    }

    fun isConfirmedWebPromo(hasWebContainer: Boolean, markers: List<Pair<String, String>>): Boolean =
        hasWebContainer && markers.any { (id, text) -> id == "tv_small_tags" && text.trim() == "广告" }

    fun classify(markers: List<Pair<String, String>>): CardKind? {
        // Decide once for the whole card. A later normal title must not undo
        // the decision made by an advertisement badge on the same recycled row.
        if (markers.any { (id, text) -> id == "tv_small_tags" && text.trim() == "广告" }) {
            return CardKind.AD
        }
        if (markers.any { (id, text) -> id == "tv_small_tags" && text.trim() == "置顶" }) {
            return CardKind.PINNED
        }
        return null
    }
}
