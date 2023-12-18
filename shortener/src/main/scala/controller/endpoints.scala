package controller

import domain._
import domain.errors._
import sttp.tapir._
import sttp.tapir.json.circe._

object endpoints {

  val findUrlByKey: PublicEndpoint[ShortenedUrlKey, AppError, Option[ShortenedUrl], Any] =
    endpoint.get
      .in("url" / path[ShortenedUrlKey])
      .errorOut(jsonBody[AppError])
      .out(jsonBody[Option[ShortenedUrl]])

  val createUrl: PublicEndpoint[CreateShortenedUrl, AppError, ShortenedUrl, Any] =
    endpoint.post
      .in("url")
      .in(jsonBody[CreateShortenedUrl])
      .errorOut(jsonBody[AppError])
      .out(jsonBody[ShortenedUrl])
}
