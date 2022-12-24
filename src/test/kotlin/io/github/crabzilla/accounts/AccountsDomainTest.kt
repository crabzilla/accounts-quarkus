package io.github.crabzilla.accounts

import io.github.crabzilla.core.CommandTestSpecification
import io.github.crabzilla.example2.accounts.*
import io.github.crabzilla.example2.accounts.AccountCommand.*
import io.github.crabzilla.example2.accounts.AccountEvent.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.DisplayName
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.quarkus.test.junit.QuarkusTest

@QuarkusTest
@DisplayName("Accounts domain")
class AccountsDomainTest : AnnotationSpec() {

  companion object {
    private const val id: String = "acct#1"
    private fun spec() = CommandTestSpecification(Account.Initial, AccountCommandHandler(), accountEventHandler)
  }

  @Test
  fun `when opening an account`() {
    spec()
      .whenCommand(OpenAccount(id, "cpf1", "person1"))
      .then { it.state shouldBe Account.Open(id, "cpf1", "person1") }
      .then { it.events shouldBe listOf(AccountOpened(id, "cpf1", "person1")) }
  }

  @Test
  fun `when depositing $2000`() {
    spec()
      .givenEvents(AccountOpened(id, "cpf1", "person1"))
      .whenCommand(DepositMoney(2000.00))
      .then { it.state shouldBe Account.Open(id, "cpf1", "person1", 2000.00) }
      .then {
        it.events shouldBe listOf(
          AccountOpened(id, "cpf1", "person1"),
          MoneyDeposited(2000.00, 2000.00)
        )
      }
  }

  @Test
  fun `when depositing $2500`() {
    spec()
      .givenEvents(AccountOpened(id, "cpf1", "person1"))
      .then { it.state shouldBe Account.Open(id, "cpf1", "person1", 0.00) }
      .then {
        val exception = shouldThrow<DepositExceeded> {
          it.whenCommand(DepositMoney(2500.00))
        }
        exception.message shouldBe "Cannot deposit more than 2000.0"
      }
  }

  @Test
  fun `when withdrawing 100 from an account with balance = 110`() {
    spec()
      .givenEvents(AccountOpened(id, "cpf1", "person1"))
      .whenCommand(DepositMoney(110.00))
      .whenCommand(WithdrawMoney(100.00))
      .then { it.state shouldBe Account.Open(id, "cpf1", "person1", 10.00) }
      .then {
        it.events shouldBe listOf(
          AccountOpened(id, "cpf1", "person1"),
          MoneyDeposited(110.00, 110.00),
          MoneyWithdrawn(100.00, 10.00)
        )
      }
  }

  @Test
  fun `when withdrawing 100 from an account with balance = 50`() {
    spec()
      .givenEvents(AccountOpened(id, "cpf1", "person1"))
      .then { it.state shouldBe Account.Open(id, "cpf1", "person1", 0.00) }
      .then {
        val exception = shouldThrow<AccountBalanceNotEnough> {
          it.whenCommand(WithdrawMoney(2500.00))
        }
        exception.message shouldBe "Account $id doesn't have enough balance"
      }
  }

  @Test
  fun `when an account already exists`() {
    spec()
      .givenEvents(AccountOpened(id, "cpf1", "person1"))
      .then {
        val exception = shouldThrow<AccountAlreadyExists> {
          it.whenCommand(OpenAccount(id, "cpf1", "person1"))
        }
        exception.message shouldBe "Account $id already exists"
      }
  }

  @Test
  fun `when an account was not found`() {
    spec()
      .then {
        val exception = shouldThrow<IllegalStateException> {
          it.whenCommand(WithdrawMoney(2500.00))
        }
         exception.message shouldBe "Illegal transition. state: Initial command: WithdrawMoney"
      }
  }
}
