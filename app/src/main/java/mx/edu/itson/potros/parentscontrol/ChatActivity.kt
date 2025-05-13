package mx.edu.itson.potros.parentscontrol

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import androidx.compose.ui.Alignment


class ChatActivity : ComponentActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val profesorId = intent.getStringExtra("profesorId") ?: return
        val profesorNombre = intent.getStringExtra("profesorNombre") ?: "Profesor"

        setContent {
            ChatScreen(profesorId = profesorId, profesorNombre = profesorNombre)
        }
    }

    @Composable
    fun ChatScreen(profesorId: String, profesorNombre: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
        var messageText by remember { mutableStateOf("") }

        // Escucha los mensajes en tiempo real
        LaunchedEffect(profesorId) {
            db.collection("chats")
                .document(getChatId(currentUserId, profesorId))
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshots, _ ->
                    if (snapshots != null) {
                        messages = snapshots.documents.mapNotNull { it.toObject(ChatMessage::class.java) }
                    }
                }
        }

        Column(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)) {
            Text("💬 Chat con $profesorNombre", style = MaterialTheme.typography.titleLarge)

            LazyColumn(modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp)) {
                items(messages.size) { index ->
                    val msg = messages[index]
                    val align = if (msg.senderId == currentUserId) Alignment.End else Alignment.Start
                    Column(horizontalAlignment = align, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            msg.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (msg.senderId == currentUserId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                BasicTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp)
                )
                Button(onClick = {
                    if (messageText.isNotBlank()) {
                        val message = ChatMessage(
                            senderId = currentUserId,
                            text = messageText,
                            timestamp = System.currentTimeMillis()
                        )
                        db.collection("chats")
                            .document(getChatId(currentUserId, profesorId))
                            .collection("messages")
                            .add(message)
                        messageText = ""
                    }
                }) {
                    Text("Enviar")
                }
            }
        }
    }

    private fun getChatId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) "$userId1-$userId2" else "$userId2-$userId1"
    }
}
