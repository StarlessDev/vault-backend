package dev.starless.hosting.webserver.endpoints.account;

import dev.starless.hosting.objects.ServiceUser;
import dev.starless.hosting.objects.bodies.UpdateUsernameBody;
import dev.starless.hosting.objects.session.UserInfo;
import dev.starless.hosting.webserver.WebServer;
import dev.starless.hosting.webserver.WebServerEndpoint;
import dev.starless.hosting.webserver.jwt.JWTProvider;
import io.javalin.http.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.query.Query;
import org.jetbrains.annotations.NotNull;

public class UpdateNameEndpoint extends WebServerEndpoint {

    public UpdateNameEndpoint(@NotNull WebServer server) {
        super(server, HandlerType.POST, "/api/account/username");
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
                throw new BadRequestResponse("Username already exists");
            }

            user.setUsername(body.username());
            session.merge(user);
        });

        this.server.setAuthCookie(ctx, new UserInfo(info.id(), body.username()))
                .status(HttpStatus.OK);
    }
}
