package io.llmttestrunner

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    var textFieldValue by remember { mutableStateOf("") }
    var submittedText by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // TextField
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = { textFieldValue = it },
            label = { Text("Enter text") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("textField"),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Submit Button
        Button(
            onClick = {
                submittedText = textFieldValue
                Toast.makeText(
                    context,
                    "Submitted: $textFieldValue",
                    Toast.LENGTH_SHORT
                ).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("submitButton")
        ) {
            Text("Submit")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display submitted text
        if (submittedText.isNotEmpty()) {
            Text(
                text = "Last submitted: $submittedText",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.testTag("submittedText")
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Scrollable Column with multiple Text elements
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
                .testTag("scrollableColumn")
        ) {
            Text(
                text = "Scrollable Content",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Generate multiple items to make scrolling necessary
            repeat(30) { index ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .testTag("item_$index"),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Text(
                        text = "List Item ${index + 1}",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            // Add the Submit text at the bottom of scrollable area
            Text(
                text = "Submit",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .padding(16.dp)
                    .testTag("bottomSubmitText")
            )
        }
    }
}

