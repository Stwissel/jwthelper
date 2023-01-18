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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import io.vertx.core.json.JsonObject;

/**
 * @author swissel
 */
public class Config {

    private final JsonObject values;

    private final List<String> replaceThem = List.of(
            "-----BEGIN PRIVATE KEY-----\n",
            "-----END PRIVATE KEY-----",
            "-----BEGIN RSA PRIVATE KEY-----\n",
            "-----END RSA PRIVATE KEY-----",
            "-----BEGIN PUBLIC KEY-----\n",
            "-----END PUBLIC KEY-----",
            "\n");

    public Config(final JsonObject configValues) {
        this.values = configValues;
    }

    private byte[] getBytes(final String keyType, final String defaultName)
            throws KeyStringMissingException {
        String keyName = this.values.getString(keyType, defaultName);
        String keyString = this.fileToString(keyName)
                .orElseThrow(() -> new KeyStringMissingException(keyType + " Key Missing"));
        for (String r : replaceThem) {
            keyString = keyString.replace(r, "");
        }

        return Base64.getDecoder().decode(keyString);
    }

    public Key getPrivateKey()
            throws NoSuchAlgorithmException, InvalidKeySpecException, KeyStringMissingException {
        final byte[] decoded = getBytes("privateKey", "server.key");
        final KeyFactory kf = KeyFactory.getInstance("RSA");
        final PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        return kf.generatePrivate(spec);
    }

    public Key getPublicKey()
            throws NoSuchAlgorithmException, InvalidKeySpecException, KeyStringMissingException {
        final byte[] decoded = getBytes("publicKey", "server.pubkey");
        final KeyFactory kf = KeyFactory.getInstance("RSA");
        final X509EncodedKeySpec keySpecX509 = new X509EncodedKeySpec(decoded);
        return kf.generatePublic(keySpecX509);
    }

    private Optional<String> fileToString(final String resourceName) {
        try {

            final Path path = Path.of(resourceName);
            final String result = Files.readString(path);
            return Optional.ofNullable(result);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Optional.empty();

    }

}
