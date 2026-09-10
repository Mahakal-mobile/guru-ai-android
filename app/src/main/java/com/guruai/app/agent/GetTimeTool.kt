package com.guruai.app.agent

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GetTimeTool : Tool {
    override val name = "get_time"

    override fun execute(args: String): String {
        val sdf = SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault())
        return "अभी समय है: ${sdf.format(Date())}"
    }
}
