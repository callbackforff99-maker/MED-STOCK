package com.zerox.medistock

import android.content.ContentValues
import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.zerox.medistock.data.MedicineDao
import com.zerox.medistock.data.MedicineEntity
import com.zerox.medistock.data.StockMovementEntity
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeParseException
import java.util.Locale

@Composable
internal fun MonthlyReportScreen(
    context: Context,
    dao: MedicineDao,
    medicines: List<MedicineEntity>,
    shopId: Long,
    onBack: () -> Unit
) {
    val today = remember { LocalDate.now() }
    var startText by remember { mutableStateOf(today.withDayOfMonth(1).toString()) }
    var endText by remember { mutableStateOf(today.toString()) }
    var shopName by remember {
        mutableStateOf(context.getSharedPreferences("stock_x_settings", Context.MODE_PRIVATE).getString("shop_name", "") ?: "")
    }
    var preview by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    val startDate = try { LocalDate.parse(startText) } catch (_: Exception) { null }
    val endDate = try { LocalDate.parse(endText) } catch (_: Exception) { null }
    val valid = startDate != null && endDate != null && !endDate.isBefore(startDate)
    val zone = remember { ZoneId.systemDefault() }
    val from = startDate?.atStartOfDay(zone)?.toInstant()?.toEpochMilli() ?: 0L
    val to = endDate?.plusDays(1)?.atStartOfDay(zone)?.toInstant()?.toEpochMilli() ?: 0L
    val movements by (if (valid) dao.observeMovements(shopId, from, to) else flowOf(emptyList()))
        .collectAsState(initial = emptyList())

    val sales = movements.filter { it.movementType == "OUT" }
    val purchases = movements.filter { it.movementType == "IN" }
    val salesTotal = sales.sumOf { it.totalAmount }
    val purchaseTotal = purchases.sumOf { it.totalAmount }
    val stockValue = medicines.sumOf { it.boxQuantity * it.boxPrice }
    val unsoldUnits = medicines.sumOf { it.boxQuantity }
    val unsoldValue = stockValue

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 18.dp, vertical = 12.dp)) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
            Text("Month-end report", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                OutlinedTextField(shopName, { shopName = it }, Modifier.fillMaxWidth(), label = { Text("Shop name") }, singleLine = true, shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00D98B), cursorColor = Color(0xFF00D98B)))
            }
            item {
                OutlinedTextField(startText, { startText = it }, Modifier.fillMaxWidth(), label = { Text("From date (YYYY-MM-DD)") }, singleLine = true, shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00D98B), cursorColor = Color(0xFF00D98B)))
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(endText, { endText = it }, Modifier.fillMaxWidth(), label = { Text("To date (YYYY-MM-DD)") }, singleLine = true, shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00D98B), cursorColor = Color(0xFF00D98B)))
                Text("Choose a month or enter a custom date range.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { startText = today.withDayOfMonth(1).toString(); endText = today.toString() }) { Text("This month") }
                    TextButton(onClick = {
                        val prev = today.minusMonths(1)
                        startText = prev.withDayOfMonth(1).toString()
                        endText = prev.withDayOfMonth(prev.lengthOfMonth()).toString()
                    }) { Text("Previous month") }
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("REPORT SUMMARY", color = Color(0xFF00D98B), style = MaterialTheme.typography.labelLarge)
                        Text("Sales revenue: ৳" + money(salesTotal))
                        Text("Stock purchase cost: ৳" + money(purchaseTotal))
                        Text("Current stock value: ৳" + money(stockValue))
                        Text("Unsold units: $unsoldUnits")
                        Text("Unsold stock value: ৳" + money(unsoldValue))
                        Text("Stock IN: ${purchases.sumOf { it.quantity }} units  •  Stock OUT: ${sales.sumOf { it.quantity }} units", color = Color.Gray)
                    }
                }
            }
            item {
                Text("Movement records: ${movements.size}", color = Color.Gray)
                if (movements.isEmpty() && valid) Text("No stock IN/OUT history recorded for this date range. Older movements cannot be reconstructed.", color = Color(0xFF9A4D16))
            }
            item {
                Button(
                    onClick = {
                        context.getSharedPreferences("stock_x_settings", Context.MODE_PRIVATE).edit().putString("shop_name", shopName.trim()).apply()
                        preview = true
                        status = ""
                    },
                    enabled = valid && shopName.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D98B), contentColor = Color(0xFF062016))
                ) { Text("Preview report", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) }
                if (!valid) Text("Enter a valid date range.", color = MaterialTheme.colorScheme.error)
                if (status.isNotBlank()) Text(status, color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    if (preview) {
        AlertDialog(
            onDismissRequest = { preview = false },
            title = { Text("PDF preview") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(shopName, style = MaterialTheme.typography.titleMedium)
                    Text("$startText  —  $endText")
                    HorizontalDivider()
                    Text("Sales revenue     ৳${money(salesTotal)}")
                    Text("Purchases         ৳${money(purchaseTotal)}")
                    Text("Current stock     ৳${money(stockValue)}")
                    Text("Unsold products   $unsoldUnits units")
                    Text("Recorded movements: ${movements.size}")
                    if (movements.isEmpty()) Text("Historical sales data is not available for this period.", color = Color(0xFF9A4D16))
                }
            },
            confirmButton = {
                Button(onClick = {
                    status = createStockReport(context, shopName, startText, endText, medicines, movements, salesTotal, purchaseTotal, stockValue)
                    preview = false
                }, shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D98B), contentColor = Color(0xFF062016))) { Text("Download PDF") }
            },
            dismissButton = { TextButton(onClick = { preview = false }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)) { Text("Close") } }
        )
    }
}

private fun money(value: Double): String = String.format(Locale.US, "%.2f", value)

private fun createStockReport(
    context: Context,
    shop: String,
    start: String,
    end: String,
    medicines: List<MedicineEntity>,
    movements: List<StockMovementEntity>,
    sales: Double,
    purchases: Double,
    stockValue: Double
): String {
    val pdf = PdfDocument()
    val pageWidth = 595
    val pageHeight = 842
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(35, 39, 37)
        textSize = 22f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(65, 70, 67)
        textSize = 10.5f
    }
    val boldPaint = Paint(bodyPaint).apply { typeface = Typeface.DEFAULT_BOLD }
    val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(0, 150, 95)
        strokeWidth = 2.5f
    }
    var pageNumber = 0
    var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, ++pageNumber).create())
    var canvas = page.canvas
    var y = 0f

    fun drawFooter() {
        canvas.drawLine(42f, 812f, 553f, 812f, accentPaint)
        canvas.drawText("STOCK X  •  ZEROX", 42f, 829f, bodyPaint)
        canvas.drawText("Page $pageNumber", 495f, 829f, bodyPaint)
    }

    fun drawPageHeader(continued: Boolean) {
        y = 50f
        canvas.drawText("STOCK X", 42f, y, titlePaint)
        canvas.drawText("Developed by ZEROX", 420f, y, bodyPaint)
        y += 22f
        canvas.drawLine(42f, y, 553f, y, accentPaint)
        y += 27f
        if (continued) {
            canvas.drawText("PRODUCT INVENTORY (CONTINUED)", 42f, y, boldPaint)
            y += 24f
            canvas.drawText("Medicine", 42f, y, boldPaint)
            canvas.drawText("Qty", 365f, y, boldPaint)
            canvas.drawText("Stock value", 430f, y, boldPaint)
            y += 18f
        }
    }

    drawPageHeader(false)
    canvas.drawText(shop.take(54), 42f, y, titlePaint.apply { textSize = 18f })
    y += 21f
    canvas.drawText("Inventory report  |  $start to $end", 42f, y, bodyPaint)
    y += 31f
    val summaryRows = listOf(
        "Sales revenue" to "৳${money(sales)}",
        "Stock purchase cost" to "৳${money(purchases)}",
        "Current stock value" to "৳${money(stockValue)}",
        "Products currently in stock" to medicines.sumOf { it.boxQuantity }.toString(),
        "Stock IN units" to movements.filter { it.movementType == "IN" }.sumOf { it.quantity }.toString(),
        "Stock OUT units" to movements.filter { it.movementType == "OUT" }.sumOf { it.quantity }.toString()
    )
    summaryRows.forEach { (label, value) ->
        canvas.drawText(label, 48f, y, bodyPaint)
        canvas.drawText(value, 390f, y, bodyPaint)
        y += 19f
    }
    y += 10f
    canvas.drawText("PRODUCT INVENTORY", 42f, y, boldPaint)
    y += 23f
    canvas.drawText("Medicine", 42f, y, boldPaint)
    canvas.drawText("Qty", 365f, y, boldPaint)
    canvas.drawText("Stock value", 430f, y, boldPaint)
    y += 18f

    medicines.forEach { med ->
        if (y > 785f) {
            drawFooter()
            pdf.finishPage(page)
            page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, ++pageNumber).create())
            canvas = page.canvas
            drawPageHeader(true)
        }
        canvas.drawText(med.name.take(42), 42f, y, bodyPaint)
        canvas.drawText(med.boxQuantity.toString(), 365f, y, bodyPaint)
        canvas.drawText("৳${money(med.boxQuantity * med.boxPrice)}", 430f, y, bodyPaint)
        y += 18f
    }
    if (medicines.isEmpty()) {
        canvas.drawText("No medicines are currently registered.", 42f, y, bodyPaint)
        y += 18f
    }
    if (movements.isEmpty()) {
        if (y > 780f) {
            drawFooter()
            pdf.finishPage(page)
            page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, ++pageNumber).create())
            canvas = page.canvas
            drawPageHeader(true)
        }
        y += 10f
        canvas.drawText("Note: No historical stock movements were recorded for this period.", 42f, y, bodyPaint)
    }
    drawFooter()
    pdf.finishPage(page)

    return try {
        val filename = "STOCK-X-Report-$start-to-$end.pdf"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Download")
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("Could not create PDF file")
        resolver.openOutputStream(uri)?.use { pdf.writeTo(it) }
            ?: throw IllegalStateException("Could not write PDF")
        "PDF saved to Downloads/$filename"
    } catch (e: Exception) {
        "PDF download failed: ${e.message ?: "Unknown error"}"
    } finally {
        pdf.close()
    }
}
