package dev.starless.hosting.webserver;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.starless.hosting.FilesManager;
import dev.starless.hosting.ProfilePictureManager;
import dev.starless.hosting.config.Config;
import dev.starless.hosting.config.ConfigEntry;
import dev.starless.hosting.database.HibernateSessionProvider;
import dev.starless.hosting.gson.InstantAdapter;
import dev.starless.hosting.gson.ResponseAdapter;
import dev.starless.hosting.gson.ThrowableAdapter;
import dev.starless.hosting.objects.Token;
import dev.starless.hosting.objects.session.UserInfo;
import dev.starless.hosting.webserver.endpoints.StatsEndpoint;
import dev.starless.hosting.webserver.endpoints.account.AccountEndpoint;
import dev.starless.hosting.webserver.endpoints.account.UpdateNameEndpoint;
import dev.starless.hosting.webserver.endpoints.account.avatar.DeleteAvatarEndpoint;
import dev.starless.hosting.webserver.endpoints.account.avatar.AvatarEndpoint;
import dev.starless.hosting.webserver.endpoints.account.avatar.UpdateAvatarEndpoint;
import dev.starless.hosting.webserver.endpoints.file.DeleteFileEndpoint;
import dev.starless.hosting.webserver.endpoints.file.DownloadFileEndpoint;
import dev.starless.hosting.webserver.endpoints.file.FileInfoEndpoint;
import dev.starless.hosting.webserver.endpoints.file.UploadFileEndpoint;
import dev.starless.hosting.webserver.endpoints.auth.LoginEndpoint;
import dev.starless.hosting.webserver.endpoints.auth.LogoutEndpoint;
import dev.starless.hosting.webserver.endpoints.auth.RegisterEndpoint;
import dev.starless.hosting.webserver.jwt.JWTProvider;
import dev.starless.hosting.webserver.middleware.AuthMiddleware;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Cookie;
import io.javalin.http.SameSite;
import io.javalin.json.JavalinGson;
import lombok.AccessLevel;
import lombok.Getter;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
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
    private final ProfilePictureManager pfpManager;
    private final Tika tika = new Tika();

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
        this.pfpManager = new ProfilePictureManager(configuration);

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
        this.endpoints.add(new LogoutEndpoint(this));
        this.endpoints.add(new AuthMiddleware(this));

        // Account endpoints
        this.endpoints.add(new AccountEndpoint(this));
        this.endpoints.add(new UpdateNameEndpoint(this));
        this.endpoints.add(new AvatarEndpoint(this));
        this.endpoints.add(new UpdateAvatarEndpoint(this));
        this.endpoints.add(new DeleteAvatarEndpoint(this));
        // Files endpoints
        this.endpoints.add(new FileInfoEndpoint(this));
        this.endpoints.add(new UploadFileEndpoint(this));
        this.endpoints.add(new DownloadFileEndpoint(this));
        this.endpoints.add(new DeleteFileEndpoint(this));
        this.endpoints.add(new StatsEndpoint(this));

        this.endpoints.stream()
                .flatMap(endpoint -> endpoint.buildEndpoint().stream())
                .forEach(server::addEndpoint);

        this.server.start(
                config.getString(ConfigEntry.API_HOST),
                config.getInt(ConfigEntry.API_PORT)
        );
    }

    public void stop() {
        this.server.stop();
    }

    public Token getAuthToken(final UserInfo info) {
        final Duration expiresAfter = Duration.ofDays(7L);
        final String token = JWTProvider.getInstance().sign(
                this.getGson().toJsonTree(info),
                expiresAfter
        );
        return new Token(token, expiresAfter);
    }

    public Context setAuthCookie(final Context ctx, final UserInfo info) {
        final Token token = this.getAuthToken(info);
        final Cookie cookie = new Cookie(
                "session",
                token.token(),
                "/",
                Math.toIntExact(token.expiry().toSeconds()),
                false
        );
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setSameSite(SameSite.NONE);

        return ctx.cookie(cookie);
    }
}
