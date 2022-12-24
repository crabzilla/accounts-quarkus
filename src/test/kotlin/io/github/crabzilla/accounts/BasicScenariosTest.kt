package io.github.crabzilla.accounts

import io.github.crabzilla.example2.accounts.DepositMoneyRequest
import io.github.crabzilla.example2.accounts.OpenAccountRequest
import io.github.crabzilla.example2.accounts.WithdrawMoneyRequest
import io.quarkus.test.junit.QuarkusTest
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.mutiny.pgclient.PgPool
import org.awaitility.Awaitility.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.hamcrest.core.IsEqual.equalTo
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import javax.inject.Inject

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Accounts feature")
class BasicScenariosTest {

  @Inject
  lateinit var pgPool: PgPool

  @BeforeAll
  fun cleanDatabaseBefore() {
    pgPool.query("truncate events, commands, accounts_view, transfers_view restart identity").execute()
      .flatMap { pgPool.query("update subscriptions set sequence = 0").execute() }
      .await().indefinitely()
  }

  @AfterAll
  fun cleanDatabaseAfter() {
    pgPool.query("truncate events, commands, accounts_view, transfers_view restart identity").execute()
      .flatMap { pgPool.query("update subscriptions set sequence = 0").execute() }
      .await().indefinitely()
  }

  @Test
  @Order(1)
  fun `there are no accounts`() {
    Given {
      contentType(ContentType.JSON)
    } When {
      get("/accounts/view1")
    } Then {
      statusCode(200)
      body("size()", equalTo(0))
    }
  }

  @Test
  @Order(2)
  fun `when opening an account`() {
    val response: JsonObject =
      Given {
        val request = OpenAccountRequest("cpf1", "acct1")
        body(request)
        contentType(ContentType.JSON)
      } When {
        put("/accounts/$id")
      } Then {
        statusCode(200)
      } Extract {
        JsonObject(body().asString())
      }
    assertEquals(1, response.getLong("version"))
  }

  @Test
  @Order(3)
  fun `then the account is opened`() {
    val response: JsonArray =
      Given {
         contentType(ContentType.JSON)
      } When {
        get("/accounts/view1")
      } Then {
        statusCode(200)
        body("size()", equalTo(1))
      } Extract {
        JsonArray(body().asString())
      }
    val record1 = response.getJsonObject(0)
    assertEquals(id, record1.getString("id"))
    assertEquals(0L, record1.getLong("balance"))
  }

  @Test
  @Order(4)
  fun `when depositing 1000`() {
    val response: JsonObject =
      Given {
        val request = DepositMoneyRequest(1000.0)
        body(request)
        contentType(ContentType.JSON)
      } When {
        post("/accounts/$id/deposit")
      } Then {
        statusCode(200)
      } Extract {
        JsonObject(body().asString())
      }
    assertEquals(2, response.getLong("version"))
  }

  @Test
  @Order(5)
  fun `and withdrawing 600`() {
    val response: JsonObject =
      Given {
        val request = WithdrawMoneyRequest(600.0)
        body(request)
        contentType(ContentType.JSON)
      } When {
        post("/accounts/$id/withdraw")
      } Then {
        statusCode(200)
      } Extract {
        JsonObject(body().asString())
      }
    assertEquals(3, response.getLong("version"))
  }

  @Test
  @Order(6)
  fun `then the balance is 400`() {
    await().untilCallTo {
        Given {
          contentType(ContentType.JSON)
        } When {
          get("/accounts/view1")
        } Then {
          statusCode(200)
          body("size()", equalTo(1))
        } Extract {
          JsonArray(body().asString())
            .getJsonObject(0)
        }
    } matches { json ->
      with(json!!) {
        getString("id") == id && getLong("balance")== 400L
      }
    }
  }

  companion object {
    var id: String = "acct#1"
  }
}