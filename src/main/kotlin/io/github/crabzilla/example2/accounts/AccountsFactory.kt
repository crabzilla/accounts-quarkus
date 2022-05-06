package io.github.crabzilla.example2.accounts


import com.fasterxml.jackson.databind.ObjectMapper
import io.github.crabzilla.CrabzillaContext
import io.github.crabzilla.command.FeatureController
import io.github.crabzilla.command.FeatureOptions
import io.github.crabzilla.jackson.JacksonJsonObjectSerDer
import io.github.crabzilla.projection.ProjectorConfig
import io.vertx.core.AbstractVerticle
import javax.enterprise.context.ApplicationScoped

class AccountsFactory {

  companion object {
    const val projectionName: String = "accounts-view"
  }

  @ApplicationScoped
  fun create(context: CrabzillaContext, json: ObjectMapper)
  : FeatureController<Account, AccountCommand, AccountEvent> {
    val jsonSerDer = JacksonJsonObjectSerDer(json, accountComponent)
    val options = FeatureOptions(eventProjector = AccountOpenedProjector(json))
    return context.featureController(accountComponent, jsonSerDer, options)
  }

  @ApplicationScoped
  fun create(context: CrabzillaContext): AbstractVerticle {
    val config = ProjectorConfig(projectionName, stateTypes = listOf("Account"))
    return context.postgresProjector(config, AccountsView1Projector())
  }

}