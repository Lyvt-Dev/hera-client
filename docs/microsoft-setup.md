# Microsoft-Anmeldung

## Für Spieler

In der installierten Hera-App **Account → Mit Microsoft anmelden** wählen. Ein eigenes Microsoft-Fenster öffnet sich. Nach der Anmeldung verbindet Hera das Konto automatisch und schließt das Fenster. Kein Gerätecode und keine zusätzliche Launcher-Bestätigung sind erforderlich. Fenster schließen oder **Anmeldung abbrechen** beendet den Versuch.

## Für den Herausgeber

Die öffentliche Hera-Anwendungs-ID ist eingebaut. Die registrierte App muss persönliche Microsoft-Konten zulassen. Unter **Authentifizierung → Plattform hinzufügen → Mobile Anwendungen und Desktopanwendungen** muss folgende Adresse registriert sein:

`https://login.microsoftonline.com/common/oauth2/nativeclient`

Beim Live-Test wurde zunächst die fehlende redirect_uri abgelehnt. Nach der Bestätigung des Herausgebers wurde am 25. Oktober 2026 erneut geprüft: Microsoft zeigt die reguläre Kontoauswahl. Das beweist die akzeptierte Autorisierungsanfrage, noch keinen vollständigen Minecraft-Login.

Hera nutzt Authorization Code mit PKCE/S256, zufälligem State und einem externen Tauri-Webview ohne Hera-IPC-Berechtigungen. Antwort-URL, State, doppelte Parameter, Abbruch und Zeitlimit werden geprüft. Codes und Tokens bleiben im nativen Prozess; React erhält nur Account-ID und Namen.

Refresh-Tokens liegen im Betriebssystem-Schlüsselspeicher; SQLite speichert ID und Namen. Logout entfernt die lokalen Credentials, nicht die globalen Microsoft-Websessions. Xbox-/Minecraft-Zugriff, Besitzprüfung und echter Spielstart bleiben mit dem Besitzerkonto zu prüfen. Entra-Registrierung allein garantiert keine Minecraft-API-Zulassung.

Abweichende Herausgeberkonfiguration liegt unter **Einstellungen → Erweiterte Kontoeinstellungen**. Spieler müssen keine App registrieren. HERA_MICROSOFT_CLIENT_ID kann den Build-Standard überschreiben. Kein Clientgeheimnis gehört in den Launcher.

Referenzen: [Microsoft Authorization Code + PKCE](https://learn.microsoft.com/en-us/entra/identity-platform/v2-oauth2-auth-code-flow), [Desktop-Konfiguration](https://learn.microsoft.com/en-us/entra/identity-platform/scenario-desktop-app-configuration). NoRisks öffentlicher Launcher wurde als Ablauf-/Lizenzreferenz geprüft; seine Implementierung und App-Identität wurden nicht kopiert.
