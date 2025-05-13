package mx.edu.itson.potros.parentscontrol

data class Hijo(
    val nombre: String = "",
    val calificacion: Double = 0.0,
    val tareas: List<Actividad> = listOf(),
    val trabajos: List<Actividad> = listOf(),
    val profesores: List<Profesor> = listOf(),
    val documentId: String = ""

)