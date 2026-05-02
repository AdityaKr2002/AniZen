package tachiyomi.domain.anime.service

object SeasonRecognition {

    private const val NUMBER_PATTERN = """([0-9]+)(\.[0-9]+)?(\.?[a-z]+)?"""

    /**
     * All cases with s.xx, s xx, season xx, or sxx
     */
    private val basic = Regex("""(?<=\bs\.|\bs|season|chapter|arc|vol|volume) *$NUMBER_PATTERN""", RegexOption.IGNORE_CASE)

    /**
     * Ordinal support: 2nd season, 3rd season, etc.
     */
    private val ordinals = Regex("""(\d+)(?:st|nd|rd|th)\s+(?:season|part|cour|volume|arc|chapter)""", RegexOption.IGNORE_CASE)

    /**
     * Part support: Part 1, Part 2
     */
    private val parts = Regex("""(?<=\bpart) *$NUMBER_PATTERN""", RegexOption.IGNORE_CASE)

    /**
     * Format tags support
     */
    private val formatTags = Regex("""\b(OVA|OAV|ONA|Special|Movie|BD|Remux)\b""", RegexOption.IGNORE_CASE)

    /**
     * Example: Boku no Hero Academia 2 -R> 2
     */
    private val number = Regex(NUMBER_PATTERN)

    /**
     * Roman numeral support (Upgraded to XX)
     */
    private val romanNumerals = Regex("""\b(I|II|III|IV|V|VI|VII|VIII|IX|X|XI|XII|XIII|XIV|XV|XVI|XVII|XVIII|XIX|XX)\b(?:\s+|$)""", RegexOption.IGNORE_CASE)

    private val textOrdinalMap = mapOf(
        "first" to 1.0, "second" to 2.0, "third" to 3.0, "fourth" to 4.0, "fifth" to 5.0,
        "sixth" to 6.0, "seventh" to 7.0, "eighth" to 8.0, "ninth" to 9.0, "tenth" to 10.0
    )
    private val textOrdinals = Regex("""\b(${textOrdinalMap.keys.joinToString("|")})\b\s+(?:season|part|cour|volume|arc|chapter)""", RegexOption.IGNORE_CASE)

    /**
     * Regex to remove tags
     */
    private val tagRegex = Regex("""^\[[^\]]+\]|\[[^\]]+\]\s*${'$'}|^\([^\)]+\)|\([^\)]+\)\s*${'$'}""")

    /**
     * Regex used to remove unwanted qualities and year
     */
    private val unwanted = Regex("""\b\d+p\b|\d+x\d+|Hi10|\(\d+\)|BD|RE|Remux|Dual.Audio|Multi-Audio|Multi-Sub|x264|x265|HEVC|10bit""", RegexOption.IGNORE_CASE)

    private val unwantedWhiteSpace = Regex("""\s(?=extra|special|omake)""", RegexOption.IGNORE_CASE)

    private val romanMap = mapOf(
        "I" to 1.0, "II" to 2.0, "III" to 3.0, "IV" to 4.0, "V" to 5.0,
        "VI" to 6.0, "VII" to 7.0, "VIII" to 8.0, "IX" to 9.0, "X" to 10.0,
        "XI" to 11.0, "XII" to 12.0, "XIII" to 13.0, "XIV" to 14.0, "XV" to 15.0,
        "XVI" to 16.0, "XVII" to 17.0, "XVIII" to 18.0, "XIX" to 19.0, "XX" to 20.0
    )

    private val stopwords = setOf(
        "the", "of", "and", "in", "to", "for", "with", "is", "at", "from", "on", "by", "an", "as",
        "no", "wa", "wo", "ni", "ga", "de", "mo", "to", "da", "na", "ka"
    )

    fun getSignatureWords(title: String): Set<String> {
        return title.lowercase()
            .replace(Regex("""[^a-z0-9\s]"""), " ")
            .split(Regex("""\s+"""))
            .filter { it.length > 2 && it !in stopwords }
            .filter { !romanNumerals.matches(it) }
            .toSet()
    }

    fun diceCoefficient(s1: String, s2: String): Double {
        val str1 = s1.lowercase().replace(Regex("""\s+"""), "")
        val str2 = s2.lowercase().replace(Regex("""\s+"""), "")
        if (str1 == str2) return 1.0
        if (str1.length < 2 || str2.length < 2) return 0.0

        val set1 = str1.zipWithNext { a, b -> "$a$b" }.toSet()
        val set2 = str2.zipWithNext { a, b -> "$a$b" }.toSet()

        val intersection = set1.intersect(set2).size
        return 2.0 * intersection / (set1.size + set2.size)
    }

    private val negativeKeywords = Regex("""(?i)\b(?:Spin-off|Alternative|Anthology|Recap|Summary|MV|PV|Trailer|Promo|CM|Teaser|Live Action|Stage Play|Remake|Version|Collection|Dub|Sub|No\.?\s*1)\b""")

    fun isUnrelated(originalTitle: String, candidateTitle: String): Boolean {
        // Hard Block: If candidate has "No. 1" but original doesn't, it's garbage.
        if (candidateTitle.contains(Regex("""(?i)No\.?\s*1""")) && !originalTitle.contains(Regex("""(?i)No\.?\s*1"""))) return true
        return negativeKeywords.containsMatchIn(candidateTitle)
    }

    fun getWordSet(title: String): Set<String> {
        return title.lowercase()
            .replace(Regex("""[^a-z0-9\s]"""), " ")
            .split(Regex("""\s+"""))
            .filter { it.length > 1 }
            .toSet()
    }

    fun jaroWinklerSimilarity(s1: String, s2: String): Double {
        val str1 = s1.lowercase().trim()
        val str2 = s2.lowercase().trim()
        if (str1 == str2) return 1.0
        if (str1.isEmpty() || str2.isEmpty()) return 0.0

        val len1 = str1.length
        val len2 = str2.length
        val matchWindow = (Math.max(len1, len2) / 2) - 1
        val matches1 = BooleanArray(len1)
        val matches2 = BooleanArray(len2)

        var matches = 0
        for (i in 0 until len1) {
            val start = Math.max(0, i - matchWindow)
            val end = Math.min(i + matchWindow + 1, len2)
            for (j in start until end) {
                if (matches2[j]) continue
                if (str1[i] == str2[j]) {
                    matches1[i] = true
                    matches2[j] = true
                    matches++
                    break
                }
            }
        }

        if (matches == 0) return 0.0

        var transpositions = 0
        var k = 0
        for (i in 0 until len1) {
            if (!matches1[i]) continue
            while (!matches2[k]) k++
            if (str1[i] != str2[k]) transpositions++
            k++
        }

        val m = matches.toDouble()
        val jaro = (m / len1 + m / len2 + (m - transpositions / 2.0) / m) / 3.0
        
        // Winkler adjustment
        var prefix = 0
        for (i in 0 until Math.min(4, Math.min(len1, len2))) {
            if (str1[i] == str2[i]) prefix++ else break
        }
        
        return jaro + prefix * 0.1 * (1.0 - jaro)
    }

    fun getRootTitle(title: String): String {
        return title
            // Remove everything starting from explicit season/part keywords
            .replace(Regex("""(?i)\s*[:\-\–\—]?\s*(?:Season|S|Part|Cour|Vol|Volume|Chapter|Arc)\s*(?:\d+|I|II|III|IV|V|VI|VII|VIII|IX|X|XI|XII|XIII|XIV|XV|XVI|XVII|XVIII|XIX|XX).*"""), "")
            .replace(Regex("""(?i)\s*\d+(?:st|nd|rd|th)\s+(?:Season|Part|Cour|Volume|Arc|Chapter).*"""), "")
            .replace(Regex("""(?i)\s*(?:First|Second|Third|Fourth|Fifth|Sixth|Seventh|Eighth|Ninth|Tenth)\s+(?:Season|Part|Cour|Volume|Arc|Chapter).*"""), "")
            // Special handling for "Final" to avoid titles like "Final Fantasy"
            .replace(Regex("""(?i)\s+(?:Final\s+Season|Final\s+Part|The\s+Final\s+Season|The\s+Final\s+Part|Conclusion|Ending)$"""), "")
            // Remove common tags
            .replace(Regex("""(?i)\s+\(?(?:TV|OAV|OVA|ONA|Special|Movie|BD|Remux)\)?.*"""), "")
            .replace(Regex("""(?i)\s*\[(?:1080p|720p|480p|BD|DVD|Web|Eng-Sub|Softsubs)\]"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    fun parseSeasonNumber(animeTitle: String, seasonName: String, existingNumber: Double? = null): Double {
        if (existingNumber != null && (existingNumber == -2.0 || existingNumber > -1.0)) {
            return existingNumber
        }

        val rootTitle = getRootTitle(animeTitle)
        
        // 1. Clean name for matching
        var cleanSeasonName = seasonName.lowercase()
            .replace(Regex("""(?i)\s*\[(?:1080p|720p|480p|BD|DVD|Web|Eng-Sub|Softsubs)\]"""), "")
            .replace(Regex("""(?i)\s+\(?(?:TV|OAV|OVA|ONA|Special|Movie|BD|Remux)\)?.*"""), "")
            .trim()

        var matchingContext = cleanSeasonName
            .replace(rootTitle.lowercase(), "")
            .replace(',', '.')
            .replace('-', '.')
            .replace(unwantedWhiteSpace, "")
            .trim()

        while (tagRegex.containsMatchIn(matchingContext)) {
            matchingContext = tagRegex.replace(matchingContext, "")
        }
        
        matchingContext = matchingContext.replace(Regex("""\b\d{3,4}p?\b"""), "").trim()

        // 2. Try Explicit Detection
        ordinals.find(matchingContext)?.let { return it.groups[1]?.value?.toDoubleOrNull() ?: 1.0 }
        parts.find(matchingContext)?.let { return getSeasonNumberFromMatch(it) }
        textOrdinals.find(matchingContext)?.let { 
            val word = it.groups[1]?.value?.lowercase()
            return textOrdinalMap[word] ?: 1.0
        }
        romanNumerals.find(matchingContext)?.let {
            val roman = it.groups[1]?.value?.uppercase()
            return romanMap[roman] ?: -1.0
        }
        basic.find(matchingContext)?.let { return getSeasonNumberFromMatch(it) }

        // 3. Format tags
        if (cleanSeasonName.contains("movie", ignoreCase = true)) return -2.0
        if (cleanSeasonName.contains("ova", ignoreCase = true) || cleanSeasonName.contains("oav", ignoreCase = true)) return -3.0
        if (cleanSeasonName.contains("ona", ignoreCase = true)) return -4.0
        if (cleanSeasonName.contains("special", ignoreCase = true)) return -5.0
        
        // Final check anchored to "Season" or "Part"
        if (cleanSeasonName.contains(Regex("""(?i)final\s+(?:season|part|chapter)""")) || 
            cleanSeasonName.endsWith("conclusion", ignoreCase = true)) {
            return 99.0
        }

        // 4. Strict Identity Logic
        val fullOriginal = animeTitle.lowercase().replace(Regex("""[^a-z0-9]"""), "")
        val fullCandidate = seasonName.lowercase().replace(Regex("""[^a-z0-9]"""), "")
        
        if (fullOriginal == fullCandidate) {
            return 1.0
        }

        // Sequel check
        if (matchingContext.length > 1) {
            val numberInSubtitle = number.find(matchingContext)
            return if (numberInSubtitle != null) {
                getSeasonNumberFromMatch(numberInSubtitle)
            } else {
                2.0 
            }
        }

        return 1.0
    }

    private fun getSeasonNumberFromMatch(match: MatchResult): Double {
        return try {
            val initial = match.groups[1]?.value?.toDouble() ?: 0.0
            val subSeasonDecimal = match.groups[2]?.value
            val subSeasonAlpha = match.groups[3]?.value
            val addition = checkForDecimal(subSeasonDecimal, subSeasonAlpha)
            initial + addition
        } catch (e: Exception) {
            -1.0
        }
    }

    private fun checkForDecimal(decimal: String?, alpha: String?): Double {
        if (!decimal.isNullOrEmpty()) {
            return decimal.toDoubleOrNull() ?: 0.0
        }

        if (!alpha.isNullOrEmpty()) {
            val alphaLower = alpha.lowercase()
            return when {
                alphaLower.contains("extra") -> 0.99
                alphaLower.contains("omake") -> 0.98
                alphaLower.contains("special") -> 0.97
                else -> {
                    val trimmedAlpha = alphaLower.trimStart('.')
                    if (trimmedAlpha.length == 1) {
                        val num = trimmedAlpha[0].code - ('a'.code - 1)
                        if (num in 1..9) num / 10.0 else 0.0
                    } else 0.0
                }
            }
        }

        return 0.0
    }
}