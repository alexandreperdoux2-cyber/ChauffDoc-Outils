# Obtenir le .apk de Chauff'Doc (gratuit, sans rien installer)

Ce dossier contient l'app Android complète, avec un fichier qui la
compile automatiquement en .apk via GitHub Actions (gratuit).

## Étapes

1. Crée un compte GitHub si tu n'en as pas (gratuit) : https://github.com/signup
2. Crée un nouveau dépôt (bouton "New repository"), par ex. `chauffdoc-app`.
3. Mets tout le contenu de ce dossier `ChauffDoc/` dans ce dépôt (glisser-déposer
   les fichiers sur la page du dépôt, ou `git push` si tu connais git).
4. Va dans l'onglet **Actions** du dépôt : une action "Build APK" se lance
   automatiquement (2-3 minutes).
5. Une fois terminée (coche verte), clique dessus puis, en bas de page,
   télécharge l'artefact **ChauffDoc-debug-apk** : c'est ton fichier .apk.
6. Transfère-le sur ton téléphone Android (par mail, Drive, câble USB...)
   et ouvre-le pour l'installer (autorise "sources inconnues" si demandé).

## Alternative : Android Studio en local

Si tu préfères tout faire sur ton ordinateur :
1. Installe Android Studio (gratuit) : https://developer.android.com/studio
2. Ouvre ce dossier `ChauffDoc/` avec "Open" dans Android Studio.
3. Laisse-le synchroniser (première fois : quelques minutes).
4. Menu *Build > Build App Bundle(s) / APK(s) > Build APK(s)*.
5. Le .apk apparaît dans `app/build/outputs/apk/debug/`.

Le .apk généré par ces deux méthodes est identique.
