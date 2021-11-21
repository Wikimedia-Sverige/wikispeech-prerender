package se.wikimedia.wikispeech.prerender.service;

import org.influxdb.BatchOptions;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class Influx extends AbstractLifecycle implements SmartLifecycle {

    private final String serverURL = "https://influx.wikimedia.se";
    private String username = "root";
    private String password = "root";
    private InfluxDB influxDB;

    @Override
    protected void doStart() {
        username = System.getProperty("influx.username");
        username = System.getProperty("influx.password");
        influxDB = InfluxDBFactory.connect(serverURL, username, password);
        String databaseName = "wikispeech_prerender";
        influxDB.query(new Query("CREATE DATABASE " + databaseName));
        influxDB.setDatabase(databaseName);

        String retentionPolicyName = "two_years";
        influxDB.query(new Query("CREATE RETENTION POLICY " + retentionPolicyName
                + " ON " + databaseName + " DURATION 2y REPLICATION 1 DEFAULT"));
        influxDB.setRetentionPolicy(retentionPolicyName);

        influxDB.enableBatch(
                BatchOptions.DEFAULTS
                        .threadFactory(runnable -> {
                            Thread thread = new Thread(runnable);
                            thread.setDaemon(true);
                            return thread;
                        })
        );
    }

    @Override
    protected void doStop() {
        influxDB.close();
    }

    public void write(Point point) {
        influxDB.write(point);
    }
}
