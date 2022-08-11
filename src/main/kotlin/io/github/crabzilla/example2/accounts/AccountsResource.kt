package io.github.crabzilla.example2.accounts

import io.github.crabzilla.example2.accounts.AccountCommand.*
import io.github.crabzilla.example2.accounts.AccountsRequests.DepositMoneyRequest
import io.github.crabzilla.example2.accounts.AccountsRequests.OpenAccountRequest
import io.github.crabzilla.example2.accounts.AccountsRequests.WithdrawMoneyRequest
import io.github.crabzilla.stack.command.CommandServiceApi
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import io.vertx.core.json.JsonObject
import io.vertx.mutiny.pgclient.PgPool
import io.vertx.mutiny.sqlclient.Row
import org.jboss.resteasy.reactive.RestPath
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import javax.ws.rs.*
import javax.ws.rs.core.MediaType.APPLICATION_JSON

object AccountsRequests {
  data class OpenAccountRequest(val cpf: String, val name: String)
  data class DepositMoneyRequest(val amount: Double)
  data class WithdrawMoneyRequest(val amount: Double)
}

@Path("/accounts")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
internal class AccountsResource(private val pgPool: PgPool,
                                private val serviceApi: CommandServiceApi<AccountCommand>
) {

  @GET
  @Path("/view1")
  fun view1(): Multi<JsonObject> {
    return pgPool.query("SELECT * from accounts_view order by name").execute()
      .onItem().transformToMulti { set -> Multi.createFrom().iterable(set) }
      .onItem().transform { row: Row -> row.toJson() }
  }

  @PUT
  @Path("/{stateId}")
  fun open(@RestPath("stateId") stateId: UUID, request: OpenAccountRequest): Uni<JsonObject> {
    val command = OpenAccount(stateId, request.cpf, request.name)
    log.debug("command $command")
    val future = serviceApi.handle(stateId, command).map { event -> event.toJsonObject() }
    return Uni.createFrom().completionStage(future.toCompletionStage())
  }


  @POST
  @Path("{stateId}/deposit")
  fun deposit(@RestPath("stateId") stateId: UUID, request: DepositMoneyRequest): Uni<JsonObject> {
    val command = DepositMoney(request.amount)
    log.debug("command $stateId - $command")
    val future = serviceApi.handle(stateId, command).map { event -> event.toJsonObject() }
    return Uni.createFrom().completionStage(future.toCompletionStage())
  }

  @POST
  @Path("{stateId}/withdraw")
  fun withdraw(@RestPath("stateId") stateId: UUID, request: WithdrawMoneyRequest): Uni<JsonObject> {
    val command = WithdrawMoney(request.amount)
    log.debug("command $command")
    val future = serviceApi.handle(stateId, command).map { event -> event.toJsonObject() }
    return Uni.createFrom().completionStage(future.toCompletionStage())
  }

  companion object {
    private val log: Logger = LoggerFactory.getLogger(AccountsResource::class.java)
  }
}