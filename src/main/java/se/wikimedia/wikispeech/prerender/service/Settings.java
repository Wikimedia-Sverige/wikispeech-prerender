package se.wikimedia.wikispeech.prerender.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Properties;

@Component
public class Settings {

    private final Properties properties;

    @Autowired
    public Settings(ResourceLoader resourceLoader) throws IOException  {
        Properties properties = new Properties();
        properties.load(resourceLoader.getResource("classpath:prerender.properties").getInputStream());
        this.properties = properties;
    }

    public String getString(String key, String defaultValue) {
        String value = (String)properties.get(key);
        if (value == null) return defaultValue;
        return value;
    }

    public Boolean getBoolean(String key, Boolean defaultValue) {
        String value = (String)properties.get(key);
        if (value == null) return defaultValue;
        return Boolean.parseBoolean(value);
    }

    public Integer getInteger(String key, Integer defaultValue) {
        String value = (String)properties.get(key);
        if (value == null) return defaultValue;
        return Integer.parseInt(value);
    }

    public Float getFloat(String key, Float defaultValue) {
        String value = (String)properties.get(key);
        if (value == null) return defaultValue;
        return Float.parseFloat(value);
    }

    public Double getDouble(String key, Double defaultValue) {
        String value = (String)properties.get(key);
        if (value == null) return defaultValue;
        return Double.parseDouble(value);
    }

    public Duration getDuration(String key, Duration defaultValue) {
        String value = (String)properties.get(key);
        if (value == null) return defaultValue;
        return Duration.parse(value);
    }

}
