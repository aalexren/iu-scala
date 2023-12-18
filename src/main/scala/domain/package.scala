import derevo.circe.{decoder, encoder}
import derevo.derive
import io.estatico.newtype.macros.newtype
import sttp.tapir.{Codec, CodecFormat, Schema}
import doobie.Read
import tofu.logging.derivation._

import java.net.URL
import java.time.Instant
import java.util.UUID

package object domain {

  @derive(loggable, encoder, decoder)
  @newtype
  case class ShortenedUrlKey(value: String)

  object ShortenedUrlKey {
    implicit val read: Read[ShortenedUrlKey] = Read[String].map(ShortenedUrlKey.apply)
    implicit val schema: Schema[ShortenedUrlKey] =
      Schema.schemaForString.map(string => Some(ShortenedUrlKey(string)))(_.value)
    implicit val codec: Codec[String, ShortenedUrlKey, CodecFormat.TextPlain] =
      Codec.string.map(ShortenedUrlKey(_))(_.value)

    def generate: ShortenedUrlKey =
      ShortenedUrlKey(UUID.randomUUID().toString.take(6))
  }

  @derive(loggable, encoder, decoder)
  @newtype
  case class OriginalUrl(value: String)

  object OriginalUrl {
    implicit val read: Read[OriginalUrl] = Read[String].map(OriginalUrl.apply)
    implicit val schema: Schema[OriginalUrl] =
      Schema.schemaForString.map(string => Some(OriginalUrl(string)))(_.value)
  }

//  @derive(loggable, encoder, decoder)
//  @newtype
//  case class ResourceAvailabilityCheckUrl(value: String)
//
//  object ResourceAvailabilityCheckUrl {
//    implicit val read: Read[ResourceAvailabilityCheckUrl] =
//      Read[String].map(ResourceAvailabilityCheckUrl.apply)
//    implicit val schema: Schema[ResourceAvailabilityCheckUrl] =
//      Schema.schemaForString.map(string => Some(ResourceAvailabilityCheckUrl(string)))(_.value)
//    implicit val codec: Codec[String, ResourceAvailabilityCheckUrl, CodecFormat.TextPlain] =
//      Codec.string.map(ResourceAvailabilityCheckUrl(_))(_.value)
//  }

  @derive(loggable, encoder, decoder)
  @newtype
  case class ResourceAvailabilityCheckResult(value: Boolean)

  object ResourceAvailabilityCheckResult {
    implicit val read: Read[ResourceAvailabilityCheckResult] =
      Read[Boolean].map(ResourceAvailabilityCheckResult.apply)
    implicit val schema: Schema[ResourceAvailabilityCheckResult] =
      Schema.schemaForBoolean.map(boolean => Some(ResourceAvailabilityCheckResult(boolean)))(
        _.value
      )
    implicit val codec: Codec[String, ResourceAvailabilityCheckResult, CodecFormat.TextPlain] =
      Codec.boolean.map(ResourceAvailabilityCheckResult(_))(_.value)
  }

  @derive(loggable, encoder, decoder)
  @newtype
  case class CustomDate(value: Instant)

  object CustomDate {
    implicit val read: Read[CustomDate] =
      Read[Long].map(n => CustomDate(Instant.ofEpochMilli(n)))

    implicit val schema: Schema[CustomDate] =
      Schema.schemaForLong
        .map(long => Some(CustomDate(Instant.ofEpochMilli(long))))(_.value.toEpochMilli)
  }
}
