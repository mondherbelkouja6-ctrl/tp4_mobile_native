package tn.isetbizerte.tp4_alertes

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object Alertes {
    const val CANAL_ID = "alertes_critiques"
    const val EXTRA_ACTION = "action"
    const val EXTRA_TITRE = "titre"
    const val EXTRA_ID = "id"

    fun creerCanal(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CANAL_ID,
                "Alertes critiques",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Alertes des capteurs nécessitant une action"
            }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    fun autorisee(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    @SuppressLint("MissingPermission")
    fun afficher(context: Context, titre: String, texte: String): Int {
        val id = (System.currentTimeMillis() % 100_000).toInt()
        val notification = NotificationCompat.Builder(context, CANAL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(titre)
            .setContentText(texte)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(intentVoir(context, id, titre))
            .addAction(0, "Voir", intentVoir(context, id, titre))
            .addAction(0, "Ignorer", intentIgnorer(context, id))
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
        return id
    }

    fun fermer(context: Context, id: Int) {
        NotificationManagerCompat.from(context).cancel(id)
    }

    private fun intentVoir(context: Context, id: Int, titre: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ACTION, "voir")
            putExtra(EXTRA_TITRE, titre)
            putExtra(EXTRA_ID, id)
        }
        return PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun intentIgnorer(context: Context, id: Int): PendingIntent {
        val intent = Intent(context, AlerteReceiver::class.java).apply {
            putExtra(EXTRA_ID, id)
        }
        return PendingIntent.getBroadcast(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

class AlerteReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Alertes.fermer(context, intent.getIntExtra(Alertes.EXTRA_ID, 0))
    }
}
