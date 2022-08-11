package io.github.crabzilla.example2.accounts

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.crabzilla.stack.EventProjector
import io.github.crabzilla.stack.EventRecord
import io.vertx.core.Future
import io.vertx.core.Future.succeededFuture
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import java.util.*

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
      is AccountEvent.AccountOpened ->
        register(conn, id, event.cpf, event.name)
      else ->
        succeededFuture() // ignore event
    }
  }
}

class AccountsView1Projector : EventProjector {

  override fun project(conn: SqlConnection, record: EventRecord): Future<Void> {
    fun updateBalance(conn: SqlConnection, id: UUID, finalBalance: Double) : Future<Void> {
      return conn
        .preparedQuery("update accounts_view set balance = $2 where id = $1")
        .execute(Tuple.of(id, finalBalance))
        .mapEmpty()
    }
    val (payload, _, id) = record.extract()
    return when (payload.getString("type")) {
      "MoneyDeposited" ->
        updateBalance(conn, id, payload.getDouble("finalBalance"))
      "MoneyWithdrawn" ->
        updateBalance(conn, id, payload.getDouble("finalBalance"))
      else ->
        succeededFuture()
    }
  }

}