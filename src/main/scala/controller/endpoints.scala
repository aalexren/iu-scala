package controller

import domain._
import domain.errors._
import sttp.tapir._
import sttp.tapir.json.circe._
import sttp.tapir.docs.apispec.DocsExtensionAttribute._

object endpoints {
  val urlGroupDescription = "url shortener"

  val findUrlByKey: PublicEndpoint[ShortenedUrlKey, AppError, Option[ShortenedUrl], Any] =
    endpoint.get
      .in("url" / path[ShortenedUrlKey]("key").description("Shorten URL"))
      .errorOut(jsonBody[AppError])
      .out(jsonBody[Option[ShortenedUrl]])
      .tag(urlGroupDescription)

  val createUrl: PublicEndpoint[CreateShortenedUrl, AppError, ShortenedUrl, Any] =
    endpoint.post
      .in("url")
      .in(jsonBody[CreateShortenedUrl])
      .errorOut(jsonBody[AppError])
      .out(jsonBody[ShortenedUrl])
      .tag(urlGroupDescription)

  val checkUrlAvailability
    : PublicEndpoint[ShortenedUrlKey, AppError, ResourceAvailabilityCheckResult, Any] =
    endpoint.get
      .in(
        "url" / query[ShortenedUrlKey]("key")
          .description("Check URL availability")
          .example(ShortenedUrlKey("someurl"))
      )
      .errorOut(jsonBody[AppError])
      .out(jsonBody[ResourceAvailabilityCheckResult])
      .tag(urlGroupDescription)
}
