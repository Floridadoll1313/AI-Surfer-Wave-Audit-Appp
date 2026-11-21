package com.example.data.model

import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Faithful Kotlin port of the web Wave Audit scoring and report builders
 * (otdaisurfer/AI-Surfer-: src/features/wave-audit/scoring.ts and
 * src/features/wave-audit/report.ts). Do not invent new scoring: the
 * numbers, labels and copy below match the web implementation exactly.
 */

private val CATEGORY_LABELS: Map<String, String> = mapOf(
    "leads" to "Lead & Sales Follow-Up",
    "operations" to "Workflow Automation",
    "content" to "Content & Marketing",
    "support" to "Customer Care",
    "visibility" to "Opportunity Discovery",
)

private val AGENT_BY_CATEGORY: Map<String, String> = mapOf(
    "leads" to "Sales Rider",
    "operations" to "Automation Architect",
    "content" to "Content Creator",
    "support" to "Customer Care Cove",
    "visibility" to "Wave Scout",
)

private val SCORE_BONUSES: Map<String, Map<String, Int>> = mapOf(
    "businessType" to mapOf(
        "multi-location" to 14,
        "ecommerce" to 10,
        "service" to 8,
        "professional" to 8,
        "local" to 7,
    ),
    "teamSize" to mapOf(
        "51+" to 16,
        "11-50" to 12,
        "2-10" to 8,
        "solo" to 5,
    ),
    "timeDrain" to mapOf(
        "multiple" to 16,
        "repetitive" to 14,
        "admin" to 11,
        "content" to 10,
        "support" to 10,
    ),
    "lostOpportunity" to mapOf(
        "multiple" to 20,
        "leads" to 17,
        "followup" to 15,
        "operations" to 13,
        "content" to 11,
        "support" to 10,
    ),
    "aiPriority" to mapOf(
        "multiple" to 20,
        "sales" to 17,
        "automation" to 16,
        "marketing" to 13,
        "support" to 12,
        "leads" to 14,
    ),
)

private fun addCategoryPoints(
    buckets: MutableMap<String, Int>,
    answer: String,
    points: Int,
) {
    if (answer == "multiple") {
        buckets.keys.forEach { category -> buckets[category] = (buckets[category] ?: 0) + points }
        return
    }
    val category = if (answer == "followup" || answer == "sales") "leads" else answer
    if (category in buckets) buckets[category] = (buckets[category] ?: 0) + points
}

fun calculateWaveAuditResult(answers: WaveCheckAnswers): WaveAuditResult {
    // Buckets keep web insertion order (leads, operations, content, support,
    // visibility); sortedByDescending is stable, so ties rank like the web.
    val categories = linkedMapOf(
        "leads" to 0,
        "operations" to 0,
        "content" to 0,
        "support" to 0,
        "visibility" to 0,
    )

    var score = 28
    score += SCORE_BONUSES["businessType"]?.get(answers.businessType) ?: 0
    score += SCORE_BONUSES["teamSize"]?.get(answers.teamSize) ?: 0
    score += SCORE_BONUSES["timeDrain"]?.get(answers.timeDrain) ?: 0
    score += SCORE_BONUSES["lostOpportunity"]?.get(answers.lostOpportunity) ?: 0
    score += SCORE_BONUSES["aiPriority"]?.get(answers.aiPriority) ?: 0

    addCategoryPoints(categories, answers.timeDrain, 8)
    addCategoryPoints(categories, answers.lostOpportunity, 12)
    addCategoryPoints(categories, answers.aiPriority, 12)

    if (answers.businessType == "multi-location") score += 4
    if (answers.teamSize == "51+") score += 4

    val ranked = categories.entries.sortedByDescending { it.value }
    val topEntry = ranked[0]
    val secondEntry = ranked.getOrNull(1)

    val multipleHighImpact = listOf(
        answers.timeDrain,
        answers.lostOpportunity,
        answers.aiPriority,
    ).count { it == "multiple" } >= 2

    val recommendedAgent: String = if (multipleHighImpact) {
        "Big Kahuna"
    } else {
        AGENT_BY_CATEGORY[topEntry.key] ?: "Wave Scout"
    }
    val topCategory: String = if (multipleHighImpact) {
        "Multi-Area AI Transformation"
    } else {
        CATEGORY_LABELS[topEntry.key] ?: "Opportunity Discovery"
    }

    val opportunities: List<String> = if (multipleHighImpact) {
        listOf(
            "Connect multiple high-friction areas into one AI operating system.",
            "Prioritize the workflows with the clearest revenue or time-saving impact.",
        )
    } else {
        listOf(
            opportunityFor(topEntry.key, answers),
            if (secondEntry != null && secondEntry.key != topEntry.key) {
                opportunityFor(secondEntry.key, answers)
            } else {
                "Build a practical first automation around your highest-friction task."
            },
        )
    }

    val normalizedTop = min(1.0, topEntry.value / 40.0)
    val finalScore = (score + normalizedTop * 7).roundToInt().coerceIn(0, 100)
    val confidenceLabel = when {
        finalScore >= 82 -> "High opportunity"
        finalScore >= 65 -> "Strong opportunity"
        else -> "Emerging opportunity"
    }

    return WaveAuditResult(
        score = finalScore,
        topCategory = topCategory,
        opportunities = opportunities.filter { it.isNotBlank() }.take(2),
        recommendedAgent = recommendedAgent,
        confidenceLabel = confidenceLabel,
        revenueLeaks = detectRevenueLeaks(answers),
    )
}

private fun opportunityFor(category: String, answers: WaveCheckAnswers): String {
    return when (category) {
        "leads" -> if (answers.lostOpportunity == "followup") {
            "Recover more revenue with faster, more consistent lead follow-up."
        } else {
            "Turn more incoming interest into qualified conversations and sales."
        }
        "operations" -> "Automate repetitive handoffs, admin, and workflow steps that drain team time."
        "content" -> "Create more consistent marketing content without adding another full-time workload."
        "support" -> "Reduce repetitive customer questions with faster answers and better self-service."
        else -> "Spot missed opportunities and turn scattered business data into clearer next actions."
    }
}

fun detectRevenueLeaks(answers: WaveCheckAnswers): List<RevenueLeak> {
    val leaks = mutableListOf<RevenueLeak>()

    if (answers.lostOpportunity == "leads" || answers.lostOpportunity == "multiple") {
        leaks.add(
            RevenueLeak(
                id = "missed-leads",
                title = "Missed or unworked leads",
                impact = "High",
                signal = "Your answers suggest qualified opportunities may not be entering a consistent sales process.",
                recommendedFix = "Capture, qualify, and route every new lead into one follow-up workflow.",
                recommendedAgent = "Sales Rider",
                recommendedOffer = "Wave Starter",
            ),
        )
    }

    if (answers.lostOpportunity == "followup" || answers.lostOpportunity == "multiple" ||
        answers.aiPriority == "sales"
    ) {
        leaks.add(
            RevenueLeak(
                id = "slow-followup",
                title = "Slow lead follow-up",
                impact = "High",
                signal = "Sales follow-up is a priority or a stated opportunity leak.",
                recommendedFix = "Trigger immediate acknowledgement, timed follow-ups, and an owner alert when a lead goes quiet.",
                recommendedAgent = "Sales Rider",
                recommendedOffer = "Wave Starter",
            ),
        )
    }

    if (answers.lostOpportunity == "operations" || answers.lostOpportunity == "multiple" ||
        answers.aiPriority == "automation"
    ) {
        leaks.add(
            RevenueLeak(
                id = "stalled-deals",
                title = "Deals or quotes stuck in the pipeline",
                impact = "High",
                signal = "Manual workflows can leave opportunities waiting between stages.",
                recommendedFix = "Add stage-age alerts, automatic next-step tasks, and escalation for stalled opportunities.",
                recommendedAgent = "Automation Architect",
                recommendedOffer = "Wave Builder",
            ),
        )
    }

    if (answers.timeDrain == "repetitive" || answers.timeDrain == "multiple") {
        leaks.add(
            RevenueLeak(
                id = "payment-followup",
                title = "Completed work without payment follow-up",
                impact = "Medium",
                signal = "Repetitive admin can include invoice, payment, and completion follow-up that slips through the cracks.",
                recommendedFix = "Automate completion-to-invoice reminders and overdue-payment follow-up while keeping human approval for exceptions.",
                recommendedAgent = "Automation Architect",
                recommendedOffer = "Wave Builder",
            ),
        )
    }

    if (answers.aiPriority == "marketing" || answers.aiPriority == "multiple") {
        leaks.add(
            RevenueLeak(
                id = "dormant-customers",
                title = "Dormant customers with no reactivation",
                impact = "Medium",
                signal = "Existing customers may be an underused source of repeat business when reactivation is inconsistent.",
                recommendedFix = "Segment past customers and trigger useful, permission-aware reactivation campaigns.",
                recommendedAgent = "Content Creator",
                recommendedOffer = if (answers.aiPriority == "multiple") "Tsunami Growth" else "Wave Builder",
            ),
        )
    }

    return leaks.take(5)
}

private val BUSINESS_LABELS: Map<String, String> = mapOf(
    "service" to "service business",
    "local" to "local business",
    "ecommerce" to "e-commerce business",
    "multi-location" to "multi-location business",
)

private val TEAM_LABELS: Map<String, String> = mapOf(
    "solo" to "solo",
    "2-10" to "2–10 person",
    "11-50" to "11–50 person",
    "51+" to "51+ person",
)

private val AGENT_FIT: Map<String, String> = mapOf(
    "Wave Scout" to "Finds overlooked opportunities and turns scattered signals into a focused next move.",
    "Sales Rider" to "Helps respond to leads faster, organize follow-up, and move more conversations toward a sale.",
    "Content Creator" to "Builds a repeatable content current so marketing stays consistent without swallowing the week.",
    "Customer Care Cove" to "Creates faster, more consistent answers for routine customer questions and service requests.",
    "Automation Architect" to "Connects repetitive steps and handoffs into practical workflows that save time.",
    "Big Kahuna" to "Coordinates a multi-area AI transformation when several major opportunities need one strategy.",
)

private data class ReportPlaybook(
    val diagnosis: String,
    val plan: List<ReportPhase>,
    val metrics: List<String>,
    val firstRecommendation: String,
)

private val PLAYBOOKS: Map<String, ReportPlaybook> = mapOf(
    "Lead & Sales Follow-Up" to ReportPlaybook(
        diagnosis = "Interest is reaching the shoreline, but slow or inconsistent follow-up can let qualified buyers drift away before a real conversation begins.",
        firstRecommendation = "Start with an automatic first response that acknowledges every new lead, asks one qualifying question, and creates a clear human follow-up task.",
        plan = listOf(
            ReportPhase(
                window = "Days 1–7",
                title = "Map the lead current",
                action = "List every place a lead arrives, who owns the response, and where conversations currently stall.",
            ),
            ReportPhase(
                window = "Days 8–14",
                title = "Launch the first-response wave",
                action = "Connect one lead source to an immediate reply, simple qualification, and a visible follow-up queue.",
            ),
            ReportPhase(
                window = "Days 15–30",
                title = "Tune for conversions",
                action = "Review response and conversion data weekly, then improve timing, questions, and handoffs.",
            ),
        ),
        metrics = listOf("Lead response time", "Qualified conversations started", "Follow-up conversion rate"),
    ),
    "Workflow Automation" to ReportPlaybook(
        diagnosis = "Repetitive work and manual handoffs are creating drag. The first win is one dependable workflow that returns time without disrupting the business.",
        firstRecommendation = "Choose the highest-frequency manual task and automate its intake, routing, and completion notice before expanding to a second workflow.",
        plan = listOf(
            ReportPhase(
                window = "Days 1–7",
                title = "Find the time leak",
                action = "Track repeated tasks for one week and rank them by frequency, effort, and business impact.",
            ),
            ReportPhase(
                window = "Days 8–14",
                title = "Build one clean workflow",
                action = "Automate the safest high-value sequence with a clear owner and a manual fallback.",
            ),
            ReportPhase(
                window = "Days 15–30",
                title = "Measure and expand",
                action = "Confirm time saved and error reduction, then connect the next related handoff.",
            ),
        ),
        metrics = listOf("Hours of manual work removed", "Workflow completion time", "Handoff error rate"),
    ),
    "Content & Marketing" to ReportPlaybook(
        diagnosis = "Marketing demand is outrunning available time. A repeatable content system can turn one strong idea into consistent, channel-ready material.",
        firstRecommendation = "Create one weekly source piece and use AI to reshape it into an email, social posts, and a customer-question answer.",
        plan = listOf(
            ReportPhase(
                window = "Days 1–7",
                title = "Choose the message current",
                action = "Collect the ten questions customers ask most and select one clear weekly theme.",
            ),
            ReportPhase(
                window = "Days 8–14",
                title = "Build the reuse system",
                action = "Create a reviewable workflow that turns each source idea into several channel-specific assets.",
            ),
            ReportPhase(
                window = "Days 15–30",
                title = "Publish and learn",
                action = "Ship consistently, track meaningful engagement, and strengthen the themes that create conversations.",
            ),
        ),
        metrics = listOf("Content pieces published", "Qualified engagement", "Content-assisted leads"),
    ),
    "Customer Care" to ReportPlaybook(
        diagnosis = "Routine questions are consuming attention that should stay available for complex customer needs and relationship-building.",
        firstRecommendation = "Start with the ten most common questions and create approved answers with a clear path to a human whenever confidence is low.",
        plan = listOf(
            ReportPhase(
                window = "Days 1–7",
                title = "Chart common questions",
                action = "Group recent customer requests by topic, urgency, and the best approved response.",
            ),
            ReportPhase(
                window = "Days 8–14",
                title = "Open the care cove",
                action = "Launch guided answers for one channel with clear escalation rules and response ownership.",
            ),
            ReportPhase(
                window = "Days 15–30",
                title = "Improve the experience",
                action = "Review unresolved questions and customer feedback, then expand the approved answer library.",
            ),
        ),
        metrics = listOf("First-response time", "Questions resolved without handoff", "Customer satisfaction"),
    ),
    "Multi-Area AI Transformation" to ReportPlaybook(
        diagnosis = "Several high-impact areas are competing for attention. The opportunity is large, but sequencing matters more than adding disconnected tools.",
        firstRecommendation = "Select one revenue workflow and one time-saving workflow, define success for each, and connect them through a single 30-day operating plan.",
        plan = listOf(
            ReportPhase(
                window = "Days 1–7",
                title = "Set the transformation map",
                action = "Rank opportunities by revenue impact, time savings, risk, and ease of implementation.",
            ),
            ReportPhase(
                window = "Days 8–14",
                title = "Launch the lead wave",
                action = "Deploy the highest-confidence workflow with ownership, safeguards, and a manual fallback.",
            ),
            ReportPhase(
                window = "Days 15–30",
                title = "Connect the system",
                action = "Measure the first workflow, document lessons, and connect the next priority without duplicating data.",
            ),
        ),
        metrics = listOf("Revenue opportunities advanced", "Hours returned to the team", "Workflow adoption rate"),
    ),
    "Opportunity Discovery" to ReportPlaybook(
        diagnosis = "The biggest constraint is visibility into where AI can create a measurable business win. A focused discovery sprint will prevent random tool adoption.",
        firstRecommendation = "Inventory customer questions, repeated internal work, and missed sales signals, then score each opportunity by value and feasibility.",
        plan = listOf(
            ReportPhase(
                window = "Days 1–7",
                title = "Gather the signals",
                action = "Collect recurring customer questions, bottlenecks, and missed opportunities from the last month.",
            ),
            ReportPhase(
                window = "Days 8–14",
                title = "Rank the waves",
                action = "Score each use case for business impact, effort, data readiness, and operational risk.",
            ),
            ReportPhase(
                window = "Days 15–30",
                title = "Prove one use case",
                action = "Run a small measured pilot and use the evidence to choose the next investment.",
            ),
        ),
        metrics = listOf("Opportunities identified", "Pilot time to value", "Measured business impact"),
    ),
)

fun buildWaveAuditReport(
    answers: WaveCheckAnswers,
    result: WaveAuditResult,
): WaveAuditReport {
    val playbook = PLAYBOOKS[result.topCategory] ?: PLAYBOOKS.getValue("Opportunity Discovery")
    val business = BUSINESS_LABELS[answers.businessType] ?: "business"
    val team = TEAM_LABELS[answers.teamSize] ?: "growing"

    return WaveAuditReport(
        score = result.score,
        opportunityLevel = result.confidenceLabel,
        headline = "Your Biggest Wave: ${result.topCategory}",
        businessSnapshot = "Your $team $business has a ${result.confidenceLabel.lowercase()} for practical AI improvement, led by ${result.topCategory.lowercase()}.",
        diagnosis = playbook.diagnosis,
        priorityActions = (result.opportunities + playbook.firstRecommendation).take(3),
        plan = playbook.plan,
        metrics = playbook.metrics,
        agentName = result.recommendedAgent,
        agentFit = AGENT_FIT[result.recommendedAgent] ?: AGENT_FIT.getValue("Wave Scout"),
        firstRecommendation = playbook.firstRecommendation,
    )
}

/**
 * Port of formatWaveAuditReport in report.ts — the plain-text report used
 * for Copy Report. Note the web version computes a revenue-leak section
 * but does not include it in the returned text; this port mirrors that.
 */
fun formatWaveAuditReport(
    report: WaveAuditReport,
    submissionId: String,
    result: WaveAuditResult? = null,
): String {
    val priorities = report.priorityActions
        .mapIndexed { index, action -> "${index + 1}. $action" }
        .joinToString("\n")
    val plan = report.plan
        .joinToString("\n\n") { phase -> "${phase.window} — ${phase.title}\n${phase.action}" }
    val metrics = report.metrics.joinToString("\n") { metric -> "• $metric" }

    return buildString {
        append("OCEAN TIDE DROP AI SURFER — AI WAVE REPORT\n\n")
        append("AI Opportunity Score: ${report.score}/100 (${report.opportunityLevel})\n")
        append("${report.headline}\n\n")
        append("BUSINESS SNAPSHOT\n${report.businessSnapshot}\n\n")
        append("WHAT THIS MEANS\n${report.diagnosis}\n\n")
        append("PRIORITY ACTIONS\n$priorities\n\n")
        append("30-DAY WAVE PLAN\n$plan\n\n")
        append("SIGNALS TO TRACK\n$metrics\n\n")
        append("RECOMMENDED AI SURFER AGENT\n${report.agentName}: ${report.agentFit}\n\n")
        append("FIRST RECOMMENDATION\n${report.firstRecommendation}\n\n")
        append("Receipt: $submissionId")
    }
}
