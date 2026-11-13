package tn.isetbizerte.tp4_alertes

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ServiceSurveillance : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_ARRETER) {
            stopSelf()
            return START_NOT_STICKY
        }

        val seuil = intent?.getDoubleExtra(EXTRA_SEUIL, 40.0) ?: 40.0
        creerCanalService()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(
            this,
            ID_NOTIF,
            notificationEnCours("Démarrage..."),
            type,
        )

        job?.cancel()
        job = scope.launch {
            var enAlerte = false
            while (isActive) {
                val temperature = temperatureBatterie()
                Log.d("TP4", "Surveillance : $temperature °C (seuil $seuil °C)")
                NotificationManagerCompat.from(this@ServiceSurveillance).notify(
                    ID_NOTIF,
                    notificationEnCours("Batterie : %.1f °C (seuil %.1f °C)".format(temperature, seuil)),
                )

                if (temperature >= seuil && !enAlerte) {
                    if (Alertes.autorisee(this@ServiceSurveillance)) {
                        Alertes.afficher(
                            this@ServiceSurveillance,
                            "Température élevée",
                            "Batterie : %.1f °C (seuil %.1f °C)".format(temperature, seuil),
                        )
                    }
                    enAlerte = true
                } else if (temperature < seuil) {
                    enAlerte = false
                }
                delay(10_000)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        job?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private fun temperatureBatterie(): Double {
        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        return (intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10.0
    }

    private fun creerCanalService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CANAL_SERVICE,
                "Surveillance",
                NotificationManager.IMPORTANCE_LOW,
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun notificationEnCours(texte: String): Notification =
        NotificationCompat.Builder(this, CANAL_SERVICE)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("Surveillance active")
            .setContentText(texte)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

    companion object {
        const val EXTRA_SEUIL = "seuil"
        private const val ACTION_ARRETER = "tn.isetbizerte.tp4_alertes.ARRETER"
        private const val CANAL_SERVICE = "surveillance"
        private const val ID_NOTIF = 1
    }
}

class ControleService(private val context: Context) : MethodChannel.MethodCallHandler {
    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "demarrer" -> {
                val intent = Intent(context, ServiceSurveillance::class.java).apply {
                    putExtra(ServiceSurveillance.EXTRA_SEUIL, call.argument<Double>("seuil") ?: 40.0)
                }
                ContextCompat.startForegroundService(context, intent)
                result.success(null)
            }
            "arreter" -> {
                context.stopService(Intent(context, ServiceSurveillance::class.java))
                result.success(null)
            }
            else -> result.notImplemented()
        }
    }
}
