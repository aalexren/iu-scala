package domain

import derevo.circe.{decoder, encoder}
import derevo.derive
//import doobie.Read
//import sttp.tapir.Schema
import sttp.tapir.derevo.schema
import tofu.logging.derivation._

@derive(loggable, encoder, decoder, schema)
final case class CreateShortenedUrl(url: OriginalUrl)

@derive(loggable, encoder, decoder, schema)
final case class ShortenedUrl(key: ShortenedUrlKey, url: OriginalUrl, createdDate: CustomDate)
