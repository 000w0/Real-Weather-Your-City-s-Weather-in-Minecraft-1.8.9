package ru.example.realweather.client;

import java.time.LocalTime;

public final class CityTimeMapper {
    private static final long NANOS_PER_DAY = 86400000000000L;

    private CityTimeMapper() {
    }

    public static long toMinecraftTicks(LocalTime localTime) {
        long ticks = localTime.toNanoOfDay() * 24000L / NANOS_PER_DAY;
        return (ticks + 18000L) % 24000L;
    }
}
