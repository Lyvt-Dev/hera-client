# Hera Cape Service 0.1.0 — lokaler Testdienst

Benötigt Node.js 22 oder neuer. Keine npm-Pakete, Datenbank oder Zugangsschlüssel erforderlich.

1. ZIP in einen eigenen Ordner entpacken.
2. `start-capes.cmd` öffnen oder dort `node server.mjs --lan` ausführen. Das Fenster geöffnet lassen.
3. In Hera Launcher 0.3.0 unter **Capes → Gemeinsamer Cape-Dienst** die Adresse setzen, **Geteilte Capes anzeigen** aktivieren und speichern.
4. Auf dem Dienst-PC: `http://127.0.0.1:3847`. Auf anderen PCs: `http://DEINE-LAN-IP:3847`, z. B. `http://192.168.178.20:3847`. Mit `ipconfig` findest du die IPv4-Adresse. Alle müssen denselben PC/Dienst verwenden. Falls Windows fragt, Node.js für das private Netzwerk zulassen; TCP-Port 3847 muss im privaten Netzwerk erreichbar sein.
5. Mit deinem Microsoft-/Minecraft-Konto angemeldet ein lokales Cape wählen, dann **Eigenes Cape veröffentlichen** drücken. Nach Änderungen erneut veröffentlichen.
6. Alle starten ein Fabric-1.21.11-Profil mit Hera Mod 0.10.0. Geladene Mitspieler sehen das veröffentlichte Cape nach einigen Sekunden. F5 zeigt dein eigenes Cape. Minecrafts Cape-Skin-Einstellung muss eingeschaltet sein.

Der Dienst speichert PNGs nach Minecraft-UUID in `cape-data/` und lädt sie nach einem Neustart weiter. Ein Konto hat ein veröffentlichtes Cape pro Dienst, unabhängig vom lokalen Launcher-Profil. **Vom Dienst entfernen** löscht nur die veröffentlichte Kopie; dein lokales Cape bleibt. Das lokale Aktivieren/Deaktivieren oder Ersetzen lädt Änderungen nicht automatisch hoch.

Dienst beenden: Strg+C. Andere Hera-Capes werden bei einem fehlgeschlagenen Abruf entfernt, spätestens nach etwa zehn Sekunden ohne erfolgreichen Kontakt. Dein lokales Cape funktioniert weiter. Offizielle Minecraft-Capes bleiben unverändert.

`node server.mjs` ohne `--lan` lauscht ausschließlich auf localhost. Optional: `HERA_CAPE_PORT` auf einen anderen Port setzen und die Launcher-Adresse entsprechend ändern. Dieser Testaufbau richtet keine Firewallregel, Portweiterleitung oder öffentliche Domain ein. Für Freunde außerhalb deines LANs ist ein gemeinsames privates VPN-Netz oder ein eigener HTTPS-Endpunkt erforderlich; hierfür gibt es keine automatische Einrichtung.

Uploads werden über Minecrafts offiziellen Session-Nachweis auf Kontobesitz geprüft. Minecraft-/Microsoft-Tokens werden niemals an diesen Dienst gesendet oder auf ihm gespeichert. Der Server bestätigt den Nachweis ausschließlich bei Mojang; Internetzugang ist dafür notwendig. Kein Offline-/UUID-Impersonation-Modus. Lesende Cape-Abfragen sind öffentlich: ein Cape ist kein privater Inhalt. HTTP ist nur für den LAN-Test vorgesehen, für öffentliche Nutzung HTTPS verwenden.

Testen: `node --test server.test.mjs` (im Repository). Der Dienst wurde automatisiert mit simuliertem Mojang-Verifier geprüft; die echte Kontoprüfung und Zwei-Spieler-Anzeige sind Teil deines Tests.
