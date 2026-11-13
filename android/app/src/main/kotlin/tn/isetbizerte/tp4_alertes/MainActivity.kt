package tn.isetbizerte.tp4_alertes

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private lateinit var canalAlerte: MethodChannel
    private var actionEnAttente: Map<String, Any?>? = null
    private var resultatPermission: MethodChannel.Result? = null

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        val messenger = flutterEngine.dartExecutor.binaryMessenger
        Alertes.creerCanal(this)
        canalAlerte = MethodChannel(messenger, "alerte/notifier")
        actionEnAttente = lireAction(intent)
        canalAlerte.setMethodCallHandler { call, result ->
            when (call.method) {
                "demanderPermission" -> {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED
                    ) {
                        result.success(true)
                    } else {
                        resultatPermission = result
                        requestPermissions(
                            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                            REQ_NOTIF,
                        )
                    }
                }
                "notifier" -> {
                    val titre = call.argument<String>("titre") ?: "Alerte"
                    val texte = call.argument<String>("texte") ?: ""
                    if (Alertes.autorisee(this)) {
                        result.success(Alertes.afficher(this, titre, texte))
                    } else {
                        result.error("NOTIF_REFUSEES", "Notifications non autorisées", null)
                    }
                }
                "actionInitiale" -> {
                    result.success(actionEnAttente)
                    actionEnAttente = null
                }
                else -> result.notImplemented()
            }
        }

        MethodChannel(messenger, "sync/planifier")
            .setMethodCallHandler(Planificateur(this))
        MethodChannel(messenger, "service/surveillance")
            .setMethodCallHandler(ControleService(this))
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        lireAction(intent)?.let { action ->
            if (::canalAlerte.isInitialized) canalAlerte.invokeMethod("surAction", action)
        }
    }

    private fun lireAction(intent: Intent?): Map<String, Any?>? {
        val action = intent?.getStringExtra(Alertes.EXTRA_ACTION) ?: return null
        val titre = intent.getStringExtra(Alertes.EXTRA_TITRE) ?: "Alerte"
        val id = intent.getIntExtra(Alertes.EXTRA_ID, 0)
        Alertes.fermer(this, id)
        return mapOf("action" to action, "titre" to titre, "id" to id)
    }

    @Deprecated("Deprecated by Android; kept for the permission result callback")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_NOTIF) {
            resultatPermission?.success(
                grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED,
            )
            resultatPermission = null
        }
    }

    companion object {
        private const val REQ_NOTIF = 43
    }
}
