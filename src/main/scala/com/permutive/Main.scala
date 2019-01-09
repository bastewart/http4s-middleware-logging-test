package com.permutive

import cats.effect._
import cats.syntax.all._
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.circe.Json
import org.http4s.circe.jsonDecoder
import org.http4s.client.Client
import org.http4s.client.asynchttpclient.AsyncHttpClient
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.jetty.JettyClient
import org.http4s.client.middleware.{Logger => Http4sLogger}
import org.http4s.client.okhttp.OkHttpBuilder

import scala.concurrent.ExecutionContext.global

object Main extends IOApp {

  def addLogger[F[_] : Concurrent](client: Client[F]): Client[F] =
    Http4sLogger(logHeaders = true, logBody = true)(client)

  def asyncClient[F[_] : ConcurrentEffect]: Resource[F, Client[F]] =
    AsyncHttpClient.resource[F]()
      .map(addLogger[F])

  def blazeClient[F[_]: ConcurrentEffect]: Resource[F, Client[F]] =
    BlazeClientBuilder(global).resource
      .map(addLogger[F])

  def jettyClient[F[_] : ConcurrentEffect]: Resource[F, Client[F]] =
    JettyClient.resource()
      .map(addLogger[F])

  def okHttpClient[F[_] : ConcurrentEffect : ContextShift]: Resource[F, Client[F]] =
    OkHttpBuilder.withDefaultClient(global)
      .flatMap(_.resource)
      .map(addLogger[F])

  def sendTest[F[_] : Sync : Logger](description: String)(client: Client[F]): F[Unit] = {
    for {
      _ <- Logger[F].info(s"Starting test of $description")
      _ <- client.expect[Json]("https://api.github.com/users/octocat/orgs")(jsonDecoder[F])
      _ <- Logger[F].info(s"Finished test of $description")
    } yield ()
  }

  def testAll[F[_] : ConcurrentEffect : ContextShift : Logger]: F[Unit] = {
    for {
      _ <- asyncClient.use(sendTest("Async"))
      _ <- blazeClient.use(sendTest("Blaze"))
      _ <- jettyClient.use(sendTest("Jetty"))
      _ <- okHttpClient.use(sendTest("Ok"))
    } yield ()
  }

  override def run(args: List[String]): IO[ExitCode] = {
    implicit val logger: Logger[IO] = Slf4jLogger.unsafeCreate[IO]

    testAll[IO].as(ExitCode.Success)
  }

}
