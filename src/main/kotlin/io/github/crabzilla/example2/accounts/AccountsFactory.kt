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
    return Pair(JacksonJsonObjectSerDer(json, accountComponent),
      FeatureOptions(eventProjector = AccountOpenedProjector(json)))
      .let {
        context.featureController(accountComponent, it.first, it.second)
      }
  }

  @ApplicationScoped
  fun create(context: CrabzillaContext): AbstractVerticle {
    val config = ProjectorConfig(projectionName, stateTypes = listOf("Account"))
    return context.postgresProjector(config, AccountsView1Projector())
  }

}