package service

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

import java.time.Instant

trait UrlStorage {
  def findByKey(key: ShortenedUrlKey): IO[Either[InternalError, Option[ShortenedUrl]]]

  def createUrl(url: CreateShortenedUrl): IO[Either[InternalError, ShortenedUrl]]
}

object UrlStorage {
  def make(sql: UrlSql, transactor: Transactor[IO]): UrlStorage = {
    val logs: Make[IO] = Logging.Make.plain[IO]
    implicit val logging: Id[Logging[IO]] = logs.forService[UrlStorage]
    val impl = new Impl(sql, transactor)
    new LoggingImpl(impl)
  }

  private final class Impl(urlSql: UrlSql, transactor: Transactor[IO]) extends UrlStorage {
    override def findByKey(key: ShortenedUrlKey): IO[Either[InternalError, Option[ShortenedUrl]]] =
      urlSql.findByKey(key).transact(transactor).attempt.map(_.leftMap(InternalError.apply))

    override def createUrl(url: CreateShortenedUrl): IO[Either[InternalError, ShortenedUrl]] =
      IO.delay(ShortenedUrlKey.generate)
        .flatMap(key => urlSql.createUrl(key, url, CustomDate(Instant.now)).transact(transactor).attempt)
        .map {
          case Left(th)      => InternalError(th).asLeft
          case Right(result) => result.asRight
        }
  }

  private final class LoggingImpl(storage: UrlStorage)(implicit logging: Logging[IO])
    extends UrlStorage {

    override def findByKey(key: ShortenedUrlKey): IO[Either[InternalError, Option[ShortenedUrl]]] =
      surroundWithLogs(storage.findByKey(key))("Finding URL by key")(err =>
        (s"Failed to find URL by key: ${err.message}", Some(err.cause0))
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
  }
}
