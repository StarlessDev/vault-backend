package dev.starless.hosting.webserver.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

public class JWTProvider {

    @Getter
    private static final JWTProvider instance = new JWTProvider();
    private static final String JWT_ISSUER = "vault";

    private final JWTKeys keys;
    private final Logger logger;

    private JWTVerifier verifier;
    private Algorithm algorithm;

    public JWTProvider() {
        this.keys = new JWTKeys();
        this.logger = LoggerFactory.getLogger(JWTProvider.class);

        this.verifier = null;
        this.algorithm = null;
    }

    public void init() {
        keys.load();
        if (!keys.isLoaded()) {
            logger.error("Could not load RSA keys!");
            System.exit(1);
            return;
        }

        try {
            this.algorithm = Algorithm.RSA256(keys.getPublicKey(), keys.getPrivateKey());
        } catch (JWTCreationException exception) {
            logger.error("JWT initialization failed", exception);
            System.exit(1);
            return;
        }

        this.verifier = JWT.require(algorithm)
                .withIssuer(JWT_ISSUER)
                .build();
    }

    public String sign(final JsonElement data,
                       final Duration expiresAfter) {
        try {
            final Instant now = Instant.now();
            final JsonObject payload = new JsonObject();
            payload.add("data", data);

            return JWT.create()
                    .withIssuer(JWT_ISSUER)
                    .withPayload(payload.toString())
                    .withIssuedAt(now)
                    .withExpiresAt(now.plus(expiresAfter))
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            logger.error("Invalid Signing configuration / Couldn't convert Claims.", exception);
        }
        return null;
    }

    public JsonElement verify(String token) {
        DecodedJWT decodedJWT;
        try {
            decodedJWT = verifier.verify(token);
        } catch (JWTVerificationException exception){
            logger.error("Invalid signature/claims", exception);
            return null;
        }

        if (!algorithm.getName().equals(decodedJWT.getAlgorithm())) {
            return null;
        }

        return JsonParser.parseString(new String(
                Base64.getDecoder().decode(decodedJWT.getPayload()),
                StandardCharsets.UTF_8
        ));
    }
}
