package com.example.ui.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object OutstandingPaymentNotificationManager {

    private const val CHANNEL_ID = "outstanding_payments_channel"
    private const val CHANNEL_NAME = "Outstanding Payments"
    private const val PREFS_NAME = "outstanding_notification_prefs"
    private const val KEY_ACTIVE_NOTIFICATION_IDS = "active_notification_ids"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    ?: return
            val existing = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications for unpaid, partially paid, and pending transactions"
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    fun syncOutstandingNotifications(context: Context) {
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                performSyncInternal(appContext)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun performSyncInternal(context: Context) {
        createNotificationChannel(context)

        // Check notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPerm = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!hasPerm) {
                return
            }
        }

        val db = AppDatabase.getInstance(context)
        val sales = db.saleDao().getAllSalesList()
        val expenses = db.expenseDao().getAllExpensesList()
        val salaries = db.salaryDao().getAllSalariesList()

        val activeNotificationIdsNow = mutableSetOf<Int>()
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 1. Process Sales / Customer Receivables
        for (sale in sales) {
            val isPaidStatus = sale.paymentStatus.equals("Paid", ignoreCase = true)
            val isOutstanding = !isPaidStatus && (
                    sale.remainingBalance > 0.001 ||
                    sale.paymentStatus.equals("Unpaid", ignoreCase = true) ||
                    sale.paymentStatus.equals("Partial", ignoreCase = true) ||
                    sale.paymentStatus.equals("Partially Paid", ignoreCase = true) ||
                    sale.paymentStatus.equals("Pending", ignoreCase = true)
            )

            val notifId = (sale.id % 100000 + 10000).toInt()

            if (isOutstanding && sale.remainingBalance > 0.001) {
                activeNotificationIdsNow.add(notifId)
                val statusText = when {
                    sale.paymentStatus.equals("Partial", ignoreCase = true) ||
                    sale.paymentStatus.equals("Partially Paid", ignoreCase = true) -> "Partially Paid"
                    sale.paymentStatus.equals("Unpaid", ignoreCase = true) -> "Unpaid"
                    else -> sale.paymentStatus
                }
                val contentText = "Payment Pending – Customer: ${sale.customerName} – Invoice #${sale.invoiceNumber} – Outstanding: Rs. ${"%,.2f".format(sale.remainingBalance)}"

                val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Sale Outstanding ($statusText)")
                    .setContentText(contentText)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setOngoing(true)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(false)

                notificationManager.notify(notifId, builder.build())
            } else {
                notificationManager.cancel(notifId)
            }
        }

        // 2. Process Expenses & Supplier Payables / Purchases
        for (expense in expenses) {
            val isPaidStatus = expense.paymentStatus.equals("Paid", ignoreCase = true)
            val isOutstanding = !isPaidStatus && (
                    expense.remainingBalance > 0.001 ||
                    expense.paymentStatus.equals("Unpaid", ignoreCase = true) ||
                    expense.paymentStatus.equals("Partial", ignoreCase = true) ||
                    expense.paymentStatus.equals("Partially Paid", ignoreCase = true) ||
                    expense.paymentStatus.equals("Pending", ignoreCase = true)
            )

            val notifId = (expense.id % 100000 + 30000).toInt()

            if (isOutstanding && expense.remainingBalance > 0.001) {
                activeNotificationIdsNow.add(notifId)
                val isPurchase = expense.category.equals("Purchases", ignoreCase = true) ||
                        expense.category.equals("Stock", ignoreCase = true) ||
                        expense.category.equals("Procurement", ignoreCase = true)
                val titleText = if (isPurchase) "Supplier Payable / Purchase Pending" else "Expense Payment Pending"
                val descStr = expense.description.ifBlank { expense.category }
                val contentText = if (isPurchase) {
                    "Payment Pending – Supplier/Purchase: $descStr – Outstanding: Rs. ${"%,.2f".format(expense.remainingBalance)}"
                } else {
                    "Payment Pending – Expense: ${expense.category} ($descStr) – Outstanding: Rs. ${"%,.2f".format(expense.remainingBalance)}"
                }

                val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(titleText)
                    .setContentText(contentText)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setOngoing(true)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(false)

                notificationManager.notify(notifId, builder.build())
            } else {
                notificationManager.cancel(notifId)
            }
        }

        // 3. Process Employee Salary Payments
        for (salary in salaries) {
            val isPaidStatus = salary.paymentStatus.equals("Paid", ignoreCase = true)
            val isOutstanding = !isPaidStatus && (
                    salary.paymentStatus.equals("Unpaid", ignoreCase = true) ||
                    salary.paymentStatus.equals("Pending", ignoreCase = true) ||
                    salary.paymentStatus.equals("Partial", ignoreCase = true) ||
                    salary.paymentStatus.equals("Partially Paid", ignoreCase = true)
            )

            val notifId = (salary.id % 100000 + 50000).toInt()

            if (isOutstanding && salary.netSalary > 0.001) {
                activeNotificationIdsNow.add(notifId)
                val contentText = "Payment Pending – Salary: ${salary.employeeName} (${salary.monthYear}) – Outstanding: Rs. ${"%,.2f".format(salary.netSalary)}"

                val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Salary Payment Pending")
                    .setContentText(contentText)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setOngoing(true)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(false)

                notificationManager.notify(notifId, builder.build())
            } else {
                notificationManager.cancel(notifId)
            }
        }

        // 4. Cancel any stored active notification IDs that are no longer outstanding
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val previouslySavedIds = prefs.getStringSet(KEY_ACTIVE_NOTIFICATION_IDS, emptySet()) ?: emptySet()
        for (savedIdStr in previouslySavedIds) {
            val savedId = savedIdStr.toIntOrNull() ?: continue
            if (!activeNotificationIdsNow.contains(savedId)) {
                notificationManager.cancel(savedId)
            }
        }

        // Save active notification IDs
        val updatedSet = activeNotificationIdsNow.map { it.toString() }.toSet()
        prefs.edit().putStringSet(KEY_ACTIVE_NOTIFICATION_IDS, updatedSet).apply()
    }
}
