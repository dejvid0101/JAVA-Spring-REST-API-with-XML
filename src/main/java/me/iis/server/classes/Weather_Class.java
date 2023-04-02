package me.iis.server.classes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


public class Weather_Class {
    public double latitude;

    public double longitude;

    public double generationtime_ms;

    public double utc_offset_seconds;

    public String timezone;

    public String timezone_abbreviation;

    public double elevation;

    public Current_Weather current_weather;

    @Override
    public String toString() {
        return "Weather_Class{" +
                "latitude=" + latitude +
                ", longitude=" + longitude +
                ", generationtime_ms=" + generationtime_ms +
                ", utc_offset_seconds=" + utc_offset_seconds +
                ", timezone='" + timezone + '\'' +
                ", timezone_abbreviation='" + timezone_abbreviation + '\'' +
                ", elevation=" + elevation +
                ", current_weather=" + current_weather +
                '}';
    }
}
