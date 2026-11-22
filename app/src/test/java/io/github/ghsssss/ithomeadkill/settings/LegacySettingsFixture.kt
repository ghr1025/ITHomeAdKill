package io.github.ghsssss.ithomeadkill.settings

/** 显式旧格式夹具，不能由当前序列化器生成，以免把迁移缺陷一起复制进测试。 */
internal fun legacyPreferences(
    eventCalendar: Boolean = true,
    revision: Long = 41L,
): Map<String, Any> = linkedMapOf(
    "settings_schema" to 1,
    "settings_revision" to revision,
    "settings_writer_id" to "6ba7b810-9dad-41d1-80b4-00c04fd430c8",
    "auto_refresh_minutes" to 10,
    "master_enabled" to false,
    "hide_ad_cards" to false,
    "hide_pinned_cards" to true,
    "hide_event_calendar" to eventCalendar,
    "hide_web_promos" to false,
    "hide_home_carousel" to true,
    "hide_article_ai_summary" to true,
    "hide_article_rating" to true,
    "hide_article_recommendations" to true,
)

/** schema 2 的键和值显式冻结；新增 Toggle 不能悄悄扩展旧快照。 */
internal fun schemaTwoPreferences(revision: Long = 41L): Map<String, Any> = linkedMapOf(
    "settings_schema" to 2,
    "settings_revision" to revision,
    "settings_writer_id" to "6ba7b810-9dad-41d1-80b4-00c04fd430c8",
    "auto_refresh_minutes" to 10,
    "master_enabled" to false,
    "hide_ad_cards" to false,
    "hide_pinned_cards" to true,
    "hide_web_promos" to false,
    "hide_home_carousel" to true,
    "hide_article_ai_summary" to true,
    "hide_article_rating" to true,
    "hide_article_rewards" to true,
    "hide_article_recommendations" to true,
)

internal fun legacySettingsSnapshots(revision: Long = 41L): List<Map<String, Any>> = listOf(
    legacyPreferences(eventCalendar = false, revision = revision),
    legacyPreferences(eventCalendar = true, revision = revision),
    schemaTwoPreferences(revision = revision),
)
