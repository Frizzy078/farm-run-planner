# Farm Run Planner (RuneLite plugin)

Kijkt in je bank, inventory en equipment en maakt een farm run:
- welke zaden/saplings, compost, teleports en tools je moet meenemen
- de route langs alle patches, met per patch de teleport
- markeert de items in je bank zodat je ze snel pakt
- rekent uit hoeveel coins je nodig hebt (200 per boom, 30 voor de boot naar Brimhaven)
- volgt je run stap voor stap: na het planten gaat hij automatisch naar de volgende patch
- kleurt de juiste teleport-optie (bijv. Rub -> "Farming Guild") in het menu en het teleport-venster
- markeert de patch in de wereld en zet een gele pijl (ook op de minimap) naar de patch

Ondersteunt: **Herb run**, **Tree run**, **Fruit tree run**.

## Eenmalig: inloggen met je Jagex account

Een zelfgemaakte plugin draait in een aparte RuneLite, niet in de RuneLite van de Jagex Launcher.
Om daar met je Jagex account in te loggen, laat je de Jagex Launcher eenmalig je inlogsessie opslaan:

1. Open Terminal en start de RuneLite-instellingen:
   `/Applications/RuneLite.app/Contents/MacOS/RuneLite --configure`
2. Vul bij **Client arguments** in: `--insecure-write-credentials` en klik **Save**.
3. Start RuneLite zoals altijd via de **Jagex Launcher** en log in. RuneLite slaat nu je sessie op in
   `~/.runelite/credentials.properties`. Sluit RuneLite daarna.
4. Haal `--insecure-write-credentials` weer weg via stap 1 (anders wordt het bestand elke keer opnieuw geschreven).

**Let op:** `credentials.properties` geeft toegang tot je account. Deel dit bestand nooit.
Wil je het ongedaan maken: verwijder het bestand.

## Plugin starten

Dubbelklik niet, maar start in Terminal vanuit deze map:

    ./start-plugin.sh

Dit gebruikt Java uit de map `.tools` en de RuneLite-bestanden die al op je Mac staan.
Je logt automatisch in met de opgeslagen sessie.

## Gebruik

1. Klik rechts in de zijbalk op het groene boompje (Farm Run Planner).
2. Open je bank één keer.
3. Kies de run en klik **Maak plan**.

Instellingen (moersleutel -> Farm Run Planner): spreuken gebruiken, huis in Hosidius,
Trollheim- en Lletya-patches, highlight-kleur.

## Bekende beperkingen
- Zaden in de Seed vault (Farming Guild) worden niet meegeteld.
- Runes in een rune pouch of elementen van een staff worden niet meegeteld.
- Een spirit tree wordt altijd als beschikbaar gezien (Tree Gnome Village quest nodig).
