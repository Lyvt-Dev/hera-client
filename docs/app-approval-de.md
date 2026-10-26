# Minecraft-App-Freigabe für Hera

Der Herausgeber richtet diese Freigabe einmalig für Hera ein. Spieler brauchen keine eigene Microsoft-App.

## Beobachtung

Der Besitzer meldete bei Version 0.1.1 einen HTTP 403 beim Schritt „Minecraft-Anmeldung“. Dieser Schritt folgt im Code erst nach erfolgreicher Microsoft-, Xbox-Live- und XSTS-Anmeldung. Die bisherige Meldung hat den konkreten Minecraft-Fehler verworfen; daher ist eine fehlende App-Freigabe bislang eine begründete Vermutung, keine bestätigte Diagnose.

Version 0.1.2 erkennt ausdrücklich `Invalid app registration` im Minecraft-Fehlerfeld und zeigt `MC_APP_NOT_APPROVED`. Andere 403-Antworten erhalten `MC_AUTH_FORBIDDEN`. Unbekannte Antworttexte, Kontodaten und Tokens werden nicht an die Oberfläche oder in Logs übernommen.

## Angaben für die Herausgeber-Anfrage

- Anwendung: Hera Client
- Öffentliche Microsoft-Client-ID: `7a968ec3-4976-49b6-980f-4dcb76b3a95e`
- Projekt/Quellcode: https://github.com/Lyvt-Dev/hera-client
- Beschreibung: Eigenständiger Minecraft-Java-Launcher mit profilbezogenen Installationen, Mod-Verwaltung und Microsoft-Anmeldung. Der Code prüft Spielbesitz und Profil; Tokens bleiben im nativen Prozess bzw. OS-Schlüsselspeicher.
- Ablauf: Microsoft Authorization Code + PKCE → Xbox Live → XSTS → Minecraft Services.
- Betroffener Dienst: `https://api.minecraftservices.com/authentication/login_with_xbox`
- Persönliche Kontaktdaten und gegebenenfalls verlangte Erklärungen muss der Herausgeber selbst korrekt ergänzen.

Der Weiterleitungslink https://aka.ms/mce-reviewappid führt zum aktuell erreichbaren Microsoft-Forms-Antrag „AppID Review“. Die Felder wurden direkt gelesen: EULA-/Richtlinienbestätigung, gültige zur Azure-Registrierung passende Kontaktadresse, Antragstyp, App-Name, Client-ID, Tenant-ID, Website und Begründung. Laut Formular werden Anträge wöchentlich geprüft; eine Freigabe oder konkrete Bearbeitungsdauer wird nicht zugesagt. Die öffentlichen Hera-Angaben wurden als ungesendeter Entwurf vorbereitet. Kontaktadresse und rechtliche Bestätigung bleiben beim Herausgeber. Es wurde kein Antrag versendet.

Der auf Microsoft Q&A veröffentlichte Erfahrungsbericht eines Launcher-Entwicklers beschreibt eine Freigabe nach Einreichung der Client-ID: https://learn.microsoft.com/en-us/answers/questions/5984225/minecraft-services-http-403-invalid-app-registrati. Dies ist ein Erfahrungsbericht und keine Freigabe für Hera.

Eine andere Launcher-ID oder ein fremder Anmeldedienst wird nicht als Ersatz verwendet. Nach bestätigter Freigabe denselben Hera-Login erneut mit einem Minecraft-Java-Konto testen und erst anschließend erfolgreichen Spielstart dokumentieren.
