package dev.starless.hosting;

import dev.starless.hosting.config.Config;
import dev.starless.hosting.database.HibernateSessionProvider;
import dev.starless.hosting.webserver.WebServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Backend {

    private final Logger logger = LoggerFactory.getLogger(Backend.class);

    private final Config config;
    private HibernateSessionProvider hibernate;
    private WebServer webServer;

    public Backend() {
        this.config = new Config();
    }

    public void init() {
        if (!config.init()) {
            logger.error("Something went wrong while reading from config!");
            return;
        }

        this.hibernate = new HibernateSessionProvider(config);
        this.hibernate.init();

        this.webServer = new WebServer(config, hibernate);
        this.webServer.start();
    }

    public void shutdown() {
        if (this.webServer != null) {
            this.webServer.stop();
        }
        if (this.hibernate != null) {
            this.hibernate.close();
        }
    }
}
