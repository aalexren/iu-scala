package domain

import cats.syntax.option._
import derevo.circe.{decoder, encoder}
import derevo.derive
import io.circe.Decoder.Result
import io.circe.{Decoder, Encoder, HCursor, Json}
import sttp.tapir.Schema
import sttp.tapir.derevo.schema

object errors {
  @derive(encoder, decoder)
  sealed abstract class AppError(
    val message: String,
    val cause: Option[Throwable] = None
  )
  @derive(encoder, decoder)
  case class UrlNotFound(key: ShortenedUrlKey)
    extends AppError(s"URL with shorten version ${key.value} not found")
  @derive(encoder, decoder)
  case class ServiceUnavailable(key: ShortenedUrlKey)
    extends AppError(s"Service with with shorten url version ${key.value} unavailable")

  @derive(encoder, decoder)
  case class InvalidUrlFormat(url: String) extends AppError(s"Invalid URL format: $url")

  @derive(encoder, decoder)
  case class InternalError(
    cause0: Throwable
  ) extends AppError("Internal error", cause0.some)
  @derive(encoder, decoder)
  case class DecodedError(override val message: String) extends AppError(message = message)

  implicit val throwableEncoder: Encoder[Throwable] =
    Encoder.encodeString.contramap(_.getMessage)
  implicit val throwableDecoder: Decoder[Throwable] =
    Decoder.decodeString.map(new Throwable(_))
  implicit val schema: Schema[AppError] =
    Schema.schemaForString.map[AppError](str => Some(DecodedError(str)))(
      _.message
    )
}
