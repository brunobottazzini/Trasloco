package com.bottazzini.trasloco.utils

class TraslocoSolver {

    private val visited = HashSet<String>()
    private var deadline: Long = Long.MAX_VALUE

    fun isSolvable(deck: List<String>, timeBudgetMs: Long = 500): Boolean {
        require(deck.size == 40) { "Deck must contain 40 cards" }
        deadline = System.currentTimeMillis() + timeBudgetMs
        visited.clear()
        return dfs(initialState(deck))
    }

    private fun dfs(state: State): Boolean {
        if (System.currentTimeMillis() > deadline) return false
        if (state.isWon()) return true
        val key = state.encode()
        if (key in visited) return false
        visited.add(key)

        for (move in state.legalMoves()) {
            if (dfs(state.applyMove(move))) return true
        }
        return false
    }

    private fun initialState(deck: List<String>): State {
        val codes = IntArray(40) { cardCode(deck[it]) }
        val subDecks = Array(4) { line ->
            ArrayList<Int>(10).apply {
                for (i in 0..9) add(codes[line * 10 + i])
            }
        }
        val tableau = Array(12) { ArrayList<Int>(8) }
        for (line in 0..3) {
            for (pos in 0..2) {
                tableau[line * 3 + pos].add(subDecks[line].removeAt(0))
            }
        }
        return State(subDecks, tableau, IntArray(4) { -1 })
    }

    private class State(
        val subDecks: Array<ArrayList<Int>>,
        val tableau: Array<ArrayList<Int>>,
        val foundations: IntArray
    ) {
        fun isWon(): Boolean {
            for (f in foundations) {
                if (f == -1 || rank(f) != 10) return false
            }
            return true
        }

        fun encode(): String {
            val sb = StringBuilder(120)
            for (s in subDecks) {
                for (c in s) sb.append(c).append(',')
                sb.append('|')
            }
            for (t in tableau) {
                for (c in t) sb.append(c).append(',')
                sb.append(';')
            }
            // Foundations are interchangeable: sort to canonicalize equivalent states
            val sortedFnd = foundations.copyOf().apply { sort() }
            for (f in sortedFnd) sb.append(f).append(',')
            return sb.toString()
        }

        fun legalMoves(): List<Move> {
            val moves = ArrayList<Move>(24)

            // 1. Tableau-to-foundation (try first: monotonic progress, prunes search)
            for (tIdx in 0..11) {
                val stack = tableau[tIdx]
                if (stack.isEmpty()) continue
                val card = stack.last()
                for (fIdx in 0..3) {
                    val top = foundations[fIdx]
                    if (top == -1) {
                        if (rank(card) == 1) {
                            moves.add(Move(MoveType.T2F, tIdx, fIdx))
                            break // any empty foundation works for an Asso; one is enough
                        }
                    } else if (suit(card) == suit(top) && rank(card) == rank(top) + 1) {
                        moves.add(Move(MoveType.T2F, tIdx, fIdx))
                        break // foundation match is unique by suit
                    }
                }
            }

            // 2. Tableau-to-tableau
            for (srcIdx in 0..11) {
                val src = tableau[srcIdx]
                if (src.isEmpty()) continue
                val srcCard = src.last()
                for (dstIdx in 0..11) {
                    if (dstIdx == srcIdx) continue
                    val dst = tableau[dstIdx]
                    if (dst.isEmpty()) continue
                    val dstCard = dst.last()
                    if (suit(srcCard) == suit(dstCard) && rank(srcCard) == rank(dstCard) - 1) {
                        moves.add(Move(MoveType.T2T, srcIdx, dstIdx))
                    }
                }
            }

            // 3. Sub-deck deal: only one deal move per line (the next card goes to first empty pos)
            for (line in 0..3) {
                if (subDecks[line].isEmpty()) continue
                for (pos in 0..2) {
                    if (tableau[line * 3 + pos].isEmpty()) {
                        moves.add(Move(MoveType.DEAL, line, 0))
                        break
                    }
                }
            }

            return moves
        }

        fun applyMove(move: Move): State {
            val newSubDecks = Array(4) { ArrayList(subDecks[it]) }
            val newTableau = Array(12) { ArrayList(tableau[it]) }
            val newFoundations = foundations.copyOf()

            when (move.type) {
                MoveType.DEAL -> {
                    val line = move.a
                    val card = newSubDecks[line].removeAt(0)
                    for (pos in 0..2) {
                        val tIdx = line * 3 + pos
                        if (newTableau[tIdx].isEmpty()) {
                            newTableau[tIdx].add(card)
                            break
                        }
                    }
                }
                MoveType.T2F -> {
                    val card = newTableau[move.a].removeAt(newTableau[move.a].size - 1)
                    newFoundations[move.b] = card
                }
                MoveType.T2T -> {
                    val card = newTableau[move.a].removeAt(newTableau[move.a].size - 1)
                    newTableau[move.b].add(card)
                }
            }
            return State(newSubDecks, newTableau, newFoundations)
        }
    }

    private enum class MoveType { DEAL, T2F, T2T }
    private data class Move(val type: MoveType, val a: Int, val b: Int)

    companion object {
        // Card code: suit * 10 + (rank - 1). Suits: b=0, c=1, d=2, s=3. Rank: 1..10
        private fun cardCode(name: String): Int {
            val suitChar = name[0]
            val rank = name.substring(1).toInt()
            val suit = when (suitChar) {
                'b' -> 0
                'c' -> 1
                'd' -> 2
                's' -> 3
                else -> error("Unknown suit: $suitChar in $name")
            }
            return suit * 10 + (rank - 1)
        }

        private fun suit(code: Int) = code / 10
        private fun rank(code: Int) = (code % 10) + 1
    }
}
