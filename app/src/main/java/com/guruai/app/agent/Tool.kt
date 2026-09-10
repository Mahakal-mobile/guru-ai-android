package com.guruai.app.agent

interface Tool {
    val name: String
    fun execute(args: String): String
}
