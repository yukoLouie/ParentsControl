package mx.edu.itson.potros.parentscontrol

data class Actividad(
    val titulo: String = "",
    val materia: String = "",
    val maestro: Profesor = Profesor(),
    val fechaAsignacion: String = "",
    val fechaEntrega: String? = null,
    val fechaExpiracion: String = "",
    val entregada: Boolean = false,
    val calificacion: Double? = null // null si no ha sido revisado aún
)
