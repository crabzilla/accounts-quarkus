package io.github.crabzilla.example2.accounts

import io.github.crabzilla.stack.EventProjector
import io.github.crabzilla.stack.EventRecord
import io.vertx.core.Future
import io.vertx.core.Future.succeededFuture
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import java.util.*

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