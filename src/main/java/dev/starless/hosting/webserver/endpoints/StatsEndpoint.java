package dev.starless.hosting.webserver.endpoints;

import com.google.gson.JsonObject;
import dev.starless.hosting.objects.session.UserInfo;
import dev.starless.hosting.webserver.WebServer;
import dev.starless.hosting.webserver.WebServerEndpoint;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import io.javalin.http.UnauthorizedResponse;
import jakarta.persistence.Tuple;
import org.hibernate.StatelessSession;
import org.hibernate.query.Query;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class StatsEndpoint extends WebServerEndpoint {

    public StatsEndpoint(@NotNull WebServer server) {
        super(server, "/api/stats", HandlerType.GET);
    }

    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        final UserInfo info = ctx.attribute(SESSION_OBJECT_NAME);
        if (info == null) {
            throw new UnauthorizedResponse();
        }

        // Very dirty, but it works
        record UserStats(long userUploadsNumber, long userUploadsSize) {
        }

        final long totalFilesServed, totalFileSize;
        final long userUploadsNumber, userUploadsSize;
        try (final StatelessSession session = server.getHibernate().getSessionFactory().openStatelessSession()) {
            // Get number of files served at the moment
            final Query<Long> filesNumberQuery = session.createQuery(
                    "SELECT COUNT(u.id) FROM UserUpload u",
                    Long.class
            );
            totalFilesServed = Objects.requireNonNullElse(filesNumberQuery.getSingleResultOrNull(), 0L);

            // Get total bytes served at the moment
            final Query<Long> bytesQuery = session.createQuery(
                    "SELECT SUM(u.size) AS totalFileSize FROM UserUpload u",
                    Long.class
            );
            totalFileSize = Objects.requireNonNullElse(bytesQuery.getSingleResultOrNull(), 0L);

            final Query<Tuple> statsQuery = session.createQuery(
                    "SELECT COUNT(u.id) AS userUploadsNumber, SUM(u.size) AS userUploadsSize FROM UserUpload u WHERE u.uploaderId = :id",
                    Tuple.class
            );
            statsQuery.setParameter("id", info.id());

            final Tuple tuple = statsQuery.getSingleResultOrNull();
            if (tuple == null) {
                userUploadsNumber = 0;
                userUploadsSize = 0;
            } else {
                userUploadsNumber = this.getLongOrZero(tuple, 0);
                userUploadsSize = this.getLongOrZero(tuple, 1);
            }
        }

        final JsonObject obj = new JsonObject();
        obj.addProperty("totalFilesServed", totalFilesServed);
        obj.addProperty("totalFileSize", totalFileSize);
        obj.addProperty("userUploadsNumber", userUploadsNumber);
        obj.addProperty("userUploadsSize", userUploadsSize);
        ctx.json(obj);
    }

    private long getLongOrZero(final Tuple tuple, final int index) {
        try {
            Long value = tuple.get(index, Long.class);
            return Objects.requireNonNullElse(value, 0L);
        } catch (IllegalArgumentException ex) {
            return 0L;
        }
    }
}
