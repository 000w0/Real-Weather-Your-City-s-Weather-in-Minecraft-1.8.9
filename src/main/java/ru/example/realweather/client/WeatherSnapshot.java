package ru.example.realweather.client;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public final class WeatherSnapshot {
    public final WeatherCondition condition;
    public final double temperatureCelsius;
    public final ZoneId cityZone;
    public final LocalDateTime observationTime;
    public final Instant fetchedAt;

    public WeatherSnapshot(WeatherCondition condition, double temperatureCelsius,
                           ZoneId cityZone, LocalDateTime observationTime, Instant fetchedAt) {
        this.condition = condition;
        this.temperatureCelsius = temperatureCelsius;
        this.cityZone = cityZone;
        this.observationTime = observationTime;
        this.fetchedAt = fetchedAt;
    }
}
