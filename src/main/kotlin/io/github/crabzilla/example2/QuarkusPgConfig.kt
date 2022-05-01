package io.github.crabzilla.example2

import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithName

@ConfigMapping(prefix = "quarkus.datasource")
interface QuarkusPgConfig {
  fun dbKind(): String
  fun username(): String
  fun password(): String
  @WithName("reactive.url")
  fun url(): String
}