package com.example.ui.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class OutstandingPaymentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        OutstandingPaymentNotificationManager.syncOutstandingNotifications(context)
        OutstandingPaymentScheduler.schedulePeriodicCheck(context)
        OutstandingPaymentNotificationWorker.scheduleWork(context)
    }
}
