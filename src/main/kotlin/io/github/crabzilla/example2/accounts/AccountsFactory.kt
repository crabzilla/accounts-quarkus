package io.github.crabzilla.example2.accounts


import com.fasterxml.jackson.databind.ObjectMapper
import io.github.crabzilla.jackson.JacksonJsonObjectSerDer
import io.github.crabzilla.stack.CrabzillaContext
import io.github.crabzilla.stack.command.FeatureOptions
import io.github.crabzilla.stack.command.FeatureService
import io.github.crabzilla.stack.subscription.SubscriptionConfig
import io.vertx.core.AbstractVerticle
import javax.enterprise.context.ApplicationScoped

class AccountsFactory {

  companion object {
    const val projectionName: String = "accounts-view"
  }

  @ApplicationScoped
  fun create(context: CrabzillaContext, json: ObjectMapper)
  : FeatureService<Account, AccountCommand, AccountEvent> {
    val jsonSerDer = JacksonJsonObjectSerDer(json, accountComponent)
    val options = FeatureOptions(eventProjector = AccountOpenedProjector(json))
    return context.featureService(accountComponent, jsonSerDer, options)
  }

  @ApplicationScoped
  fun create(context: CrabzillaContext): AbstractVerticle {
    val config = SubscriptionConfig(projectionName, stateTypes = listOf("Account"))
    return context.subscriptionWithPostgresSink(config, AccountsView1Projector()).first
  }

}