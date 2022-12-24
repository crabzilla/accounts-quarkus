package io.github.crabzilla.example2.transfers

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.github.crabzilla.core.CommandHandler
import io.github.crabzilla.core.CommandSession
import io.github.crabzilla.core.EventHandler
import io.github.crabzilla.example2.transfers.Transfer.Requested
import io.github.crabzilla.example2.transfers.TransferCommand.RegisterResult
import io.github.crabzilla.example2.transfers.TransferCommand.RequestTransfer
import io.github.crabzilla.example2.transfers.TransferEvent.*
import io.github.crabzilla.stack.command.CommandServiceConfig

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = true)
@JsonSubTypes(
  JsonSubTypes.Type(TransferRequested::class, name = "TransferRequested"),
  JsonSubTypes.Type(TransferSucceeded::class, name = "TransferSucceeded"),
  JsonSubTypes.Type(TransferFailed::class, name = "TransferFailed")
)
sealed class TransferEvent {
  data class TransferRequested(val id: String,
                               val amount: Double = 0.00,
                               val fromAccountId: String,
                               val toAccountId: String) : TransferEvent()
  data class TransferSucceeded(val id: String) : TransferEvent()
  data class TransferFailed(val id: String, val errorMessage: String) : TransferEvent()
}

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = true)
@JsonSubTypes(
  JsonSubTypes.Type(RequestTransfer::class, name = "RequestTransfer"),
  JsonSubTypes.Type(RegisterResult::class, name = "RegisterResult")
)
sealed class TransferCommand {
  data class RequestTransfer(val id: String,
                             val amount: Double = 0.00,
                             val fromAccountId: String,
                             val toAccountId: String) : TransferCommand()
  data class RegisterResult(val succeeded: Boolean, val errorMessage: String?) : TransferCommand()
}


class TransferAlreadyExists(id: String) : IllegalArgumentException("Transfer $id already exists")

sealed class Transfer {
  object Initial: Transfer() {
    override fun toString(): String {
      return "Transfer.Initial"
    }
    fun request(id: String, amount: Double, fromAccountId: String, toAccountId: String): List<TransferRequested> {
      return listOf(TransferRequested(id, amount, fromAccountId, toAccountId))
    }
  }
  data class Requested(val id: String,
                  val amount: Double,
                  val fromAccountId: String,
                  val toAccountId: String): Transfer() {
    fun setSuccess(): List<TransferEvent> {
      return listOf(TransferSucceeded(this.id))
    }
    fun setFailed(reason: String): List<TransferEvent> {
      return listOf(TransferFailed(this.id, reason))
    }
  }
  data class Succeeded(val requested: Requested): Transfer()
  data class Failed(val requested: Requested, val reason: String): Transfer()
}

val transferEventHandler = EventHandler<Transfer, TransferEvent> { state, event ->
  when (state) {
    is Transfer.Initial ->
      when (event) {
        is TransferRequested -> Requested(event.id, event.amount, event.fromAccountId, event.toAccountId)
        else -> state
      }
    is Requested ->
      when (event) {
        is TransferSucceeded -> Transfer.Succeeded(state)
        is TransferFailed -> Transfer.Failed(state, event.errorMessage)
        else -> state
      }
    else -> state
  }
}

class TransferCommandHandler : CommandHandler<Transfer, TransferCommand, TransferEvent>(transferEventHandler) {

  override fun handle(command: TransferCommand, state: Transfer): CommandSession<Transfer, TransferEvent> {
    return when (command) {
      is RequestTransfer -> {
        when (state) {
          is Transfer.Initial -> {
             with(state).execute {
               state.request(command.id, command.amount, command.fromAccountId, command.toAccountId)
             }
          }
          else -> throw TransferAlreadyExists(command.id)
        }
      }
      is RegisterResult -> {
        when (state) {
          is Requested -> {
            if (command.succeeded) {
              with(state).execute { state.setSuccess() }
            } else {
              with(state).execute { state.setFailed(command.errorMessage!!) }
            }
          }
          else -> throw buildException(state, command)
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
  TransferCommandHandler(),
  Transfer.Initial
)
