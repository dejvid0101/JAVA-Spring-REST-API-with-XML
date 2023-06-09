package me.iis.server.classes;

import jakarta.xml.bind.annotation.*;

import java.time.LocalDateTime;


@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Current_Weather {

    @XmlElement
    public double temperature;
    @XmlElement
    public double windspeed;
    @XmlElement
    public double winddirection;
    @XmlElement
    public int weathercode;
    @XmlElement
    public String time;

    @XmlTransient
    public boolean is_day;

    @Override
    public String toString() {
        return "Current_Weather{" +
                "Temperature=" + temperature +
                ", Windspeed=" + windspeed +
                ", Winddirection=" + winddirection +
                ", WeatherCode=" + weathercode +
                ", Time=" + time +
                '}';
    }
}
