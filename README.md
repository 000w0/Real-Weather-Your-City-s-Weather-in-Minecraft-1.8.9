# Real Weather 1.2.0 for Minecraft Forge 1.8.9

Real Weather is a client-side mod that displays a selected city's current temperature and weather in Minecraft. It also synchronizes the visible weather and time of day with Open-Meteo. The mod does not send changes to the server, so server time, precipitation, lightning, and other gameplay mechanics remain under server control.

## Installation

1. Install Minecraft 1.8.9 with Forge `11.15.1.2318`.
2. Put `realweather-1.8.9-1.2.0.jar` in the **client's** `.minecraft/mods` folder. Remove any older Real Weather JAR from that folder. The mod is not needed on the server.
3. Launch the game and join a world. Press **O** (the letter O) to open the settings. You can rebind this key in Minecraft's Controls menu.
4. Enter a city name and click **Search**. Check the coordinates returned by the search, then click **Save**. You can also enter latitude and longitude manually.
5. In the same screen, set the update interval, enable or disable the mod, and toggle city time synchronization. Click **Move panel** to drag the HUD panel; its position is saved when you release the mouse.

On first launch, the mod creates `.minecraft/config/realweather.cfg`. An example configuration with Kyiv coordinates is included at [`config/realweather.cfg`](config/realweather.cfg). You can change settings in game without restarting.

## Languages

The settings, HUD, and status messages follow Minecraft's selected language. English and Russian are included; other languages use English. Change the language in Minecraft's Language menu and reopen the settings screen to refresh its buttons. City search requests names in English or Russian to match the selected language. Previously saved city names are kept as entered.

## City time and HUD data

When city time synchronization is enabled, the client's time of day follows the city's local clock: 00:00 is midnight, 06:00 is sunrise, 12:00 is noon, and 18:00 is sunset. This follows the clock, not the city's astronomical sunrise and sunset. Open-Meteo provides the time zone with `timezone=auto`; the mod calculates the current local time and corrects the client clock before each rendered frame to prevent server time updates from making the sky flash. Only the visible client world time changes.

The HUD shows the city, temperature in °C, weather description, local time, and the observation time of the Open-Meteo data. The time of the last successful request is written to the log. If the connection fails, the last received values remain on the panel with a **No connection** status. The current weather is left unchanged when an update fails.

## Weather mapping

| Open-Meteo codes | Client weather |
| --- | --- |
| `0–3`, `45`, `48` | No precipitation |
| `51–57`, `61–67`, `80–82` | Rain |
| `71`, `73`, `75`, `77`, `85`, `86` | Snow in cold biomes; rain otherwise |
| `95–97`, `99` | Thunderstorm |

Minecraft 1.8.9 uses the same rain flag for rain and snow. The vanilla renderer chooses the precipitation effect based on biome temperature. As a result, Open-Meteo rain may look like snow in a cold biome, and biomes that do not allow precipitation may show no rain. The HUD still displays the actual Open-Meteo conditions.

## Building

You need **JDK 8**. The first build requires access to Gradle and the Forge Maven repository. This project uses ForgeGradle 2.1 and Gradle 2.7.

1. Open a terminal in the `real-weather-forge-1.8.9` project root.
2. Run `java -version` and confirm that Java 8 is selected.
3. On Windows, run `gradlew.bat setupDecompWorkspace`, then `gradlew.bat build`. On Linux or macOS, run `./gradlew setupDecompWorkspace`, then `./gradlew build`.
4. The built mod is at `build/libs/realweather-1.8.9-1.2.0.jar`. Do not install the `-sources.jar` file.

## Verification

1. Press **O**, search for a city, save it, and confirm that the panel shows the temperature, weather, and local time.
2. Click **Move panel**, drag the panel, leave the world, and rejoin. It should remain at the saved position.
3. Toggle **City time** and confirm that the time change is visible only on your client.
4. For a quick update check, set the interval to `1` minute. Look for `Open-Meteo update succeeded at ...` in `logs/latest.log`.
5. Disconnect from the internet or enter invalid coordinates. The mod should leave the current weather unchanged and log an error. On a network failure, the HUD marks the displayed weather data as the last received values.
6. On a multiplayer server, compare the weather and time with a client without the mod. The server world should not change.
7. Switch Minecraft between English and Russian, then reopen the settings. Check the buttons, HUD, status messages, and city search results in each language.

Weather and city searches run on a background thread with 5-second connection and read timeouts. Results are applied on the client thread through `Minecraft.addScheduledTask`. No API key is used.
