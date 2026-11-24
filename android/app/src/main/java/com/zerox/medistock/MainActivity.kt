package com.zerox.medistock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Forest = Color(0xFF176B4D)
private val Canvas = Color(0xFFF5F7F4)
private val Ink = Color(0xFF1E2924)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(
                primary = Forest,
                background = Canvas,
                surface = Color.White,
                onBackground = Ink,
                onSurface = Ink
            )) {
                Surface(modifier = Modifier.fillMaxSize(), color = Canvas) {
                    DashboardScreen()
                }
            }
        }
    }
}

@Composable
private fun DashboardScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    Scaffold(
        containerColor = Canvas,
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                listOf("Home", "Inventory", "Shortlist", "More").forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = when (index) {
                                    1 -> Icons.Default.Inventory2
                                    2 -> Icons.Default.ShoppingCart
                                    else -> Icons.Default.Inventory2
                                },
                                contentDescription = label
                            )
                        },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 22.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("MEDI STOCK", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Forest)
                        Text("Stock management, made simple", color = Color.Gray, fontSize = 13.sp)
                    }
                    Icon(Icons.Default.NotificationsNone, contentDescription = "Notifications", tint = Forest)
                }
            }
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Forest),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Good day!", color = Color.White.copy(alpha = .8f))
                        Spacer(Modifier.height(6.dp))
                        Text("Your shop at a glance", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(14.dp))
                        Text("Main Shop  •  Active", color = Color.White.copy(alpha = .9f))
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Total Medicines", "—", Modifier.weight(1f))
                    StatCard("Stock Value", "৳ —", Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Low Stock", "—", Modifier.weight(1f))
                    StatCard("Today's Stock In", "—", Modifier.weight(1f))
                }
            }
            item {
                Text("Quick actions", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickAction("Stock In", Modifier.weight(1f))
                    QuickAction("Stock Out", Modifier.weight(1f))
                    QuickAction("Add Medicine", Modifier.weight(1f))
                }
            }
            item {
                Text("Recent activity", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
                Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("No activity yet", fontWeight = FontWeight.SemiBold)
                        Text("Your stock movements will appear here.", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = Color.Gray, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Text(value, color = Forest, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun QuickAction(label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(76.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, color = Forest, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
