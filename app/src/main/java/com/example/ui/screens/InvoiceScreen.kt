package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.OrderJsonParser
import com.example.data.model.SaleOrderEntity
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatCurrency
import com.example.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceScreen(
    sale: SaleOrderEntity?,
    onBackClick: () -> Unit,
    onNewSaleClick: () -> Unit
) {
    val context = LocalContext.current
    var isThermalReceiptMode by remember { mutableStateOf(false) }

    val storagePermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Storage access granted! Tap Save PDF again to save.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Storage access denied. Cannot save PDF.", Toast.LENGTH_SHORT).show()
        }
    }

    if (sale == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(64.dp), tint = AppTextSecondary)
                Spacer(modifier = Modifier.height(12.dp))
                Text("No invoice selected", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onBackClick) { Text("Go to Sales") }
            }
        }
        return
    }

    val items = remember(sale.itemsJson) {
        OrderJsonParser.jsonToSaleItems(sale.itemsJson)
    }

    val formattedDate = remember(sale.createdAt) {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(sale.createdAt))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Invoice ${sale.invoiceNumber}", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Toggle Format
                    FilterChip(
                        selected = isThermalReceiptMode,
                        onClick = { isThermalReceiptMode = !isThermalReceiptMode },
                        label = { Text(if (isThermalReceiptMode) "Thermal 80mm" else "A4 Format") },
                        leadingIcon = {
                            Icon(
                                if (isThermalReceiptMode) Icons.Default.ReceiptLong else Icons.Default.Description,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isThermalReceiptMode) {
                ThermalReceiptView(sale = sale, items = items, formattedDate = formattedDate)
            } else {
                ModernA4InvoiceView(sale = sale, items = items, formattedDate = formattedDate)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons Area (Below and outside the invoice preview)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 620.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                InvoicePdfGenerator.shareInvoicePdf(
                                    context = context,
                                    sale = sale,
                                    items = items,
                                    formattedDate = formattedDate,
                                    isThermal = isThermalReceiptMode
                                )
                            },
                            modifier = Modifier.weight(1f).testTag("share_invoice_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share PDF", maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = {
                                val hasStorageAccess = if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.S_V2) {
                                    androidx.core.content.ContextCompat.checkSelfPermission(
                                        context,
                                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                } else {
                                    true
                                }

                                if (!hasStorageAccess) {
                                    Toast.makeText(context, "Storage permission is required to save PDF on this version of Android.", Toast.LENGTH_LONG).show()
                                    storagePermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                } else {
                                    InvoicePdfGenerator.downloadAndOpenPdf(
                                        context = context,
                                        sale = sale,
                                        items = items,
                                        formattedDate = formattedDate,
                                        isThermal = isThermalReceiptMode
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("download_invoice_pdf_button")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save PDF", maxLines = 1)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                printInvoiceDocument(context, sale, items, formattedDate)
                            },
                            modifier = Modifier.weight(1f).testTag("print_invoice_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandNavySecondary)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Print", maxLines = 1)
                        }

                        Button(
                            onClick = onNewSaleClick,
                            modifier = Modifier.weight(1f).testTag("new_sale_from_invoice"),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary)
                        ) {
                            Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("New Sale", maxLines = 1)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ModernA4InvoiceView(
    sale: SaleOrderEntity,
    items: List<com.example.data.model.SaleOrderItem>,
    formattedDate: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 700.dp)
            .border(1.dp, AppOutlineLight, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // Header: Company branding & Invoice title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(BrandBluePrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Inventory2, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Signix World",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp,
                                    color = BrandNavySecondary
                                )
                            )
                            Text(
                                text = "TAX INVOICE",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BrandBluePrimary
                                )
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "INVOICE #",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = AppTextSecondary
                        )
                    )
                    Text(
                        text = sale.invoiceNumber,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BrandNavySecondary
                        )
                    )
                    if (sale.taxInvoiceNumber.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "TAX INVOICE #",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = AppTextSecondary
                            )
                        )
                        Text(
                            text = sale.taxInvoiceNumber,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = BrandNavySecondary
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    StatusBadge(status = sale.paymentStatus)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = AppOutlineLight, thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // Bill To & Invoice Meta
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "INVOICE TO / CUSTOMER",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = AppTextSecondary,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = sale.customerName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = AppTextPrimary)
                    )
                    if (sale.customerPhone.isNotBlank()) {
                        Text(text = "Phone: ${sale.customerPhone}", style = MaterialTheme.typography.bodySmall.copy(color = AppTextSecondary))
                    }
                    if (sale.customerAddress.isNotBlank()) {
                        Text(text = "Address: ${sale.customerAddress}", style = MaterialTheme.typography.bodySmall.copy(color = AppTextSecondary))
                    }
                }

                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(
                        text = "INVOICE DETAILS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = AppTextSecondary,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Date : $formattedDate", style = MaterialTheme.typography.bodySmall.copy(color = AppTextPrimary, fontWeight = FontWeight.Medium))
                    Text("Payment : ${sale.paymentMethod}", style = MaterialTheme.typography.bodySmall.copy(color = AppTextPrimary))
                    Text("Tax ID : ${sale.taxId}", style = MaterialTheme.typography.bodySmall.copy(color = AppTextSecondary))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Items Table Header
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = BrandNavySecondary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("#", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color.White), modifier = Modifier.width(24.dp))
                    Text("Item & Description", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color.White), modifier = Modifier.weight(2f))
                    Text("Qty", textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color.White), modifier = Modifier.weight(0.6f))
                    Text("Price", textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color.White), modifier = Modifier.weight(1f))
                    Text("Total", textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color.White), modifier = Modifier.weight(1f))
                }
            }

            items.forEachIndexed { idx, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${idx + 1}", style = MaterialTheme.typography.bodySmall.copy(color = AppTextSecondary), modifier = Modifier.width(24.dp))
                    Row(
                        modifier = Modifier.weight(2f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (item.imageUrl.isNotBlank() && File(item.imageUrl).exists()) {
                            AsyncImage(
                                model = File(item.imageUrl),
                                contentDescription = item.productName,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Column {
                            Text(item.productName, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = AppTextPrimary))
                            Text("SKU: ${item.sku}", style = MaterialTheme.typography.labelSmall.copy(color = AppTextSecondary, fontSize = 10.sp))
                        }
                    }
                    Text("${item.quantity}", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.weight(0.6f))
                    Text(formatCurrency(item.unitPrice), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(color = AppTextPrimary), modifier = Modifier.weight(1f))
                    Text(formatCurrency(item.subtotal), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = AppTextPrimary), modifier = Modifier.weight(1f))
                }
                HorizontalDivider(color = AppOutlineLight.copy(alpha = 0.5f), thickness = 0.8.dp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Totals and Summary Box
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Notes & Payment instructions
                Column(modifier = Modifier.weight(1.2f)) {
                    if (sale.notes.isNotBlank()) {
                        Text("Notes:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        Text(sale.notes, style = MaterialTheme.typography.bodySmall.copy(color = AppTextSecondary))
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Text("Payment Terms:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    Text("Thank you for your business. Goods once sold can be returned within 14 days with original receipt.", style = MaterialTheme.typography.bodySmall.copy(color = AppTextSecondary, fontSize = 10.sp))
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Financial Breakdown
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    InvoiceSummaryRow("Subtotal:", formatCurrency(sale.subtotal))
                    if (sale.discountAmount > 0.0) {
                        InvoiceSummaryRow("Discount:", "-${formatCurrency(sale.discountAmount)}", color = DangerRed)
                    }
                    if (sale.taxAmount > 0.0) {
                        InvoiceSummaryRow("Tax (${sale.taxRatePercent}%):", formatCurrency(sale.taxAmount))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(color = AppOutlineLight)
                    Spacer(modifier = Modifier.height(6.dp))
                    InvoiceSummaryRow("Grand Total:", formatCurrency(sale.grandTotal), isBold = true, fontSize = 16.sp)
                    InvoiceSummaryRow("Paid Amount:", formatCurrency(sale.paidAmount), color = SuccessGreen, isBold = true)
                    InvoiceSummaryRow("Balance Due:", formatCurrency(sale.remainingBalance), color = if (sale.remainingBalance > 0) DangerRed else AppTextSecondary, isBold = true)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = AppOutlineLight)
            Spacer(modifier = Modifier.height(12.dp))

            // Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Authorized Signature: __________________",
                    style = MaterialTheme.typography.bodySmall.copy(color = AppTextSecondary, fontSize = 10.sp)
                )
                Text(
                    text = "Powered by Signix World",
                    style = MaterialTheme.typography.bodySmall.copy(color = AppTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
fun ThermalReceiptView(
    sale: SaleOrderEntity,
    items: List<com.example.data.model.SaleOrderItem>,
    formattedDate: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 380.dp)
            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFD)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "INVENTORY MASTER STORE",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            )
            Text(
                text = "100 Innovation Parkway, Austin TX",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            )
            Text(
                text = "Tel: +1 (800) 555-0199",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text("------------------------------------------", fontFamily = FontFamily.Monospace, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("RECEIPT #: ${sale.invoiceNumber}", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("DATE: $formattedDate", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("CUST: ${sale.customerName}", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text("==========================================", fontFamily = FontFamily.Monospace, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("ITEM", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                Text("QTY", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center)
                Text("PRICE", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                Text("TOTAL", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            }
            Spacer(modifier = Modifier.height(4.dp))

            items.forEach { item ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(item.productName, fontFamily = FontFamily.Monospace, fontSize = 11.sp, modifier = Modifier.weight(2f), maxLines = 1)
                    Text("${item.quantity}", fontFamily = FontFamily.Monospace, fontSize = 11.sp, modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center)
                    Text(formatCurrency(item.unitPrice), fontFamily = FontFamily.Monospace, fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                    Text(formatCurrency(item.subtotal), fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text("------------------------------------------", fontFamily = FontFamily.Monospace, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))

            ThermalRow("SUBTOTAL", formatCurrency(sale.subtotal))
            if (sale.discountAmount > 0.0) {
                ThermalRow("DISCOUNT", "-${formatCurrency(sale.discountAmount)}")
            }
            if (sale.taxAmount > 0.0) {
                ThermalRow("TAX (${sale.taxRatePercent}%)", formatCurrency(sale.taxAmount))
            }
            Spacer(modifier = Modifier.height(2.dp))
            ThermalRow("GRAND TOTAL", formatCurrency(sale.grandTotal), isBold = true)
            ThermalRow("PAID (${sale.paymentMethod})", formatCurrency(sale.paidAmount))
            ThermalRow("BALANCE DUE", formatCurrency(sale.remainingBalance), isBold = true)

            Spacer(modifier = Modifier.height(8.dp))
            Text("==========================================", fontFamily = FontFamily.Monospace, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))

            // Barcode illustration
            Surface(
                color = Color.Black,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(24.dp)
            ) {}
            Text(sale.invoiceNumber, fontFamily = FontFamily.Monospace, fontSize = 10.sp, letterSpacing = 2.sp)

            Spacer(modifier = Modifier.height(8.dp))
            Text("THANK YOU FOR YOUR PURCHASE!", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("Please come again", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
        }
    }
}

@Composable
fun ThermalRow(label: String, value: String, isBold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal)
        Text(value, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun InvoiceSummaryRow(
    label: String,
    value: String,
    color: Color = AppTextPrimary,
    isBold: Boolean = false,
    fontSize: androidx.compose.ui.unit.TextUnit = 13.sp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                fontSize = fontSize,
                color = if (isBold) AppTextPrimary else AppTextSecondary
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
                fontSize = fontSize,
                color = color
            )
        )
    }
}

fun printInvoiceDocument(
    context: Context,
    sale: SaleOrderEntity,
    items: List<com.example.data.model.SaleOrderItem>,
    formattedDate: String
) {
    try {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager != null) {
            val webView = WebView(context)
            val rowsHtml = items.joinToString("") {
                val imgTag = if (it.imageUrl.isNotBlank() && File(it.imageUrl).exists()) {
                    "<img src='file://${it.imageUrl}' style='width:24px;height:24px;object-fit:cover;border-radius:4px;vertical-align:middle;margin-right:8px;'/>"
                } else ""
                "<tr><td>$imgTag${it.productName}</td><td>${it.sku}</td><td style='text-align:center'>${it.quantity}</td><td style='text-align:right'>${formatCurrency(it.unitPrice)}</td><td style='text-align:right'>${formatCurrency(it.subtotal)}</td></tr>"
            }
            val htmlDocument = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Invoice ${sale.invoiceNumber}</title>
                    <style>
                        body { font-family: Arial, sans-serif; padding: 20px; color: #1E293B; }
                        .header { display: flex; justify-content: space-between; border-bottom: 2px solid #0284C7; padding-bottom: 10px; }
                        h1 { color: #0284C7; margin: 0; }
                        table { width: 100%; border-collapse: collapse; margin-top: 20px; }
                        th, td { border: 1px solid #CBD5E1; padding: 8px; text-align: left; }
                        th { background-color: #F1F5F9; }
                        .summary { float: right; margin-top: 20px; width: 300px; }
                        .summary-row { display: flex; justify-content: space-between; padding: 4px 0; }
                        .total { font-weight: bold; font-size: 1.1em; color: #0284C7; }
                    </style>
                </head>
                <body>
                    <div class="header">
                        <div>
                            <h1>INVENTORY MASTER</h1>
                            <p>Enterprise Solutions Inc.<br>Austin, TX | support@inventorymaster.io</p>
                        </div>
                        <div style="text-align:right">
                            <h2>INVOICE #${sale.invoiceNumber}</h2>
                            <p>Date: $formattedDate<br>Status: <strong>${sale.paymentStatus}</strong></p>
                        </div>
                    </div>
                    <div style="margin-top:20px;">
                        <strong>Bill To:</strong> ${sale.customerName}<br>
                        Phone: ${sale.customerPhone}<br>
                        Address: ${sale.customerAddress}
                    </div>
                    <table>
                        <thead>
                            <tr><th>Item</th><th>SKU</th><th>Qty</th><th>Price</th><th>Total</th></tr>
                        </thead>
                        <tbody>
                            $rowsHtml
                        </tbody>
                    </table>
                    <div class="summary">
                        <div class="summary-row"><span>Subtotal:</span> <span>${formatCurrency(sale.subtotal)}</span></div>
                        <div class="summary-row"><span>Discount:</span> <span>-${formatCurrency(sale.discountAmount)}</span></div>
                        <div class="summary-row"><span>Tax (${sale.taxRatePercent}%):</span> <span>${formatCurrency(sale.taxAmount)}</span></div>
                        <hr>
                        <div class="summary-row total"><span>Grand Total:</span> <span>${formatCurrency(sale.grandTotal)}</span></div>
                        <div class="summary-row"><span>Paid:</span> <span>${formatCurrency(sale.paidAmount)}</span></div>
                        <div class="summary-row"><span>Balance:</span> <span>${formatCurrency(sale.remainingBalance)}</span></div>
                    </div>
                </body>
                </html>
            """.trimIndent()

            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    val printAdapter = webView.createPrintDocumentAdapter("Invoice-${sale.invoiceNumber}")
                    val jobName = "Invoice_${sale.invoiceNumber}"
                    printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
                }
            }
            webView.loadDataWithBaseURL(null, htmlDocument, "text/html", "UTF-8", null)
            Toast.makeText(context, "Preparing Print...", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Print error: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
