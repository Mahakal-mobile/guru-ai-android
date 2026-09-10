package com.guruai.app.agent

class AgentLoop(
    private val llmClient: suspend (String) -> String,
    private val toolRegistry: ToolRegistry
) {
    suspend fun run(goal: String, maxSteps: Int = 6): String {
        val state = AgentState(goal = goal)

        for (i in 1..maxSteps) {
            if (state.isFinished) break

            val planPrompt = """
                Goal: ${state.goal}
                Completed steps: ${state.completedSteps}
                Last result: ${state.lastResult ?: "none yet"}

                Decide the next small step.
                If the goal is fully done, reply with exactly: FINISHED
                If you need a tool, reply in this exact format:
                TOOL: tool_name | argument
            """.trimIndent()

            val plan = llmClient(planPrompt)
            state.currentStep = plan
            state.steps.add(plan)

            when {
                plan.contains("FINISHED", ignoreCase = true) -> {
                    state.isFinished = true
                }
                plan.trim().startsWith("TOOL:", ignoreCase = true) -> {
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
            All steps taken: ${state.completedSteps}

            Give the final answer to the user now, in a natural, friendly way.
        """.trimIndent()

        return llmClient(finalPrompt)
    }
}
