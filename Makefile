.PHONY: help

help:
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'

build:  ## create service's docker image
	@sbt docker:publishLocal

format:
	@sbt scalafmtAll && sbt scalafmt

up:  ## start compose service
	@docker compose up -d --no-deps --remove-orphans

down:  ## stop and delete compose service
	@docker compose down