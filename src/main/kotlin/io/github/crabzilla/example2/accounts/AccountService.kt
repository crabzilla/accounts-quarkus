package io.github.crabzilla.example2.accounts

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.crabzilla.command.CommandController
import io.github.crabzilla.command.CommandControllerOptions
import io.github.crabzilla.command.CommandSideEffect
import io.github.crabzilla.jackson.JacksonJsonObjectSerDer
import io.quarkus.vertx.ConsumeEvent
import io.smallrye.mutiny.Uni
import io.vertx.mutiny.core.Vertx
import io.vertx.mutiny.pgclient.PgPool
import javax.annotation.PostConstruct
import javax.enterprise.context.ApplicationScoped


@ApplicationScoped
class AccountService(private val vertx: Vertx,
                     private val pgPool: PgPool,
                     private val json: ObjectMapper,
                     private val projector: AccountOpenedProjector) {

  private lateinit var acctController: CommandController<Account, AccountCommand, AccountEvent>

  @PostConstruct
  fun init() {
    val acctSerDer = JacksonJsonObjectSerDer(json, accountComponent)
    val options = CommandControllerOptions(eventProjector = projector)
    acctController = CommandController(vertx.delegate, pgPool.delegate, accountComponent, acctSerDer, options)
  }

  @ConsumeEvent("command:accounts")
  fun handle(request: AccountCommandRequest): Uni<CommandSideEffect> {
    return acctController.handle(request.metadata, request.command).toCompletionStage()
      .let {
        Uni.createFrom().completionStage(it)
      }
  }

}