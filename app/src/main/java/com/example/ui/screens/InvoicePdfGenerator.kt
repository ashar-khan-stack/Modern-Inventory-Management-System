package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.SaleOrderEntity
import com.example.data.model.SaleOrderItem
import com.example.ui.components.formatCurrency
import java.io.File
import java.io.FileOutputStream

object InvoicePdfGenerator {

    /**
     * Generates a real, high-resolution A4 PDF document for the invoice.
     */
    fun generateA4Pdf(
        context: Context,
        sale: SaleOrderEntity,
        items: List<SaleOrderItem>,
        formattedDate: String
    ): File {
        val pdfDocument = PdfDocument()
        val pageWidth = 595 // Standard A4 width in points
        val pageHeight = 842 // Standard A4 height in points
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Paints
        val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59)
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val paintBold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val paintPrimary = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(2, 132, 199) // #0284C7
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val paintSecondary = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 116, 139) // #64748B
            textSize = 9f
        }

        val paintFill = Paint().apply {
            style = Paint.Style.FILL
        }

        val paintStroke = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f
            color = Color.rgb(226, 232, 240)
        }

        // Draw Background
        canvas.drawColor(Color.WHITE)

        val margin = 36f
        var curY = 45f

        // 1. Top Brand Logo & Company Info
        // Draw Brand Icon box
        paintFill.color = Color.rgb(2, 132, 199)
        canvas.drawRoundRect(RectF(margin, curY, margin + 36f, curY + 36f), 8f, 8f, paintFill)
        paintBold.color = Color.WHITE
        paintBold.textSize = 14f
        canvas.drawText("SW", margin + 6f, curY + 23f, paintBold)

        // Company Name
        paintBold.color = Color.rgb(15, 23, 42)
        paintBold.textSize = 15f
        canvas.drawText("Signix World", margin + 44f, curY + 16f, paintBold)
        paintPrimary.textAlign = Paint.Align.LEFT
        paintPrimary.textSize = 11f
        canvas.drawText("TAX INVOICE", margin + 44f, curY + 30f, paintPrimary)

        // Right side: Tax Invoice Header
        paintBold.textAlign = Paint.Align.RIGHT
        paintBold.textSize = 9.5f
        paintBold.color = Color.rgb(100, 116, 139)
        canvas.drawText("INVOICE #", pageWidth - margin, curY + 10f, paintBold)

        paintBold.textSize = 11f
        paintBold.color = Color.rgb(15, 23, 42)
        canvas.drawText(sale.invoiceNumber, pageWidth - margin, curY + 22f, paintBold)

        if (sale.taxInvoiceNumber.isNotBlank()) {
            paintBold.textSize = 8.5f
            paintBold.color = Color.rgb(100, 116, 139)
            canvas.drawText("TAX INVOICE #", pageWidth - margin, curY + 33f, paintBold)

            paintBold.textSize = 10f
            paintBold.color = Color.rgb(15, 23, 42)
            canvas.drawText(sale.taxInvoiceNumber, pageWidth - margin, curY + 44f, paintBold)
        }

        curY += 56f

        // Top Divider
        canvas.drawLine(margin, curY, pageWidth - margin, curY, paintStroke)
        curY += 16f

        // 2. Bill To & Invoice Info Box
        val colWidth = (pageWidth - margin * 2) / 2f

        // Bill To (Left)
        paintBold.textAlign = Paint.Align.LEFT
        paintBold.textSize = 9.5f
        paintBold.color = Color.rgb(100, 116, 139)
        canvas.drawText("BILL TO / CUSTOMER", margin, curY, paintBold)

        paintBold.color = Color.rgb(15, 23, 42)
        paintBold.textSize = 11f
        canvas.drawText(sale.customerName, margin, curY + 14f, paintBold)

        paintText.textAlign = Paint.Align.LEFT
        paintText.textSize = 9f
        var custY = curY + 26f
        if (sale.customerPhone.isNotBlank()) {
            canvas.drawText("Phone: ${sale.customerPhone}", margin, custY, paintText)
            custY += 12f
        }
        if (sale.customerAddress.isNotBlank()) {
            canvas.drawText("Address: ${sale.customerAddress}", margin, custY, paintText)
            custY += 12f
        }

        // Invoice Meta (Right)
        val rightX = pageWidth - margin
        paintBold.textAlign = Paint.Align.RIGHT
        paintBold.textSize = 9.5f
        paintBold.color = Color.rgb(100, 116, 139)
        canvas.drawText("INVOICE DETAILS", rightX, curY, paintBold)

        paintText.textAlign = Paint.Align.RIGHT
        paintText.textSize = 9f
        canvas.drawText("Date: $formattedDate", rightX, curY + 14f, paintText)
        canvas.drawText("Payment: ${sale.paymentMethod} (${sale.paymentStatus})", rightX, curY + 26f, paintText)
        canvas.drawText("Tax ID: ${sale.taxId}", rightX, curY + 38f, paintText)
        if (sale.taxInvoiceNumber.isNotBlank()) {
            canvas.drawText("Tax Inv: ${sale.taxInvoiceNumber}", rightX, curY + 50f, paintText)
        }

        curY = maxOf(custY, curY + 56f) + 10f

        // 3. Items Table Header
        paintFill.color = Color.rgb(241, 245, 249)
        canvas.drawRoundRect(RectF(margin, curY, pageWidth - margin, curY + 22f), 4f, 4f, paintFill)

        paintBold.textSize = 9f
        paintBold.color = Color.rgb(71, 85, 105)
        paintBold.textAlign = Paint.Align.LEFT
        canvas.drawText("#", margin + 8f, curY + 14f, paintBold)
        canvas.drawText("Item & Description", margin + 35f, curY + 14f, paintBold)

        paintBold.textAlign = Paint.Align.CENTER
        canvas.drawText("Qty", margin + 320f, curY + 14f, paintBold)

        paintBold.textAlign = Paint.Align.RIGHT
        canvas.drawText("Price", margin + 410f, curY + 14f, paintBold)
        canvas.drawText("Total", pageWidth - margin - 8f, curY + 14f, paintBold)

        curY += 26f

        // 4. Items Table Rows
        items.forEachIndexed { index, item ->
            val rowStartY = curY
            val itemHeight = 32f

            // Check if product has an image
            var textStartX = margin + 35f
            if (item.imageUrl.isNotBlank()) {
                val imgFile = File(item.imageUrl)
                if (imgFile.exists()) {
                    try {
                        val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                        val bmp = BitmapFactory.decodeFile(imgFile.absolutePath, opts)
                        if (bmp != null) {
                            val destRect = RectF(textStartX, rowStartY, textStartX + 24f, rowStartY + 24f)
                            canvas.drawBitmap(bmp, null, destRect, null)
                            bmp.recycle()
                            textStartX += 30f
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            // Index
            paintText.textAlign = Paint.Align.LEFT
            paintText.textSize = 9f
            paintText.color = Color.rgb(100, 116, 139)
            canvas.drawText("${index + 1}", margin + 8f, rowStartY + 14f, paintText)

            // Product Name & SKU
            paintBold.textAlign = Paint.Align.LEFT
            paintBold.textSize = 9.5f
            paintBold.color = Color.rgb(15, 23, 42)
            canvas.drawText(item.productName, textStartX, rowStartY + 11f, paintBold)

            paintSecondary.textAlign = Paint.Align.LEFT
            paintSecondary.textSize = 8f
            canvas.drawText("SKU: ${item.sku}", textStartX, rowStartY + 21f, paintSecondary)

            // Qty
            paintText.textAlign = Paint.Align.CENTER
            paintText.color = Color.rgb(15, 23, 42)
            canvas.drawText("${item.quantity}", margin + 320f, rowStartY + 14f, paintText)

            // Price
            paintText.textAlign = Paint.Align.RIGHT
            paintText.color = Color.rgb(71, 85, 105)
            canvas.drawText(formatCurrency(item.unitPrice), margin + 410f, rowStartY + 14f, paintText)

            // Total
            paintBold.textAlign = Paint.Align.RIGHT
            paintBold.textSize = 9.5f
            paintBold.color = Color.rgb(15, 23, 42)
            canvas.drawText(formatCurrency(item.subtotal), pageWidth - margin - 8f, rowStartY + 14f, paintBold)

            curY += itemHeight
            canvas.drawLine(margin, curY, pageWidth - margin, curY, paintStroke)
            curY += 6f
        }

        curY += 10f

        // 5. Summary Section & Notes
        val summaryBoxLeft = pageWidth - margin - 200f

        // Left notes
        val notesWidth = summaryBoxLeft - margin - 20f
        paintBold.textAlign = Paint.Align.LEFT
        paintBold.textSize = 9f
        paintBold.color = Color.rgb(15, 23, 42)
        canvas.drawText("Terms & Instructions:", margin, curY, paintBold)

        paintSecondary.textSize = 8f
        canvas.drawText("Thank you for your business.", margin, curY + 12f, paintSecondary)
        canvas.drawText("Goods once sold can be returned within 14 days with original receipt.", margin, curY + 22f, paintSecondary)
        if (sale.notes.isNotBlank()) {
            canvas.drawText("Notes: ${sale.notes}", margin, curY + 34f, paintSecondary)
        }

        // Right Financial Summary
        var sumY = curY
        fun drawSummaryLine(label: String, value: String, isBold: Boolean = false, colorVal: Int = Color.rgb(15, 23, 42)) {
            val p = if (isBold) paintBold else paintText
            p.textSize = if (isBold) 10f else 9f
            p.textAlign = Paint.Align.LEFT
            p.color = Color.rgb(100, 116, 139)
            canvas.drawText(label, summaryBoxLeft, sumY, p)

            p.textAlign = Paint.Align.RIGHT
            p.color = colorVal
            canvas.drawText(value, pageWidth - margin - 8f, sumY, p)
            sumY += 14f
        }

        drawSummaryLine("Subtotal:", formatCurrency(sale.subtotal))
        if (sale.discountAmount > 0.0) {
            drawSummaryLine("Discount:", "-${formatCurrency(sale.discountAmount)}", colorVal = Color.rgb(220, 38, 38))
        }
        if (sale.taxAmount > 0.0) {
            drawSummaryLine("Tax (${sale.taxRatePercent}%):", formatCurrency(sale.taxAmount))
        }
        canvas.drawLine(summaryBoxLeft, sumY - 2f, pageWidth - margin, sumY - 2f, paintStroke)
        sumY += 4f
        drawSummaryLine("Grand Total:", formatCurrency(sale.grandTotal), isBold = true, colorVal = Color.rgb(2, 132, 199))
        drawSummaryLine("Paid Amount:", formatCurrency(sale.paidAmount), isBold = true, colorVal = Color.rgb(22, 163, 74))
        drawSummaryLine("Balance Due:", formatCurrency(sale.remainingBalance), isBold = true, colorVal = if (sale.remainingBalance > 0) Color.rgb(220, 38, 38) else Color.rgb(100, 116, 139))

        // 6. Footer & Signature
        val footerY = pageHeight - 50f
        canvas.drawLine(margin, footerY, pageWidth - margin, footerY, paintStroke)

        paintSecondary.textAlign = Paint.Align.LEFT
        paintSecondary.textSize = 8.5f
        canvas.drawText("Authorized Signature: _______________________", margin, footerY + 20f, paintSecondary)

        paintSecondary.textAlign = Paint.Align.RIGHT
        canvas.drawText("Powered by Signix World", pageWidth - margin, footerY + 20f, paintSecondary)

        pdfDocument.finishPage(page)

        // Save PDF to cache / files dir
        val outputDir = File(context.cacheDir, "invoices")
        if (!outputDir.exists()) outputDir.mkdirs()
        val outputFile = File(outputDir, "Invoice_${sale.invoiceNumber}.pdf")

        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return outputFile
    }

    /**
     * Generates a real Thermal Receipt PDF document (80mm width).
     */
    fun generateThermalPdf(
        context: Context,
        sale: SaleOrderEntity,
        items: List<SaleOrderItem>,
        formattedDate: String
    ): File {
        val pdfDocument = PdfDocument()
        val pageWidth = 226 // ~80mm at 72dpi
        val estimatedHeight = 350 + (items.size * 25)
        val pageHeight = maxOf(450, estimatedHeight)
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        canvas.drawColor(Color.WHITE)

        val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 8.5f
            typeface = Typeface.MONOSPACE
        }

        val paintBold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 9.5f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        val margin = 12f
        var curY = 24f

        // Center Header
        paintBold.textAlign = Paint.Align.CENTER
        paintBold.textSize = 11f
        canvas.drawText("INVENTORY MASTER", pageWidth / 2f, curY, paintBold)
        curY += 12f

        paintText.textAlign = Paint.Align.CENTER
        paintText.textSize = 7.5f
        canvas.drawText("100 Innovation Pkwy, Austin TX", pageWidth / 2f, curY, paintText)
        curY += 10f
        canvas.drawText("Tel: +1 (800) 555-0199", pageWidth / 2f, curY, paintText)
        curY += 12f

        canvas.drawText("------------------------------------", pageWidth / 2f, curY, paintText)
        curY += 10f

        // Info
        paintText.textAlign = Paint.Align.LEFT
        canvas.drawText("REC #: ${sale.invoiceNumber}", margin, curY, paintText)
        curY += 10f
        canvas.drawText("DATE: $formattedDate", margin, curY, paintText)
        curY += 10f
        canvas.drawText("CUST: ${sale.customerName}", margin, curY, paintText)
        curY += 12f

        paintText.textAlign = Paint.Align.CENTER
        canvas.drawText("====================================", pageWidth / 2f, curY, paintText)
        curY += 12f

        // Table Header
        paintBold.textAlign = Paint.Align.LEFT
        paintBold.textSize = 8f
        canvas.drawText("ITEM", margin, curY, paintBold)
        paintBold.textAlign = Paint.Align.CENTER
        canvas.drawText("QTY", margin + 110f, curY, paintBold)
        paintBold.textAlign = Paint.Align.RIGHT
        canvas.drawText("TOTAL", pageWidth - margin, curY, paintBold)
        curY += 10f

        // Items
        items.forEach { item ->
            paintText.textAlign = Paint.Align.LEFT
            val itemName = if (item.productName.length > 14) item.productName.substring(0, 12) + ".." else item.productName
            canvas.drawText(itemName, margin, curY, paintText)

            paintText.textAlign = Paint.Align.CENTER
            canvas.drawText("${item.quantity}", margin + 110f, curY, paintText)

            paintText.textAlign = Paint.Align.RIGHT
            canvas.drawText(formatCurrency(item.subtotal), pageWidth - margin, curY, paintText)
            curY += 12f
        }

        paintText.textAlign = Paint.Align.CENTER
        canvas.drawText("------------------------------------", pageWidth / 2f, curY, paintText)
        curY += 12f

        // Totals
        fun drawThermalTotal(lbl: String, amt: String, isB: Boolean = false) {
            val p = if (isB) paintBold else paintText
            p.textAlign = Paint.Align.LEFT
            canvas.drawText(lbl, margin, curY, p)
            p.textAlign = Paint.Align.RIGHT
            canvas.drawText(amt, pageWidth - margin, curY, p)
            curY += 11f
        }

        drawThermalTotal("SUBTOTAL", formatCurrency(sale.subtotal))
        if (sale.discountAmount > 0.0) drawThermalTotal("DISCOUNT", "-${formatCurrency(sale.discountAmount)}")
        if (sale.taxAmount > 0.0) drawThermalTotal("TAX (${sale.taxRatePercent}%)", formatCurrency(sale.taxAmount))
        drawThermalTotal("GRAND TOTAL", formatCurrency(sale.grandTotal), isB = true)
        drawThermalTotal("PAID (${sale.paymentMethod})", formatCurrency(sale.paidAmount))
        drawThermalTotal("BALANCE DUE", formatCurrency(sale.remainingBalance), isB = true)

        curY += 6f
        paintText.textAlign = Paint.Align.CENTER
        canvas.drawText("====================================", pageWidth / 2f, curY, paintText)
        curY += 14f

        paintBold.textAlign = Paint.Align.CENTER
        paintBold.textSize = 8.5f
        canvas.drawText("THANK YOU FOR YOUR PURCHASE!", pageWidth / 2f, curY, paintBold)
        curY += 10f
        paintText.textAlign = Paint.Align.CENTER
        paintText.textSize = 7.5f
        canvas.drawText("Please visit us again", pageWidth / 2f, curY, paintText)

        pdfDocument.finishPage(page)

        val outputDir = File(context.cacheDir, "receipts")
        if (!outputDir.exists()) outputDir.mkdirs()
        val outputFile = File(outputDir, "Receipt-${sale.invoiceNumber}.pdf")

        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return outputFile
    }

    /**
     * Shares the generated real PDF file using Android's native sharing mechanism.
     */
    fun shareInvoicePdf(
        context: Context,
        sale: SaleOrderEntity,
        items: List<SaleOrderItem>,
        formattedDate: String,
        isThermal: Boolean = false
    ) {
        try {
            val pdfFile = if (isThermal) {
                generateThermalPdf(context, sale, items, formattedDate)
            } else {
                generateA4Pdf(context, sale, items, formattedDate)
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Invoice #${sale.invoiceNumber} - ${sale.customerName}")
                putExtra(Intent.EXTRA_TEXT, "Attached is Invoice #${sale.invoiceNumber} from Inventory Master.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Invoice PDF (${pdfFile.name})")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error sharing PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Downloads/opens the real PDF file.
     */
    fun downloadAndOpenPdf(
        context: Context,
        sale: SaleOrderEntity,
        items: List<SaleOrderItem>,
        formattedDate: String,
        isThermal: Boolean = false
    ) {
        try {
            val pdfFile = if (isThermal) {
                generateThermalPdf(context, sale, items, formattedDate)
            } else {
                generateA4Pdf(context, sale, items, formattedDate)
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            Toast.makeText(context, "PDF generated: ${pdfFile.name}", Toast.LENGTH_SHORT).show()
            context.startActivity(Intent.createChooser(viewIntent, "Open Invoice PDF"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Invoice PDF saved at: ${sale.invoiceNumber}.pdf", Toast.LENGTH_LONG).show()
        }
    }
}
