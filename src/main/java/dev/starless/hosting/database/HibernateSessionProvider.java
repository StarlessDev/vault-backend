package dev.starless.hosting.database;

import dev.starless.hosting.config.Config;
import dev.starless.hosting.config.ConfigEntry;
import dev.starless.hosting.objects.ServiceUser;
import dev.starless.hosting.objects.UserUpload;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import static org.hibernate.cfg.JdbcSettings.*;

@RequiredArgsConstructor
public class HibernateSessionProvider {

    private final Config config;
    @Getter
    private SessionFactory sessionFactory = null;

    public void init() {
        if (sessionFactory != null) {
            this.close();
        }

        this.sessionFactory = new Configuration()
                .addAnnotatedClass(ServiceUser.class)
                .addAnnotatedClass(UserUpload.class)
                // Create automatically tables according to schemas
                .setProperty("hibernate.hbm2ddl.auto", "update")
                // use Hikari connection pool
                .setProperty(CONNECTION_PROVIDER, "org.hibernate.hikaricp.internal.HikariCPConnectionProvider")
                .setProperty(HIKARI_POOL_NAME, "FileHosting-Backend")
                .setProperty(HIKARI_MIN_IDLE_SIZE, 5)
                .setProperty(HIKARI_MAX_SIZE, 10)
                .setProperty(HIKARI_IDLE_TIMEOUT, 30000)
                // Use MariaDB (this is setting hikari properties directly)
                .setProperty("hibernate.hikari.dataSourceClassName", "org.mariadb.jdbc.MariaDbDataSource")
                .setProperty("hibernate.hikari.dataSource.url", config.getString(ConfigEntry.DATABASE_URL))
                .setProperty("hibernate.hikari.dataSource.user", config.getString(ConfigEntry.DATABASE_USER))
                .setProperty("hibernate.hikari.dataSource.password", config.getString(ConfigEntry.DATABASE_PASSWORD))
                // display SQL in console
                .setProperty(SHOW_SQL, true)
                .setProperty(FORMAT_SQL, true)
                .setProperty(HIGHLIGHT_SQL, true)
                .buildSessionFactory();

    }

    public void close() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }
}
