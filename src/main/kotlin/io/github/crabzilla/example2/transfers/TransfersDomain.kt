package io.github.crabzilla.example2.transfers

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.github.crabzilla.core.CommandHandler
import io.github.crabzilla.core.CommandSession
import io.github.crabzilla.core.EventHandler
import io.github.crabzilla.example2.transfers.TransferCommand.RegisterResult
import io.github.crabzilla.example2.transfers.TransferCommand.RequestTransfer
import io.github.crabzilla.example2.transfers.TransferEvent.TransferConcluded
import io.github.crabzilla.example2.transfers.TransferEvent.TransferRequested
import io.github.crabzilla.stack.command.CommandServiceConfig
import java.util.*


@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = true)
@JsonSubTypes(
  JsonSubTypes.Type(TransferRequested::class, name = "TransferRequested"),
  JsonSubTypes.Type(TransferConcluded::class, name = "TransferConcluded")
)
sealed class TransferEvent {
  data class TransferRequested(val id: UUID,
                               val amount: Double = 0.00,
                               val fromAccountId: UUID,
                               val toAccountId: UUID) : TransferEvent()
  data class TransferConcluded(val succeeded: Boolean, val errorMessage: String?) : TransferEvent()

}

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = true)
@JsonSubTypes(
  JsonSubTypes.Type(RequestTransfer::class, name = "RequestTransfer"),
  JsonSubTypes.Type(RegisterResult::class, name = "RegisterResult")
)
sealed class TransferCommand {
  data class RequestTransfer(val id: UUID,
                             val amount: Double = 0.00,
                             val fromAccountId: UUID,
                             val toAccountId: UUID) : TransferCommand()
  data class RegisterResult(val succeeded: Boolean, val errorMessage: String?) : TransferCommand()
}

data class Transfer(
  val id: UUID,
  val amount: Double = 0.00,
  val fromAccountId: UUID,
  val toAccountId: UUID,
  val succeeded: Boolean?,
  val errorMessage: String?
) {
  companion object {
    fun fromEvent(event: TransferRequested): Transfer {
      return Transfer(id = event.id, amount = event.amount, fromAccountId =  event.fromAccountId,
        toAccountId = event.toAccountId, succeeded = null, errorMessage = null)
    }
  }
}

val transferEventHandler = EventHandler<Transfer, TransferEvent> { state, event ->
  when (event) {
    is TransferRequested -> Transfer.fromEvent(event)
    is TransferConcluded -> state!!.copy(succeeded = event.succeeded, errorMessage = event.errorMessage)
  }
}

class TransferAlreadyExists(id: UUID) : IllegalArgumentException("Transfer $id already exists")
class TransferNotFound : NullPointerException("Transfer not found")

class TransferCommandHandler : CommandHandler<Transfer, TransferCommand, TransferEvent>(transferEventHandler) {
  override fun handle(command: TransferCommand, state: Transfer?): CommandSession<Transfer, TransferEvent> {
    return when (command) {
      is RequestTransfer -> {
        with (command) {
          if (state != null) throw TransferAlreadyExists(id)
          withNew(listOf(TransferRequested(id, amount, fromAccountId, toAccountId)))
        }
      }
      is RegisterResult -> {
        if (state == null) throw TransferNotFound()
        with(state).execute {
          listOf(TransferConcluded(command.succeeded, command.errorMessage))
        }
      }
    }
  }
}

val transferComponent = CommandServiceConfig(
  Transfer::class,
  TransferCommand::class,
  TransferEvent::class,
  transferEventHandler,
  TransferCommandHandler()
)
