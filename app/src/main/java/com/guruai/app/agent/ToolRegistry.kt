package com.guruai.app.agent

class ToolRegistry {

    private val tools = mutableMapOf<String, Tool>()

    fun register(tool: Tool) {
        tools[tool.name.lowercase()] = tool
    }

    fun execute(command: String): String {
        // Example command: "TOOL: web_search | kotlin coroutines"
        val cleanCommand = command.removePrefix("TOOL:").trim()
        val parts = cleanCommand.split("|", limit = 2)

        val toolName = parts[0].trim().lowercase()
        val args = if (parts.size > 1) parts[1].trim() else ""

        val tool = tools[toolName]
            ?: return "टूल नहीं मिला: $toolName"

        return try {
            tool.execute(args)
        } catch (e: Exception) {
            "टूल एरर: ${e.message}"
        }
    }
}
