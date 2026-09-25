package ru.example.realweather.client;

public final class CityLocation {
    public final String displayName;
    public final double latitude;
    public final double longitude;

    public CityLocation(String displayName, double latitude, double longitude) {
        this.displayName = displayName;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
