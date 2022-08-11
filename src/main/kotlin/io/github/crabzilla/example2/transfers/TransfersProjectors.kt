package io.github.crabzilla.example2.transfers

import io.github.crabzilla.stack.EventProjector
import io.github.crabzilla.stack.EventRecord
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import org.slf4j.LoggerFactory
import java.util.*
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class TransferProjector : EventProjector {

  companion object {
    private val log = LoggerFactory.getLogger(TransferProjector::class.java)
  }

  override fun project(conn: SqlConnection, record: EventRecord): Future<Void> {
    fun request(id: UUID, payload: JsonObject): Future<Void> {
      val tuple = Tuple.of(id,
        payload.getDouble("amount"),
        UUID.fromString(payload.getString("fromAccountId")),
        UUID.fromString(payload.getString("toAccountId"))
      )
      log.info("Will project new transfers {}", tuple.deepToString())
      return conn
        .preparedQuery("insert into " +
                "transfers_view (id, amount, from_acct_id, to_acct_id) " +
                "values ($1, $2, $3, $4)")
        .execute(tuple)
        .mapEmpty()
    }
    fun register(id: UUID, payload: JsonObject): Future<Void> {
      val tuple = Tuple.of(id,
        payload.getBoolean("succeeded"),
        payload.getString("errorMessage")
      )
      log.info("Will project transfers result {}", tuple.deepToString())
      return conn
        .preparedQuery("update transfers_view " +
                "set pending = false, succeeded = $2, error_message = $3" +
                "where id = $1")
        .execute(tuple)
        .mapEmpty()
    }

    val (payload, metadata, id) = record.extract()
    return when (val eventName = payload.getString("type")) {
      "TransferRequested" -> request(id, payload)
      "TransferConcluded" -> register(id, payload)
      else -> Future.failedFuture("Unknown event $eventName")
    }
  }

}