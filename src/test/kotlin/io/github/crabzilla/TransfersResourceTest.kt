package io.github.crabzilla

import io.github.crabzilla.example2.accounts.AccountsFactory
import io.github.crabzilla.example2.accounts.AccountsRequests.DepositMoneyRequest
import io.github.crabzilla.example2.accounts.AccountsRequests.OpenAccountRequest
import io.github.crabzilla.example2.transfers.PendingTransfersVerticle.Companion.HANDLE_ENDPOINT
import io.github.crabzilla.example2.transfers.TransfersRequests.RequestTransferRequest
import io.github.crabzilla.stack.subscription.SubscriptionApi
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.smallrye.mutiny.Uni
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.mutiny.core.Vertx
import io.vertx.mutiny.pgclient.PgPool
import org.hamcrest.CoreMatchers
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import java.util.UUID
import javax.inject.Inject

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class TransfersResourceTest {

  @Inject
  lateinit var vertx: Vertx

  @Inject
  lateinit var pgPool: PgPool

  @Test
  @Order(1)
  fun `There no transfers`() {
    pgPool.query("delete from events").execute()
      .flatMap { pgPool.query("delete from commands").execute() }
      .flatMap { pgPool.query("delete from accounts_view").execute() }
      .flatMap { pgPool.query("delete from transfers_view").execute() }
      .await().indefinitely()
    RestAssured.given()
      .contentType(ContentType.JSON)
      .`when`()["/transfers/view1"]
      .then()
      .statusCode(200)
      .body("size()", CoreMatchers.`is`(0))
  }

  @Test
  @Order(2)
  fun `creating Account 1`() {
    val request = OpenAccountRequest(UUID.randomUUID().toString(), "acct1")
    val response = RestAssured.given()
      .contentType(ContentType.JSON)
      .body(request)
      .`when`().put("/accounts/$account1Id")
      .then()
      .statusCode(200)
      .extract().response()
    val resp = JsonArray(response.body().prettyPrint())
    assertEquals(1, resp.size())
    val record1 = JsonObject(resp.list[0] as Map<String?, Any?>?)
    assertEquals(1L, record1.getLong("version"))
    assertEquals("AccountOpened", record1.getJsonObject("eventPayload").getString("type"))
  }

  @Test
  @Order(3)
  fun `creating Account 2`() {
    val request = OpenAccountRequest(UUID.randomUUID().toString(), "acct2")
    val response = RestAssured.given()
      .contentType(ContentType.JSON)
      .body(request)
      .`when`().put("/accounts/$account2Id")
      .then()
      .statusCode(200)
      .extract().response()
    val resp = JsonArray(response.body().prettyPrint())
    assertEquals(1, resp.size())
    val record1 = JsonObject(resp.list[0] as Map<String?, Any?>?)
    assertEquals(1L, record1.getLong("version"))
    assertEquals("AccountOpened", record1.getJsonObject("eventPayload").getString("type"))
  }

  @Test
  @Order(4)
  fun `depositing 1000 on account 1`() {
    val request = DepositMoneyRequest(1000.0)
    val response = RestAssured.given()
      .contentType(ContentType.JSON)
      .body(request)
      .`when`().post("/accounts/$account1Id/deposit")
      .then()
      .statusCode(200)
      .extract().response()
    val resp = JsonArray(response.body().prettyPrint())
    assertEquals(1, resp.size())
    val record1 = JsonObject(resp.list[0] as Map<String?, Any?>?)
    assertEquals(2L, record1.getLong("version"))
    assertEquals("MoneyDeposited", record1.getJsonObject("eventPayload").getString("type"))
  }

  @Test
  @Order(5)
  fun `requesting transfer 600 from account 1 to account 2`() {
    val request = RequestTransferRequest(600.0, account1Id, account2Id)
    val response = RestAssured.given()
      .contentType(ContentType.JSON)
      .body(request)
      .`when`().put("/transfers/$transferId")
      .then()
      .statusCode(200)
      .extract().response()
    val resp = JsonArray(response.body().prettyPrint())
    assertEquals(1, resp.size())
    val record1 = JsonObject(resp.list[0] as Map<String?, Any?>?)
    assertEquals(1L, record1.getLong("version"))
    assertEquals("TransferRequested", record1.getJsonObject("eventPayload").getString("type"))

    val subscriptionApi = SubscriptionApi(vertx.eventBus().delegate, AccountsFactory.projectionName)
    Uni.createFrom().completionStage(subscriptionApi.handle().toCompletionStage())
      .await().indefinitely()

  }

  @Test
  @Order(6)
  fun `checking accounts and transfers view`() {

    val subscriptionApi = SubscriptionApi(vertx.eventBus().delegate, AccountsFactory.projectionName)
      vertx.eventBus().request<Nothing>(HANDLE_ENDPOINT,null)
        .flatMap { Uni.createFrom().completionStage(subscriptionApi.handle().toCompletionStage()) }
        .await().indefinitely()

    val response = RestAssured.given()
      .contentType(ContentType.JSON)
      .`when`()["/accounts/view1"]
      .then()
      .statusCode(200)
      .extract().response()

    val resp = JsonArray(response.body().prettyPrint())
    assertEquals(2, resp.size())

    val record1 = JsonObject(resp.list[0] as Map<String?, Any?>?)
    assertEquals(account1Id, UUID.fromString(record1.getString("id")))
    assertEquals(400L, record1.getLong("balance"))

    val record2 = JsonObject(resp.list[1] as Map<String?, Any?>?)
    assertEquals(account2Id, UUID.fromString(record2.getString("id")))
    assertEquals(600L, record2.getLong("balance"))

    val response2 = RestAssured.given()
      .contentType(ContentType.JSON)
      .`when`()["/transfers/view1"]
      .then()
      .statusCode(200)
      .extract().response()
    val resp2 = JsonArray(response2.body().prettyPrint())

    val record3 = JsonObject(resp2.list[0] as Map<String?, Any?>?)
    assertEquals(transferId, UUID.fromString(record3.getString("id")))
    assertEquals(false, record3.getBoolean("pending"))
    assertEquals(true, record3.getBoolean("succeeded"))
    assertEquals(null, record3.getString("error_message"))

  }

  companion object {
    val account1Id: UUID = UUID.randomUUID()
    val account2Id: UUID = UUID.randomUUID()
    val transferId: UUID = UUID.randomUUID()
  }
}