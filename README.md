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