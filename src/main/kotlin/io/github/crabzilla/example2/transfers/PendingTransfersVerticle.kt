package io.github.crabzilla.example2.transfers


import io.github.crabzilla.example2.transfers.TransferService.Companion.PendingTransfer
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import org.slf4j.LoggerFactory
import java.lang.management.ManagementFactory

class PendingTransfersVerticle(private val pgPool: PgPool,
                               private val service: TransferService) : AbstractVerticle() {

  companion object {
    private val log = LoggerFactory.getLogger(PendingTransfersVerticle::class.java)
    private val node = ManagementFactory.getRuntimeMXBean().name
    private const val DEFAULT_INTERVAL = 30_000L
    const val HANDLE_ENDPOINT = "PendingTransfersVerticle.handle"
  }

  private var isBusy: Boolean = false

  override fun start() {

    // TODO use a subscriber to be greedy
//    val pgSubscriber = PgSubscriber.subscriber(vertx, PgConnectOptionsFactory.from(PgConfigFactory.toPgConfig(config)))

    log.info("Starting with interval (ms) = {}", config().getLong("transfers.processor.interval", DEFAULT_INTERVAL))

    vertx.eventBus().consumer<String>(HANDLE_ENDPOINT) { msg ->
      log.info("Received a request to pull and process")
      pullAndProcess(service)
        .onComplete {
          if (it.succeeded()) {
            msg.reply(node)
          } else {
            msg.fail(500, it.cause().message)
          }
        }
    }

    vertx.setPeriodic(config().getLong("transfers.processor.interval", DEFAULT_INTERVAL)) {
      pullAndProcess(service)
    }

  }

  private fun pullAndProcess(service: TransferService): Future<Void> {
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
            row.getUUID("to_acct_id"),
            row.getUUID("causation_id"),
            row.getUUID("correlation_id")
          )
        }.toList()
      }
  }

}