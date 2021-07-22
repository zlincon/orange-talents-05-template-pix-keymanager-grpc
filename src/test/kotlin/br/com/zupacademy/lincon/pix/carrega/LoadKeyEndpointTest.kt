package br.com.zupacademy.lincon.pix.carrega

import br.com.zupacademy.lincon.CarregaChavePixRequest
import br.com.zupacademy.lincon.KeymanagerCarregaServiceGrpc
import br.com.zupacademy.lincon.integration.*
import br.com.zupacademy.lincon.pix.ChavePix
import br.com.zupacademy.lincon.pix.ContaAssociada
import br.com.zupacademy.lincon.pix.TipoDeChave
import br.com.zupacademy.lincon.pix.TipoDeChave.*
import br.com.zupacademy.lincon.pix.TipoDeConta
import br.com.zupacademy.lincon.pix.registra.ChavePixRepository
import br.com.zupacademy.lincon.violations
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.http.HttpResponse
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject

@MicronautTest(transactional = false)
internal class LoadKeyEndpointTest(
  val repository: ChavePixRepository,
  val grpcClient: KeymanagerCarregaServiceGrpc.KeymanagerCarregaServiceBlockingStub
) {

  @Inject
  lateinit var bcbClient: BancoCentralClient

  companion object {
    val CLIENTE_ID = UUID.randomUUID()
  }

  @BeforeEach
  fun setup() {
    repository.save(chave(tipo = EMAIL, chave = "rafael.ponte@zup.com.br", clienteId = CLIENTE_ID))
    repository.save(chave(tipo = CPF, chave = "63657520325", clienteId = UUID.randomUUID()))
    repository.save(chave(tipo = ALEATORIA, chave = "randomkey-1", clienteId = CLIENTE_ID))
    repository.save(chave(tipo = CELULAR, chave = "+551155554321", clienteId = CLIENTE_ID))
  }

  @AfterEach
  fun cleanUp() {
    repository.deleteAll()
  }

  @MockBean(BancoCentralClient::class)
  fun bcbClient(): BancoCentralClient {
    return Mockito.mock(BancoCentralClient::class.java)
  }

  @Factory
  class Clients {
    @Bean
    fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): KeymanagerCarregaServiceGrpc.KeymanagerCarregaServiceBlockingStub {
      return KeymanagerCarregaServiceGrpc.newBlockingStub(channel)
    }
  }

  @Test
  fun `must load key by pixId and clientId`() {
    val chaveExistente = repository.findByChave("+551155554321").get()

    val response = grpcClient.carrega(
      CarregaChavePixRequest.newBuilder()
        .setPixId(
          CarregaChavePixRequest.FiltroPorPixId.newBuilder()
            .setPixId(chaveExistente.id.toString())
            .setClienteId(chaveExistente.clienteId.toString())
            .build()
        )
        .build()
    )

    with(response) {
      assertEquals(chaveExistente.id.toString(), this.pixId)
      assertEquals(chaveExistente.clienteId.toString(), this.clienteId)
      assertEquals(chaveExistente.tipo.name, this.chave.tipo.name)
      assertEquals(chaveExistente.chave, this.chave.chave)
    }
  }

  @Test
  fun `must not load key by pixId and clientId when invalid filter`() {
    val thrown = assertThrows<StatusRuntimeException> {
      grpcClient.carrega(
        CarregaChavePixRequest.newBuilder()
          .setPixId(
            CarregaChavePixRequest.FiltroPorPixId.newBuilder()
              .setPixId("")
              .setClienteId("")
              .build()
          )
          .build()
      )
    }

    with(thrown) {
      assertEquals(Status.INVALID_ARGUMENT.code, status.code)
      assertEquals("Dados inválidos", status.description)
      MatcherAssert.assertThat(
        violations(), Matchers.containsInAnyOrder(
          Pair("pixId", "must not be blank"),
          Pair("clienteId", "must not be blank"),
          Pair("pixId", "Não é um formato válido de UUID"),
          Pair("clienteId", "Não é um formato válido de UUID"),
        )
      )
    }
  }

  @Test
  fun `must not load key by pixId and clientId when invalid filter does not exist`() {
    val pixIdNaoExistente = UUID.randomUUID().toString()
    val clienteIdNaoExistente = UUID.randomUUID().toString()
    val thrown = assertThrows<StatusRuntimeException> {
      grpcClient.carrega(
        CarregaChavePixRequest.newBuilder()
          .setPixId(
            CarregaChavePixRequest.FiltroPorPixId.newBuilder()
              .setPixId(pixIdNaoExistente)
              .setClienteId(clienteIdNaoExistente)
              .build()
          )
          .build()
      )
    }

    with(thrown) {
      assertEquals(Status.NOT_FOUND.code, status.code)
      assertEquals("Chave Pix não encontrada", status.description)
    }
  }

  @Test
  fun `do not load key by key value when record exists locally`() {
    val chaveExistente = repository.findByChave("rafael.ponte@zup.com.br").get()

    val response = grpcClient.carrega(
      CarregaChavePixRequest.newBuilder()
        .setChave("rafael.ponte@zup.com.br")
        .build()
    )

    with(response) {
      assertEquals(chaveExistente.id.toString(), this.pixId)
      assertEquals(chaveExistente.clienteId.toString(), this.clienteId)
      assertEquals(chaveExistente.tipo.name, this.chave.tipo.name)
      assertEquals(chaveExistente.chave, this.chave.chave)
    }
  }

  @Test
  fun `must load key by key value when record does not exist locally but exists in BCB`() {
    val bcbResponse = pixKeyDetailsResponse()
    `when`(bcbClient.findByKey(key = "user.from.another.bank@santander.com.br"))
      .thenReturn(HttpResponse.ok(pixKeyDetailsResponse()))

    val response = grpcClient.carrega(
      CarregaChavePixRequest.newBuilder()
        .setChave("user.from.another.bank@santander.com.br")
        .build()
    )

    with(response) {
      assertEquals("null", this.pixId)
      assertEquals("null", this.clienteId)
      assertEquals(bcbResponse.keyType.name, this.chave.tipo.name)
      assertEquals(bcbResponse.key, this.chave.chave)
    }
  }

  @Test
  fun `must not load key by key value when record does not exist locally or in BCB`() {
    `when`(bcbClient.findByKey(key = "not.existing.user@santander.com.br"))
      .thenReturn(HttpResponse.notFound())

    val thrown = assertThrows<StatusRuntimeException> {
      grpcClient.carrega(
        CarregaChavePixRequest.newBuilder()
          .setChave("not.existing.user@santander.com.br")
          .build()
      )
    }

    with(thrown) {
      assertEquals(Status.NOT_FOUND.code, status.code)
      assertEquals("Chave Pix não encontrada", status.description)
    }
  }

  @Test
  fun `must not load key by key value when invalid filter`() {
    val thrown = assertThrows<StatusRuntimeException> {
      grpcClient.carrega(
        CarregaChavePixRequest.newBuilder()
          .setChave("").build()
      )
    }

    with(thrown) {
      assertEquals(Status.INVALID_ARGUMENT.code, status.code)
      assertEquals("Dados inválidos", status.description)
      MatcherAssert.assertThat(
        violations(), Matchers.containsInAnyOrder(
          Pair("chave", "must not be blank")
        )
      )
    }
  }

  @Test
  fun `should not load when invalid filter`() {
    val thrown = assertThrows<StatusRuntimeException> {
      grpcClient.carrega(CarregaChavePixRequest.newBuilder().build())
    }

    with(thrown) {
      assertEquals(Status.INVALID_ARGUMENT.code, status.code)
      assertEquals("Chave Pix inválida ou não informada", status.description)
    }
  }

  private fun pixKeyDetailsResponse(): PixKeyDetailsResponse {
    return PixKeyDetailsResponse(
      keyType = PixKeyType.EMAIL,
      key = "user.from.another.bank@santander.com.br",
      bankAccount = bankAccount(),
      owner = owner(),
      createdAt = LocalDateTime.now()
    )
  }

  private fun owner(): Owner {
    return Owner(
      type = Owner.OwnerType.NATURAL_PERSON,
      name = "Another User",
      taxIdNumber = "12345678901"
    )
  }

  private fun bankAccount(): BankAccount {
    return BankAccount(
      participant = "90400888",
      branch = "9871",
      accountNumber = "987654",
      accountType = BankAccount.AccountType.SVGS
    )
  }

  private fun chave(
    tipo: TipoDeChave,
    chave: String = UUID.randomUUID().toString(),
    clienteId: UUID = UUID.randomUUID()
  ): ChavePix {
    return ChavePix(
      clienteId = clienteId,
      tipo = tipo,
      chave = chave,
      tipoDeConta = TipoDeConta.CONTA_CORRENTE,
      conta = ContaAssociada(
        instituicao = "UNIBANCO ITAU",
        nomeDoTitular = "Rafael Ponte",
        cpfDoTitular = "12345678900",
        agencia = "1218",
        numeroDaConta = "123456"
      )
    )
  }


}