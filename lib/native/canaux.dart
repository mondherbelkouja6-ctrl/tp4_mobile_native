import 'package:flutter/services.dart';

/// Point d'acces unique de Flutter vers les fonctionnalites Android natives.
class CanauxNatifs {
  static const MethodChannel _alerte = MethodChannel('alerte/notifier');
  static const MethodChannel _sync = MethodChannel('sync/planifier');
  static const MethodChannel _service = MethodChannel('service/surveillance');

  static Future<bool> demanderPermissionNotif() async =>
      await _alerte.invokeMethod<bool>('demanderPermission') ?? false;

  static Future<int?> notifier(String titre, String texte) =>
      _alerte.invokeMethod<int>('notifier', {'titre': titre, 'texte': texte});

  static Future<Map<String, dynamic>?> actionInitiale() async {
    final value = await _alerte.invokeMethod<Map<dynamic, dynamic>>('actionInitiale');
    return value?.map((key, value) => MapEntry(key.toString(), value));
  }

  static void ecouterActions(
    void Function(Map<String, dynamic>) traiter,
  ) {
    _alerte.setMethodCallHandler((call) async {
      if (call.method == 'surAction' && call.arguments is Map) {
        final args = Map<String, dynamic>.from(call.arguments as Map);
        traiter(args);
      }
    });
  }

  static Future<void> planifier(int seuil) =>
      _sync.invokeMethod<void>('planifier', {'seuil': seuil});

  static Future<void> verifierMaintenant(int seuil) =>
      _sync.invokeMethod<void>('maintenant', {'seuil': seuil});

  static Future<void> annulerPlanification() =>
      _sync.invokeMethod<void>('annuler');

  static Future<void> demarrerSurveillance(double seuil) =>
      _service.invokeMethod<void>('demarrer', {'seuil': seuil});

  static Future<void> arreterSurveillance() =>
      _service.invokeMethod<void>('arreter');
}
