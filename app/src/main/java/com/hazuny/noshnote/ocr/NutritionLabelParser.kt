package com.hazuny.noshnote.ocr

/** Values read from a nutrition label. They retain the label's serving basis. */
data class NutritionLabelValues(
    val caloriesKcal: Double,
    val proteinG: Double,
    val basisDescription: String?,
)

data class NutritionLabelCandidates(
    val caloriesKcal: Double?,
    val proteinG: Double?,
    val basisDescription: String?,
)

object NutritionLabelParser {
    private data class Candidate(val value: Double, val score: Int, val hasLabel: Boolean, val hasUnit: Boolean)

    private val caloriesLabel = Regex("열량|칼로리|에너지|calories?|energy", RegexOption.IGNORE_CASE)
    private val proteinLabel = Regex("단백질|protein", RegexOption.IGNORE_CASE)
    private val numberPattern = "(?:[0-9]+(?:[.,][0-9]+)*|[.,][0-9]+)"
    private val number = Regex("($numberPattern)", RegexOption.IGNORE_CASE)
    private val standaloneNumber = Regex(numberPattern)
    private val basisLabel = Regex(
        "1회제공량|1회분량|총내용량|100g당|100ml당|영양정보기준|servingsize|servingspercontainer|per100g|per100ml",
        RegexOption.IGNORE_CASE,
    )
    private val dailyTerms = Regex("1일|일일|하루|매일|daily|perday", RegexOption.IGNORE_CASE)
    private val nutritionTerms = Regex("영양성분|영양소|nutritionfacts|nutritioninformation", RegexOption.IGNORE_CASE)
    private val referenceTerms = Regex("기준치|기준량|기준|권장|dailyvalue|referenceintake", RegexOption.IGNORE_CASE)
    private val ratioTerms = Regex("비율|퍼센트|%", RegexOption.IGNORE_CASE)
    private val nutritionContext = Regex("영양정보|영양성분|총내용량|1회제공량|100g당|100ml당|nutritionfacts|servingsize|per100g|per100ml", RegexOption.IGNORE_CASE)
    private val servingBasisContext = Regex("총내용량|1회제공량|1회분량|100g당|100ml당|servingsize|servingspercontainer|per100g|per100ml", RegexOption.IGNORE_CASE)
    private val whitespace = Regex("\\s+")

    fun parse(recognizedText: String): NutritionLabelValues? {
        val candidates = parseCandidates(recognizedText)
        val calories = candidates.caloriesKcal ?: return null
        val protein = candidates.proteinG ?: return null
        return NutritionLabelValues(calories, protein, candidates.basisDescription)
    }

    fun parseCandidates(recognizedText: String): NutritionLabelCandidates {
        val lines = normalizedLines(recognizedText)

        val calories = findBestCandidate(
            lines = lines,
            label = caloriesLabel,
            unit = "(?:kcal|㎉)",
            acceptedRange = 0.0..100_000.0,
            minimumScore = 50,
            penalizeDailyReference = true,
            requireLabelAndUnit = false,
        )
        val protein = findBestCandidate(
            lines = lines,
            label = proteinLabel,
            unit = "(?:g|그램)",
            acceptedRange = 0.0..1_000.0,
            minimumScore = 180,
            penalizeDailyReference = false,
            requireLabelAndUnit = true,
        )
        val basis = recognizedText.lineSequence()
            .map(String::trim)
            .firstOrNull { basisLabel.containsMatchIn(it.replace(whitespace, "")) }
            ?.take(60)

        return NutritionLabelCandidates(calories, protein, basis)
    }

    private fun normalizedLines(text: String): List<String> = text.lineSequence()
        .map { it.replace(whitespace, "").trim() }
        .filter(String::isNotEmpty)
        .toList()

    private fun findBestCandidate(
        lines: List<String>,
        label: Regex,
        unit: String,
        acceptedRange: ClosedFloatingPointRange<Double>,
        minimumScore: Int,
        penalizeDailyReference: Boolean,
        requireLabelAndUnit: Boolean,
    ): Double? {
        val candidates = mutableListOf<Candidate>()
        val valueWithUnit = Regex("($numberPattern)($unit)", RegexOption.IGNORE_CASE)
        val standaloneUnit = Regex(unit, RegexOption.IGNORE_CASE)

        fun add(raw: String?, lineIndex: Int, hasLabel: Boolean, hasUnit: Boolean) {
            val value = raw?.let(::parseNumber)?.takeIf { it in acceptedRange } ?: return
            var score = (if (hasLabel) 180 else 0) + (if (hasUnit) 60 else 0)
            val lineText = lines[lineIndex]
            if (nutritionContext.containsMatchIn(lineText)) score += 15
            val hasServingBasis = servingBasisContext.containsMatchIn(lineText)
            if (hasServingBasis) score += 60
            // A label on a neighboring table row must not make an unrelated number look labeled.
            // Nutrition reference text often contains a plausible-looking "2,000 kcal" value.
            // Score the whole nearby sentence strongly down, even if another row's label is close.
            if (penalizeDailyReference && hasUnit && !hasServingBasis && hasDailyReferenceContext(lines, lineIndex)) {
                score -= 180
            }
            candidates += Candidate(value, score, hasLabel, hasUnit)
        }

        lines.forEachIndexed { index, line ->
            val labelMatch = label.find(line)
            if (labelMatch != null) {
                val tail = line.substring(labelMatch.range.last + 1)
                val firstNumber = number.find(tail)
                val numberDistance = firstNumber?.range?.first
                // Only bind a value to this label when it appears immediately after it.
                // OCR can merge several nutrition rows into one line; searching the whole
                // tail would otherwise attach a later nutrient's value to protein.
                if (firstNumber != null && numberDistance != null && numberDistance <= 8) {
                    val matchingUnit = Regex(
                        "^${Regex.escape(firstNumber.value)}$unit",
                        RegexOption.IGNORE_CASE,
                    ).containsMatchIn(tail.substring(firstNumber.range.first))
                    add(firstNumber.value, index, hasLabel = true, hasUnit = matchingUnit)
                } else if (firstNumber == null) {
                    lines.getOrNull(index + 1)?.let { nextLine ->
                        val nextUnitMatch = valueWithUnit.find(nextLine)?.takeIf { it.range.first == 0 }
                        val nextNumberMatch = number.find(nextLine)?.takeIf { it.range.first == 0 }
                        val nextRaw = nextUnitMatch?.groupValues?.get(1)
                            ?: nextNumberMatch?.groupValues?.get(1)
                        add(
                            nextRaw,
                            index + 1,
                            hasLabel = true,
                            hasUnit = nextUnitMatch != null,
                        )
                    }
                }
            }

            valueWithUnit.findAll(line).forEach { match ->
                val prefix = line.substring(0, match.range.first)
                val lastLabel = label.findAll(prefix).lastOrNull()
                val labelDistance = lastLabel?.let { match.range.first - it.range.last - 1 }
                val directlyFollowsLabel = labelDistance != null && labelDistance in 0..8
                add(match.groupValues[1], index, hasLabel = directlyFollowsLabel, hasUnit = true)
            }

            if (line.matches(standaloneUnit)) {
                lines.getOrNull(index - 1)?.takeIf(standaloneNumber::matches)?.let { previous ->
                    add(previous, index - 1, hasLabel = label.containsMatchIn(lines[index - 1]), hasUnit = true)
                }
            }
            if (standaloneNumber.matches(line) && lines.getOrNull(index + 1)?.matches(standaloneUnit) == true) {
                add(line, index, hasLabel = label.containsMatchIn(line), hasUnit = true)
            }
        }

        return candidates
            .filter {
                it.score >= minimumScore &&
                    (!requireLabelAndUnit || (it.hasLabel && it.hasUnit))
            }
            .maxByOrNull(Candidate::score)
            ?.value
    }

    private fun hasDailyReferenceContext(lines: List<String>, index: Int): Boolean {
        val start = (index - 1).coerceAtLeast(0)
        val end = (index + 1).coerceAtMost(lines.lastIndex)
        val nearbyText = lines.subList(start, end + 1).joinToString("")
        val hasDay = dailyTerms.containsMatchIn(nearbyText)
        val hasNutrition = nutritionTerms.containsMatchIn(nearbyText)
        val hasReference = referenceTerms.containsMatchIn(nearbyText)
        val hasRatio = ratioTerms.containsMatchIn(nearbyText)
        return (hasDay && (hasNutrition || hasRatio)) || (hasNutrition && hasReference && hasRatio)
    }

    private fun findNumber(text: String, expectedUnit: String): String? {
        val withUnit = Regex("($numberPattern)$expectedUnit", RegexOption.IGNORE_CASE)
            .find(text)
            ?.groupValues
            ?.getOrNull(1)
        return withUnit ?: number.find(text)?.groupValues?.getOrNull(1)
    }

    private fun parseNumber(raw: String): Double? {
        val normalized = if (raw.matches(Regex("[0-9]{1,3}(,[0-9]{3})+(\\.[0-9]+)?"))) {
            raw.replace(",", "")
        } else {
            raw.replace(',', '.')
        }
        return normalized.toDoubleOrNull()?.takeIf(Double::isFinite)
    }
}
