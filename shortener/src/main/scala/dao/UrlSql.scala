package dao

import cats.implicits.toFunctorOps
import cats.{Applicative, Monad}
import cats.syntax.applicative._
import cats.syntax.either._
import domain._
import domain.errors._
import doobie._
import doobie.implicits._

import java.time.Instant

trait UrlSql {
  def findByKey(key: ShortenedUrlKey): ConnectionIO[Option[ShortenedUrl]]
  def createUrl(key: ShortenedUrlKey, url: CreateShortenedUrl, created_at: CustomDate): ConnectionIO[ShortenedUrl]
}

object UrlSql {

  object sqls {
    def findByKeySql(key: ShortenedUrlKey): Query0[ShortenedUrl] =
      sql"""
           SELECT *
           FROM urls
           WHERE key = ${key.value}
      """.query[ShortenedUrl]

    def createUrlSql(key: ShortenedUrlKey, url: CreateShortenedUrl, created_at: CustomDate): Update0 =
      sql"""
           INSERT INTO urls (key, url, created_at)
           VALUES (${key.value}, ${url.url.value}, ${created_at.value.toEpochMilli})
      """.update
  }

  private final class Impl extends UrlSql {

    import sqls._

    override def findByKey(key: ShortenedUrlKey): ConnectionIO[Option[ShortenedUrl]] =
      findByKeySql(key).option

    override def createUrl(
      key: ShortenedUrlKey,
      url: CreateShortenedUrl,
      created_at: CustomDate,
    ): ConnectionIO[ShortenedUrl] = {
      createUrlSql(key, url, created_at)
        .withUniqueGeneratedKeys[ShortenedUrlKey]("key")
        .map((key: ShortenedUrlKey) =>
          ShortenedUrl(key, url.url, created_at)
        )
    }
  }

  def make: UrlSql = new Impl
}
