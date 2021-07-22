package br.com.zupacademy.lincon.pix.remove

import br.com.zupacademy.lincon.KeymanagerRemoveServiceGrpc
import br.com.zupacademy.lincon.RemoveChavePixRequest
import br.com.zupacademy.lincon.integration.BancoCentralClient
import br.com.zupacademy.lincon.integration.DeletePixKeyRequest
import br.com.zupacademy.lincon.integration.DeletePixKeyResponse
import br.com.zupacademy.lincon.pix.TipoDeConta
import br.com.zupacademy.lincon.pix.ChavePix
import br.com.zupacademy.lincon.pix.registra.ChavePixRepository
import br.com.zupacademy.lincon.pix.ContaAssociada
import br.com.zupacademy.lincon.pix.TipoDeChave
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
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject

@MicronautTest(transactional = false)
class RemoveKeyEndpointTest(
  val repository: ChavePixRepository,
  val grpcClient: KeymanagerRemoveServiceGrpc.KeymanagerRemoveServiceBlockingStub
) {

  @Inject
  lateinit var bcbClient: BancoCentralClient

  lateinit var CHAVE_EXISTENTE: ChavePix

  @BeforeEach
  fun setup() {
    CHAVE_EXISTENTE = repository.save(
      chave(
        tipo = TipoDeChave.EMAIL,
        chave = "rponte@gmail.com",
        clienteId = UUID.randomUUID()
      )
    )
  }

  @MockBean(BancoCentralClient::class)
  fun bcbClient(): BancoCentralClient {
    return mock(BancoCentralClient::class.java)
  }

  @AfterEach
  fun cleanUp() {
    repository.deleteAll()
  }

  @Factory
  class Clients {
    @Bean
    fun blockingStub(
      @GrpcChannel(GrpcServerChannel.NAME) channel:
      ManagedChannel
    ): KeymanagerRemoveServiceGrpc
    .KeymanagerRemoveServiceBlockingStub {
      return KeymanagerRemoveServiceGrpc.newBlockingStub(channel)
    }
  }

  @Test
  fun `must remove existing pix key`() {
    `when`(
      bcbClient.delete(
        "rponte@gmail.com", DeletePixKeyRequest
          ("rponte@gmail.com")
      )
    )
      .thenReturn(
        HttpResponse.ok(
          DeletePixKeyResponse(
            key = "rponte@gmail.com",
            participant = ContaAssociada.ITAU_UNIBANCO_ISPB,
            deletedAt = LocalDateTime.now()
          )
        )
      )

    val response = grpcClient.remove(
      RemoveChavePixRequest.newBuilder()
        .setPixId(CHAVE_EXISTENTE.id.toString())
        .setClienteId(CHAVE_EXISTENTE.clienteId.toString())
        .build()
    )

    with(response) {
      assertEquals(CHAVE_EXISTENTE.id.toString(), pixId)
      assertEquals(CHAVE_EXISTENTE.clienteId.toString(), clienteId)
    }
  }

  @Test
  fun `should not remove existing pix key when there is an error in the BCB service`() {
    `when`(
      bcbClient.delete(
        "rponte@gmail.com",
        DeletePixKeyRequest("rponte@gmail.com")
      )
    )
      .thenReturn(HttpResponse.unprocessableEntity())

    val thrown = assertThrows<StatusRuntimeException> {
      grpcClient.remove(
        RemoveChavePixRequest.newBuilder()
          .setPixId(CHAVE_EXISTENTE.id.toString())
          .setClienteId(CHAVE_EXISTENTE.clienteId.toString())
          .build()
      )
    }

    with(thrown) {
      assertEquals(Status.FAILED_PRECONDITION.code, status.code)
      assertEquals(
        "Erro ao remover chave Pix no Banco Central do Brasil " +
            "(BCB)",
        status.description
      )
    }
  }

  @Test
  fun `should not remove pix key when key does not exist`() {
    val pixIdNaoExistente = UUID.randomUUID().toString()

    val thrown = assertThrows<StatusRuntimeException> {
      grpcClient.remove(
        RemoveChavePixRequest.newBuilder()
          .setPixId(pixIdNaoExistente)
          .setClienteId(CHAVE_EXISTENTE.clienteId.toString())
          .build()
      )
    }

    with(thrown) {
      assertEquals(Status.NOT_FOUND.code, status.code)
      assertEquals(
        "Chave Pix não encontrada ou não pertence ao cliente " +
            "informado",
        status.description
      )
    }
  }

  @Test
  fun `should not remove pix key when key exists but belongs to another client`() {
    val outroClienteID = UUID.randomUUID().toString()

    val thrown = assertThrows<StatusRuntimeException> {
      grpcClient.remove(
        RemoveChavePixRequest.newBuilder()
          .setPixId(CHAVE_EXISTENTE.id.toString())
          .setClienteId(outroClienteID)
          .build()
      )
    }

    with(thrown) {
      assertEquals(Status.NOT_FOUND.code, status.code)
      assertEquals(
        "Chave Pix não encontrada ou não pertence ao cliente " +
            "informado", status.description
      )
    }
  }

  @Test
  fun `should not remove pix key when invalid parameters`() {
    val thrown = assertThrows<StatusRuntimeException> {
      grpcClient.remove(RemoveChavePixRequest.newBuilder().build())
    }

    with(thrown) {
      assertEquals(Status.INVALID_ARGUMENT.code, status.code)
      assertEquals("Dados inválidos", status.description)
      assertThat(
        violations(), containsInAnyOrder(
          Pair("pixId", "must not be blank"),
          Pair("pixId", "Não é um formato válido de UUID"),
          Pair("clientId", "must not be blank"),
          Pair("clientId", "Não é um formato válido de UUID"),
        )
      )
    }
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