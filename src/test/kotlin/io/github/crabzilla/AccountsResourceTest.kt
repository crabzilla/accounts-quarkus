package io.github.crabzilla

import io.github.crabzilla.example2.accounts.AccountsRequests.DepositMoneyRequest
import io.github.crabzilla.example2.accounts.AccountsRequests.OpenAccountRequest
import io.github.crabzilla.example2.accounts.AccountsRequests.WithdrawMoneyRequest
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.mutiny.pgclient.PgPool
import org.hamcrest.CoreMatchers
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import java.util.*
import javax.inject.Inject


@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class AccountsResourceTest() {

  @Inject
  lateinit var pgPool: PgPool

  @Test
  @Order(1)
  fun noAccounts() {
    pgPool.query("delete from events").execute()
      .flatMap { pgPool.query("delete from commands").execute() }
      .flatMap { pgPool.query("delete from accounts_view").execute() }
      .flatMap { pgPool.query("delete from transfers_view").execute() }
      .await().indefinitely()
    RestAssured.given()
      .contentType(ContentType.JSON)
      .`when`()["/accounts/view1"]
      .then()
      .statusCode(200)
      .body("size()", CoreMatchers.`is`(0))
  }

  @Test
  @Order(2)
  fun creatingAccount() {
    val request = OpenAccountRequest(UUID.randomUUID().toString(), "test")
    val response = RestAssured.given()
      .contentType(ContentType.JSON)
      .body(request)
      .`when`().put("/accounts/$id")
      .then()
      .statusCode(200)
      .extract().response()
    val resp = JsonObject(response.body().asString())
    assertEquals(1, resp.getLong("version"))
  }

  @Test
  @Order(3)
  @Throws(InterruptedException::class)
  fun gotAccounts() {
    val response = RestAssured.given()
      .contentType(ContentType.JSON)
      .`when`()["/accounts/view1"]
      .then()
      .statusCode(200)
      .extract().response()
    val resp = JsonArray(response.body().asString())
    assertEquals(1, resp.size())
    val record1 = JsonObject(resp.list[0] as Map<String?, Any?>?)
    assertEquals(id, UUID.fromString(record1.getString("id")))
    assertEquals(0L, record1.getLong("balance"))
  }

  @Test
  @Order(4)
  fun deposit_1000() {
    val request = DepositMoneyRequest(1000.0)
    val response = RestAssured.given()
      .contentType(ContentType.JSON)
      .body(request)
      .`when`().post("/accounts/$id/deposit")
      .then()
      .statusCode(200)
      .extract().response()
    val resp = JsonObject(response.body().asString())
    assertEquals(2, resp.getLong("version"))
  }

  @Test
  @Order(5)
  fun withdraw_600() {
    val request = WithdrawMoneyRequest(600.0)
    val response = RestAssured.given()
      .contentType(ContentType.JSON)
      .body(request)
      .`when`().post("/accounts/$id/withdraw")
      .then()
      .statusCode(200)
      .extract().response()
    val resp = JsonObject(response.body().asString())
    assertEquals(3, resp.getLong("version"))
  }

  @Test
  @Order(6)
  @Throws(InterruptedException::class)
  fun gotAccountWith400() {
    Thread.sleep(1100)
    val response = RestAssured.given()
      .contentType(ContentType.JSON)
      .`when`()["/accounts/view1"]
      .then()
      .statusCode(200)
      .extract().response()
    val resp = JsonArray(response.body().asString())
    assertEquals(1, resp.size())
    val record1 = JsonObject(resp.list[0] as Map<String?, Any?>?)
    assertEquals(id, UUID.fromString(record1.getString("id")))
    assertEquals(400L, record1.getLong("balance"))
  }

  companion object {
    var id: UUID = UUID.randomUUID()
  }
}