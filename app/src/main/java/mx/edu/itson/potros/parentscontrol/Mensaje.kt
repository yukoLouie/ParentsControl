package mx.edu.itson.potros.parentscontrol

data class Mensaje(
    val emisorId: String = "",
    val receptorId: String = "",
    val mensaje: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
