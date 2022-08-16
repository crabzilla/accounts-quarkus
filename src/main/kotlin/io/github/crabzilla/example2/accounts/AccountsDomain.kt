package io.github.crabzilla.example2.accounts

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.github.crabzilla.core.*
import io.github.crabzilla.example2.accounts.AccountCommand.*
import io.github.crabzilla.example2.accounts.AccountEvent.*
import io.github.crabzilla.stack.command.CommandServiceConfig
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = true)
@JsonSubTypes(
  JsonSubTypes.Type(AccountOpened::class, name = "AccountOpened"),
  JsonSubTypes.Type(MoneyDeposited::class, name = "MoneyDeposited"),
  JsonSubTypes.Type(MoneyWithdrawn::class, name = "MoneyWithdrawn")
)
sealed class AccountEvent {
  data class AccountOpened(val id: UUID, val cpf: String, val name: String) : AccountEvent()
  data class MoneyDeposited(val amount: Double, val finalBalance: Double) : AccountEvent()
  data class MoneyWithdrawn(val amount: Double, val finalBalance: Double) : AccountEvent()
}

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = true)
@JsonSubTypes(
  JsonSubTypes.Type(OpenAccount::class, name = "OpenAccount"),
  JsonSubTypes.Type(DepositMoney::class, name = "DepositMoney"),
  JsonSubTypes.Type(WithdrawMoney::class, name = "WithdrawMoney")
)
sealed class AccountCommand {
  data class OpenAccount(val id: UUID, val cpf: String, val name: String) : AccountCommand()
  data class DepositMoney(val amount: Double) : AccountCommand()
  data class WithdrawMoney(val amount: Double) : AccountCommand()
}

data class Account(val id: UUID, val cpf: String, val name: String, val balance: Double = 0.00) {
  companion object {
    fun fromEvent(event: AccountOpened): Account {
      return Account(id = event.id, cpf = event.cpf, name = event.name)
    }
  }
}

val accountEventHandler = EventHandler<Account, AccountEvent> { state, event ->
  when (event) {
    is AccountOpened -> Account.fromEvent(event)
    is MoneyDeposited -> state!!.copy(balance = state.balance + event.amount)
    is MoneyWithdrawn -> state!!.copy(balance = state.balance - event.amount)
  }
}

class AccountAlreadyExists(id: UUID) : IllegalArgumentException("Account $id already exists")
class AccountNotFound : NullPointerException("Account not found")
class AccountBalanceNotEnough(id: UUID) : IllegalStateException("Account $id doesn't have enough balance")
class DepositExceeded(amount: Double) : IllegalStateException("Cannot deposit more than $amount")

class AccountCommandHandler : CommandHandler<Account, AccountCommand, AccountEvent>(accountEventHandler) {
  companion object {
    private const val LIMIT = 2000.00
    private fun Account.deposit(amount: Double): List<AccountEvent> {
      if (amount > LIMIT) {
        throw DepositExceeded(LIMIT)
      }
      return listOf(MoneyDeposited(amount, balance + amount))
    }
    private fun Account.withdraw(amount: Double): List<AccountEvent> {
      if (balance < amount) throw AccountBalanceNotEnough(id)
      return listOf(MoneyWithdrawn(amount, balance - amount))
    }
  }

  override fun handle(command: AccountCommand, state: Account?): CommandSession<Account, AccountEvent> {
    fun nonNullState() = state ?: throw AccountNotFound()
    with(command) {
      return when (this) {
        is OpenAccount -> {
          if (state != null) throw AccountAlreadyExists(id)
          withNew(listOf(AccountOpened(id = id, cpf, name)))
        }
        is DepositMoney -> with(nonNullState()).execute { it.deposit(amount) }
        is WithdrawMoney -> with(nonNullState()).execute { it.withdraw(amount) }
      }
    }
  }
}

val accountComponent = CommandServiceConfig(
  Account::class,
  AccountCommand::class,
  AccountEvent::class,
  accountEventHandler,
  AccountCommandHandler()
)