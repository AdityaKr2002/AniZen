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
    private val ordinals = Regex("""(\d+)(?:st|nd|rd|th)\s+(?:season|part|cour|volume|arc|chapter|year|semester)""", RegexOption.IGNORE_CASE)

    /**
     * Part support: Part 1, Part 2
     */
    private val parts = Regex("""(?<=\bpart|semester|cour) *$NUMBER_PATTERN""", RegexOption.IGNORE_CASE)

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
            .filter { it.length >= 2 && it !in stopwords }
            .filter { !romanNumerals.matches(it) }
            .toSet()
    }

    /**
     * Sorts words alphabetically and compares. 
     * Handles "Attack on Titan" vs "Titan, Attack on"
     */
    fun tokenSortSimilarity(s1: String, s2: String): Double {
        val sig1 = getSignatureWords(s1).sorted().joinToString("")
        val sig2 = getSignatureWords(s2).sorted().joinToString("")
        if (sig1.isEmpty() || sig2.isEmpty()) return 0.0
        
        return diceCoefficient(sig1, sig2)
    }

    /**
     * Checks if one title is an acronym of the other (e.g. "MT" vs "Mushoku Tensei")
     */
    fun isAcronymMatch(original: String, candidate: String): Boolean {
        val (short, long) = if (original.length < candidate.length) original to candidate else candidate to original
        if (short.length < 2 || short.any { it.isWhitespace() }) return false
        
        val acronym = getSignatureWords(long)
            .sortedBy { long.indexOf(it) } // Keep original order
            .mapNotNull { it.firstOrNull() }
            .joinToString("")
            .lowercase()
            
        return acronym.contains(short.lowercase())
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

    private val negativeKeywords = Regex("""(?i)\b(?:Spin-off|Alternative|Anthology|Recap|Summary|MV|PV|Trailer|Promo|CM|Teaser|Live Action|Stage Play|Remake|Version|Collection|Dub|Sub|No\.?\s*1|Preview)\b""")

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
            // 1. Remove common tags and extra info first (e.g. "(TV)", "[BD]")
            .replace(Regex("""(?i)\s+\(?(?:TV|OAV|OVA|ONA|Special|Movie|BD|Remux)\)?.*"""), "")
            .replace(Regex("""(?i)\s*\[(?:1080p|720p|480p|BD|DVD|Web|Eng-Sub|Softsubs)\]"""), "")
            // 2. Remove explicit season/part keywords and everything after them
            .replace(Regex("""(?i)\s*[:\-\–\—]?\s*(?:Season|S|Part|Cour|Vol|Volume|Chapter|Arc|Year|Semester)\s*(?:\d+|I|II|III|IV|V|VI|VII|VIII|IX|X|XI|XII|XIII|XIV|XV|XVI|XVII|XVIII|XIX|XX).*"""), "")
            .replace(Regex("""(?i)\s*\d+(?:st|nd|rd|th)\s+(?:Season|Part|Cour|Volume|Arc|Chapter|Year|Semester).*"""), "")
            .replace(Regex("""(?i)\s*(?:First|Second|Third|Fourth|Fifth|Sixth|Seventh|Eighth|Ninth|Tenth)\s+(?:Season|Part|Cour|Volume|Arc|Chapter|Year|Semester).*"""), "")
            // 3. Remove Roman numeral + Subtitle combos (e.g. "Mushoku Tensei II: Jobless...")
            .replace(Regex("""\s+(?:II|III|IV|V|VI|VII|VIII|IX|X|XI|XII|XIII|XIV|XV|XVI|XVII|XVIII|XIX|XX)\s*[:\-\–\—].*""", RegexOption.IGNORE_CASE), "")
            // 4. Remove bare numbers (2-10) or Roman numerals (II-XX) at the very end
            .replace(Regex("""\s+(?:[2-9]|10|II|III|IV|V|VI|VII|VIII|IX|X|XI|XII|XIII|XIV|XV|XVI|XVII|XVIII|XIX|XX)$""", RegexOption.IGNORE_CASE), "")
            // 5. Special handling for "Final" to avoid titles like "Final Fantasy"
            .replace(Regex("""(?i)\s+(?:Final\s+Season|Final\s+Part|The\s+Final\s+Season|The\s+Final\s+Part|Conclusion|Ending)$"""), "")
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

        // 2. Dual-Layer Detection (Season + Part)
        var season: Double? = null
        var part: Double? = null

        // Try to find Season
        ordinals.find(matchingContext)?.let { 
            val matchedText = it.value.lowercase()
            when {
                matchedText.contains("season") || matchedText.contains("year") -> {
                    season = it.groups[1]?.value?.toDoubleOrNull()
                }
                matchedText.contains("part") || matchedText.contains("cour") || matchedText.contains("semester") -> {
                    part = it.groups[1]?.value?.toDoubleOrNull()
                }
            }
        }

        if (season == null) {
            basic.find(matchingContext)?.let { 
                season = it.groups[1]?.value?.toDoubleOrNull()
            }
        }

        // Try to find Part if not already found via ordinals
        if (part == null) {
            parts.find(matchingContext)?.let {
                part = it.groups[1]?.value?.toDoubleOrNull()
            }
        }

        // Roman Numerals fallback for Season
        if (season == null) {
            romanNumerals.find(matchingContext)?.let {
                val roman = it.groups[1]?.value?.uppercase()
                season = romanMap[roman]
            }
        }

        // Handle text ordinals
        if (season == null || part == null) {
            textOrdinals.findAll(matchingContext).forEach { match ->
                val word = match.groups[1]?.value?.lowercase()
                val value = textOrdinalMap[word] ?: 1.0
                val matchedText = match.value.lowercase()
                if (matchedText.contains("season") || matchedText.contains("year")) {
                    if (season == null) season = value
                } else {
                    if (part == null) part = value
                }
            }
        }

        // Combine Season and Part
        if (season != null || part != null) {
            val s = season ?: 1.0 // Default to Season 1 if only Part is found
            val p = (part ?: 0.0) / 100.0
            return s + p
        }

        // 3. Format tags
        if (cleanSeasonName.contains(Regex("""\b(?:movie|film|theatrical)\b""", RegexOption.IGNORE_CASE))) return -2.0
        if (cleanSeasonName.contains(Regex("""\b(?:ova|oav)\b""", RegexOption.IGNORE_CASE))) return -3.0
        if (cleanSeasonName.contains(Regex("""\b(?:ona)\b""", RegexOption.IGNORE_CASE))) return -4.0
        if (cleanSeasonName.contains(Regex("""\b(?:special|omake|extra|recap|summary|reawakening|re-awakening|preview)\b""", RegexOption.IGNORE_CASE))) return -5.0
        
        // Final check anchored to "Season" or "Part"
        if (cleanSeasonName.contains(Regex("""(?i)final\s+(?:season|part|chapter)""")) || 
            cleanSeasonName.endsWith("conclusion", ignoreCase = true)) {
            return 99.0
        }

        // 4. Strict Identity Logic
        val fullRoot = rootTitle.lowercase().replace(Regex("""[^a-z0-9]"""), "")
        val fullCandidate = seasonName.lowercase().replace(Regex("""[^a-z0-9]"""), "")
        
        if (fullRoot == fullCandidate) {
            return 1.0
        }

        // 5. Number Extraction from context
        if (matchingContext.length > 1) {
            val numberInSubtitle = number.find(matchingContext)
            if (numberInSubtitle != null) {
                return getSeasonNumberFromMatch(numberInSubtitle)
            }
        }

        // 6. No number found? It's a related Special/Side-story, not Season 2.
        return -5.0
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