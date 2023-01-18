# JWT Helper

Local tool to generate JWT tokens mainly for testing JWT consuming APIs like Salesforce or Domino REST API

Use it: `java -jar jwthelper-2.0.0-fat.jar`

By default it will listen on port 8080 and
look for they key files `server.key` for the private and
`server.pubkey` for the public key

The environment variable `PORT` can be used to specify a different port

Stop with CTRL+C

## config.json

A file `config.json` can be used to overwrite these values:

```json
{
  "privateKey": "server.key",
  "publicKey": "server.pubkey"
}
```

The values can be absolute or relative path

## Creating a JWT Token

Send a `POST` request to `localhost:8080/create` with a Json payload:

```json
{
  "issuer": "That's the client token string",
  "audience": "https://test.salesforce.com",
  "subject": "john@doe.com",
  "duration": 300000
}
```

Duration is optional. The return value is the JWT token

## Validating a JWT Token

Send a `POST` request to `localhost:8080/validate` with a Json payload:

```json
{
  "jwt": "The JWT String"
}
```

Return value will be the validated claim as Json string. Any Error throws an Error 400

## Generating a key pair

Uses RSA only, Use ssh-keygen and openssl:

```bash
ssh-keygen -t rsa -b 4096 -m PEM -f raw.key
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in raw.key -out server.key
openssl rsa -in raw.key -pubout -outform PEM -out server.pubkey
```

## Postman sample

In the Postman directory

## Change log

- 2.0 Java17, Vert.x 4.x
- 1.0 Initial release
