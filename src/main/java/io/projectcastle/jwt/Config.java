/** ========================================================================= *
 * Copyright (C)  2017, 2018 Salesforce Inc ( http://www.salesforce.com/      *
 *                            All rights reserved.                            *
 *                                                                            *
 *  @author     Stephan H. Wissel (stw) <swissel@salesforce.com>              *
 *                                       @notessensei                         *
 * @version     1.0                                                           *
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
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Scanner;

import io.vertx.core.json.JsonObject;

/**
 * @author swissel
 *
 */
public class Config {

    private final JsonObject values;

    public Config(final JsonObject configValues) {
        this.values = configValues;
    }

    public Key getPrivateKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        Key key = null;
        String privateKeyName = this.values.getString("privateKey", "server.key");
        String keyString = this.fileToString(privateKeyName);
        keyString = keyString.replace("-----BEGIN PRIVATE KEY-----\n", "")
                .replace("-----END PRIVATE KEY-----", "").replaceAll("\n", "");
        final KeyFactory kf = KeyFactory.getInstance("RSA");
        final byte[] decoded = Base64.getDecoder().decode(keyString);
        final PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        key = kf.generatePrivate(spec);
        return key;
    }

    public Key getPublicKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        Key key = null;
        String pubKeyName = this.values.getString("publicKey", "server.pubkey");
        String keyString = this.fileToString(pubKeyName);
        keyString = keyString.replace("-----BEGIN PUBLIC KEY-----\n", "")
                .replace("-----END PUBLIC KEY-----", "").replaceAll("\n", "");
        final byte[] decoded = Base64.getDecoder().decode(keyString);
        final KeyFactory kf = KeyFactory.getInstance("RSA");
        final X509EncodedKeySpec keySpecX509 = new X509EncodedKeySpec(decoded);
        key = kf.generatePublic(keySpecX509);
        return key;
    }

    private String fileToString(final String resourceName) {
        try {
            InputStream in = null;
            File directPath = new File(resourceName);
            if (directPath.exists()) {
                in = new FileInputStream(directPath);
                System.out.println("Resource loaded from file:" + directPath.getAbsolutePath());
            } else {
                System.out.println("Couldn't find file " + directPath.getAbsolutePath());
            }

            final Scanner scanner = new Scanner(in);
            final String result = scanner.useDelimiter("\\Z").next();
            scanner.close();
            if (in != null) {
                in.close();
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
