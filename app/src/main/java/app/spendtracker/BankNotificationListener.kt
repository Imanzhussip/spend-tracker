package app.spendtracker

import android.app.Notification
import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors

/** Receives every notification, but reads only those from the bank apps the user picked. */
class BankNotificationListener : NotificationListenerService() {
    private val worker = Executors.newSingleThreadExecutor()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in BankApps.selected(this)) return

        val notification = sbn.notification
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        if (notification.flags and Notification.FLAG_ONGOING_EVENT != 0) return

        val extras = notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val body = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString()
        val key = sbn.key

        worker.execute { handle(key, title, body) }
    }

    private fun handle(key: String, title: String?, body: String?) {
        val db = Db.get(this)
        val text = listOfNotNull(title, body).joinToString(" — ")
        if (db.isRepeatAlert(key, text, REPEAT_WINDOW_MILLIS)) return

        when (val result = AmountParser.parse(title, body, Currencies.bank(this))) {
            is ParseResult.Spend -> {
                // This thread only handles alerts, so waiting here can't hold up anything else.
                if (result.currency != Currencies.display(this).code) {
                    runBlocking { Rates.refreshIfDue(this@BankNotificationListener, RATE_TIMEOUT_MILLIS) }
                }
                val money = moneyView(this)
                db.logAlert(key, text, "Added ${formatAmount(result.cents, result.currency)}")
                val id = db.addExpense(
                    cents = result.cents,
                    currency = result.currency,
                    convertedCents = money.inDisplay(result.cents, result.currency),
                    convertedCurrency = money.display.code,
                )
                CategoryPrompt.show(this, id, money.show(result.cents, result.currency))
            }
            is ParseResult.Skipped -> db.logAlert(key, text, "Skipped: ${result.reason}")
        }
    }

    override fun onListenerDisconnected() {
        requestRebind(ComponentName(this, BankNotificationListener::class.java))
    }

    override fun onDestroy() {
        worker.shutdown()
        super.onDestroy()
    }

    private companion object {
        const val REPEAT_WINDOW_MILLIS = 2 * 60 * 1000L

        // Short, so a bad connection only briefly delays the question about the purchase.
        const val RATE_TIMEOUT_MILLIS = 4_000
    }
}
