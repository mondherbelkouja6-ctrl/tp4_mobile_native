import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'native/canaux.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const MaterialApp(
    debugShowCheckedModeBanner: false,
    home: PageAlertes(),
  ));
}

class PageAlertes extends StatefulWidget {
  const PageAlertes({super.key});

  @override
  State<PageAlertes> createState() => _PageAlertesState();
}

class _PageAlertesState extends State<PageAlertes> {
  String _derniereAction = 'aucune';
  double _seuilBatterie = 20;
  double _seuilTemperature = 40;
  bool _surveillance = false;

  @override
  void initState() {
    super.initState();
    CanauxNatifs.ecouterActions(_traiterAction);
    CanauxNatifs.actionInitiale().then((action) {
      if (mounted && action != null) _traiterAction(action);
    });
  }

  void _traiterAction(Map<String, dynamic> action) {
    if (!mounted) return;
    setState(() {
      _derniereAction = '${action['action']}: ${action['titre']} (id ${action['id']})';
    });
  }

  void _message(String texte) {
    if (!mounted) return;
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text(texte)));
  }

  Future<void> _autoriser() async {
    try {
      final ok = await CanauxNatifs.demanderPermissionNotif();
      _message(ok ? 'Notifications autorisées' : 'Notifications refusées');
    } on PlatformException catch (e) {
      _message('Erreur ${e.code}: ${e.message}');
    }
  }

  Future<void> _envoyerAlerte() async {
    try {
      final id = await CanauxNatifs.notifier(
        'Patient critique',
        'Fréquence cardiaque : 142 bpm',
      );
      _message('Alerte envoyée (id ${id ?? 'inconnu'})');
    } on PlatformException catch (e) {
      _message('Erreur ${e.code}: ${e.message}');
    }
  }

  Future<void> _basculerSurveillance(bool active) async {
    try {
      if (active) {
        await CanauxNatifs.demarrerSurveillance(_seuilTemperature);
      } else {
        await CanauxNatifs.arreterSurveillance();
      }
      if (mounted) setState(() => _surveillance = active);
    } on PlatformException catch (e) {
      _message('Erreur ${e.code}: ${e.message}');
    }
  }

  Widget _carte(String titre, List<Widget> contenu) => Card(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(titre, style: Theme.of(context).textTheme.titleMedium),
              const SizedBox(height: 8),
              ...contenu,
            ],
          ),
        ),
      );

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(title: const Text('TP 4 - Alertes natives')),
        body: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            _carte('1. Notification native', [
              Wrap(spacing: 8, runSpacing: 8, children: [
                OutlinedButton(
                  onPressed: _autoriser,
                  child: const Text('Autoriser'),
                ),
                FilledButton(
                  onPressed: _envoyerAlerte,
                  child: const Text('Envoyer une alerte'),
                ),
              ]),
            ]),
            _carte('2. Dernière action reçue', [Text(_derniereAction)]),
            _carte('3. Vérification périodique (WorkManager)', [
              Text('Alerte si batterie ≤ ${_seuilBatterie.round()} %'),
              Slider(
                value: _seuilBatterie,
                min: 5,
                max: 100,
                divisions: 19,
                label: '${_seuilBatterie.round()} %',
                onChanged: (value) => setState(() => _seuilBatterie = value),
              ),
              Wrap(spacing: 8, runSpacing: 8, children: [
                FilledButton(
                  onPressed: () async {
                    await CanauxNatifs.planifier(_seuilBatterie.round());
                    _message('Vérification toutes les 15 min');
                  },
                  child: const Text('Planifier'),
                ),
                OutlinedButton(
                  onPressed: () => CanauxNatifs
                      .verifierMaintenant(_seuilBatterie.round())
                      .then((_) => _message('Vérification lancée')),
                  child: const Text('Vérifier maintenant'),
                ),
                TextButton(
                  onPressed: () => CanauxNatifs
                      .annulerPlanification()
                      .then((_) => _message('Planification annulée')),
                  child: const Text('Annuler'),
                ),
              ]),
            ]),
            _carte('4. Surveillance continue (service au premier plan)', [
              Text('Alerte si température ≥ ${_seuilTemperature.round()} °C'),
              Slider(
                value: _seuilTemperature,
                min: 20,
                max: 50,
                divisions: 30,
                label: '${_seuilTemperature.round()} °C',
                onChanged: (value) => setState(() => _seuilTemperature = value),
                onChangeEnd: (value) {
                  if (_surveillance) {
                    CanauxNatifs.demarrerSurveillance(value);
                  }
                },
              ),
              SwitchListTile(
                contentPadding: EdgeInsets.zero,
                title: const Text('Surveillance'),
                value: _surveillance,
                onChanged: _basculerSurveillance,
              ),
            ]),
          ],
        ),
      );
}
