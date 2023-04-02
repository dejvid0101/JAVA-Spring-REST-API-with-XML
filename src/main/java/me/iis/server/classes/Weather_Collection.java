package me.iis.server.classes;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.List;
@XmlRootElement(name = "weatherCollection")
@XmlAccessorType(XmlAccessType.FIELD)
public class Weather_Collection {
    public Weather_Collection() {
    }

    @XmlElement(name="weather")
    private List<Current_Weather> collection;

    public Weather_Collection(List<Current_Weather> collection) {
        this.collection = collection;
    }

    public List<Current_Weather> getCollection() {
        return collection;
    }

    public void setCollection(List<Current_Weather> collection) {
        this.collection = collection;
    }
}
