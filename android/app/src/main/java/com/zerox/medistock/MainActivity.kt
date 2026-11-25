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
   val scheme = if (darkMode) darkColorScheme(primary = Green, onPrimary = Color(0xFF002116), secondary = Color(0xFF6DE7B4), background = Bg, onBackground = Color(0xFFF4F7F5), surface = DarkSurface, onSurface = Color(0xFFF4F7F5), surfaceVariant = Color(0xFF202B25), onSurfaceVariant = MutedText, outline = Color(0xFF34443B), error = Crimson) else lightColorScheme(primary = Color(0xFF087A53), onPrimary = Color.White, background = Color(0xFFF3F7F4), onBackground = Color(0xFF14221B), surface = Color.White, onSurface = Color(0xFF14221B), surfaceVariant = Color(0xFFE7EFEA), onSurfaceVariant = Color(0xFF62736A), outline = Color(0xFFD3DED7))
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
     }
    )
    else MediStockApp(this@MainActivity, dao, darkMode, prefs) { enabled -> darkMode = enabled; prefs.edit().putBoolean("dark_mode", enabled).apply() }
   }
  }
 }
}

@Composable
private fun GuestWelcome(onExplore: () -> Unit, onCreateAccount: () -> Unit) {
 Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp), contentAlignment = Alignment.Center) {
  Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(17.dp)) {
   Box(Modifier.size(112.dp).background(Brush.linearGradient(listOf(Color(0xFF123D2B), Color(0xFF071A12))), RoundedCornerShape(34.dp)), contentAlignment = Alignment.Center) {
    Icon(Icons.Default.Inventory2, null, tint = Color(0xFF00E69A), modifier = Modifier.size(57.dp))
   }
   Text("STOCK X", color = Green, fontSize = 31.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 3.sp)
   Text("Your shop. In control.", color = MaterialTheme.colorScheme.onBackground, fontSize = 24.sp, fontWeight = FontWeight.Bold)
   Text("Manage medicine inventory, track stock movement and review shop reports — all in one place.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontSize = 13.sp)
   Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(25.dp), modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
     Row(verticalAlignment = Alignment.CenterVertically) {
      Box(Modifier.size(38.dp).background(Green.copy(alpha = .13f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Explore, null, tint = Green) }
      Spacer(Modifier.width(10.dp))
      Column {
       Text("Guest preview", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
       Text("Take a look before creating a profile", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
      }
     }
     Text("No sign-up needed to explore. Demo entries stay on this device.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
     Button(onClick = onExplore, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color(0xFF062016))) { Text("Explore as guest", fontWeight = FontWeight.Bold) }
     OutlinedButton(onClick = onCreateAccount, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Green)) { Text("Create account") }
    }
   }
   Text("DEVELOPED BY ZEROX", color = MutedText, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp)
  }
 }
}
@Composable
private fun ProfileSetup(onComplete: (String, String) -> Unit) {
 var owner by remember { mutableStateOf("") }
 var shop by remember { mutableStateOf("") }
 Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp), verticalArrangement = Arrangement.Center) {
  Box(Modifier.size(68.dp).background(Green.copy(alpha = .13f), RoundedCornerShape(22.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.PersonAddAlt1, null, tint = Green, modifier = Modifier.size(32.dp)) }
  Spacer(Modifier.height(22.dp))
  Text("Set up your shop", fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
  Text("A few details to personalize your STOCK X workspace.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
  Spacer(Modifier.height(24.dp))
  OutlinedTextField(owner, { owner = it }, Modifier.fillMaxWidth(), label = { Text("Owner name") }, leadingIcon = { Icon(Icons.Default.Person, null, tint = Green) }, singleLine = true, shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, cursorColor = Green))
  Spacer(Modifier.height(12.dp))
  OutlinedTextField(shop, { shop = it }, Modifier.fillMaxWidth(), label = { Text("Shop name") }, leadingIcon = { Icon(Icons.Default.Store, null, tint = Green) }, singleLine = true, shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, cursorColor = Green))
  Spacer(Modifier.height(18.dp))
  Button(onClick = { onComplete(owner.trim(), shop.trim()) }, enabled = owner.isNotBlank() && shop.isNotBlank(), modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color(0xFF062016))) { Text("Create profile & continue", fontWeight = FontWeight.Bold) }
  Text("Your guest inventory remains saved on this device.", color = MutedText, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
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
  bottomBar = {
   NavigationBar(
    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp).shadow(18.dp, RoundedCornerShape(26.dp)),
    containerColor = MaterialTheme.colorScheme.surface,
    contentColor = MaterialTheme.colorScheme.onSurface,
    tonalElevation = 0.dp,
    windowInsets = WindowInsets(0)
   ) {
    val labels = listOf("Home", "Inventory", "Shortlist", "More")
    val icons = listOf(Icons.Default.Home, Icons.Default.Inventory2, Icons.Default.ShoppingCart, Icons.Default.MoreHoriz)
    labels.forEachIndexed { i, label ->
     NavigationBarItem(
      selected = page == i,
      onClick = { page = i },
      icon = { Icon(icons[i], label) },
      label = { Text(label, fontWeight = if (page == i) FontWeight.Bold else FontWeight.Medium) },
      colors = NavigationBarItemDefaults.colors(
       selectedIconColor = Color(0xFF061A12),
       selectedTextColor = Green,
       indicatorColor = Green,
       unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
       unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
      )
     )
    }
   }
  }
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
 val lowItems = data.filter { it.quantity <= it.minimumStock }
 val low = lowItems.size
 val value = data.sumOf { it.quantity * it.unitPrice }
 LazyColumn(modifier.fillMaxSize().padding(horizontal = 18.dp), contentPadding = PaddingValues(top = 22.dp, bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
  item {
   Row(verticalAlignment = Alignment.CenterVertically) {
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
     Text("GOOD DAY, \${ownerName.uppercase()}", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.7.sp)
     Text("Your dashboard", color = MaterialTheme.colorScheme.onBackground, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
     Text("Here’s what’s happening in your shop.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
    }
    Box(Modifier.size(46.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
     Icon(Icons.Default.Store, null, tint = Green, modifier = Modifier.size(23.dp))
    }
   }
  }
  item {
   Card(colors = CardDefaults.cardColors(containerColor = Color.Transparent), shape = RoundedCornerShape(26.dp), modifier = Modifier.fillMaxWidth()) {
    Box(Modifier.background(Brush.linearGradient(listOf(Color(0xFF0C5C3D), Color(0xFF073322), Color(0xFF101B16)))).fillMaxWidth().padding(20.dp)) {
     Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
       Text("ACTIVE SHOP", color = Color.White.copy(alpha = .72f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp, modifier = Modifier.weight(1f))
       Surface(color = Color.White.copy(alpha = .12f), shape = RoundedCornerShape(30.dp)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
         Box(Modifier.size(6.dp).background(Color(0xFF4DFFB0), RoundedCornerShape(50)))
         Spacer(Modifier.width(6.dp))
         Text("ACTIVE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
       }
      }
      Text(shopName, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
      Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = .16f)))
      Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
       Column {
        Text("MEDICINES", color = Color.White.copy(alpha = .65f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text(data.size.toString(), color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
       }
       Column {
        Text("LOW STOCK", color = Color.White.copy(alpha = .65f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text(low.toString(), color = Color(0xFFFFD28B), fontSize = 21.sp, fontWeight = FontWeight.Bold)
       }
      }
     }
    }
   }
  }
  item {
   Row(verticalAlignment = Alignment.CenterVertically) {
    Text("Stock overview", color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
    Text("LIVE SUMMARY", color = MaterialTheme.colorScheme.primary, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
   }
   Spacer(Modifier.height(10.dp))
   Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
    DashboardMetric("Stock value", "৳" + String.format(Locale.US, "%.0f", value), Icons.Default.AccountBalanceWallet, Modifier.weight(1f))
    DashboardMetric("Total items", data.size.toString(), Icons.Default.Medication, Modifier.weight(1f))
   }
   Spacer(Modifier.height(10.dp))
   Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
    DashboardMetric("Low stock", low.toString(), Icons.Default.Warning, Modifier.weight(1f), if (low > 0) Color(0xFFFFC078) else Green)
    DashboardMetric("Units in hand", data.sumOf { it.quantity }.toString(), Icons.Default.Inventory2, Modifier.weight(1f))
   }
  }
  item {
   Row(verticalAlignment = Alignment.CenterVertically) {
    Text("Quick actions", color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
    Text("SHORTCUTS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
   }
   Spacer(Modifier.height(10.dp))
   Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
    DashboardAction("Add medicine", "Register new stock item", Icons.Default.Add, add, Modifier.weight(1f))
    DashboardAction("Inventory", "Browse all medicines", Icons.Default.Inventory2, inventory, Modifier.weight(1f))
   }
  }
  item {
   Row(verticalAlignment = Alignment.CenterVertically) {
    Column(Modifier.weight(1f)) {
     Text("Needs attention", color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
     Text("Medicines running low", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
    TextButton(onClick = inventory) { Text("View all") }
   }
   if (lowItems.isEmpty()) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
     Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
      Icon(Icons.Default.CheckCircle, null, tint = Green, modifier = Modifier.size(26.dp))
      Spacer(Modifier.width(12.dp))
      Column {
       Text("Everything looks good", fontWeight = FontWeight.Bold)
       Text("No medicines are below their minimum stock level.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
      }
     }
    }
   } else {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
     lowItems.take(4).forEach { item ->
      Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
       Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).background(Color(0xFF332519), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
         Icon(Icons.Default.Medication, null, tint = Color(0xFFFFC078))
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
         Text(item.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
         Text(item.company.ifBlank { item.strength }, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
         Text("\${item.quantity} left", color = Color(0xFFFFC078), fontWeight = FontWeight.Bold, fontSize = 13.sp)
         Text("Min \${item.minimumStock}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        }
       }
      }
     }
    }
   }
  }
 }
}

@Composable
private fun DashboardMetric(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, accent: Color = Green) {
 Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(20.dp)) {
  Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
   Box(Modifier.size(34.dp).background(accent.copy(alpha = .13f), RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
    Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
   }
   Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 21.sp, fontWeight = FontWeight.Bold, maxLines = 1)
   Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
  }
 }
}

@Composable
private fun DashboardAction(label: String, detail: String, icon: androidx.compose.ui.graphics.vector.ImageVector, action: () -> Unit, modifier: Modifier) {
 Card(modifier.clickable(onClick = action), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(20.dp)) {
  Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
   Box(Modifier.size(40.dp).background(Green.copy(alpha = .13f), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
    Icon(icon, null, tint = Green)
   }
   Spacer(Modifier.width(10.dp))
   Column(Modifier.weight(1f)) {
    Text(label, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
   }
  }
 }
}
@Composable
private fun InventoryScreen(data: List<MedicineEntity>, search: String, onSearch: (String) -> Unit, modifier: Modifier, onStock: (MedicineEntity, String) -> Unit) {
 val filtered = data.filter { it.name.contains(search, true) || it.company.contains(search, true) || it.strength.contains(search, true) }
 Column(modifier.fillMaxSize().padding(horizontal = 18.dp)) {
  Text("Inventory", color = MaterialTheme.colorScheme.onBackground, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 22.dp))
  Text("\${data.size} medicines in your shop", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp, bottom = 16.dp))
  OutlinedTextField(search, onSearch, Modifier.fillMaxWidth(), placeholder = { Text("Search name, company or strength") }, leadingIcon = { Icon(Icons.Default.Search, null, tint = Green) }, singleLine = true, shape = RoundedCornerShape(17.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, cursorColor = Green))
  Spacer(Modifier.height(14.dp))
  Row(verticalAlignment = Alignment.CenterVertically) {
   Text("Medicine list", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
   Text("\${filtered.size} results", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
  }
  Spacer(Modifier.height(10.dp))
  if (filtered.isEmpty()) EmptyCard("No medicines found", "Try another search or add your first medicine.")
  else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 18.dp)) { items(filtered, key = { it.id }) { medicine -> MedicineCard(medicine, onStock) } }
 }
}
@Composable
private fun MedicineCard(item: MedicineEntity, onStock: (MedicineEntity, String) -> Unit) {
 val low = item.quantity <= item.minimumStock
 Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(21.dp), modifier = Modifier.fillMaxWidth()) {
  Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
   Row(verticalAlignment = Alignment.CenterVertically) {
    Box(Modifier.size(43.dp).background(Green.copy(alpha = .12f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
     Icon(Icons.Default.Medication, null, tint = Green, modifier = Modifier.size(23.dp))
    }
    Spacer(Modifier.width(11.dp))
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
     Text(item.name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp)
     Text(listOf(item.strength, item.company).filter { it.isNotBlank() }.joinToString(" • "), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
    }
    Surface(color = if (low) Color(0xFF3B2A1B) else Color(0xFF123426), shape = RoundedCornerShape(30.dp)) {
     Text(if (low) "LOW STOCK" else "IN STOCK", color = if (low) Color(0xFFFFC078) else Color(0xFF63E6AC), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp))
    }
   }
   Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = .35f)))
   Row(verticalAlignment = Alignment.CenterVertically) {
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
     Text("QUANTITY", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
     Text(item.quantity.toString(), color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
     Text("৳" + String.format(Locale.US, "%.2f", item.unitPrice) + " / unit", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
    }
    OutlinedButton(onClick = { onStock(item, "OUT") }, shape = RoundedCornerShape(13.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)) { Icon(Icons.Default.Remove, null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(3.dp)); Text("OUT") }
    Spacer(Modifier.width(7.dp))
    Button(onClick = { onStock(item, "IN") }, shape = RoundedCornerShape(13.dp), colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color(0xFF062016))) { Icon(Icons.Default.Add, null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(3.dp)); Text("IN") }
   }
  }
 }
}
@Composable
private fun ShortlistScreen(items: List<MedicineEntity>, modifier: Modifier) {
 Column(modifier.fillMaxSize().padding(horizontal = 18.dp)) {
  Text("Purchase shortlist", color = MaterialTheme.colorScheme.onBackground, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 22.dp))
  Text("Items that need to be reordered", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp, bottom = 16.dp))
  Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF102B1F)), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
   Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
    Icon(Icons.Default.ShoppingCart, null, tint = Green, modifier = Modifier.size(25.dp))
    Spacer(Modifier.width(12.dp))
    Column {
     Text("\${items.size} items to purchase", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
     Text("Based on your minimum stock level", color = Color(0xFF9EB8A9), fontSize = 11.sp)
    }
   }
  }
  Spacer(Modifier.height(14.dp))
  if (items.isEmpty()) EmptyCard("All stocked up", "No medicines currently need reordering.")
  else LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp), contentPadding = PaddingValues(bottom = 18.dp)) {
   items(items, key = { it.id }) { m ->
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(19.dp), modifier = Modifier.fillMaxWidth()) {
     Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
      Box(Modifier.size(42.dp).background(Color(0xFF382919), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.ShoppingBasket, null, tint = Color(0xFFFFC078)) }
      Spacer(Modifier.width(11.dp))
      Column(Modifier.weight(1f)) {
       Text(m.name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
       Text(m.company.ifBlank { m.strength }, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
       Text("Minimum: \${m.minimumStock}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
      }
      Column(horizontalAlignment = Alignment.End) {
       Text("\${m.quantity} left", color = Color(0xFFFFC078), fontWeight = FontWeight.Bold, fontSize = 13.sp)
       Text("Reorder", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
      }
     }
    }
   }
  }
 }
}
@Composable
private fun MoreScreen(modifier: Modifier, darkMode: Boolean, onDarkModeChange: (Boolean) -> Unit, onReports: () -> Unit, ownerName: String, shopName: String) {
 val uri = LocalUriHandler.current
 LazyColumn(modifier.fillMaxSize().padding(horizontal = 18.dp), contentPadding = PaddingValues(top = 22.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
  item {
   Text("Settings", color = MaterialTheme.colorScheme.onBackground, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
   Text("Manage your shop and app preferences", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
  }
  item {
   Card(colors = CardDefaults.cardColors(containerColor = Color.Transparent), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
    Row(Modifier.background(Brush.linearGradient(listOf(Color(0xFF0C5C3D), Color(0xFF10231A)))).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
     Box(Modifier.size(54.dp).background(Color.White.copy(alpha = .12f), RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Store, null, tint = Color(0xFF63E6AC), modifier = Modifier.size(27.dp)) }
     Spacer(Modifier.width(13.dp))
     Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
      Text(shopName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
      Text("Owner: $ownerName", color = Color.White.copy(alpha = .7f), fontSize = 12.sp)
     }
     Icon(Icons.Default.Verified, null, tint = Color(0xFF63E6AC))
    }
   }
  }
  item { Text("PREFERENCES", color = Green, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp) }
  item {
   Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(20.dp)) {
    ListItem(leadingContent = { Icon(Icons.Default.DarkMode, null, tint = Green) }, headlineContent = { Text("Dark mode", fontWeight = FontWeight.SemiBold) }, supportingContent = { Text(if (darkMode) "Obsidian Emerald is active" else "Switch to Obsidian Emerald") }, trailingContent = { Switch(checked = darkMode, onCheckedChange = onDarkModeChange, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF062016), checkedTrackColor = Green)) })
   }
  }
  item {
   Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().clickable(onClick = onReports)) {
    ListItem(leadingContent = { Box(Modifier.size(40.dp).background(Green.copy(alpha = .13f), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.PictureAsPdf, null, tint = Green) } }, headlineContent = { Text("Month-end report", fontWeight = FontWeight.SemiBold) }, supportingContent = { Text("Date range, sales and stock summary PDF") }, trailingContent = { Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) })
   }
  }
  item { Text("DEVELOPER", color = Green, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp) }
  item {
   Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(18.dp)) {
     Row(verticalAlignment = Alignment.CenterVertically) {
      Box(Modifier.size(58.dp).background(Brush.linearGradient(listOf(Color(0xFF00D98B), Color(0xFF075B3D))), RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) { Text("S", color = Color(0xFF04130C), fontSize = 29.sp, fontWeight = FontWeight.Black) }
      Spacer(Modifier.width(13.dp))
      Column(Modifier.weight(1f)) {
       Text("SHAFIN", color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.4.sp)
       Text("DEVELOPED BY ZEROX", color = Green, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
       Text("App Developer", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
      }
      Icon(Icons.Default.Verified, null, tint = Green)
     }
     Spacer(Modifier.height(16.dp))
     Text("CONNECT WITH ME", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
     Spacer(Modifier.height(9.dp))
     Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
      DeveloperLink("Instagram", Icons.Default.CameraAlt, Modifier.weight(1f)) { uri.openUri("https://www.instagram.com/shafin_434?bqrl=MWlzOHk2eWQ0YjVqNA==") }
      DeveloperLink("Facebook", Icons.Default.People, Modifier.weight(1f)) { uri.openUri("https://www.facebook.com/share/1CR39ArkRv/") }
     }
    }
   }
  }
  item { Text("STOCK X  •  ZEROX", color = MutedText, fontSize = 10.sp, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
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
 Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF020806), Color(0xFF09251A), Color(0xFF020403)))), contentAlignment = Alignment.Center) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
   Box(Modifier.size(156.dp).shadow(25.dp, RoundedCornerShape(38.dp)).background(Brush.linearGradient(listOf(Color(0xFF123D2B), Color(0xFF04140D))), RoundedCornerShape(38.dp)), contentAlignment = Alignment.Center) {
    Icon(Icons.Default.MedicalServices, null, tint = Color(0xFF00F59B), modifier = Modifier.size(82.dp))
    Text("X", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Black, modifier = Modifier.offset(x = 28.dp, y = 25.dp))
   }
   Spacer(Modifier.height(22.dp))
   Text("STOCK X", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 3.sp)
   Spacer(Modifier.height(7.dp))
   Text("Developed by ZEROX", color = Color(0xFF00D98A), fontSize = 12.sp, letterSpacing = 1.2.sp)
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
 AlertDialog(onDismissRequest = close, shape = RoundedCornerShape(26.dp), containerColor = MaterialTheme.colorScheme.surface, title = { Text("Add medicine", fontWeight = FontWeight.Bold) }, text = {
  Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
   Text("Enter product details and opening stock.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
   OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Medicine name *") }, singleLine = true, shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, cursorColor = Green))
   OutlinedTextField(strength, { strength = it }, Modifier.fillMaxWidth(), label = { Text("Strength") }, singleLine = true, shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, cursorColor = Green))
   OutlinedTextField(company, { company = it }, Modifier.fillMaxWidth(), label = { Text("Company") }, singleLine = true, shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, cursorColor = Green))
   OutlinedTextField(price, { price = it }, Modifier.fillMaxWidth(), label = { Text("Unit price (৳)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, cursorColor = Green))
   OutlinedTextField(qty, { qty = it }, Modifier.fillMaxWidth(), label = { Text("Opening quantity") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, cursorColor = Green))
   OutlinedTextField(min, { min = it }, Modifier.fillMaxWidth(), label = { Text("Low-stock threshold") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, cursorColor = Green))
  }
 }, confirmButton = { Button(enabled = name.isNotBlank(), onClick = { save(name.trim(), strength.trim(), company.trim(), price.toDoubleOrNull() ?: 0.0, qty.toIntOrNull()?.coerceAtLeast(0) ?: 0, min.toIntOrNull()?.coerceAtLeast(0) ?: 10) }, shape = RoundedCornerShape(13.dp), colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color(0xFF062016))) { Text("Save medicine", fontWeight = FontWeight.Bold) } }, dismissButton = { TextButton(onClick = close) { Text("Cancel") } })
}
@Composable
private fun StockDialog(item: MedicineEntity, mode: String, close: () -> Unit, confirm: (Int) -> Unit) {
 var amount by remember { mutableStateOf("1") }
 val number = amount.toIntOrNull() ?: 0
 val isIn = mode == "IN"
 AlertDialog(onDismissRequest = close, shape = RoundedCornerShape(26.dp), containerColor = MaterialTheme.colorScheme.surface, title = { Text(if (isIn) "Stock in" else "Stock out", fontWeight = FontWeight.Bold) }, text = {
  Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
   Text(item.name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
   Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp)) { Text("Available: \${item.quantity}", modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) }
    Surface(color = if (isIn) Color(0xFF123426) else Color(0xFF38251D), shape = RoundedCornerShape(12.dp)) { Text(if (isIn) "STOCK IN" else "STOCK OUT", modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp), color = if (isIn) Color(0xFF63E6AC) else Color(0xFFFFB18B), fontSize = 10.sp, fontWeight = FontWeight.Bold) }
   }
   OutlinedTextField(amount, { amount = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("Quantity") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, cursorColor = Green))
   if (!isIn && number > item.quantity) Text("Insufficient stock", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
  }
 }, confirmButton = { Button(enabled = number > 0 && (isIn || number <= item.quantity), onClick = { confirm(number) }, shape = RoundedCornerShape(13.dp), colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color(0xFF062016))) { Text("Confirm movement", fontWeight = FontWeight.Bold) } }, dismissButton = { TextButton(onClick = close) { Text("Cancel") } })
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
 Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) { Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold); Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) } }
}
