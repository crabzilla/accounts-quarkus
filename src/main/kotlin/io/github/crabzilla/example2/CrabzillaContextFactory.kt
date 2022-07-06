package io.github.crabzilla.example2

import io.github.crabzilla.stack.CrabzillaContext
import io.github.crabzilla.stack.DefaultCrabzillaContextFactory
import io.github.crabzilla.stack.command.CommandServiceApiFactory
import io.github.crabzilla.stack.command.DefaultCommandServiceApiFactory
import io.github.crabzilla.stack.subscription.DefaultSubscriptionApiFactory
import io.github.crabzilla.stack.subscription.SubscriptionApiFactory
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgPool
import javax.enterprise.context.ApplicationScoped

class CrabzillaContextFactory {

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