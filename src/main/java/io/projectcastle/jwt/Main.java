/** ========================================================================= *
 * Copyright (C)  2017, 2018 Salesforce Inc ( http://www.salesforce.com/      *
 *                            All rights reserved.                            *
 *                      2023 HCL Inc                                          *
 *  @author     Stephan H. Wissel (stw) <stw@linux.com>                       *
 *                                       @notessensei                         *
 * @version     2.0                                                           *
 * ========================================================================== *
 *                                                                            *
 * Licensed under the  Apache License, Version 2.0  (the "License").  You may *
 * not use this file except in compliance with the License.  You may obtain a *
 * copy of the License at <http://www.apache.org/licenses/LICENSE-2.0>.       *
 *                                                                            *
 * Unless  required  by applicable  law or  agreed  to  in writing,  software *
 * distributed under the License is distributed on an  "AS IS" BASIS, WITHOUT *
 * WARRANTIES OR  CONDITIONS OF ANY KIND, either express or implied.  See the *
 * License for the  specific language  governing permissions  and limitations *
 * under the License.                                                         *
 *                                                                            *
 * ========================================================================== *
 */
package io.projectcastle.jwt;

import java.io.File;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

/**
 * @author swissel
 */
public class Main extends AbstractVerticle {

    public static final String CONFIG_FILE = "config.json";

    /**
     * @param args
     */
    public static void main(final String[] args) {
        Runner.runVerticle(Main.class.getName(), true);

    }

    private final Logger logger = LogManager.getLogger(this.getClass());
    private Router router;
    private Config appConfig;
    private int port = 8080;

    @Override
    public void start(final Promise<Void> startPromise) throws Exception {

        this.loadAppConfig()
                .compose(v -> this.loadRoutes())
                .compose(v -> {
                    // Launch the server
                    this.logger.info("Listening on port {}", this.port);
                    return this.vertx.createHttpServer()
                            .requestHandler(this.router)
                            .listen(this.port);
                })
                .onSuccess(v -> startPromise.complete())
                .onFailure(startPromise::fail);
    }

    private void createHandler(final RoutingContext ctx) {
        final JsonObject payload;
        final Key key;
        try {
            payload = ctx.body().asJsonObject();
            key = this.appConfig.getPrivateKey();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | KeyStringMissingException e) {
            e.printStackTrace();
            ctx.response().setStatusCode(400).end(e.getMessage());
            return;
        }

        final Claims claims = Jwts.claims();

        claims.setIssuer(payload.getString("issuer", "issuer"));
        claims.setAudience(payload.getString("audience", "audience"));
        claims.setSubject(payload.getString("subject", "subject"));
        final int duration = payload.getInteger("duration", 300000);
        claims.setExpiration(new Date(System.currentTimeMillis() + duration));

        final String compactJws = Jwts.builder()
                .setHeaderParam("alg", "RS256")
                .addClaims(claims)
                .signWith(SignatureAlgorithm.RS256, key)
                .compact();

        ctx.response().end(compactJws);
    }

    private Future<Void> loadAppConfig() {

        // Get Port for web server
        final String portCandidate = System.getenv("PORT");
        try {
            if ((portCandidate != null) && !"".equals(portCandidate)) {
                this.port = Integer.parseInt(portCandidate);
            }
        } catch (final Exception e) {
            this.logger.fatal(e);
            return Future.failedFuture(e);
        }

        // Populate configuration object
        final File cFile = new File(Main.CONFIG_FILE);

        if (!cFile.exists()) {
            System.out.println("Running without " + cFile.getAbsolutePath());
            this.appConfig = new Config(new JsonObject());
            return Future.succeededFuture();
        }

        Promise<Void> configLoad = Promise.promise();

        final ConfigStoreOptions fileConfig =
                new ConfigStoreOptions().setType("file").setFormat("json")
                        .setConfig(new JsonObject().put("path", Main.CONFIG_FILE));

        final ConfigRetrieverOptions retrieverOptions =
                new ConfigRetrieverOptions().addStore(fileConfig);
        final ConfigRetriever retriever = ConfigRetriever.create(this.getVertx(), retrieverOptions);

        retriever.getConfig()
                .onFailure(err -> {
                    System.out.println("Running without " + cFile.getAbsolutePath());
                    this.appConfig = new Config(new JsonObject());
                    configLoad.complete();
                })
                .onSuccess(cfg -> {
                    this.appConfig = new Config(cfg);
                    configLoad.complete();
                });

        return configLoad.future();
    }

    private Future<Void> loadRoutes() {
        this.router = Router.router(this.getVertx());
        this.router.route("/").handler(this::rootHandler);
        this.router.post("/*").handler(BodyHandler.create());
        this.router.post("/create").handler(this::createHandler);
        this.router.post("/validate").handler(this::validateHandler);
        return Future.succeededFuture();
    }

    private void rootHandler(final RoutingContext ctx) {
        ctx.response().end("Use POST to /create or /valdate");
    }

    private void validateHandler(final RoutingContext ctx) {

        final JsonObject payload;
        final String jwtString;

        try {
            payload = ctx.body().asJsonObject();
            jwtString = payload.getString("jwt", "jwt");
            final Claims claims = Jwts.parser().setSigningKey(this.appConfig.getPublicKey())
                    .parseClaimsJws(jwtString)
                    .getBody();
            final JsonObject result = new JsonObject();
            claims.forEach(result::put);
            ctx.response().putHeader("Content-Type", "application/json")
                    .end(result.encodePrettily());
        } catch (ExpiredJwtException | UnsupportedJwtException | MalformedJwtException
                | SignatureException | KeyStringMissingException
                | IllegalArgumentException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
            ctx.response().setStatusCode(400).end(e.getMessage());
        }

    }

}
