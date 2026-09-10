package com.guruai.app.agent

class AgentLoop(
    private val llmClient: (String) -> String,
    private val toolRegistry: ToolRegistry
) {
    fun run(goal: String, maxSteps: Int = 6): String {
        val state = AgentState(goal = goal)

        for (i in 1..maxSteps) {
            if (state.isFinished) break

            val planPrompt = """
                Goal: ${state.goal}
                Completed steps: ${state.completedSteps}
                Last result: ${state.lastResult ?: "कुछ नहीं"}
                
                अगला छोटा स्टेप बताओ (सिर्फ एक स्टेप)।
                अगर गोल पूरा हो गया तो "FINISHED" लिखो।
                अगर टूल चाहिए तो इस फॉर्मेट में लिखो:
                TOOL: tool_name | argument
            """.trimIndent()

            val plan = llmClient(planPrompt)
            state.currentStep = plan
            state.steps.add(plan)

            when {
                plan.contains("FINISHED", ignoreCase = true) -> {
                    state.isFinished = true
                }
                plan.startsWith("TOOL:", ignoreCase = true) -> {
                    val result = toolRegistry.execute(plan)
                    state.lastResult = result
                    state.completedSteps.add("$plan → $result")
                }
                else -> {
                    state.completedSteps.add(plan)
                    state.lastResult = plan
                }
            }
        }

        val finalPrompt = """
            Goal: ${state.goal}
            सारे स्टेप्स: ${state.completedSteps}
            
            अब फाइनल जवाब दो।
        """.trimIndent()

        return llmClient(finalPrompt)
    }
}
