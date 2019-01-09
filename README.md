Test if the client `RequestLogger` works in `http4s`.

`sbt run` should test all client implementations (`Async`, `Blaze`, `Jetty`, `OkHttp`).

Currently:
 * `ResponseLogger` works for all clients
 * `RequestLogger` does not work for `Async` or `OkHttp`
