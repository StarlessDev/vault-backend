package dev.starless.hosting.webserver.endpoints.account;

import dev.starless.hosting.objects.ServiceUser;
import dev.starless.hosting.objects.bodies.UpdateUsernameBody;
import dev.starless.hosting.objects.session.UserInfo;
import dev.starless.hosting.webserver.Response;
import dev.starless.hosting.webserver.WebServer;
import dev.starless.hosting.webserver.WebServerEndpoint;
import io.javalin.http.*;
import org.hibernate.query.Query;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

public class UpdateNameEndpoint extends WebServerEndpoint {

    public UpdateNameEndpoint(@NotNull WebServer server) {
        super(server, "/api/account/username", HandlerType.POST);
    }

    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        final UserInfo info = ctx.attribute(SESSION_OBJECT_NAME);
        if (info == null) {
            throw new UnauthorizedResponse();
        }

        final UpdateUsernameBody body = ctx.bodyValidator(UpdateUsernameBody.class)
                .check(obj -> obj.username() != null, "INVALID_BODY")
                .get();

        final AtomicBoolean updated = new AtomicBoolean(false);
        server.getHibernate().getSessionFactory().inTransaction(session -> {
            final ServiceUser user = session.get(ServiceUser.class, info.id());
            if (user == null) {
                throw new NotFoundResponse();
            }

            // Check if username already exists
            Query<Long> query = session.createQuery(
                    "SELECT COUNT(u) FROM ServiceUser u WHERE u.username = :username AND u.id != :id",
                    Long.class
            );
            query.setParameter("username", body.username());
            query.setParameter("id", info.id());

            if (query.getSingleResult() > 0) {
                return;
            }

            user.setUsername(body.username());
            session.merge(user);
            updated.set(true);
        });

        if (updated.get()) {
            this.server.setAuthCookie(ctx, new UserInfo(info.id(), body.username()));
        } else {
            this.sendResponse(ctx, Response.badRequest("This username is already in use"));
        }
    }
}
