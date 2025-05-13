package mx.edu.itson.potros.parentscontrol

data class ChatMessage(
    val senderId: String = "",
    val text: String = "",
    val timestamp: Long = 0L
)