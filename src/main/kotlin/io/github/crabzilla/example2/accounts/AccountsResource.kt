package io.github.crabzilla.example2.accounts


import io.github.crabzilla.example2.accounts.AccountCommand.*
import io.github.crabzilla.stack.command.CommandServiceApi
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.vertx.UniHelper.toUni
import io.vertx.core.json.JsonObject
import io.vertx.mutiny.pgclient.PgPool
import io.vertx.mutiny.sqlclient.Row
import org.jboss.resteasy.reactive.RestPath
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.ws.rs.*
import javax.ws.rs.core.MediaType.APPLICATION_JSON


data class OpenAccountRequest(val cpf: String, val name: String)
data class DepositMoneyRequest(val amount: Double)
data class WithdrawMoneyRequest(val amount: Double)

@Path("/accounts")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
private class AccountsResource(private val pgPool: PgPool,
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
  fun open(@RestPath("stateId") stateId: String, request: OpenAccountRequest): Uni<String> {
    val command = OpenAccount(stateId, request.cpf, request.name)
    log.debug("command $command")
    return toUni(serviceApi.handle(stateId, command).map { event -> event.toJsonObject().encode() })
  }

  @POST
  @Path("{stateId}/deposit")
  fun deposit(@RestPath("stateId") stateId: String, request: DepositMoneyRequest): Uni<JsonObject> {
    val command = DepositMoney(request.amount)
    log.debug("command $stateId - $command")
    return toUni(serviceApi.handle(stateId, command).map { event -> event.toJsonObject() })
  }

  @POST
  @Path("{stateId}/withdraw")
  fun withdraw(@RestPath("stateId") stateId: String, request: WithdrawMoneyRequest): Uni<JsonObject> {
    val command = WithdrawMoney(request.amount)
    log.debug("command $command")
    return toUni(serviceApi.handle(stateId, command).map { event -> event.toJsonObject() })
  }

  companion object {
    private val log: Logger = LoggerFactory.getLogger(AccountsResource::class.java)
  }
}