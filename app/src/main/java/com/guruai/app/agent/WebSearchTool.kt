package com.guruai.app.agent

class WebSearchTool : Tool {
    override val name = "web_search"

    override fun execute(args: String): String {
        // अभी के लिए सिंपल प्लेसहोल्डर
        // बाद में असली वेब सर्च लगा सकते हैं
        return "Search results for: $args (अभी प्लेसहोल्डर है)"
    }
}
