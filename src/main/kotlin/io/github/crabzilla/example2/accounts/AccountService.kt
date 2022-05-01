package io.github.crabzilla.example2.accounts

import io.github.crabzilla.command.CommandController
import io.github.crabzilla.command.CommandSideEffect
import io.quarkus.vertx.ConsumeEvent
import io.smallrye.mutiny.Uni
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class AccountService(val controller: CommandController<Account, AccountCommand, AccountEvent>) {

  @ConsumeEvent("command:accounts")
  fun handle(request: AccountCommandRequest): Uni<CommandSideEffect> {
    return controller.handle(request.metadata, request.command).toCompletionStage()
      .let {
        Uni.createFrom().completionStage(it)
      }
  }

}