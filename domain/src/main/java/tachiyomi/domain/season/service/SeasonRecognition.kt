package tachiyomi.domain.season.service

object SeasonRecognition {

    private const val NUMBER_PATTERN = """([0-9]+)(\.[0-9]+)?(\.?[a-z]+)?"""

    private val basic = Regex("""(?<=\bs\.|\bs|season) *$NUMBER_PATTERN""")

    private val number = Regex(NUMBER_PATTERN)

    private val tagRegex = Regex("""^\[[^\]]+\]|\[[^\]]+\]\s*${'$'}|^\([^\)]+\)|\([^\)]+\)\s*${'$'}""")

    private val unwanted = Regex("""\b\d+p\b|\d+x\d+|Hi10|\(\d+\)""")

    private val unwantedWhiteSpace = Regex("""\s(?=extra|special|omake)""")

    fun parseSeasonNumber(animeTitle: String, seasonName: String, seasonNumber: Double? = null): Double {
        if (seasonNumber != null && (seasonNumber == -2.0 || seasonNumber > -1.0)) {
            return seasonNumber
        }

        var cleanSeasonName = seasonName.lowercase()
            .replace(animeTitle.lowercase(), "").trim()
            .replace(',', '.')
            .replace('-', '.')
            .replace(unwantedWhiteSpace, "")

        while (tagRegex.containsMatchIn(cleanSeasonName)) {
            cleanSeasonName = tagRegex.replace(cleanSeasonName, "")
        }

        val numberMatch = number.findAll(cleanSeasonName)

        when {
            numberMatch.none() -> {
                return seasonNumber ?: -1.0
            }
            numberMatch.count() > 1 -> {
                unwanted.replace(cleanSeasonName, "").let { name ->
                    basic.find(name)?.let { return getSeasonNumberFromMatch(it) }
                    number.find(name)?.let { return getSeasonNumberFromMatch(it) }
                }
            }
        }

        return getSeasonNumberFromMatch(numberMatch.first())
    }

    private fun getSeasonNumberFromMatch(match: MatchResult): Double {
        return match.let {
            val initial = it.groups[1]?.value?.toDouble()!!
            val subSeasonDecimal = it.groups[2]?.value
            val subSeasonAlpha = it.groups[3]?.value
            val addition = checkForDecimal(subSeasonDecimal, subSeasonAlpha)
            initial.plus(addition)
        }
    }

    private fun checkForDecimal(decimal: String?, alpha: String?): Double {
        if (!decimal.isNullOrEmpty()) {
            return decimal.toDouble()
        }

        if (!alpha.isNullOrEmpty()) {
            if (alpha.contains("extra")) {
                return 0.99
            }

            if (alpha.contains("omake")) {
                return 0.98
            }

            if (alpha.contains("special")) {
                return 0.97
            }

            val trimmedAlpha = alpha.trimStart('.')
            if (trimmedAlpha.length == 1) {
                return parseAlphaPostFix(trimmedAlpha[0])
            }
        }

        return 0.0
    }

    private fun parseAlphaPostFix(alpha: Char): Double {
        val number = alpha.code - ('a'.code - 1)
        if (number >= 10) return 0.0
        return number / 10.0
    }
}
