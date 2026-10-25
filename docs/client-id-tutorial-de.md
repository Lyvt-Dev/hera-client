# Hera: eigene Microsoft-Client-ID einrichten

## 1. Deinen aktuellen Fehler einordnen

Im Portal wurde nach deiner Anmeldung `AADSTS16000` angezeigt: Dein privates Microsoft-Konto ist im Verzeichnis **Microsoft Services** nicht als Benutzer vorhanden. Dort kannst du keine eigene Hera-App registrieren. Erneutes Anmelden mit demselben Konto in dasselbe Verzeichnis behebt diese fehlende Verzeichniszuordnung nicht.

Du benötigst Zugriff auf ein eigenes Microsoft-Entra-Verzeichnis und die Berechtigung, dort Anwendungen zu registrieren. Das Microsoft-Konto zum Entwickeln der App und die späteren Minecraft-Spielerkonten sind unterschiedliche Rollen; nicht jeder Hera-Nutzer braucht ein Azure-Konto.

## 2. Ein eigenes Verzeichnis zugänglich machen

1. Öffne das [Azure-Portal](https://portal.azure.com/) mit deinem eigenen Konto.
2. Falls noch kein geeignetes Azure-Konto vorhanden ist, folge Microsofts [Azure-Konto-Einrichtung](https://azure.microsoft.com/free/). Verifizierung, Bedingungen und eventuell angezeigte Zahlungsinformationen prüfst/bestätigst du selbst. Eine zusätzliche kostenpflichtige Ressource wird für diesen Hera-Code nicht benötigt; die Konto-/Tenant-Berechtigung muss trotzdem vorhanden sein.
3. Öffne Microsoft Entra ID und prüfe über **Verzeichnisse + Abonnements / Directories + subscriptions**, ob du in ein eigenes Verzeichnis wechseln kannst. Verwende nicht das fremde Verzeichnis „Microsoft Services“.
4. Wenn du einen eigenen Tenant anlegen darfst: **Entra ID → Übersicht / Overview → Mandanten verwalten / Manage tenants → Erstellen / Create**. Wähle einen normalen **Microsoft Entra ID / Workforce**-Tenant für die App-Registrierung, keinen External-ID/B2C-Kunden-Login als Ersatz.
5. Gib einen Namen (z. B. `Hera Development`), eine verfügbare Anfangsdomäne (`<dein-name>.onmicrosoft.com`) und dein tatsächliches Land an. Prüfe die Zusammenfassung und erstelle den Tenant, sofern das Angebot und die Bedingungen für dich passen.
6. Wechsle anschließend in dieses eigene Verzeichnis.

Microsoft beschränkt die Erstellung zusätzlicher Workforce-Tenants abhängig vom Kontostatus und den Berechtigungen; Free-/Trial-Konten dürfen nicht beliebig zusätzliche Tenants im Admin Center erstellen. Falls „Create“ fehlt oder gesperrt ist, ist das kein Hera-Fehler. Folge den Voraussetzungen in [Microsofts Tenant-Anleitung](https://learn.microsoft.com/en-us/entra/fundamentals/create-new-tenant). Ein eigener vorhandener Tenant oder eine berechtigte Einladung in einen Tenant ist ebenfalls geeignet.

## 3. Hera registrieren

1. Öffne das [Microsoft Entra Admin Center](https://entra.microsoft.com/).
2. Stelle sicher, dass dein eigenes Verzeichnis ausgewählt ist.
3. Gehe zu **Entra ID → App-Registrierungen / App registrations → Neue Registrierung / New registration**.
4. Name: **Hera Client**.
5. Unterstützte Kontotypen: **Nur persönliche Microsoft-Konten / Personal Microsoft accounts only**. Die Option für Organisationskonten **und** persönliche Microsoft-Konten ist ebenfalls mit unserem `consumers`-Login vereinbar; nur Organisationskonten genügt für Minecraft-Spielerkonten nicht.
6. Die optionale **Umleitungs-URI / Redirect URI** für Heras aktuellen Device-Code-Flow leer lassen.
7. Wähle **Registrieren / Register**.

Quelle: [Microsoft: App registrieren](https://learn.microsoft.com/en-us/entra/identity-platform/quickstart-register-app).

## 4. Öffentlichen Client-Flow einschalten

1. Öffne die gerade registrierte App.
2. Gehe zu **Authentifizierung / Authentication**.
3. Unter **Erweiterte Einstellungen / Advanced settings**: **Öffentliche Clientflows zulassen / Allow public client flows → Ja / Yes**.
4. Speichern.

Hera ist eine öffentliche Desktop-App und nutzt den Device-Code-Flow. **Kein Client-Secret erstellen oder in Hera eingeben.** Für diesen Flow ist keine Redirect URI erforderlich. Quelle: [Microsoft: Desktop-App konfigurieren](https://learn.microsoft.com/en-us/entra/identity-platform/scenario-desktop-app-configuration#enable-public-client-flow).

## 5. Die richtige ID kopieren

Unter **Übersicht / Overview** die **Anwendungs-ID (Client) / Application (client) ID** kopieren. Sie sieht wie `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx` aus.

Nicht die Verzeichnis-/Tenant-ID, nicht die Objekt-ID und kein Secret verwenden. Die Client-ID ist öffentlich und darf im Launcher stehen.

## 6. In Hera speichern und testen

1. Die **Hera-Desktop-App** öffnen.
2. **Account → Microsoft-Client-ID** auswählen.
3. Die Client-ID einfügen und **Client-ID speichern** wählen.
4. **Mit Microsoft anmelden** anklicken.
5. Microsoft-URL selbst öffnen, den angezeigten Gerätecode eingeben und den Login mit dem Konto durchführen, das Minecraft Java besitzt.
6. Zurück in Hera **Anmeldung abschließen** wählen.

Kein Neubau nötig. Der Browser speichert eine dort eingetragene Client-ID getrennt lokal; die native Anmeldung und der OS-Schlüsselspeicher stehen nur in der Desktop-App bereit.

## 7. Minecraft-Appfreigabe beachten

Eine registrierte Entra-App ist nicht automatisch für alle Minecraft-Dienste zugelassen. Wenn Microsoft/Xbox funktionieren, aber Minecraft `403` oder `Invalid app registration` zurückgibt, kann eine Freigabe deiner Anwendung durch Minecraft erforderlich sein. Nicht die Client-ID eines fremden Launchers übernehmen.

Der bekannte Microsoft-Weiterleitungslink zur AppID-Prüfung ist [aka.ms/mce-reviewappid](https://aka.ms/mce-reviewappid). Prüfe die dort tatsächlich angezeigten Anforderungen selbst; es gibt keine garantierte sofortige Freigabe. Die beobachtete Anmeldekette ist außerdem in einem [Microsoft-Q&A-Fall zu Minecraft-Appregistrierungen](https://learn.microsoft.com/en-us/answers/questions/5984225/minecraft-services-http-403-invalid-app-registrati) beschrieben. Die Appfreigabe ist getrennt vom Speichern der ID in Hera.

Aktueller Stand: Es wurde für dich noch keine App angelegt und keine Client-ID erzeugt. Du führst die Microsoft-Einrichtung auf eigenen Wunsch selbst durch.
