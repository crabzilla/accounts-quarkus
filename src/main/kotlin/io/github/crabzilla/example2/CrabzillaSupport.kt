package io.github.crabzilla.example2

import io.github.crabzilla.stack.CrabzillaContext
import io.github.crabzilla.stack.DefaultCrabzillaContextFactory
import io.github.crabzilla.stack.command.CommandServiceApiFactory
import io.github.crabzilla.stack.command.DefaultCommandServiceApiFactory
import io.github.crabzilla.stack.subscription.DefaultSubscriptionApiFactory
import io.github.crabzilla.stack.subscription.SubscriptionApi
import io.github.crabzilla.stack.subscription.SubscriptionApiFactory
import io.quarkus.runtime.StartupEvent
import io.smallrye.common.annotation.Blocking
import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithName
import io.smallrye.mutiny.Uni
import io.vertx.core.AbstractVerticle
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgPool
import org.slf4j.LoggerFactory
import javax.enterprise.context.ApplicationScoped
import javax.enterprise.event.Observes
import javax.enterprise.inject.Instance


@ConfigMapping(prefix = "quarkus.datasource")
interface QuarkusPgConfig {
  fun dbKind(): String
  fun username(): String
  fun password(): String
  @WithName("reactive.url")
  fun url(): String
}

class CrabzillaFactory {

  @ApplicationScoped
  fun context(vertx: Vertx, pgPool: PgPool, quarkusPgConfig: QuarkusPgConfig): CrabzillaContext {
    fun toPgConfig(quarkusPgConfig: QuarkusPgConfig): JsonObject {
      return JsonObject()
        .put("url", quarkusPgConfig.url())
        .put("username", quarkusPgConfig.username())
        .put("password", quarkusPgConfig.password())
    }
    val pgConfig: JsonObject = toPgConfig(quarkusPgConfig)
    return DefaultCrabzillaContextFactory().new(vertx, pgConfig, pgPool)
  }

  @ApplicationScoped
  fun commandServiceFactory(context: CrabzillaContext) : CommandServiceApiFactory {
    return DefaultCommandServiceApiFactory(context)
  }

  @ApplicationScoped
  fun subscriptionFactory(context: CrabzillaContext) : SubscriptionApiFactory {
    return DefaultSubscriptionApiFactory(context)
  }

}

@ApplicationScoped
class CrabzillaDeployer {

  @Blocking
  fun init(@Observes e: StartupEvent?,
           vertx: io.vertx.mutiny.core.Vertx,
           subs: Instance<SubscriptionApi>
  ) {
    for (sub in subs) {
      log.info("Deploying subscription " + sub.name())
      Uni.createFrom().completionStage(sub.deploy().toCompletionStage()).await().indefinitely()
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(CrabzillaDeployer::class.java)
  }

}