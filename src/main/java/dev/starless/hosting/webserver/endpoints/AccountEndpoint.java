package dev.starless.hosting.webserver.endpoints;

import com.google.gson.JsonObject;
import dev.starless.hosting.objects.session.UserInfo;
import dev.starless.hosting.objects.UserUpload;
import dev.starless.hosting.objects.UserUpload_;
import dev.starless.hosting.webserver.WebServer;
import dev.starless.hosting.webserver.WebServerEndpoint;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import io.javalin.http.UnauthorizedResponse;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AccountEndpoint extends WebServerEndpoint {

    public AccountEndpoint(@NotNull WebServer server) {
        super(server, HandlerType.GET, "/api/account");
    }

    @Override
    public void handle(@NotNull Context ctx) {
        final UserInfo info = ctx.sessionAttribute(SESSION_OBJECT_NAME);
        if (info == null) {
            throw new UnauthorizedResponse();
        }

        ctx.future(() -> CompletableFuture.runAsync(() -> {
            final List<UserUpload> files = new ArrayList<>();
            server.getHibernate().getSessionFactory().inSession(session -> {

                final CriteriaBuilder cb = session.getCriteriaBuilder();
                final CriteriaQuery<UserUpload> query = cb.createQuery(UserUpload.class);
                final Root<UserUpload> root = query.from(UserUpload.class);

                files.addAll(session.createQuery(
                        query.select(root).where(cb.equal(root.get(UserUpload_.UPLOADER_ID), info.id()))
                ).getResultList());
            });

            JsonObject payload = new JsonObject();
            payload.addProperty("id", info.id());
            payload.addProperty("name", info.name());
            payload.add("uploads", server.getGson().toJsonTree(files));

            ctx.json(payload);
        }));
    }
}
