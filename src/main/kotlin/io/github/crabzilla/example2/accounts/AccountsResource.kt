package io.github.crabzilla.example2.accounts

import io.github.crabzilla.command.CommandMetadata
import io.github.crabzilla.example2.accounts.AccountCommand.DepositMoney
import io.github.crabzilla.example2.accounts.AccountCommand.OpenAccount
import io.github.crabzilla.example2.accounts.AccountCommand.WithdrawMoney
import io.github.crabzilla.example2.accounts.AccountsRequests.DepositMoneyRequest
import io.github.crabzilla.example2.accounts.AccountsRequests.OpenAccountRequest
import io.github.crabzilla.example2.accounts.AccountsRequests.WithdrawMoneyRequest
import io.github.crabzilla.example2.accounts.AccountsCommandService.Companion.AccountCommandRequest
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.mutiny.core.eventbus.EventBus
import io.vertx.mutiny.pgclient.PgPool
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
internal class AccountsResource(private val bus: EventBus, private val pgPool: PgPool) {

  companion object {
    private val log: Logger = LoggerFactory.getLogger(AccountsResource::class.java)
  }

  @GET
  @Path("/view1")
  @Produces(APPLICATION_JSON)
  fun view1(): Multi<JsonObject> {
    return pgPool.query("SELECT * from accounts_view").execute()
      .onItem().transformToMulti { set -> Multi.createFrom().iterable(set) }
      .onItem().transform { row: io.vertx.mutiny.sqlclient.Row ->
        row.toJson()
      }
  }

  @PUT
  @Path("/{stateId}")
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  fun open(request: OpenAccountRequest, @RestPath("stateId") stateId: UUID): Uni<JsonArray> {
    val command = OpenAccount(stateId, request.cpf, request.name)
    log.info("command $command")
    val metadata = CommandMetadata.new(stateId)
    val commandRequest = AccountCommandRequest(metadata, command)
    return bus.request<JsonArray>("command:accounts", commandRequest)
      .onItem()
      .transform {
        log.info(it.body().encodePrettily())
        it.body()
      }
  }


  @POST
  @Path("{stateId}/deposit")
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  fun deposit(request: DepositMoneyRequest, @RestPath("stateId") stateId: UUID): Uni<JsonArray> {
    val command = DepositMoney(request.amount)
    log.info("command $command")
    val metadata = CommandMetadata.new(stateId)
    val commandRequest = AccountCommandRequest(metadata, command)
    return bus.request<JsonArray>("command:accounts", commandRequest)
      .onItem()
      .transform {
        log.info(it.body().encodePrettily())
        it.body()
      }
  }

  @POST
  @Path("{stateId}/withdraw")
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  fun withdraw(request: WithdrawMoneyRequest, @RestPath("stateId") stateId: UUID): Uni<JsonArray> {
    val command = WithdrawMoney(request.amount)
    log.info("command $command")
    val metadata = CommandMetadata.new(stateId)
    val commandRequest = AccountCommandRequest(metadata, command)
    return bus.request<JsonArray>("command:accounts", commandRequest)
      .onItem()
      .transform {
        log.info(it.body().encodePrettily())
        it.body()
      }
  }

}