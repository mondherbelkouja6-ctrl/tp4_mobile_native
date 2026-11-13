package tn.isetbizerte.tp4_alertes

import android.content.Context
import android.os.BatteryManager
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import java.util.concurrent.TimeUnit

class VerificationWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val seuil = inputData.getInt(CLE_SEUIL, 20)
        val batterie = applicationContext
            .getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val niveau = batterie.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

        Log.d("TP4", "Vérification : batterie $niveau % (seuil $seuil %)")
        if (niveau in 0..seuil && Alertes.autorisee(applicationContext)) {
            Alertes.creerCanal(applicationContext)
            Alertes.afficher(
                applicationContext,
                "Batterie faible",
                "Niveau $niveau % (seuil $seuil %)",
            )
        }
        return Result.success()
    }

    companion object {
        const val CLE_SEUIL = "seuil"
        const val NOM = "verification_periodique"
    }
}

class Planificateur(context: Context) : MethodChannel.MethodCallHandler {
    private val workManager = WorkManager.getInstance(context)

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        val seuil = call.argument<Int>("seuil") ?: 20
        val data = workDataOf(VerificationWorker.CLE_SEUIL to seuil)
        when (call.method) {
            "planifier" -> {
                val request = PeriodicWorkRequestBuilder<VerificationWorker>(
                    15,
                    TimeUnit.MINUTES,
                ).setInputData(data).build()
                workManager.enqueueUniquePeriodicWork(
                    VerificationWorker.NOM,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request,
                )
                result.success(null)
            }
            "maintenant" -> {
                val request = OneTimeWorkRequestBuilder<VerificationWorker>()
                    .setInputData(data)
                    .build()
                workManager.enqueue(request)
                result.success(null)
            }
            "annuler" -> {
                workManager.cancelUniqueWork(VerificationWorker.NOM)
                result.success(null)
            }
            else -> result.notImplemented()
        }
    }
}
