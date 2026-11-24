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

private val Green = Color(0xFF00D98B)
private val Bg = Color(0xFF080B0A)
private val DarkSurface = Color(0xFF151E19)
private val MutedText = Color(0xFF89968F)
private val Crimson = Color(0xFFFF626B)

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  val dao = MedicineDatabase.get(this).dao()
  val prefs = getSharedPreferences("stock_x_settings", MODE_PRIVATE)
  setContent {
   var darkMode by remember { mutableStateOf(prefs.getBoolean("dark_mode", true)) }
   val scheme = if (darkMode) darkColorScheme(primary = Green, onPrimary = Color(0xFF002116), secondary = Color(0xFF6DE7B4), background = Bg, onBackground = Color(0xFFF4F7F5), surface = DarkSurface, onSurface = Color(0xFFF4F7F5), surfaceVariant = Color(0xFF202B25), onSurfaceVariant = MutedText, outline = Color(0xFF34443B), error = Crimson) else lightColorScheme(primary = Color(0xFF087A53), onPrimary = Color.White, background = Color(0xFFF3F7F4), onBackground = Color(0xFF14221B), surface = Color.White, onSurface = Color(0xFF14221B), surfaceVariant = Color(0xFFE7EFEA), onSurfaceVariant = Color(0xFF62736A), outline = Color(0xFFD3DED7)
   MaterialTheme(colorScheme = scheme) {
    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { delay(1800); showSplash = false }
    var guestExplored by remember { mutableStateOf(prefs.getBoolean("guest_explored", false)) }
    var profileCreated by remember { mutableStateOf(prefs.getBoolean("profile_created", false)) }
    var guestSession by remember { mutableStateOf(false) }
    if (showSplash) StockXSplash()
    else if (!guestExplored && !profileCreated) GuestWelcome(
     onExplore = { guestExplored = true; guestSession = true; prefs.edit().putBoolean("guest_explored", true).apply() },
     onCreateAccount = { guestExplored = true; prefs.edit().putBoolean("guest_explored", true).apply() }
    )
    else if (!profileCreated && !guestSession) ProfileSetup(
     onComplete = { owner, shop ->
      prefs.edit().putBoolean("profile_created", true).putString("owner_name", owner).putString("shop_name", shop).apply()
      profileCreated = true
     },
     onExplore = { guestSession = true }
    )
    else MediStockApp(this@MainActivity, dao, darkMode, prefs) { enabled -> darkMode = enabled; prefs.edit().putBoolean("dark_mode", enabled).apply() }
   }
  }
 }
}

@Composable
private fun GuestWelcome(onExplore: () -> Unit, onCreateAccount: () -> Unit) {
 Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp), contentAlignment = Alignment.Center) {
  Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
   Box(Modifier.size(104.dp).background(Color(0xFF10271D), RoundedCornerShape(32.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Inventory2, null, tint = Green, modifier = Modifier.size(54.dp)) }
   Text("STOCK X", color = Green, fontSize = 31.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 3.sp)
   Text("Your shop. In control.", color = MaterialTheme.colorScheme.onBackground, fontSize = 23.sp, fontWeight = FontWeight.Bold)
   Text("Explore inventory, stock movement and reports before creating your profile.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
   Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
     Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Explore, null, tint = Green); Spacer(Modifier.width(10.dp)); Text("Guest preview", fontWeight = FontWeight.Bold) }
     Text("No sign-up needed to take a look around. Your locally entered demo data stays on this device.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
     Button(onClick = onExplore, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Icon(Icons.Default.ArrowForward, null); Spacer(Modifier.width(8.dp)); Text("Explore as guest") }
     TextButton(onClick = onCreateAccount, modifier = Modifier.fillMaxWidth()) { Text("I want to create my profile") }
    }
   }
   Text("DEVELOPED BY ZEROX", color = MutedText, fontSize = 11.sp, letterSpacing = 2.sp)
  }
 }
}

@Composable
private fun ProfileSetup(onComplete: (String, String) -> Unit, onExplore: () -> Unit) {
 var owner by remember { mutableStateOf("") }
 var shop by remember { mutableStateOf("") }
 Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp), verticalArrangement = Arrangement.Center) {
  Box(Modifier.size(66.dp).background(Color(0xFF10271D), RoundedCornerShape(22.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.PersonAddAlt1, null, tint = Green, modifier = Modifier.size(32.dp)) }
  Spacer(Modifier.height(22.dp))
  Text("Welcome back.", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
  Text("Create your profile to continue using STOCK X.", color = MaterialTheme.colorScheme.onSurfaceVariant)
  Spacer(Modifier.height(24.dp))
  OutlinedTextField(owner, { owner = it }, Modifier.fillMaxWidth(), label = { Text("Owner name") }, leadingIcon = { Icon(Icons.Default.Person, null) }, singleLine = true, shape = RoundedCornerShape(16.dp))
  Spacer(Modifier.height(12.dp))
  OutlinedTextField(shop, { shop = it }, Modifier.fillMaxWidth(), label = { Text("Shop name") }, leadingIcon = { Icon(Icons.Default.Store, null) }, singleLine = true, shape = RoundedCornerShape(16.dp))
  Spacer(Modifier.height(18.dp))
  Button(onClick = { onComplete(owner.trim(), shop.trim()) }, enabled = owner.isNotBlank() && shop.isNotBlank(), modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) { Text("Create profile & continue") }
  Text("Your guest inventory remains saved on this device.", color = MutedText, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
  TextButton(onClick = onExplore, modifier = Modifier.fillMaxWidth()) { Text("Continue guest session") }
 }
}

@Composable
private fun MediStockApp(context: Context, dao: MedicineDao, darkMode: Boolean, prefs: android.content.SharedPreferences, onDarkModeChange: (Boolean) -> Unit) {
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
   0 -> HomeScreen(medicines, Modifier.padding(pad), { page = 1 }, { addOpen = true }, prefs.getString("owner_name", "Owner") ?: "Owner", prefs.getString("shop_name", "My Shop") ?: "My Shop")
   1 -> InventoryScreen(medicines, search, { search = it }, Modifier.padding(pad), { item, action -> selected = item; mode = action })
   2 -> ShortlistScreen(medicines.filter { it.quantity <= it.minimumStock }, Modifier.padding(pad))
   3 -> MoreScreen(Modifier.padding(pad), darkMode, onDarkModeChange, { page = 4 }, prefs.getString("owner_name", "Owner") ?: "Owner", prefs.getString("shop_name", "My Shop") ?: "My Shop")
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
private fun HomeScreen(data: List<MedicineEntity>, modifier: Modifier, inventory: () -> Unit, add: () -> Unit, ownerName: String, shopName: String) {
 val low = data.count { it.quantity <= it.minimumStock }
 val value = data.sumOf { it.quantity * it.unitPrice }
 LazyColumn(modifier.fillMaxSize().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
  item { Column(Modifier.padding(top = 24.dp)) { Text("STOCK X", color = MaterialTheme.colorScheme.primary, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp); Text("Stock management, made simple", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
  item { Card(colors = CardDefaults.cardColors(containerColor = Green), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp)) { Text("GOOD DAY, $ownerName", color = Color.White.copy(alpha = .75f), fontSize = 11.sp, letterSpacing = 1.5.sp); Text("Your shop at a glance", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold); Text("$shopName • Active", color = Color.White.copy(alpha = .85f)) } } }
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
private fun MoreScreen(modifier: Modifier, darkMode: Boolean, onDarkModeChange: (Boolean) -> Unit, onReports: () -> Unit, ownerName: String, shopName: String) {
 val uri = LocalUriHandler.current
 LazyColumn(modifier.fillMaxSize().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
  item { Text("Settings", fontSize = 27.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 22.dp), color = MaterialTheme.colorScheme.onBackground) }
  item {
   Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
    Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
     Box(Modifier.size(54.dp).background(Color(0xFF123A29), RoundedCornerShape(19.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Store, null, tint = Green, modifier = Modifier.size(27.dp)) }
     Spacer(Modifier.width(13.dp))
     Column(Modifier.weight(1f)) { Text(shopName, fontWeight = FontWeight.Bold, fontSize = 17.sp); Text("Owner: $ownerName", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) }
     Icon(Icons.Default.Verified, null, tint = Green)
    }
   }
  }
  item { Text("PREFERENCES", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp) }
  item {
   Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(20.dp)) {
    ListItem(leadingContent = { Icon(Icons.Default.DarkMode, null, tint = Green) }, headlineContent = { Text("Dark mode", fontWeight = FontWeight.SemiBold) }, supportingContent = { Text(if (darkMode) "ZEROX Midnight is active" else "Switch to ZEROX Midnight") }, trailingContent = { Switch(checked = darkMode, onCheckedChange = onDarkModeChange) })
   }
  }
  item {
   Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().clickable(onClick = onReports)) {
    ListItem(leadingContent = { Icon(Icons.Default.PictureAsPdf, null, tint = Green) }, headlineContent = { Text("Month-end report", fontWeight = FontWeight.SemiBold) }, supportingContent = { Text("Sales, purchases and remaining stock PDF") }, trailingContent = { Icon(Icons.Default.ChevronRight, null) })
   }
  }
  item { Text("DEVELOPER", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp) }
  item {
   Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(26.dp), modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(18.dp)) {
     Row(verticalAlignment = Alignment.CenterVertically) {
      Box(Modifier.size(62.dp).background(Brush.linearGradient(listOf(Color(0xFF00D98B), Color(0xFF075B3D))), RoundedCornerShape(22.dp)), contentAlignment = Alignment.Center) { Text("S", color = Color(0xFF04130C), fontSize = 30.sp, fontWeight = FontWeight.Black) }
      Spacer(Modifier.width(14.dp))
      Column(Modifier.weight(1f)) {
       Text("SHAFIN", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
       Text("DEVELOPED BY ZEROX", color = Green, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
       Text("App Developer", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
      }
      Icon(Icons.Default.Verified, null, tint = Green)
     }
     Spacer(Modifier.height(17.dp))
     Text("CONNECT WITH ME", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
     Spacer(Modifier.height(9.dp))
     Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
      DeveloperLink("Instagram", Icons.Default.CameraAlt, Modifier.weight(1f)) { uri.openUri("https://www.instagram.com/shafin_434?bqrl=MWlzOHk2eWQ0YjVqNA==") }
      DeveloperLink("Facebook", Icons.Default.Facebook, Modifier.weight(1f)) { uri.openUri("https://www.facebook.com/share/1CR39ArkRv/") }
     }
    }
   }
  }
  item { Text("ZEROX • STOCK X 1.0.0", color = MutedText, fontSize = 11.sp, modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
 }
}

@Composable
private fun DeveloperLink(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
 Card(modifier.clickable(onClick = onClick), shape = RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
  Row(Modifier.padding(horizontal = 12.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
   Icon(icon, null, tint = Green, modifier = Modifier.size(19.dp))
   Spacer(Modifier.width(7.dp))
   Text(label, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
  }
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
 Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(17.dp)) { Column(Modifier.padding(15.dp)) { Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp); Text(value, color = MaterialTheme.colorScheme.primary, fontSize = 20.sp, fontWeight = FontWeight.Bold) } }
}
@Composable
private fun ActionCard(label: String, action: () -> Unit, modifier: Modifier) {
 Card(modifier.height(64.dp).clickable(onClick = action), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(15.dp)) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(label, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold) } }
}
@Composable
private fun EmptyCard(title: String, description: String) {
 Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) { Text(title, fontWeight = FontWeight.Bold); Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) } }
}
