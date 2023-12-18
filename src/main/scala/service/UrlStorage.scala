package service

import scala.util.Try

import cats.Id
import cats.effect.IO
import cats.syntax.either._
import dao.UrlSql
import domain._
import domain.errors._
import doobie._
import doobie.implicits._
import tofu.logging.Logging
import tofu.logging.Logging.Make
import tofu.syntax.logging._
import cats.syntax.option._
import sttp.client3._

import java.time.Instant
import java.net.URL

trait UrlStorage {
  def findByKey(key: ShortenedUrlKey): IO[Either[AppError, Option[ShortenedUrl]]]

  def createUrl(url: CreateShortenedUrl): IO[Either[InternalError, ShortenedUrl]]

  def checkUrlAvailability(
    key: ShortenedUrlKey
  ): IO[Either[AppError, ResourceAvailabilityCheckResult]]
}

object UrlStorage {
  def make(sql: UrlSql, transactor: Transactor[IO]): UrlStorage = {
    val logs: Make[IO] = Logging.Make.plain[IO]
    implicit val logging: Id[Logging[IO]] = logs.forService[UrlStorage]
    val impl = new Impl(sql, transactor)
    new LoggingImpl(impl)
  }

  private final class Impl(urlSql: UrlSql, transactor: Transactor[IO]) extends UrlStorage {
    override def findByKey(key: ShortenedUrlKey): IO[Either[AppError, Option[ShortenedUrl]]] =
      urlSql
        .findByKey(key)
        .transact(transactor)
        .attempt
        .map {
          case Left(th)            => InternalError(th).asLeft
          case Right(Some(result)) => Right(Option(result))
          case Right(None)         => Left(UrlNotFound(key))
        }

    override def createUrl(url: CreateShortenedUrl): IO[Either[InternalError, ShortenedUrl]] =
      IO.delay(ShortenedUrlKey.generate)
        .flatMap(key =>
          urlSql.createUrl(key, url, CustomDate(Instant.now)).transact(transactor).attempt
        )
        .map {
          case Left(th)      => InternalError(th).asLeft
          case Right(result) => result.asRight
        }

    override def checkUrlAvailability(
      key: ShortenedUrlKey
    ): IO[Either[AppError, ResourceAvailabilityCheckResult]] =
      urlSql
        .findByKey(key)
        .transact(transactor)
        .attempt
        .map {
          case Left(th) => InternalError(th).asLeft
          case Right(Some(result)) =>
            Try(new URL(result.url.value)).toEither.left
              .map(_ => InvalidUrlFormat(result.url.value))
              .flatMap(url => {
                val backend = HttpClientSyncBackend()
                val response = basicRequest.get(uri"${url.toURI}").send(backend)
                if (response.isSuccess) {
                  ResourceAvailabilityCheckResult(true).asRight
                } else {
                  Left(ServiceUnavailable(key))
                }
              })
          case Right(None) => Left(UrlNotFound(key))
        }
  }

  private final class LoggingImpl(storage: UrlStorage)(implicit logging: Logging[IO])
    extends UrlStorage {

    override def findByKey(key: ShortenedUrlKey): IO[Either[AppError, Option[ShortenedUrl]]] =
      surroundWithLogs(storage.findByKey(key))("Finding URL by key")(err =>
        (s"Failed to find URL by key: ${err.message}", err.cause)
      )(success => s"Found URL: $success")

    private def surroundWithLogs[Error, Res](
      io: IO[Either[Error, Res]]
    )(
      inputLog: String
    )(errorOutputLog: Error => (String, Option[Throwable]))(
      successOutputLog: Res => String
    ): IO[Either[Error, Res]] =
      info"$inputLog" *> io.flatTap {
        case Left(error) =>
          val (logString: String, throwable: Option[Throwable]) =
            errorOutputLog(error)
          throwable.fold(error"$logString")(err => errorCause"$logString" (err))
        case Right(success) => info"${successOutputLog(success)}"
      }

    override def createUrl(url: CreateShortenedUrl): IO[Either[InternalError, ShortenedUrl]] =
      surroundWithLogs(storage.createUrl(url))("Creating Shortened URL")(err =>
        (s"Failed to create Shortened URL: ${err.message}", Some(err.cause0))
      )(success => s"Created Shortened URL: $success")

    override def checkUrlAvailability(
      key: ShortenedUrlKey
    ): IO[Either[AppError, ResourceAvailabilityCheckResult]] =
      surroundWithLogs(storage.checkUrlAvailability(key))("Checking URL Availability")(err =>
        (s"Failed to check URL availability: ${err.message}", err.cause)
      )(success => s"URL is Available: $success")
  }
}
