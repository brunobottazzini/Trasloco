package com.bottazzini.trasloco

import androidx.lifecycle.ViewModel
import java.util.LinkedList

class GameViewModel : ViewModel() {
    var hasActiveGame: Boolean = false
    var gameLost: Boolean = false
    var gameStartTimeMillis: Long = 0L
    var timerPausedTimeMillis: Long = 0L
    var isTimerPaused: Boolean = false
    var coppiedSubDeckMap: HashMap<String, List<String>> = HashMap()
    var subDeckMap: HashMap<String, List<String>> = HashMap()
    var cardTableMap: HashMap<String, ArrayList<String>> = HashMap()
    var endDeckList: HashMap<String, String> = HashMap()
    var playList: HashMap<String, LinkedList<Int>> = HashMap()
    var selectedCard: String? = null
    var selectedPositionId: Int? = null
}
