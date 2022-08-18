package io.github.crabzilla.example2.accounts

import io.github.crabzilla.stack.command.CommandServiceApi
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.vertx.UniHelper
import io.vertx.core.json.JsonObject
import org.jboss.resteasy.reactive.RestPath
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

data class OpenAccountRequest(val cpf: String, val name: String)
data class DepositMoneyRequest(val amount: Double)
data class WithdrawMoneyRequest(val amount: Double)

@Path("/accounts")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
private class AccountsCommandResource(private val serviceApi: CommandServiceApi<AccountCommand>) {

    @PUT
    @Path("/{stateId}")
    fun open(@RestPath("stateId") stateId: UUID, request: OpenAccountRequest): Uni<JsonObject> {
        val command = AccountCommand.OpenAccount(stateId, request.cpf, request.name)
        log.debug("command $command")
        return UniHelper.toUni(serviceApi.handle(stateId, command).map { event -> event.toJsonObject() })
    }

    @POST
    @Path("{stateId}/deposit")
    fun deposit(@RestPath("stateId") stateId: UUID, request: DepositMoneyRequest): Uni<JsonObject> {
        val command = AccountCommand.DepositMoney(request.amount)
        log.debug("command $stateId - $command")
        return UniHelper.toUni(serviceApi.handle(stateId, command).map { event -> event.toJsonObject() })
    }

    @POST
    @Path("{stateId}/withdraw")
    fun withdraw(@RestPath("stateId") stateId: UUID, request: WithdrawMoneyRequest): Uni<JsonObject> {
        val command = AccountCommand.WithdrawMoney(request.amount)
        log.debug("command $command")
        return UniHelper.toUni(serviceApi.handle(stateId, command).map { event -> event.toJsonObject() })
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(AccountsCommandResource::class.java)
    }
}
