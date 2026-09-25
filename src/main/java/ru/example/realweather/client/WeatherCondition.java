package ru.example.realweather.client;

public enum WeatherCondition {
    CLEAR,
    RAIN,
    SNOW,
    THUNDER;

    public static WeatherCondition fromOpenMeteoCode(int code) {
        switch (code) {
            case 0: case 1: case 2: case 3: case 45: case 48:
                return CLEAR;
            case 51: case 53: case 55: case 56: case 57:
            case 61: case 63: case 65: case 66: case 67:
            case 80: case 81: case 82:
                return RAIN;
            case 71: case 73: case 75: case 77: case 85: case 86:
                return SNOW;
            case 95: case 96: case 97: case 99:
                return THUNDER;
            default:
                return null;
        }
    }
}
