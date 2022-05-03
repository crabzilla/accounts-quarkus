package io.github.crabzilla.example2.accounts

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.crabzilla.command.CommandMetadata
import io.github.crabzilla.command.CommandSideEffect
import io.github.crabzilla.command.FeatureController
import io.quarkus.vertx.ConsumeEvent
import io.smallrye.mutiny.Uni
import io.vertx.core.json.JsonObject
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class AccountService(val controller: FeatureController<Account, AccountCommand, AccountEvent>, val json: ObjectMapper) {

  @ConsumeEvent("command:accounts")
  fun handle(request: JsonObject): Uni<CommandSideEffect> {
    val metadata = CommandMetadata.fromJsonObject(request.getJsonObject("metadata"))
    val command = json.readValue(request.getJsonObject("command").toString(), AccountCommand::class.java)
    return controller.handle(metadata, command).toCompletionStage()
      .let {
        Uni.createFrom().completionStage(it)
      }
  }

}