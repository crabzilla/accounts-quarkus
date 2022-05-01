package io.github.crabzilla.example2.accounts

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.crabzilla.EventProjector
import io.github.crabzilla.EventRecord
import io.github.crabzilla.example2.accounts.AccountEvent.AccountOpened
import io.vertx.core.Future
import io.vertx.core.Future.succeededFuture
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import java.util.UUID
import javax.enterprise.context.ApplicationScoped

// TODO propagate causation and correlation ids
@ApplicationScoped
class AccountOpenedProjector(private val json: ObjectMapper) : EventProjector {

  override fun project(conn: SqlConnection, record: EventRecord): Future<Void> {
    fun register(conn: SqlConnection, id: UUID, cpf: String, name: String): Future<Void> {
      return conn
        .preparedQuery("insert into accounts_view (id, cpf, name) values ($1, $2, $3) returning id")
        .execute(Tuple.of(id, cpf, name))
        .mapEmpty()
    }
    val (payload, _, id) = record.extract()
    return when (val event = json.readValue(payload.toString(), AccountEvent::class.java)) {
      is AccountOpened ->
        register(conn, id, event.cpf, event.name)
      else ->
        succeededFuture() // ignore event
    }
  }
}

