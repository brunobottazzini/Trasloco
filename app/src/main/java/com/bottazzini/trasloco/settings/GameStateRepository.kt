package com.bottazzini.trasloco.settings

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.LinkedList

class GameStateRepository(context: Context) {

    private val settingsHandler = SettingsHandler(context)

    fun hasSavedGame(): Boolean {
        val raw = settingsHandler.readValue(KEY) ?: return false
        return raw.isNotEmpty() && raw != SENTINEL_EMPTY
    }

    fun clear() {
        settingsHandler.updateSetting(KEY, SENTINEL_EMPTY)
    }

    fun save(state: GameStateSnapshot) {
        val json = JSONObject().apply {
            put("v", state.version)
            put("hasActiveGame", state.hasActiveGame)
            put("gameLost", state.gameLost)
            put("gameStartTimeMillis", state.gameStartTimeMillis)
            put("timerPausedTimeMillis", state.timerPausedTimeMillis)
            put("isTimerPaused", state.isTimerPaused)
            put("cardType", state.cardType)
            put("subDeckMap", mapToJson(state.subDeckMap.mapValues { ArrayList(it.value) }))
            put("cardTableMap", mapToJson(state.cardTableMap))
            put("coppiedSubDeckMap", mapToJson(state.coppiedSubDeckMap.mapValues { ArrayList(it.value) }))
            put("endDeckList", endDeckMapToJson(state.endDeckList))
            put("playList", playListMapToJson(state.playList))
        }
        settingsHandler.updateSetting(KEY, json.toString())
    }

    fun load(): GameStateSnapshot? {
        val raw = settingsHandler.readValue(KEY) ?: return null
        if (raw == SENTINEL_EMPTY || raw.isEmpty()) return null
        return try {
            val json = JSONObject(raw)
            val version = json.optInt("v", 0)
            if (version != CURRENT_VERSION) return null
            GameStateSnapshot(
                version = version,
                hasActiveGame = json.getBoolean("hasActiveGame"),
                gameLost = json.getBoolean("gameLost"),
                gameStartTimeMillis = json.getLong("gameStartTimeMillis"),
                timerPausedTimeMillis = json.getLong("timerPausedTimeMillis"),
                isTimerPaused = json.getBoolean("isTimerPaused"),
                cardType = json.getString("cardType"),
                subDeckMap = jsonToStringListMap(json.getJSONObject("subDeckMap")),
                cardTableMap = jsonToStringArrayListMap(json.getJSONObject("cardTableMap")),
                coppiedSubDeckMap = jsonToStringListMap(json.getJSONObject("coppiedSubDeckMap")),
                endDeckList = jsonToStringMap(json.getJSONObject("endDeckList")),
                playList = jsonToIntLinkedListMap(json.getJSONObject("playList"))
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun close() {
        settingsHandler.close()
    }

    companion object {
        const val KEY = "savedGameState"
        const val CURRENT_VERSION = 1
        private const val SENTINEL_EMPTY = "__empty__"

        private fun mapToJson(map: Map<String, List<String>>): JSONObject {
            val obj = JSONObject()
            map.forEach { (k, v) ->
                val arr = JSONArray()
                v.forEach { arr.put(it) }
                obj.put(k, arr)
            }
            return obj
        }

        private fun endDeckMapToJson(map: Map<String, String>): JSONObject {
            val obj = JSONObject()
            map.forEach { (k, v) -> obj.put(k, v) }
            return obj
        }

        private fun playListMapToJson(map: Map<String, LinkedList<Int>>): JSONObject {
            val obj = JSONObject()
            map.forEach { (k, list) ->
                val arr = JSONArray()
                list.forEach { arr.put(it) }
                obj.put(k, arr)
            }
            return obj
        }

        private fun jsonToStringListMap(obj: JSONObject): HashMap<String, List<String>> {
            val out = HashMap<String, List<String>>()
            obj.keys().forEach { key ->
                val arr = obj.getJSONArray(key)
                val list = ArrayList<String>(arr.length())
                for (i in 0 until arr.length()) list.add(arr.getString(i))
                out[key] = list
            }
            return out
        }

        private fun jsonToStringArrayListMap(obj: JSONObject): HashMap<String, ArrayList<String>> {
            val out = HashMap<String, ArrayList<String>>()
            obj.keys().forEach { key ->
                val arr = obj.getJSONArray(key)
                val list = ArrayList<String>(arr.length())
                for (i in 0 until arr.length()) list.add(arr.getString(i))
                out[key] = list
            }
            return out
        }

        private fun jsonToStringMap(obj: JSONObject): HashMap<String, String> {
            val out = HashMap<String, String>()
            obj.keys().forEach { key -> out[key] = obj.getString(key) }
            return out
        }

        private fun jsonToIntLinkedListMap(obj: JSONObject): HashMap<String, LinkedList<Int>> {
            val out = HashMap<String, LinkedList<Int>>()
            obj.keys().forEach { key ->
                val arr = obj.getJSONArray(key)
                val list = LinkedList<Int>()
                for (i in 0 until arr.length()) list.add(arr.getInt(i))
                out[key] = list
            }
            return out
        }
    }
}

data class GameStateSnapshot(
    val version: Int,
    val hasActiveGame: Boolean,
    val gameLost: Boolean,
    val gameStartTimeMillis: Long,
    val timerPausedTimeMillis: Long,
    val isTimerPaused: Boolean,
    val cardType: String,
    val subDeckMap: HashMap<String, List<String>>,
    val cardTableMap: HashMap<String, ArrayList<String>>,
    val coppiedSubDeckMap: HashMap<String, List<String>>,
    val endDeckList: HashMap<String, String>,
    val playList: HashMap<String, LinkedList<Int>>
)
