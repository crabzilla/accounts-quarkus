package io.github.crabzilla.example2.accounts


import com.fasterxml.jackson.databind.ObjectMapper
import io.github.crabzilla.jackson.JacksonJsonObjectSerDer
import io.github.crabzilla.stack.command.CommandServiceApi
import io.github.crabzilla.stack.command.CommandServiceApiFactory
import io.github.crabzilla.stack.command.CommandServiceOptions
import io.github.crabzilla.stack.subscription.SubscriptionApi
import io.github.crabzilla.stack.subscription.SubscriptionApiFactory
import io.github.crabzilla.stack.subscription.SubscriptionConfig
import io.github.crabzilla.stack.subscription.SubscriptionSink
import io.github.crabzilla.stack.subscription.SubscriptionSink.POSTGRES_PROJECTOR
import javax.enterprise.context.ApplicationScoped

class AccountsFactory {

  companion object {
    const val projectionName: String = "accounts-view"
  }

  @ApplicationScoped
  fun create(factory: CommandServiceApiFactory, json: ObjectMapper)
  : CommandServiceApi<AccountCommand> {
    val jsonSerDer = JacksonJsonObjectSerDer(json, accountComponent)
    val options = CommandServiceOptions(eventProjector = AccountOpenedProjector(json))
    return factory.commandService(accountComponent, jsonSerDer, options)
  }

  @ApplicationScoped
  fun create(factory: SubscriptionApiFactory): SubscriptionApi {
    val config = SubscriptionConfig(projectionName,
      initialInterval = 100, maxInterval = 1000,
      stateTypes = listOf("Account"), sink = POSTGRES_PROJECTOR
    )
    return factory.subscription(config, AccountsView1Projector())
  }

}