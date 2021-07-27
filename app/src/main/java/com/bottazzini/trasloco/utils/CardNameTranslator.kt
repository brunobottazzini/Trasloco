package com.bottazzini.trasloco.utils

class CardNameTranslator {

    companion object {
        private val semiMapper =
            mapOf("b" to "bastoni", "s" to "spade", "c" to "coppe", "d" to "denari")

        private val numeroMapper = mapOf("8" to "Fante", "9" to "Cavallo", "10" to "Re")

        fun translate(name: String): String {
            val semi = name.substring(0, 1)
            val number = if (numeroMapper.containsKey(name.substring(1, name.length))) {
                numeroMapper[name.substring(1, name.length)]
            } else {
                name.substring(1, name.length)
            }

            val fullSemiName = semiMapper[semi]
            return "$number di $fullSemiName"
        }
    }
}