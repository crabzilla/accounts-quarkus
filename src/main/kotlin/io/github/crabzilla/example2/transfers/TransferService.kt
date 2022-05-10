package io.github.crabzilla.example2.transfers

import io.github.crabzilla.example2.accounts.Account
import io.github.crabzilla.example2.accounts.AccountCommand
import io.github.crabzilla.example2.accounts.AccountCommand.DepositMoney
import io.github.crabzilla.example2.accounts.AccountCommand.WithdrawMoney
import io.github.crabzilla.example2.accounts.AccountEvent
import io.github.crabzilla.example2.transfers.TransferCommand.RegisterResult
import io.github.crabzilla.stack.command.FeatureService
import io.vertx.core.Future
import io.vertx.core.Promise
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class TransferService(
  private val acctController: FeatureService<Account, AccountCommand, AccountEvent>,
  private val transferController: FeatureService<Transfer, TransferCommand, TransferEvent>,
) {

  companion object {
    private val log: Logger = LoggerFactory.getLogger(TransferService::class.java)

    data class PendingTransfer(
      val id: UUID, val amount: Double, val fromAccountId: UUID, val toAccountId: UUID
    )
  }

  /**
   * Steps within the same db transaction:
   * fromAcctId withdrawn
   * toAcctId deposit
   * transferId register success
   * in case of error, the failure will be registered into a new db tx
   */
  fun transfer(pendingTransfer: PendingTransfer): Future<Void> {

    val promise = Promise.promise<Void>()
    val transferId = pendingTransfer.id

    acctController.withinTransaction { conn ->
      log.info("Step 1 - Will withdrawn from account {}", pendingTransfer.fromAccountId)
      val withdrawnCommand = WithdrawMoney(pendingTransfer.amount)
      acctController.handle(conn, pendingTransfer.fromAccountId, withdrawnCommand)
        .compose {
          log.info("Step 2 - Will deposit to account {}", pendingTransfer.toAccountId)
          val depositCommand = DepositMoney(pendingTransfer.amount)
          acctController.handle(conn, pendingTransfer.toAccountId, depositCommand)
        }.compose {
          log.info("Step 3 - Will register a succeeded transfers")
          val registerSuccessCommand = RegisterResult(true, null)
          transferController.handle(conn, transferId, registerSuccessCommand)
        }
    }
    .onSuccess {
      log.info("Success !!!")
      promise.complete()
    }.onFailure { error ->
      // new transaction
      log.info("Step 3 - Will register a failed transfers", error)
      val registerFailureCommand = RegisterResult(false, error.message)
      transferController.handle(transferId, registerFailureCommand)
        .onSuccess { promise.complete() }
    }

    return promise.future()

  }
}