package dev.starless.hosting.webserver.endpoints.account;

import com.google.gson.JsonObject;
import dev.starless.hosting.objects.ServiceUser;
import dev.starless.hosting.objects.UserUpload;
import dev.starless.hosting.objects.UserUpload_;
import dev.starless.hosting.objects.session.UserInfo;
import dev.starless.hosting.webserver.WebServer;
import dev.starless.hosting.webserver.WebServerEndpoint;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;
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
        super(server, "/api/account", HandlerType.GET);
    }

    @Override
    public void handle(@NotNull Context ctx) {
        final UserInfo info = ctx.attribute(SESSION_OBJECT_NAME);
        if (info == null) {
            throw new UnauthorizedResponse();
        }

        ctx.future(() -> CompletableFuture.runAsync(() -> {
            final JsonObject payload = new JsonObject();
            payload.addProperty("id", info.id());

            final List<UserUpload> files = new ArrayList<>();
            server.getHibernate().getSessionFactory().inSession(session -> {
                final ServiceUser serviceUser = session.get(ServiceUser.class, info.id());
                if (serviceUser == null) {
                    ctx.status(HttpStatus.NOT_FOUND);
                    return;
                }
                payload.addProperty("username", serviceUser.getUsername());
                payload.addProperty("email", serviceUser.getEmail());

                final CriteriaBuilder cb = session.getCriteriaBuilder();
                final CriteriaQuery<UserUpload> query = cb.createQuery(UserUpload.class);
                final Root<UserUpload> root = query.from(UserUpload.class);
                // Get user-uploaded files
                files.addAll(session.createQuery(
                        query.select(root).where(cb.equal(root.get(UserUpload_.UPLOADER_ID), info.id()))
                ).getResultList());
            });

            payload.add("uploads", server.getGson().toJsonTree(files));
            ctx.json(payload);
        }));
    }
}
