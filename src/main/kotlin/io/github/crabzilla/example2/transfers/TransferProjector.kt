package io.github.crabzilla.example2.transfers

import io.github.crabzilla.EventProjector
import io.github.crabzilla.EventRecord
import io.vertx.core.Future
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import org.slf4j.LoggerFactory
import java.util.UUID

class TransferProjector : EventProjector {

  companion object {
    private val log = LoggerFactory.getLogger(TransferProjector::class.java)
  }

  // TODO propagate causation and correlation ids
  override fun project(conn: SqlConnection, record: EventRecord): Future<Void> {
    fun request(id: UUID): Future<Void> {
      val (payload, metadata, id) = record.extract()
      val tuple = Tuple.of(id,
        payload.getDouble("amount"),
        UUID.fromString(payload.getString("fromAccountId")),
        UUID.fromString(payload.getString("toAccountId")),
        metadata.eventId,
        metadata.correlationId
      )
      log.info("Will project new transfers {}", tuple.deepToString())
      return conn
        .preparedQuery("insert into " +
                "transfers_view (id, amount, from_acct_id, to_acct_id, causation_id, correlation_id) " +
                "values ($1, $2, $3, $4, $5, $6)")
        .execute(tuple)
        .mapEmpty()
    }
    fun register(id: UUID): Future<Void> {
      val (payload, metadata, id) = record.extract()
      val tuple = Tuple.of(id,
        payload.getBoolean("succeeded"),
        payload.getString("errorMessage"),
        metadata.eventId,
        metadata.correlationId
      )
      log.info("Will project transfers result {}", tuple.deepToString())
      return conn
        .preparedQuery("update transfers_view " +
                "set pending = false, succeeded = $2, error_message = $3, causation_id = $4, correlation_id = $5 " +
                "where id = $1")
        .execute(tuple)
        .mapEmpty()
    }

    val (payload, _, id) = record.extract()
    return when (val eventName = payload.getString("type")) {
      "TransferRequested" -> request(id)
      "TransferConcluded" -> register(id)
      else -> Future.failedFuture("Unknown event $eventName")
    }
  }

}