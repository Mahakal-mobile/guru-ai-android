package com.guruai.app.agent

data class AgentState(
    val goal: String,
    val steps: MutableList<String> = mutableListOf(),
    val completedSteps: MutableList<String> = mutableListOf(),
    var currentStep: String? = null,
    var isFinished: Boolean = false,
    var lastResult: String? = null
)
