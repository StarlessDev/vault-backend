package dev.starless.hosting.webserver;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.starless.hosting.FilesManager;
import dev.starless.hosting.config.Config;
import dev.starless.hosting.config.ConfigEntry;
import dev.starless.hosting.database.HibernateSessionProvider;
import dev.starless.hosting.gson.InstantAdapter;
import dev.starless.hosting.gson.ResponseAdapter;
import dev.starless.hosting.gson.ThrowableAdapter;
import dev.starless.hosting.webserver.endpoints.AccountEndpoint;
import dev.starless.hosting.webserver.endpoints.DeleteFileEndpoint;
import dev.starless.hosting.webserver.endpoints.DownloadFileEndpoint;
import dev.starless.hosting.webserver.endpoints.UploadFileEndpoint;
import dev.starless.hosting.webserver.endpoints.auth.LoginEndpoint;
import dev.starless.hosting.webserver.endpoints.auth.RegisterEndpoint;
import dev.starless.hosting.webserver.jwt.JWTProvider;
import dev.starless.hosting.webserver.middleware.AuthMiddleware;
import io.javalin.Javalin;
import io.javalin.json.JavalinGson;
import lombok.AccessLevel;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
public class WebServer {

    @Getter(AccessLevel.NONE)
    private final Javalin server;
    @Getter(AccessLevel.NONE)
    private final Set<WebServerEndpoint> endpoints;

    private final HibernateSessionProvider hibernate;
    private final FilesManager filesManager;

    private final Logger logger;
    private final Gson gson;
    private final Config config;

    public WebServer(final Config configuration,
                     final HibernateSessionProvider hibernate) {
        this.config = configuration;
        this.logger = LoggerFactory.getLogger(WebServer.class);
        this.gson = new GsonBuilder()
                .registerTypeAdapter(Response.class, new ResponseAdapter())
                .registerTypeAdapter(Instant.class, new InstantAdapter())
                .registerTypeHierarchyAdapter(Throwable.class, new ThrowableAdapter())
                .create();

        this.hibernate = hibernate;
        this.filesManager = new FilesManager(configuration);

        this.endpoints = new HashSet<>();
        this.server = Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.useVirtualThreads = true;
            config.http.asyncTimeout = 10000L;
            config.jsonMapper(new JavalinGson(gson, true));
            config.bundledPlugins.enableCors(cors -> {
                final List<String> domains = this.config.getStringList(ConfigEntry.ALLOWED_DOMAINS);
                if (domains.isEmpty()) {
                    domains.add("localhost");
                }

                for (String domain : domains) {
                    cors.addRule(rule -> {
                        rule.allowHost(domain);
                        rule.exposeHeader("*");
                        rule.allowCredentials = true;
                    });
                }
            });
        });
    }

    public void start() {
        JWTProvider.getInstance().init();

        // Auth endpoints and middleware
        this.endpoints.add(new LoginEndpoint(this));
        this.endpoints.add(new RegisterEndpoint(this));
        this.endpoints.add(new AuthMiddleware(this));

        // App endpoints
        this.endpoints.add(new AccountEndpoint(this));
        this.endpoints.add(new UploadFileEndpoint(this));
        this.endpoints.add(new DownloadFileEndpoint(this));
        this.endpoints.add(new DeleteFileEndpoint(this));

        this.endpoints.forEach(endpoint -> server.addEndpoint(endpoint.buildEndpoint()));
        this.server.start(config.getInt(ConfigEntry.API_PORT));
    }

    public void stop() {
        this.server.stop();
    }
}
