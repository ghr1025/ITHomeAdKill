package io.github.ghsssss.ithomeadkill.settings

import android.content.Context
import org.json.JSONObject
import java.io.File

/** Reads only an app-private typed export, never a guessed legacy prefs path. */
internal object LegacySettingsExport {
    fun read(context: Context): Map<String, Any>? {
        val file = File(context.filesDir, "legacy-settings-export.json")
        if (!file.exists()) return null
        require(file.length() in 1..65_536) { "旧设置导出文件大小无效" }
        val payload = JSONObject(file.readText(Charsets.UTF_8))
        require(payload.getInt("format") == 1 &&
            payload.getString("source") == "legacy-official-sharedprefs") { "旧设置导出来源无效" }
        val entries = payload.getJSONObject("values")
        require(entries.length() in 1..64) { "旧设置导出字段数量无效" }
        val values = buildMap<String, Any> {
            entries.keys().forEach { key ->
                val entry = entries.getJSONObject(key)
                val raw = entry.get("value")
                val value: Any = when (entry.getString("type")) {
                    "boolean" -> raw as? Boolean ?: error("旧设置布尔值无效")
                    "int" -> (raw as? Int) ?: error("旧设置整数无效")
                    "long" -> when (raw) {
                        is Long -> raw
                        is Int -> raw.toLong()
                        else -> error("旧设置长整数无效")
                    }
                    "string" -> raw as? String ?: error("旧设置字符串无效")
                    else -> error("旧设置字段类型无效")
                }
                put(key, value)
            }
        }
        require(ModuleSettings.fromPreferences(values) != null) { "旧设置导出校验失败" }
        require(values[SettingsContract.SCHEMA_KEY] in listOf(1, 2)) { "只接受旧版设置导出" }
        return values
    }
}
