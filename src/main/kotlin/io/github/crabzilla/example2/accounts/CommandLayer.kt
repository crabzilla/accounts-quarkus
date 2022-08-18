package io.github.crabzilla.example2.accounts


import com.fasterxml.jackson.databind.ObjectMapper
import io.github.crabzilla.jackson.JacksonJsonObjectSerDer
import io.github.crabzilla.stack.EventProjector
import io.github.crabzilla.stack.EventRecord
import io.github.crabzilla.stack.command.CommandServiceApi
import io.github.crabzilla.stack.command.CommandServiceApiFactory
import io.github.crabzilla.stack.command.CommandServiceOptions
import io.vertx.core.Future
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import java.util.*
import javax.enterprise.context.ApplicationScoped

private class CommandLayer {

  @ApplicationScoped
  fun create(factory: CommandServiceApiFactory, json: ObjectMapper): CommandServiceApi<AccountCommand> {
    val jsonSerDer = JacksonJsonObjectSerDer(json, accountComponent)
    val options = CommandServiceOptions(eventProjector = AccountOpenedProjector(json))
    return factory.commandService(accountComponent, jsonSerDer, options)
  }

  private class AccountOpenedProjector(private val json: ObjectMapper) : EventProjector {
    override fun project(conn: SqlConnection, eventRecord: EventRecord): Future<Void> {
      fun register(conn: SqlConnection, id: UUID, cpf: String, name: String): Future<Void> {
        return conn
          .preparedQuery("insert into accounts_view (id, cpf, name) values ($1, $2, $3) returning id")
          .execute(Tuple.of(id, cpf, name))
          .mapEmpty()
      }
      val (payload, _, id) = eventRecord.extract()
      return when (val event = json.readValue(payload.toString(), AccountEvent::class.java)) {
        is AccountEvent.AccountOpened ->
          register(conn, id, event.cpf, event.name)
        else ->
          Future.succeededFuture() // ignore event
      }
    }
  }

}