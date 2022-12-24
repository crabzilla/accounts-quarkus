package io.github.crabzilla.example2.accounts

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.github.crabzilla.core.CommandHandler
import io.github.crabzilla.core.CommandSession
import io.github.crabzilla.core.EventHandler
import io.github.crabzilla.example2.accounts.AccountCommand.*
import io.github.crabzilla.example2.accounts.AccountEvent.*
import io.github.crabzilla.stack.command.CommandServiceConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = true)
@JsonSubTypes(
    JsonSubTypes.Type(AccountOpened::class, name = "AccountOpened"),
    JsonSubTypes.Type(MoneyDeposited::class, name = "MoneyDeposited"),
    JsonSubTypes.Type(MoneyWithdrawn::class, name = "MoneyWithdrawn")
)
sealed class AccountEvent {
    data class AccountOpened(val id: String, val cpf: String, val name: String) : AccountEvent()
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
    data class OpenAccount(val id: String, val cpf: String, val name: String) : AccountCommand()
    data class DepositMoney(val amount: Double) : AccountCommand()
    data class WithdrawMoney(val amount: Double) : AccountCommand()
}

class AccountAlreadyExists(id: String) : IllegalArgumentException("Account $id already exists")
class AccountBalanceNotEnough(id: String) : IllegalStateException("Account $id doesn't have enough balance")
class DepositExceeded(amount: Double) : IllegalStateException("Cannot deposit more than $amount")

sealed class Account {
    object Initial : Account() {
        override fun toString(): String {
            return "Account.Initial"
        }
        fun open(id: String, cpf: String, name: String): List<AccountOpened> {
            return listOf(AccountOpened(id, cpf, name))
        }
    }
    data class Open(val id: String, val cpf: String, val name: String, val balance: Double = 0.00) : Account() {
        fun deposit(amount: Double): List<AccountEvent> {
            if (amount > LIMIT) {
                throw DepositExceeded(LIMIT)
            }
            return listOf(MoneyDeposited(amount, balance + amount))
        }
        fun withdraw(amount: Double): List<AccountEvent> {
            if (balance < amount) throw AccountBalanceNotEnough(id)
            return listOf(MoneyWithdrawn(amount, balance - amount))
        }
    }
    companion object {
        private const val LIMIT = 2000.00
    }
}

val accountEventHandler = EventHandler<Account, AccountEvent> { state, event ->
    logger.debug("State: $state Event: $event")
    when (state) {
        is Account.Initial -> {
            when (event) {
                is AccountOpened -> Account.Open(event.id, event.cpf, event.name)
                else -> state
            }
        }
        is Account.Open -> {
            when (event) {
                is MoneyDeposited -> state.copy(balance = state.balance + event.amount)
                is MoneyWithdrawn -> state.copy(balance = state.balance - event.amount)
                else -> state
            }
        }
    }
}

class AccountCommandHandler : CommandHandler<Account, AccountCommand, AccountEvent>(accountEventHandler) {
    override fun handle(command: AccountCommand, state: Account): CommandSession<Account, AccountEvent> {
        logger.debug("State: $state Command: $command")
        return when (state) {
            is Account.Initial -> {
                when (command) {
                    is OpenAccount -> with(state).execute { state.open(command.id, command.cpf, command.name) }
                    else -> throw buildException(state, command)
                }
            }
            is Account.Open -> {
                when (command) {
                    is DepositMoney -> with(state).execute { state.deposit(command.amount) }
                    is WithdrawMoney -> with(state).execute { state.withdraw(command.amount) }
                    else -> throw AccountAlreadyExists(state.id)
                }
            }
        }
    }
}

val logger: Logger = LoggerFactory.getLogger("AccountsDomain")

val accountComponent = CommandServiceConfig(
    Account::class,
    AccountCommand::class,
    AccountEvent::class,
    accountEventHandler,
    AccountCommandHandler(),
    Account.Initial
)
