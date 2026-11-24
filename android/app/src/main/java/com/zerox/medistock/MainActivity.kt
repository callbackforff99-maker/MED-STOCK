package com.zerox.medistock
import android.os.Bundle
import android.content.Context
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.shadow
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerox.medistock.data.*
import kotlinx.coroutines.launch
import java.util.Locale

private val Green = Color(0xFF176B4D)
private val Bg = Color(0xFFF5F7F4)

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  val dao = MedicineDatabase.get(this).dao()
  val prefs = getSharedPreferences("stock_x_settings", MODE_PRIVATE)
  setContent {
   var darkMode by remember { mutableStateOf(prefs.getBoolean("dark_mode", false)) }
   val scheme = if (darkMode) darkColorScheme(primary = Color(0xFF35D69A), background = Color(0xFF080E0B), surface = Color(0xFF121B16), onBackground = Color(0xFFE8F2EC), onSurface = Color(0xFFE8F2EC)) else lightColorScheme(primary = Green, background = Bg, surface = Color.White)
   MaterialTheme(colorScheme = scheme) {
    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { delay(1800); showSplash = false }
    var guestEntered by remember { mutableStateOf(prefs.getBoolean("guest_entered", false)) }
    if (showSplash) StockXSplash()
    else if (!guestEntered) GuestWelcome(
     onExplore = { guestEntered = true; prefs.edit().putBoolean("guest_entered", true).apply() },
     onCreateAccount = { }
    )
    else MediStockApp(this@MainActivity, dao, darkMode) { enabled -> darkMode = enabled; prefs.edit().putBoolean("dark_mode", enabled).apply() }
   }
  }
 }
}

@Composable
private fun GuestWelcome(onExplore: () -> Unit, onCreateAccount: () -> Unit) {
 var showInfo by remember { mutableStateOf(false) }
 Box(Modifier.fillMaxSize().background(Bg).padding(26.dp), contentAlignment = Alignment.Center) {
  Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
   Text("STOCK X", color = Green, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
   Text("Explore before you sign up.", fontSize = 21.sp, fontWeight = FontWeight.Bold)
   Text("Browse the app and try the inventory features as a guest. You don't need to create an ID first.", color = Color.Gray)
   Button(onClick = onExplore, modifier = Modifier.fillMaxWidth()) { Text("Explore as guest") }
   OutlinedButton(onClick = { showInfo = true }, modifier = Modifier.fillMaxWidth()) { Text("Create account") }
   Text("You can create an account after exploring.", color = Color.Gray, fontSize = 12.sp)
  }
 }
 if (showInfo) AlertDialog(
  onDismissRequest = { showInfo = false },
  title = { Text("Explore first") },
  text = { Text("Account creation and cloud sign-in will be enabled in the next authentication step. For now, enter as a guest and explore STOCK X without an ID.") },
  confirmButton = { TextButton(onClick = { showInfo = false; onExplore() }) { Text("Continue as guest") } }
 )
}

@Composable
private fun MediStockApp(context: Context, dao: MedicineDao, darkMode: Boolean, onDarkModeChange: (Boolean) -> Unit) {
 val medicines by dao.observeAll().collectAsState(initial = emptyList())
 val scope = rememberCoroutineScope()
 var page by remember { mutableIntStateOf(0) }
 var addOpen by remember { mutableStateOf(false) }
 var selected by remember { mutableStateOf<MedicineEntity?>(null) }
 var mode by remember { mutableStateOf("IN") }
 var search by remember { mutableStateOf("") }
 Scaffold(containerColor = MaterialTheme.colorScheme.background,
  floatingActionButton = { if (page == 1) FloatingActionButton(onClick = { addOpen = true }, containerColor = Green, contentColor = Color.White) { Icon(Icons.Default.Add, "Add medicine") } },
  bottomBar = { NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
   val labels = listOf("Home", "Inventory", "Shortlist", "More")
   val icons = listOf(Icons.Default.Home, Icons.Default.Inventory2, Icons.Default.ShoppingCart, Icons.Default.MoreHoriz)
   labels.forEachIndexed { i, label -> NavigationBarItem(selected = page == i, onClick = { page = i }, icon = { Icon(icons[i], label) }, label = { Text(label) }) }
  } }
 ) { pad ->
  when (page) {
   0 -> HomeScreen(medicines, Modifier.padding(pad), { page = 1 }, { addOpen = true })
   1 -> InventoryScreen(medicines, search, { search = it }, Modifier.padding(pad), { item, action -> selected = item; mode = action })
   2 -> ShortlistScreen(medicines.filter { it.quantity <= it.minimumStock }, Modifier.padding(pad))
   3 -> MoreScreen(Modifier.padding(pad), darkMode, onDarkModeChange, { page = 4 })
   4 -> MonthlyReportScreen(context, dao, medicines) { page = 3 }
  }
 }
 if (addOpen) AddMedicineDialog({ addOpen = false }) { n, s, c, p, q, min ->
  scope.launch { dao.insert(MedicineEntity(name = n, strength = s, company = c, unitPrice = p, quantity = q, minimumStock = min)) }
  addOpen = false
 }
 selected?.let { item -> StockDialog(item, mode, { selected = null }) { amount ->
  val updated = if (mode == "IN") item.quantity + amount else item.quantity - amount
  scope.launch {
   dao.saveMovementAndQty(
    StockMovementEntity(medicineId = item.id, medicineName = item.name, movementType = mode, quantity = amount, unitPrice = item.unitPrice, totalAmount = amount * item.unitPrice),
    updated
   )
  }
  selected = null
 } }
}

@Composable
private fun HomeScreen(data: List<MedicineEntity>, modifier: Modifier, inventory: () -> Unit, add: () -> Unit) {
 val low = data.count { it.quantity <= it.minimumStock }
 val value = data.sumOf { it.quantity * it.unitPrice }
 LazyColumn(modifier.fillMaxSize().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
  item { Column(Modifier.padding(top = 24.dp)) { Text("STOCK X", color = MaterialTheme.colorScheme.primary, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold); Text("Stock management, made simple", color = Color.Gray) } }
  item { Card(colors = CardDefaults.cardColors(containerColor = Green), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp)) { Text("Good day!", color = Color.White.copy(alpha = .8f)); Text("Your shop at a glance", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold); Text("Main Shop • Active", color = Color.White) } } }
  item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { StatCard("Total medicines", data.size.toString(), Modifier.weight(1f)); StatCard("Stock value", "৳" + String.format(Locale.US, "%.2f", value), Modifier.weight(1f)) } }
  item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { StatCard("Low stock", low.toString(), Modifier.weight(1f)); StatCard("Items tracked", data.size.toString(), Modifier.weight(1f)) } }
  item { Text("Quick actions", fontWeight = FontWeight.Bold, fontSize = 18.sp); Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { ActionCard("Add medicine", add, Modifier.weight(1f)); ActionCard("View inventory", inventory, Modifier.weight(1f)) } }
  item { Text("Inventory overview", fontWeight = FontWeight.Bold, fontSize = 18.sp); EmptyCard(if (data.isEmpty()) "Your inventory is empty" else data.size.toString() + " medicines in your shop", if (data.isEmpty()) "Add your first medicine to get started." else low.toString() + " medicines need a stock check.") }
 }
}

@Composable
private fun InventoryScreen(data: List<MedicineEntity>, search: String, onSearch: (String) -> Unit, modifier: Modifier, onStock: (MedicineEntity, String) -> Unit) {
 val filtered = data.filter { it.name.contains(search, true) || it.company.contains(search, true) || it.strength.contains(search, true) }
 Column(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
  Text("Inventory", fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 22.dp, bottom = 12.dp))
  OutlinedTextField(search, onSearch, Modifier.fillMaxWidth(), placeholder = { Text("Search medicines") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true, shape = RoundedCornerShape(14.dp))
  Spacer(Modifier.height(12.dp))
  if (filtered.isEmpty()) EmptyCard("No medicines found", "Tap + to add your first medicine.")
  else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { items(filtered, key = { it.id }) { medicine -> MedicineCard(medicine, onStock) } }
 }
}

@Composable
private fun MedicineCard(item: MedicineEntity, onStock: (MedicineEntity, String) -> Unit) {
 Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(17.dp), modifier = Modifier.fillMaxWidth()) {
  Column(Modifier.padding(14.dp)) {
   Row(verticalAlignment = Alignment.CenterVertically) {
    Column(Modifier.weight(1f)) { Text(item.name, fontWeight = FontWeight.Bold, fontSize = 16.sp); Text(listOf(item.strength, item.company).filter { it.isNotBlank() }.joinToString(" • "), color = Color.Gray, fontSize = 12.sp) }
    Text(if (item.quantity <= item.minimumStock) "LOW STOCK" else "IN STOCK", color = if (item.quantity <= item.minimumStock) Color(0xFFB45A19) else Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
   }
   Spacer(Modifier.height(10.dp))
   Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
    Column { Text("Qty: " + item.quantity, fontWeight = FontWeight.SemiBold); Text("৳" + String.format(Locale.US, "%.2f", item.unitPrice) + " / unit", color = Color.Gray, fontSize = 12.sp) }
    Row { OutlinedButton(onClick = { onStock(item, "OUT") }) { Text("OUT") }; Spacer(Modifier.width(5.dp)); Button(onClick = { onStock(item, "IN") }) { Text("IN") } }
   }
  }
 }
}

@Composable
private fun ShortlistScreen(items: List<MedicineEntity>, modifier: Modifier) {
 Column(modifier.fillMaxSize().padding(18.dp)) {
  Text("Purchase shortlist", fontSize = 25.sp, fontWeight = FontWeight.Bold)
  Text("Medicines at or below minimum stock", color = Color.Gray)
  Spacer(Modifier.height(14.dp))
  if (items.isEmpty()) EmptyCard("All stocked up", "No medicines currently need reordering.")
  else LazyColumn { items(items, key = { it.id }) { m -> Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) { ListItem(headlineContent = { Text(m.name, fontWeight = FontWeight.Bold) }, supportingContent = { Text(m.company) }, trailingContent = { Text(m.quantity.toString() + " left", color = Color(0xFFB45A19)) }) } } }
 }
}

@Composable
private fun MoreScreen(modifier: Modifier, darkMode: Boolean, onDarkModeChange: (Boolean) -> Unit, onReports: () -> Unit) {
 val uri = LocalUriHandler.current
 Column(modifier.fillMaxSize().padding(20.dp)) {
  Text("More", fontSize = 26.sp, fontWeight = FontWeight.Bold)
  Text("Settings", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 14.dp, bottom = 8.dp))
  Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
   ListItem(
    headlineContent = { Text("Dark mode", fontWeight = FontWeight.SemiBold) },
    supportingContent = { Text(if (darkMode) "Dark theme is enabled" else "Use the dark emerald theme") },
    trailingContent = { Switch(checked = darkMode, onCheckedChange = onDarkModeChange) }
   )
  }
  Spacer(Modifier.height(10.dp))
  Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth().clickable(onClick = onReports)) {
   ListItem(headlineContent = { Text("Month-end report", fontWeight = FontWeight.SemiBold) }, supportingContent = { Text("Sales, purchases and remaining stock PDF") }, trailingContent = { Icon(Icons.Default.ChevronRight, null) })
  }
  listOf("Shop profile", "Owner details", "Backup & restore", "About STOCK X").forEach { label -> ListItem(headlineContent = { Text(label) }) }
  Spacer(Modifier.height(14.dp))
  Text("CONTACT DEVELOPER", color = Green, fontWeight = FontWeight.Bold, fontSize = 12.sp)
  Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
   Column(Modifier.padding(12.dp)) {
    Text("Developed by ZEROX", fontWeight = FontWeight.Bold, fontSize = 17.sp)
    Text("Connect with the developer", color = Color.Gray, fontSize = 12.sp)
    Spacer(Modifier.height(8.dp))
    OutlinedButton(onClick = { uri.openUri("https://www.instagram.com/shafin_434?bqrl=MWlzOHk2eWQ0YjVqNA==") }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Link, null); Spacer(Modifier.width(8.dp)); Text("Instagram") }
    OutlinedButton(onClick = { uri.openUri("https://www.facebook.com/share/1CR39ArkRv/") }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Link, null); Spacer(Modifier.width(8.dp)); Text("Facebook") }
   }
  }
  Spacer(Modifier.height(12.dp))
  Text("Offline local database • v1.0.0", color = Color.Gray, fontSize = 12.sp)
 }
}

@Composable
private fun StockXSplash() {
 Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF020806), Color(0xFF001B12), Color(0xFF020403)))), contentAlignment = Alignment.Center) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
   Box(Modifier.size(156.dp).shadow(24.dp, RoundedCornerShape(34.dp)).background(Color(0xFF03150F), RoundedCornerShape(34.dp)), contentAlignment = Alignment.Center) {
    Icon(Icons.Default.MedicalServices, null, tint = Color(0xFF00F59B), modifier = Modifier.size(82.dp))
    Text("X", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Black, modifier = Modifier.offset(x = 28.dp, y = 25.dp))
   }
   Spacer(Modifier.height(22.dp))
   Text("STOCK X", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 3.sp)
   Spacer(Modifier.height(7.dp))
   Text("Developed by ZEROX", color = Color(0xFF00D98A), fontSize = 13.sp, letterSpacing = 1.sp)
  }
 }
}

@Composable
private fun AddMedicineDialog(close: () -> Unit, save: (String, String, String, Double, Int, Int) -> Unit) {
 var name by remember { mutableStateOf("") }
 var strength by remember { mutableStateOf("") }
 var company by remember { mutableStateOf("") }
 var price by remember { mutableStateOf("0") }
 var qty by remember { mutableStateOf("0") }
 var min by remember { mutableStateOf("10") }
 AlertDialog(onDismissRequest = close, title = { Text("Add medicine") }, text = {
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
   OutlinedTextField(name, { name = it }, label = { Text("Medicine name *") }, singleLine = true)
   OutlinedTextField(strength, { strength = it }, label = { Text("Strength") }, singleLine = true)
   OutlinedTextField(company, { company = it }, label = { Text("Company") }, singleLine = true)
   OutlinedTextField(price, { price = it }, label = { Text("Unit price (৳)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
   OutlinedTextField(qty, { qty = it }, label = { Text("Opening quantity") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
   OutlinedTextField(min, { min = it }, label = { Text("Low-stock threshold") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
  }
 }, confirmButton = { Button(enabled = name.isNotBlank(), onClick = { save(name.trim(), strength.trim(), company.trim(), price.toDoubleOrNull() ?: 0.0, qty.toIntOrNull()?.coerceAtLeast(0) ?: 0, min.toIntOrNull()?.coerceAtLeast(0) ?: 10) }) { Text("Save") } }, dismissButton = { TextButton(onClick = close) { Text("Cancel") } })
}

@Composable
private fun StockDialog(item: MedicineEntity, mode: String, close: () -> Unit, confirm: (Int) -> Unit) {
 var amount by remember { mutableStateOf("1") }
 val number = amount.toIntOrNull() ?: 0
 AlertDialog(onDismissRequest = close, title = { Text(if (mode == "IN") "Stock in" else "Stock out") }, text = {
  Column { Text(item.name, fontWeight = FontWeight.Bold); Text("Available: " + item.quantity, color = Color.Gray); OutlinedTextField(amount, { amount = it.filter(Char::isDigit) }, label = { Text("Quantity") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)); if (mode == "OUT" && number > item.quantity) Text("Insufficient stock", color = Color.Red) }
 }, confirmButton = { Button(enabled = number > 0 && (mode == "IN" || number <= item.quantity), onClick = { confirm(number) }) { Text("Confirm") } }, dismissButton = { TextButton(onClick = close) { Text("Cancel") } })
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier) {
 Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(17.dp)) { Column(Modifier.padding(15.dp)) { Text(title, color = Color.Gray, fontSize = 12.sp); Text(value, color = Green, fontSize = 20.sp, fontWeight = FontWeight.Bold) } }
}
@Composable
private fun ActionCard(label: String, action: () -> Unit, modifier: Modifier) {
 Card(modifier.height(64.dp).clickable(onClick = action), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(15.dp)) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(label, color = Green, fontWeight = FontWeight.SemiBold) } }
}
@Composable
private fun EmptyCard(title: String, description: String) {
 Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) { Text(title, fontWeight = FontWeight.Bold); Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) } }
}
