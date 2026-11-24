package com.zerox.medistock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SetupGreen = Color(0xFF176B4D)

/**
 * First-start onboarding UI. Authentication and persistence are intentionally
 * delegated to the Supabase repository; never store a password in preferences.
 */
@Composable
fun StockXSetupFlow(
    onBack: () -> Unit = {},
    onSupabaseRequired: () -> Unit
) {
    var stage by remember { mutableIntStateOf(0) }
    var createAccount by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var ownerPhone by remember { mutableStateOf("") }
    var shopName by remember { mutableStateOf("") }
    var shopPhone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 26.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (stage > 0) {
            TextButton(onClick = { stage-- }) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
                Spacer(Modifier.width(6.dp)); Text("Back")
            }
        } else {
            Text("STOCK X", color = MaterialTheme.colorScheme.primary, fontSize = 25.sp, fontWeight = FontWeight.Black)
            Text("Your shop. Your stock. One simple place.", color = Color.Gray)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(3) { index ->
                Box(Modifier.weight(1f).height(5.dp).background(
                    if (index <= stage) SetupGreen else Color.LightGray,
                    RoundedCornerShape(8.dp)
                ))
            }
        }

        when (stage) {
            0 -> {
                Text(if (createAccount) "Create your account" else "Welcome back",
                    fontSize = 27.sp, fontWeight = FontWeight.Bold)
                Text("Securely access your medicine inventory from your account.", color = Color.Gray)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = createAccount, onClick = { createAccount = true }, label = { Text("Create account") })
                    FilterChip(selected = !createAccount, onClick = { createAccount = false }, label = { Text("Sign in") })
                }
                OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(),
                    label = { Text("Email address") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(),
                    label = { Text("Password") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                Button(
                    onClick = { stage = 1 },
                    enabled = email.contains("@") && password.length >= 6,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Continue", fontWeight = FontWeight.Bold) }
                Text("Your password will be handled by Supabase Authentication, not saved as plain text on this device.",
                    color = Color.Gray, fontSize = 12.sp)
            }
            1 -> {
                Text("Owner details", fontSize = 27.sp, fontWeight = FontWeight.Bold)
                Text("Tell us who manages this account.", color = Color.Gray)
                OutlinedTextField(fullName, { fullName = it }, Modifier.fillMaxWidth(),
                    label = { Text("Full name") }, singleLine = true)
                OutlinedTextField(ownerPhone, { ownerPhone = it }, Modifier.fillMaxWidth(),
                    label = { Text("Mobile number") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                Button(
                    onClick = { stage = 2 },
                    enabled = fullName.isNotBlank() && ownerPhone.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Continue", fontWeight = FontWeight.Bold) }
            }
            else -> {
                Text("Create your first shop", fontSize = 27.sp, fontWeight = FontWeight.Bold)
                Text("You can add and switch between more shops later.", color = Color.Gray)
                OutlinedTextField(shopName, { shopName = it }, Modifier.fillMaxWidth(),
                    label = { Text("Shop name") }, singleLine = true)
                OutlinedTextField(shopPhone, { shopPhone = it }, Modifier.fillMaxWidth(),
                    label = { Text("Shop phone (optional)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                OutlinedTextField(address, { address = it }, Modifier.fillMaxWidth(),
                    label = { Text("Shop address (optional)") }, minLines = 2)
                Button(
                    onClick = onSupabaseRequired,
                    enabled = shopName.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Create shop & open STOCK X", fontWeight = FontWeight.Bold) }
                Text("Account, owner and shop details will be saved after Supabase is connected. Existing local inventory will be preserved.",
                    color = Color.Gray, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Developed by ZEROX", modifier = Modifier.align(Alignment.CenterHorizontally),
            color = Color.Gray, fontSize = 12.sp)
    }
}
