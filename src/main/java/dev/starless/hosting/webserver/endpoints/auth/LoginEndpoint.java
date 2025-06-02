package dev.starless.hosting.webserver.endpoints.auth;

import dev.starless.hosting.objects.ServiceUser;
import dev.starless.hosting.objects.ServiceUser_;
import dev.starless.hosting.objects.bodies.LoginBody;
import dev.starless.hosting.objects.session.UserInfo;
import dev.starless.hosting.webserver.WebServer;
import dev.starless.hosting.webserver.WebServerEndpoint;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class LoginEndpoint extends WebServerEndpoint {

    public LoginEndpoint(@NotNull WebServer server) {
        super(server, "/api/auth/login", HandlerType.POST);
    }

    @Override
    public void handle(@NotNull Context ctx) {
        final LoginBody body = ctx.bodyValidator(LoginBody.class)
                .check(login -> login.email() != null, "INVALID_BODY")
                .check(login -> login.password() != null, "INVALID_BODY")
                .get();

        ctx.future(() -> CompletableFuture.runAsync(() -> {
            final Optional<ServiceUser> user;
            // We need to assign the user,
            // so we need a try-catch to avoid lambdas
            try (final Session session = server.getHibernate().getSessionFactory().openSession()) {
                final CriteriaBuilder cb = session.getCriteriaBuilder();
                final CriteriaQuery<ServiceUser> query = cb.createQuery(ServiceUser.class);
                final Root<ServiceUser> root = query.from(ServiceUser.class);

                user = session.createQuery(query.select(root).where(cb.equal(root.get(ServiceUser_.email), body.email())))
                        .getResultList()
                        .stream()
                        .filter(storedUser -> {
                            return BCrypt.checkpw(body.password(), storedUser.getHashedPassword());
                        })
                        .findFirst();
            }

            if (user.isPresent()) {
                final UserInfo info = user.get().toUserInfo();
                this.server.setAuthCookie(ctx, info)
                        .status(HttpStatus.OK)
                        .json(info);
            } else {
                ctx.status(HttpStatus.UNAUTHORIZED);
            }
        }));
    }
}
