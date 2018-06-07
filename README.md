# JWT Helper

Local tool to generate JWT tokens mainly for testing Salesforce APIs

Use it: `java -jar jwthelper-1.0.0-fat.jar`

By default it will listen on port 8080 and 
look for they key files `server.key` for the private and
`server.pubkey` for the public key

The environment variable `PORT` can be used to specify a different port

Stop with CTRL+C

## config.json

A file `config.json` can be used to overwrite these values:

```
{
   "privateKey" : "server.key",
    "publicKey" : "server.pubkey"
}
```
The values can be absolute or relative path

## Creating a JWT Token

Send a `POST` request to `localhost:8080/create` with a Json payload:

```
{
    "issuer" : "That's the client token string",
    "audience" : "https://test.salesforce.com",
    "subject" : "john@doe.com",
    "duration" : 300000
}
``` 
Duration is optional. The return value is the JWT token

## Validating a JWT Token

Send a `POST` request to `localhost:8080/validate` with a Json payload:

```
{
    "jwt" : "The JWT String"
}
```
Return value will be the validated claim as Json string. Any Error throws an Error 500

  
## Postman sample
In the Postman directory  