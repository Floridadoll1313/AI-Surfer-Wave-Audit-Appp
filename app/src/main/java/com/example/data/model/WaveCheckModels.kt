package com.example.data.model

/**
 * Client Wave Check data, ported verbatim from the web implementation
 * (otdaisurfer/AI-Surfer-: src/pages/wave-audit/WaveAudit.tsx,
 * src/features/wave-audit/scoring.ts, report.ts and types.ts).
 *
 * Question / option / playbook / offer copy is content data, kept here
 * exactly as on the web so the quiz, scoring and report stay in sync.
 * UI chrome strings (buttons, labels, hints) live in strings.xml.
 */

data class WaveCheckOption(
    val value: String,
    val label: String,
    val description: String,
)

data class WaveCheckQuestion(
    val key: String,
    val question: String,
    val options: List<WaveCheckOption>,
)

val WAVE_CHECK_QUESTIONS: List<WaveCheckQuestion> = listOf(
    WaveCheckQuestion(
        key = "businessType",
        question = "What type of business do you run?",
        options = listOf(
            WaveCheckOption(
                value = "service",
                label = "Service business",
                description = "Agencies, contractors, consulting, professional services",
            ),
            WaveCheckOption(
                value = "local",
                label = "Local business",
                description = "Retail, restaurant, salon, home services and more",
            ),
            WaveCheckOption(
                value = "ecommerce",
                label = "E-commerce",
                description = "Online store or product-based business",
            ),
            WaveCheckOption(
                value = "multi-location",
                label = "Multi-location",
                description = "Several teams, branches, or locations",
            ),
        ),
    ),
    WaveCheckQuestion(
        key = "teamSize",
        question = "How big is your team?",
        options = listOf(
            WaveCheckOption(
                value = "solo",
                label = "Just me",
                description = "Founder or solo operator",
            ),
            WaveCheckOption(
                value = "2-10",
                label = "2–10 people",
                description = "Small team with multiple hats",
            ),
            WaveCheckOption(
                value = "11-50",
                label = "11–50 people",
                description = "Growing operation",
            ),
            WaveCheckOption(
                value = "51+",
                label = "51+ people",
                description = "Larger team or organization",
            ),
        ),
    ),
    WaveCheckQuestion(
        key = "timeDrain",
        question = "What takes up the most time every week?",
        options = listOf(
            WaveCheckOption(
                value = "repetitive",
                label = "Repetitive admin",
                description = "Copying, updating, sorting, reporting, handoffs",
            ),
            WaveCheckOption(
                value = "support",
                label = "Customer questions",
                description = "FAQs, support requests, status updates",
            ),
            WaveCheckOption(
                value = "content",
                label = "Marketing content",
                description = "Posts, emails, campaigns, creative production",
            ),
            WaveCheckOption(
                value = "multiple",
                label = "A little of everything",
                description = "Several recurring tasks are eating the week",
            ),
        ),
    ),
    WaveCheckQuestion(
        key = "lostOpportunity",
        question = "Where are you losing the most opportunities?",
        options = listOf(
            WaveCheckOption(
                value = "leads",
                label = "Finding new leads",
                description = "Not enough qualified opportunities coming in",
            ),
            WaveCheckOption(
                value = "followup",
                label = "Following up",
                description = "Good leads go quiet or wait too long",
            ),
            WaveCheckOption(
                value = "operations",
                label = "Internal workflows",
                description = "Slow processes and manual bottlenecks",
            ),
            WaveCheckOption(
                value = "multiple",
                label = "Several areas",
                description = "More than one major leak needs attention",
            ),
        ),
    ),
    WaveCheckQuestion(
        key = "aiPriority",
        question = "What would you most like AI to improve first?",
        options = listOf(
            WaveCheckOption(
                value = "sales",
                label = "Sales & follow-up",
                description = "Respond faster and convert more opportunities",
            ),
            WaveCheckOption(
                value = "automation",
                label = "Automation",
                description = "Remove repetitive work and connect systems",
            ),
            WaveCheckOption(
                value = "marketing",
                label = "Marketing & content",
                description = "Create and distribute better content",
            ),
            WaveCheckOption(
                value = "support",
                label = "Customer care",
                description = "Answer routine questions and improve service",
            ),
            WaveCheckOption(
                value = "multiple",
                label = "A full AI makeover",
                description = "Several areas could benefit at once",
            ),
        ),
    ),
)

data class WaveCheckAnswers(
    val businessType: String = "",
    val teamSize: String = "",
    val timeDrain: String = "",
    val lostOpportunity: String = "",
    val aiPriority: String = "",
) {
    fun isComplete(): Boolean =
        businessType.isNotBlank() && teamSize.isNotBlank() && timeDrain.isNotBlank() &&
            lostOpportunity.isNotBlank() && aiPriority.isNotBlank()

    fun valueFor(key: String): String = when (key) {
        "businessType" -> businessType
        "teamSize" -> teamSize
        "timeDrain" -> timeDrain
        "lostOpportunity" -> lostOpportunity
        "aiPriority" -> aiPriority
        else -> ""
    }

    fun withAnswer(key: String, value: String): WaveCheckAnswers = when (key) {
        "businessType" -> copy(businessType = value)
        "teamSize" -> copy(teamSize = value)
        "timeDrain" -> copy(timeDrain = value)
        "lostOpportunity" -> copy(lostOpportunity = value)
        "aiPriority" -> copy(aiPriority = value)
        else -> this
    }
}

data class RevenueLeak(
    val id: String,
    val title: String,
    val impact: String, // "High" | "Medium"
    val signal: String,
    val recommendedFix: String,
    val recommendedAgent: String,
    val recommendedOffer: String, // "Wave Starter" | "Wave Builder" | "Tsunami Growth"
)

data class WaveAuditResult(
    val score: Int,
    val topCategory: String,
    val opportunities: List<String>,
    val recommendedAgent: String,
    val confidenceLabel: String,
    val revenueLeaks: List<RevenueLeak>,
)

data class ReportPhase(
    val window: String,
    val title: String,
    val action: String,
)

data class WaveAuditReport(
    val score: Int,
    val opportunityLevel: String,
    val headline: String,
    val businessSnapshot: String,
    val diagnosis: String,
    val priorityActions: List<String>,
    val plan: List<ReportPhase>,
    val metrics: List<String>,
    val agentName: String,
    val agentFit: String,
    val firstRecommendation: String,
)

data class WaveCheckOffer(
    val id: String, // "wave-starter" | "wave-builder" | "tsunami-growth"
    val name: String,
    val price: String,
    val description: String,
    val cta: String,
    val href: String,
)

val WAVE_CHECK_OFFERS: List<WaveCheckOffer> = listOf(
    WaveCheckOffer(
        id = "wave-starter",
        name = "Wave Starter",
        price = "$497",
        description = "A focused implementation sprint for one clear AI opportunity, lead leak, or repeatable workflow.",
        cta = "Buy Wave Starter",
        href = "https://buy.stripe.com/aFa8wP7JN3500Uy1tt4gg0b",
    ),
    WaveCheckOffer(
        id = "wave-builder",
        name = "Wave Builder",
        price = "$1,997",
        description = "Connect AI visibility, lead flow, follow-up, content, or automation into a broader growth system.",
        cta = "Book Wave Builder Strategy Call",
        href = "https://calendly.com/oceantidedrop/ai-surfer-business-ai-consultation",
    ),
    WaveCheckOffer(
        id = "tsunami-growth",
        name = "Tsunami Growth",
        price = "$3,997",
        description = "Strategy plus deeper implementation when several AI systems need to work together across the customer journey.",
        cta = "Book Tsunami Growth Strategy Call",
        href = "https://calendly.com/oceantidedrop/ai-surfer-business-ai-consultation",
    ),
)

/**
 * Port of recommendOffer in FullWaveReport.tsx.
 * Returns the id of the recommended offer.
 */
fun recommendOffer(answers: WaveCheckAnswers): String {
    if (answers.aiPriority == "multiple" ||
        (answers.timeDrain == "multiple" && answers.lostOpportunity == "multiple")
    ) {
        return "tsunami-growth"
    }
    if (answers.timeDrain == "multiple" ||
        answers.lostOpportunity == "operations" ||
        answers.aiPriority == "automation" ||
        answers.aiPriority == "marketing" ||
        answers.aiPriority == "support"
    ) {
        return "wave-builder"
    }
    return "wave-starter"
}
