# AFK Auto-Reconnect (Fabric Mod, MC 1.21.11)

Client-seitige Fabric-Mod. Fliegst du vom Server (Timeout, Kick, Verbindungsabbruch),
verbindet sie sich nach ein paar Sekunden automatisch neu und schickt danach `/afk`,
damit du direkt wieder in der AFK-Welt zum Sterne farmen stehst.

## Fertige .jar bauen (ganz ohne PC, direkt geht's über GitHub)

Dieses Projekt enthält `.github/workflows/build.yml` — eine GitHub-Actions-Automatik,
die dir die fertige .jar baut, ganz ohne eigene Java/Gradle-Installation:

1. Auf github.com (auch im Tablet-Browser) kostenlos einloggen/registrieren.
2. Neues, leeres Repository anlegen (z.B. "afk-reconnect").
3. Alle Dateien aus diesem Zip per Drag & Drop auf der Repo-Seite hochladen
   ("Add file" -> "Upload files"), committen.
4. Im Reiter "Actions" läuft der Build automatisch los (dauert ca. 1-2 Minuten).
5. Nach Abschluss auf den grünen Lauf klicken -> unter "Artifacts" liegt
   "afk-reconnect-jar" zum Download bereit -> entpacken, fertig ist die .jar,
   genau wie ein Download von Modrinth.

Diese .jar dann einfach in den Mods-Ordner deines Launchers (z.B. MJLauncher) legen,
zusätzlich zur passenden Fabric-API-Version für 1.21.11 (von Modrinth), genau wie bei
jedem anderen Fabric-Mod auch.

## Alternative: selbst bauen (mit PC/IntelliJ)

Voraussetzung: JDK 21.

```
./gradlew build
```

Die fertige Datei liegt danach unter `build/libs/afk-reconnect-1.0.0.jar`.

**In IntelliJ:** Projektordner als Gradle-Projekt öffnen (IntelliJ erkennt die
`build.gradle` automatisch), einmal "Reload Gradle Project" abwarten, dann rechts
in der Gradle-Sidebar `Tasks -> build -> build` doppelklicken.

## Installation im Launcher (z.B. MJLauncher)

1. Fabric API (Version für 1.21.11) von Modrinth herunterladen.
2. Beide .jar-Dateien — Fabric API und die gebaute `afk-reconnect-1.0.0.jar` — in den
   Mods-Ordner deines Launchers legen (genau wie jeden anderen von Modrinth
   heruntergeladenen Fabric-Mod).
3. Profil mit Fabric-Loader für 1.21.11 starten.

## Nutzung

- Läuft automatisch, sobald du auf einem Server bist — kein Befehl nötig.
- Mit der Taste unter `Optionen -> Steuerung -> AFK Auto-Reconnect` kannst du die
  Mod jederzeit ein-/ausschalten (standardmäßig nicht belegt, selbst zuweisen).
- Nach jedem (Re-)Connect wird automatisch `/afk` gesendet, per Default 3 Sekunden
  nach dem Join.
- Nach einem Disconnect wird nach 5s automatisch neu verbunden; jeder weitere
  Fehlversuch wartet 2s länger (bis max. 30 Versuche, damit die Mod nicht endlos
  gegen einen dauerhaft offline/gebannten Server anrennt).

## Anpassen

Alle Werte (Befehlsname, Wartezeiten, maximale Versuche) stehen ganz oben in
`src/main/java/de/ben/afkreconnect/AfkReconnectClient.java` im Block
`Einstellungen zum Anpassen`.

## Hinweis

Falls der Gradle-Build wegen einer Versions-Inkompatibilität fehlschlägt: auf
https://fabricmc.net/develop/ die zur installierten Minecraft-Version passenden
Werte für `yarn_mappings`, `loader_version` und `fabric_version` nachschauen und
in `gradle.properties` eintragen.
