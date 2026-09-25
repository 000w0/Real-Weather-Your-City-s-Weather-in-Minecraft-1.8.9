package ru.example.realweather.client;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.BlockPos;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import ru.example.realweather.RealWeatherMod;

public final class ClientWeatherController {
    public interface CitySearchCallback {
        void complete(CityLocation city, String error);
    }

    private final WeatherConfig config;
    private final KeyBinding settingsKey;
    private final ExecutorService networkExecutor;
    private final AtomicBoolean requestInFlight = new AtomicBoolean(false);
    private long nextRequestAtMillis;
    private long settingsRevision;
    private WeatherSnapshot snapshot;
    private WeatherCondition desiredWeather;
    private String lastError;

    public ClientWeatherController(WeatherConfig config, KeyBinding settingsKey) {
        this.config = config;
        this.settingsKey = settingsKey;
        this.networkExecutor = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable task) {
                Thread thread = new Thread(task, "RealWeather-OpenMeteo");
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    public WeatherConfig getConfig() {
        return config;
    }

    public WeatherSnapshot getSnapshot() {
        return snapshot;
    }

    public String getLastError() {
        return lastError;
    }

    public void settingsChanged() {
        settingsRevision++;
        nextRequestAtMillis = 0L;
        snapshot = null;
        desiredWeather = null;
        lastError = null;
    }

    public void searchCity(final String query, final CitySearchCallback callback) {
        final Minecraft minecraft = Minecraft.getMinecraft();
        final String minecraftLanguage = minecraft.getLanguageManager()
                .getCurrentLanguage().getLanguageCode().toLowerCase(Locale.ROOT);
        final String searchLanguage = minecraftLanguage.startsWith("ru") ? "ru" : "en";
        networkExecutor.execute(new Runnable() {
            @Override
            public void run() {
                CityLocation result = null;
                String error = null;
                try {
                    result = OpenMeteoClient.searchCity(query, searchLanguage);
                } catch (Exception e) {
                    error = e.getMessage();
                    RealWeatherMod.LOGGER.warn("City search failed: {}", e.toString());
                }
                final CityLocation city = result;
                final String message = error;
                minecraft.addScheduledTask(new Runnable() {
                    @Override
                    public void run() {
                        callback.complete(city, message);
                    }
                });
            }
        });
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (settingsKey.isPressed() && minecraft.currentScreen == null) {
            minecraft.displayGuiScreen(new GuiWeatherSettings(this));
        }
        if (!config.enabled || !config.valid) {
            return;
        }

        WorldClient world = minecraft.theWorld;
        if (world == null || world.provider.getHasNoSky()) {
            return;
        }
        if (desiredWeather != null) {
            applyLocally(world, desiredWeather);
        }

        long now = System.currentTimeMillis();
        if (now >= nextRequestAtMillis && requestInFlight.compareAndSet(false, true)) {
            nextRequestAtMillis = now + config.updateIntervalMinutes * 60_000L;
            requestWeather(minecraft, config.latitude, config.longitude, settingsRevision);
        }
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !config.enabled || !config.valid) {
            return;
        }
        WorldClient world = Minecraft.getMinecraft().theWorld;
        if (world == null || world.provider.getHasNoSky()) {
            return;
        }

        // A server time/weather packet can arrive between client ticks. Correct
        // the client world immediately before the sky is drawn on every frame.
        if (snapshot != null && config.syncDayNight) {
            applyCityTime(world, snapshot);
        }
        if (desiredWeather != null && weatherNeedsCorrection(world, desiredWeather)) {
            applyLocally(world, desiredWeather);
        }
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL || !config.enabled) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.theWorld != null && minecraft.currentScreen == null) {
            String hudError = config.valid ? lastError : "realweather.hud.invalidConfig";
            HudOverlay.draw(minecraft, config, snapshot, hudError,
                    event.resolution.getScaledWidth(), event.resolution.getScaledHeight());
        }
    }

    private void requestWeather(final Minecraft minecraft, final double latitude,
                                final double longitude, final long revision) {
        networkExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final WeatherSnapshot received = OpenMeteoClient.fetch(latitude, longitude);
                    minecraft.addScheduledTask(new Runnable() {
                        @Override
                        public void run() {
                            requestInFlight.set(false);
                            if (revision != settingsRevision) {
                                return;
                            }
                            snapshot = received;
                            desiredWeather = received.condition;
                            lastError = null;
                            RealWeatherMod.LOGGER.info("Open-Meteo update succeeded at {}: {} °C, {}",
                                    received.fetchedAt, received.temperatureCelsius, received.condition);
                            WorldClient world = minecraft.theWorld;
                            if (world != null && !world.provider.getHasNoSky()) {
                                applyLocally(world, received.condition);
                                if (config.syncDayNight) {
                                    applyCityTime(world, received);
                                }
                            }
                        }
                    });
                } catch (final Exception e) {
                    minecraft.addScheduledTask(new Runnable() {
                        @Override
                        public void run() {
                            requestInFlight.set(false);
                            if (revision != settingsRevision) {
                                return;
                            }
                            desiredWeather = null;
                            lastError = "realweather.hud.updateFailed";
                            RealWeatherMod.LOGGER.warn("Open-Meteo update failed; Minecraft weather "
                                    + "was left unchanged: {}", e.toString());
                        }
                    });
                }
            }
        });
    }

    private void applyCityTime(WorldClient world, WeatherSnapshot current) {
        ZonedDateTime cityTime = ZonedDateTime.ofInstant(Instant.now(), current.cityZone);
        long timeOfDay = CityTimeMapper.toMinecraftTicks(cityTime.toLocalTime());
        WorldInfo info = world.getWorldInfo();
        long currentTicks = info.getWorldTime();
        long candidate = Math.floorDiv(currentTicks, 24000L) * 24000L + timeOfDay;
        if (candidate - currentTicks > 12000L) {
            candidate -= 24000L;
        } else if (currentTicks - candidate > 12000L) {
            candidate += 24000L;
        }
        if (candidate != currentTicks) {
            info.setWorldTime(candidate);
        }
    }

    private void applyLocally(WorldClient world, WeatherCondition condition) {
        WeatherCondition localCondition = precipitationForBiome(world, condition);
        boolean shouldRain = localCondition != WeatherCondition.CLEAR;
        boolean shouldThunder = localCondition == WeatherCondition.THUNDER;
        WorldInfo info = world.getWorldInfo();

        if (info.isRaining() != shouldRain) {
            info.setRaining(shouldRain);
            info.setRainTime(Integer.MAX_VALUE / 2);
        }
        if (info.isThundering() != shouldThunder) {
            info.setThundering(shouldThunder);
            info.setThunderTime(Integer.MAX_VALUE / 2);
        }

        float rainStrength = shouldRain ? 1.0F : 0.0F;
        float thunderStrength = shouldThunder ? 1.0F : 0.0F;
        if (world.rainingStrength != rainStrength || world.prevRainingStrength != rainStrength) {
            world.setRainStrength(rainStrength);
            world.prevRainingStrength = rainStrength;
        }
        if (world.thunderingStrength != thunderStrength
                || world.prevThunderingStrength != thunderStrength) {
            world.setThunderStrength(thunderStrength);
            world.prevThunderingStrength = thunderStrength;
        }
    }

    private boolean weatherNeedsCorrection(WorldClient world, WeatherCondition condition) {
        boolean rain = condition != WeatherCondition.CLEAR;
        boolean thunder = condition == WeatherCondition.THUNDER;
        float rainStrength = rain ? 1.0F : 0.0F;
        float thunderStrength = thunder ? 1.0F : 0.0F;
        WorldInfo info = world.getWorldInfo();
        return info.isRaining() != rain || info.isThundering() != thunder
                || world.rainingStrength != rainStrength
                || world.prevRainingStrength != rainStrength
                || world.thunderingStrength != thunderStrength
                || world.prevThunderingStrength != thunderStrength;
    }

    private WeatherCondition precipitationForBiome(WorldClient world, WeatherCondition condition) {
        if (condition != WeatherCondition.SNOW) {
            return condition;
        }
        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
        if (player == null) {
            return WeatherCondition.RAIN;
        }
        BlockPos column = new BlockPos(player.posX, 0, player.posZ);
        BiomeGenBase biome = world.getBiomeGenForCoords(column);
        int precipitationHeight = world.getPrecipitationHeight(column).getY();
        float temperature = world.getWorldChunkManager().getTemperatureAtHeight(
                biome.getFloatTemperature(column), precipitationHeight);
        boolean permitsPrecipitation = biome.canRain() || biome.getEnableSnow();
        return permitsPrecipitation && temperature < 0.15F
                ? WeatherCondition.SNOW : WeatherCondition.RAIN;
    }
}
