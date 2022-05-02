package io.github.crabzilla.example2.transfers

import io.github.crabzilla.command.CommandMetadata
import io.github.crabzilla.command.CommandSideEffect
import io.github.crabzilla.command.FeatureController
import io.github.crabzilla.core.FeatureComponent
import io.github.crabzilla.example2.accounts.Account
import io.github.crabzilla.example2.accounts.AccountCommand
import io.github.crabzilla.example2.accounts.AccountEvent
import io.vertx.core.Future
import io.vertx.core.Promise
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

class TransferService(
  private val acctController: FeatureController<Account, AccountCommand, AccountEvent>,
  private val transferController: FeatureController<Transfer, TransferCommand, TransferEvent>) {
  
  companion object {
    private val log: Logger = LoggerFactory.getLogger(TransferService::class.java)
  }

  data class PendingTransfer(
    val id: UUID, val amount: Double, val fromAccountId: UUID, val toAccountId: UUID,
    val causationId: UUID, val correlationId: UUID,
  )

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
    val correlationId = pendingTransfer.correlationId

    acctController.compose { conn ->
      log.info("Step 1 - Will withdrawn from account {}", pendingTransfer.fromAccountId)
      val withdrawnMetadata = CommandMetadata.new(
        stateId = pendingTransfer.fromAccountId,
        causationId = pendingTransfer.causationId,
        correlationId = correlationId
      )
      val withdrawnCommand = AccountCommand.WithdrawMoney(pendingTransfer.amount)
      acctController.handle(conn, withdrawnMetadata, withdrawnCommand)
        .compose { r1: CommandSideEffect ->
          log.info("Step 2 - Will deposit to account {}", pendingTransfer.toAccountId)
          val depositMetadata = CommandMetadata.new(
            stateId = pendingTransfer.toAccountId,
            causationId = r1.latestEventId(),
            correlationId = correlationId)
          val depositCommand = AccountCommand.DepositMoney(pendingTransfer.amount)
          acctController.handle(conn, depositMetadata, depositCommand)
        }.compose { r2: CommandSideEffect ->
          log.info("Step 3 - Will register a succeeded transfers")
          val registerSuccessMetadata = CommandMetadata.new(
            stateId = transferId,
            causationId = r2.latestEventId(),
            correlationId = correlationId)
          val registerSuccessCommand = TransferCommand.RegisterResult(true, null)
          transferController.handle(conn, registerSuccessMetadata, registerSuccessCommand)
            .map { r2 }
        }.onSuccess {
          promise.complete()
        }.onFailure { error ->
          // new transaction
          log.info("Step 3 - Will register a failed transfers", error)
          val registerFailureMetadata = CommandMetadata.new(
            stateId = transferId,
            causationId = correlationId,
            correlationId = correlationId)
          val registerFailureCommand = TransferCommand.RegisterResult(false, error.message)
          transferController.handle(registerFailureMetadata, registerFailureCommand)
            .onSuccess { promise.complete() }
        }
    }
    return promise.future()

  }
}