package io.github.crabzilla.example2.transfers


import io.github.crabzilla.example2.transfers.TransferService.Companion.PendingTransfer
import io.github.crabzilla.stack.CrabzillaContext
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.pgclient.PgPool
import io.vertx.pgclient.pubsub.PgSubscriber
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import org.slf4j.LoggerFactory
import java.lang.management.ManagementFactory

class PendingTransfersVerticle(private val pgPool: PgPool,
                               private val subscriber: PgSubscriber,
                               private val service: TransferService) : AbstractVerticle() {

  companion object {
    private val log = LoggerFactory.getLogger(PendingTransfersVerticle::class.java)
    private val node = ManagementFactory.getRuntimeMXBean().name
    private const val DEFAULT_INTERVAL = 30_000L
  }

  private var isBusy: Boolean = false

  override fun start() {

    log.info("{} starting with interval (ms) = {}", node,
      config().getLong("transfers.processor.interval", DEFAULT_INTERVAL))

    vertx.setPeriodic(config().getLong("transfers.processor.interval", DEFAULT_INTERVAL)) {
      pullAndProcess()
    }

    subscriber.connect()
      .onSuccess {
        subscriber.channel(CrabzillaContext.POSTGRES_NOTIFICATION_CHANNEL)
          .handler { stateType ->
            if (stateType.equals("Transfer")) pullAndProcess()
          }
      }.onFailure {
        log.info("Failed to connect on subscriber")
      }

  }

  private fun pullAndProcess(): Future<Void> {
    if (isBusy) {
      log.info("Still busy.. wil try next time")
      return Future.succeededFuture()
    }
    return getPendingTransfers()
      .compose { pendingList ->
        log.info("Found ${pendingList.size} pending transfers")
        val initialFuture = Future.succeededFuture<Void>()
        pendingList.fold(
          initialFuture
        ) { currentFuture: Future<Void>, pendingTransfer ->
          currentFuture.compose {
            service.transfer(pendingTransfer)
          }
        }
      }.onComplete {
        isBusy = false
      }
  }

  /**
   * Get 100 first pending transfers
   */
  private fun getPendingTransfers(): Future<List<PendingTransfer>> {
    return pgPool.preparedQuery("select * from transfers_view where pending = true LIMIT 100")
      .execute()
      .map { rs: RowSet<Row> ->
        rs.iterator().asSequence().map { row ->
          PendingTransfer(
            row.getUUID("id"),
            row.getDouble("amount"),
            row.getUUID("from_acct_id"),
            row.getUUID("to_acct_id")
          )
        }.toList()
      }
  }

}