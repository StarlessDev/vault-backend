package dev.starless.hosting.webserver.endpoints.auth;

import dev.starless.hosting.objects.ServiceUser;
import dev.starless.hosting.objects.ServiceUser_;
import dev.starless.hosting.objects.bodies.RegisterBody;
import dev.starless.hosting.webserver.Response;
import dev.starless.hosting.webserver.WebServer;
import dev.starless.hosting.webserver.WebServerEndpoint;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class RegisterEndpoint extends WebServerEndpoint {

    public RegisterEndpoint(@NotNull WebServer server) {
        super(server, "/api/auth/register", HandlerType.POST);
    }

    @Override
    public void handle(@NotNull Context ctx) {
        final RegisterBody body = ctx.bodyValidator(RegisterBody.class)
                .check(login -> login.email() != null, "INVALID_BODY")
                .check(login -> login.username() != null, "INVALID_BODY")
                .check(login -> login.password() != null, "INVALID_BODY")
                .get();

        ctx.future(() -> CompletableFuture.runAsync(() -> {
            final SessionFactory sessionFactory = server.getHibernate().getSessionFactory();
            try (StatelessSession session = sessionFactory.openStatelessSession()) {
                final CriteriaBuilder cb = session.getCriteriaBuilder();
                final CriteriaQuery<ServiceUser> query = cb.createQuery(ServiceUser.class);
                final Root<ServiceUser> root = query.from(ServiceUser.class);

                final List<ServiceUser> same = session.createSelectionQuery(query.select(root).where(
                        cb.equal(root.get(ServiceUser_.email), body.email()),
                        cb.equal(root.get(ServiceUser_.username), body.username())
                )).getResultList();
                if (!same.isEmpty()) {
                    final String cause = same.getFirst().getEmail().equals(body.email())
                            ? "This email is already in use"
                            : "This username is already in use";

                    ctx.status(HttpStatus.BAD_REQUEST).json(Response.badRequest(cause));
                    return;
                }
            }

            final String salt = BCrypt.gensalt(10);
            final String hash = BCrypt.hashpw(body.password(), salt);

            final ServiceUser serviceUser = new ServiceUser();
            serviceUser.setEmail(body.email());
            serviceUser.setUsername(body.username());
            serviceUser.setHashedPassword(hash);
            sessionFactory.inTransaction(session -> {
                session.persist(serviceUser);
            });

            this.server.setAuthCookie(ctx, serviceUser.toUserInfo())
                    .status(HttpStatus.CREATED)
                    .json(serviceUser.toUserInfo());
        }));
    }
}
