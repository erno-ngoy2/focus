# Focus — par MalakiLabs

Compteur de données du hotspot (notification + limite), mode concentration (écran de veille orange en paysage),
lecteur audio local, tuiles de réglages rapides. Kotlin + Jetpack Compose, **aucune dépendance ajoutée** au modèle d'origine.

Ouvrir le dossier dans Android Studio → Sync → Run. minSdk 26.

Limites d'Android : une app tierce ne peut pas activer/couper le hotspot, le Wi‑Fi ou le Bluetooth. Focus ouvre le bon
panneau en un tap, affiche l'état réel, et peut couper le hotspot automatiquement uniquement sur appareil rooté
(Réglages > Coupure automatique).
