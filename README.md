# 📨 SMS Forwarder — Application Android

Application Android simple pour transférer automatiquement les SMS entrants contenant un mot-clé spécifique vers un autre numéro de téléphone.

## Fonctionnalités

- 🔍 **Filtre par mot-clé** : transfert uniquement des SMS contenant un mot précis.
- 📤 **Transfert total** : option pour transférer tous les SMS entrants.
- 🔔 **Notifications** : notification locale à chaque SMS transféré.
- 📋 **Journal** : historique des transferts avec horodatage.
- 🔄 **Démarrage auto** : reprise automatique après redémarrage du téléphone.
- 🔋 **Service continu** : exécution via un service foreground pour rester actif en arrière-plan.

## Structure

```text
SmsForwarder/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/smsforwarder/
│   │   │   ├── MainActivity.java
│   │   │   ├── SmsReceiver.java
│   │   │   ├── ForwarderService.java
│   │   │   ├── BootReceiver.java
│   │   │   └── NotificationHelper.java
│   │   └── res/
│   │       ├── layout/activity_main.xml
│   │       ├── values/colors.xml
│   │       ├── values/strings.xml
│   │       └── values/themes.xml
│   └── build.gradle
└── settings.gradle
```

## Installation dans Android Studio

1. Ouvrir Android Studio puis sélectionner ce dossier avec **File > Open**.
2. Laisser Gradle synchroniser le projet.
3. Connecter un téléphone Android réel.
4. Lancer l'application avec **Run**.

> ⚠️ L'envoi et la réception de SMS ne fonctionnent pas correctement sur la majorité des émulateurs.

## Permissions requises

- `RECEIVE_SMS`
- `READ_SMS`
- `SEND_SMS`
- `RECEIVE_BOOT_COMPLETED`
- `FOREGROUND_SERVICE`
- `POST_NOTIFICATIONS`

## Configuration minimale

- **minSdk** : Android 6.0 (API 23)
- **targetSdk** : Android 14 (API 34)
- **Langage** : Java

## Compilation via GitHub Actions

Un workflow GitHub Actions est disponible dans `.github/workflows/android-build.yml`.

- Il installe **Java 17**, **Gradle** et le **SDK Android 34**.
- Il lance `gradle assembleDebug`.
- Il publie l'APK debug en artefact de workflow.

Vous pouvez le déclencher automatiquement sur `push` et `pull_request`, ou manuellement avec **Run workflow** dans l'onglet **Actions** de GitHub.
