// HomeActivity.kt

package mx.edu.itson.potros.parentscontrol

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import mx.edu.itson.potros.parentscontrol.ui.theme.ParentsControlTheme

class HomeActivity : ComponentActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ParentsControlTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HijoList()
                }
            }
        }
    }

    @Composable
    fun HijoList() {
        var hijos by remember { mutableStateOf<List<Hijo>>(emptyList()) }
        val userId = auth.currentUser?.uid
        var listenerRegistration: ListenerRegistration? by remember { mutableStateOf(null) }
        var showAddDialog by remember { mutableStateOf(false) }

        LaunchedEffect(userId) {
            listenerRegistration?.remove()
            if (userId != null) {
                listenerRegistration = db.collection("usuarios")
                    .document(userId)
                    .collection("hijos")
                    .addSnapshotListener { snapshots, _ ->
                        if (snapshots != null) {
                            val hijosList = snapshots.documents.mapNotNull { doc ->
                                doc.toObject(Hijo::class.java)?.copy(documentId = doc.id)
                            }
                            hijos = hijosList
                        }
                    }
            }
        }

        DisposableEffect(Unit) {
            onDispose { listenerRegistration?.remove() }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(hijos) { hijo ->
                    HijoCard(hijo, onDelete = { hijoId ->
                        if (userId != null) {
                            db.collection("usuarios").document(userId)
                                .collection("hijos").document(hijoId).delete()
                        }
                    })
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("➕ Agregar hijo")
            }

            if (showAddDialog) {
                AgregarHijoDialog(onDismiss = { showAddDialog = false }) { nombre ->
                    if (userId != null) {
                        val nuevoHijo = Hijo(nombre = nombre, calificacion = 0.0)
                        db.collection("usuarios").document(userId)
                            .collection("hijos")
                            .add(nuevoHijo)
                            .addOnSuccessListener { ref ->
                                agregarDatosDePrueba(ref.id)
                            }
                    }
                }
            }
        }
    }

    @Composable
    fun AgregarHijoDialog(onDismiss: () -> Unit, onAgregar: (String) -> Unit) {
        var nombre by remember { mutableStateOf(TextFieldValue("")) }

        Dialog(onDismissRequest = onDismiss) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Agregar nuevo hijo", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text("Nombre del hijo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = onDismiss) { Text("Cancelar") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (nombre.text.isNotBlank()) {
                                    onAgregar(nombre.text.trim())
                                    onDismiss()
                                }
                            }
                        ) {
                            Text("Guardar")
                        }
                    }
                }
            }
        }
    }

    fun agregarDatosDePrueba(hijoId: String) {
        val userId = auth.currentUser?.uid ?: return

        val profesor = mapOf(
            "id" to "prof1",
            "nombre" to "Profe Carlos",
            "materia" to "Matemáticas"
        )

        val tarea = mapOf(
            "titulo" to "Tarea 1 de Álgebra",
            "materia" to "Matemáticas",
            "maestro" to profesor,
            "fechaAsignacion" to "2025-05-13",
            "fechaExpiracion" to "2025-05-20",
            "entregada" to false,
            "fechaEntrega" to null,
            "calificacion" to null
        )

        val trabajo = mapOf(
            "titulo" to "Proyecto de Geometría",
            "materia" to "Matemáticas",
            "maestro" to profesor,
            "fechaAsignacion" to "2025-05-10",
            "fechaExpiracion" to "2025-05-25",
            "entregada" to true,
            "fechaEntrega" to "2025-05-12",
            "calificacion" to 9.0
        )

        val hijoRef = db.collection("usuarios")
            .document(userId)
            .collection("hijos")
            .document(hijoId)

        hijoRef.update(
            mapOf(
                "profesores" to FieldValue.arrayUnion(profesor),
                "tareas" to FieldValue.arrayUnion(tarea),
                "trabajos" to FieldValue.arrayUnion(trabajo)
            )
        ).addOnSuccessListener {
            println("✅ Datos agregados correctamente.")
        }.addOnFailureListener {
            it.printStackTrace()
        }
    }

    // Las demás funciones (HijoCard, DetalleActividad, HijoDetailsDialog) permanecen igual
}

@Composable
    fun HijoCard(hijo: Hijo, onDelete: (String) -> Unit) {
        var showDetails by remember { mutableStateOf(false) }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDetails = true }, // <-- Mostrar detalles al hacer click
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "👦 Nombre: ${hijo.nombre}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    if (hijo.tareas.count { !it.entregada } > 0) {
                        BadgedBox(badge = {
                            Badge(containerColor = MaterialTheme.colorScheme.error) {
                                Text(hijo.tareas.count { !it.entregada }.toString())
                            }
                        }) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = "Pendientes",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Text("⭐ Calificación: ${hijo.calificacion}")
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onDelete(hijo.documentId) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("❌ Eliminar hijo")
                }
            }
        }

        if (showDetails) {
            HijoDetailsDialog(hijo = hijo, onDismiss = { showDetails = false })
        }
    }
    @Composable
    fun HijoDetailsDialog(hijo: Hijo, onDismiss: () -> Unit) {
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("👦 Detalles de ${hijo.nombre}", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("⭐ Calificación general: ${hijo.calificacion}")
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("📚 Profesores:")
                    hijo.profesores.forEach {
                        val context = LocalContext.current

                        Text(
                            text = "- ${it.nombre} (${it.materia})",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val intent = Intent(context, ChatActivity::class.java)
                                    intent.putExtra("profesorId", it.id)
                                    intent.putExtra("profesorNombre", it.nombre)
                                    context.startActivity(intent)
                                },
                            color = MaterialTheme.colorScheme.primary
                        )


                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("📌 Tareas:")
                    hijo.tareas.forEach { tarea ->
                        DetalleActividad(tarea)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("🧪 Trabajos:")
                    hijo.trabajos.forEach { trabajo ->
                        DetalleActividad(trabajo)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                        Text("Cerrar")
                    }
                }
            }
        }
    }
    @Composable
    fun DetalleActividad(actividad: Actividad) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text("📖 Título: ${actividad.titulo}", style = MaterialTheme.typography.bodyLarge)
            Text("📘 Materia: ${actividad.materia}")
            Text("👨‍🏫 Maestro: ${actividad.maestro.nombre}")
            Text("🗓️ Asignada: ${actividad.fechaAsignacion}")
            Text("⏳ Expira: ${actividad.fechaExpiracion}")

            if (actividad.entregada) {
                Text("📤 Entregada: ${actividad.fechaEntrega ?: "Desconocida"}")
                if (actividad.calificacion != null) {
                    Text("✅ Calificación: ${actividad.calificacion}")
                } else {
                    Text("🕓 Pendiente de revisión", color = MaterialTheme.colorScheme.tertiary)
                }
            } else {
                Text("❌ No entregada", color = MaterialTheme.colorScheme.error)
            }

            Divider(modifier = Modifier.padding(vertical = 4.dp))
        }
    }




