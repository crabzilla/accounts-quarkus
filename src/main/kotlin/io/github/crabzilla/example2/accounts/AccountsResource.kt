package io.github.crabzilla.example2.accounts

import io.github.crabzilla.command.CommandMetadata
import io.github.crabzilla.command.FeatureController
import io.github.crabzilla.example2.accounts.AccountCommand.DepositMoney
import io.github.crabzilla.example2.accounts.AccountCommand.OpenAccount
import io.github.crabzilla.example2.accounts.AccountCommand.WithdrawMoney
import io.github.crabzilla.example2.accounts.AccountsRequests.DepositMoneyRequest
import io.github.crabzilla.example2.accounts.AccountsRequests.OpenAccountRequest
import io.github.crabzilla.example2.accounts.AccountsRequests.WithdrawMoneyRequest
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.mutiny.pgclient.PgPool
import io.vertx.mutiny.sqlclient.Row
import org.jboss.resteasy.reactive.RestPath
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType.APPLICATION_JSON

@Path("/accounts")
internal class AccountsResource(private val pgPool: PgPool,
                                private val controller: FeatureController<Account, AccountCommand, AccountEvent>
) {

  companion object {
    private val log: Logger = LoggerFactory.getLogger(AccountsResource::class.java)
  }

  @GET
  @Path("/view1")
  @Produces(APPLICATION_JSON)
  fun view1(): Multi<JsonObject> {
    return pgPool.query("SELECT * from accounts_view order by name").execute()
      .onItem().transformToMulti { set -> Multi.createFrom().iterable(set) }
      .onItem().transform { row: Row -> row.toJson() }
  }

  @PUT
  @Path("/{stateId}")
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  fun open(@RestPath("stateId") stateId: UUID, request: OpenAccountRequest): Uni<JsonArray> {
    val command = OpenAccount(stateId, request.cpf, request.name)
    log.debug("command $command")
    val metadata = CommandMetadata.new(stateId)
    val future = controller.handle(metadata, command).map { it.toJsonArray() }
    return Uni.createFrom().completionStage(future.toCompletionStage())
  }


  @POST
  @Path("{stateId}/deposit")
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  fun deposit(@RestPath("stateId") stateId: UUID, request: DepositMoneyRequest): Uni<JsonArray> {
    val command = DepositMoney(request.amount)
    log.debug("command $stateId - $command")
    val metadata = CommandMetadata.new(stateId)
    val future = controller.handle(metadata, command).map { it.toJsonArray() }
    return Uni.createFrom().completionStage(future.toCompletionStage())
  }

  @POST
  @Path("{stateId}/withdraw")
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  fun withdraw(@RestPath("stateId") stateId: UUID, request: WithdrawMoneyRequest): Uni<JsonArray> {
    val command = WithdrawMoney(request.amount)
    log.debug("command $command")
    val metadata = CommandMetadata.new(stateId)
    val future = controller.handle(metadata, command).map { it.toJsonArray() }
    return Uni.createFrom().completionStage(future.toCompletionStage())
  }

}