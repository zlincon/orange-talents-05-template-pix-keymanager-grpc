package br.com.zupacademy.lincon.pix.lista

import br.com.zupacademy.lincon.KeyManagerListaServiceGrpc
import br.com.zupacademy.lincon.ListaChavesPixRequest
import br.com.zupacademy.lincon.pix.ChavePix
import br.com.zupacademy.lincon.pix.ContaAssociada
import br.com.zupacademy.lincon.pix.TipoDeChave
import br.com.zupacademy.lincon.pix.TipoDeConta
import br.com.zupacademy.lincon.pix.registra.ChavePixRepository
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.*
import java.util.*

@MicronautTest(transactional = false)
internal class ListKeysEndpointTest(
  val repository: ChavePixRepository,
  val grpcClient: KeyManagerListaServiceGrpc.KeyManagerListaServiceBlockingStub
) {

  companion object {
    val CLIENTE_ID = UUID.randomUUID()
  }

  @BeforeEach
  fun setup() {
    repository.save(chave(tipo = TipoDeChave.EMAIL, chave = "rafael.ponte@zup.com.br", clienteId = CLIENTE_ID))
    repository.save(chave(tipo = TipoDeChave.ALEATORIA, chave = "randomkey-x", clienteId = UUID.randomUUID()))
    repository.save(chave(tipo = TipoDeChave.ALEATORIA, chave = "randomkey-y", clienteId = CLIENTE_ID))
  }

  @AfterEach
  fun cleanUp() {
    repository.deleteAll()
  }

  @Factory
  class Clients {
    @Bean
    fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): KeyManagerListaServiceGrpc.KeyManagerListaServiceBlockingStub {
      return KeyManagerListaServiceGrpc.newBlockingStub(channel)
    }
  }

  @Test
  fun `must list all client keys`() {
    val clienteId = CLIENTE_ID.toString()

    val response = grpcClient.lista(
      ListaChavesPixRequest.newBuilder()
        .setClienteId(clienteId)
        .build()
    )

    with(response.chavesList) {
      MatcherAssert.assertThat(this, Matchers.hasSize(2))
      MatcherAssert.assertThat(
        this.map { Pair(it.tipo, it.chave) }.toList(),
        Matchers.containsInAnyOrder(
          Pair(br.com.zupacademy.lincon.TipoDeChave.ALEATORIA, "randomkey-y"),
          Pair(br.com.zupacademy.lincon.TipoDeChave.EMAIL, "rafael.ponte@zup.com.br"),
        )
      )
    }
  }

  @Test
  fun `should not list client keys when client does not have keys`() {
    val clienteSemChaves = UUID.randomUUID().toString()

    val response = grpcClient.lista(
      ListaChavesPixRequest.newBuilder()
        .setClienteId(clienteSemChaves)
        .build()
    )

    Assertions.assertEquals(0, response.chavesCount)
  }

  @Test
  fun `should not list all client keys when clientId is invalid`() {
    val clineteIdInvalido = ""

    val thrown = assertThrows<StatusRuntimeException> {
      grpcClient.lista(
        ListaChavesPixRequest.newBuilder()
          .setClienteId(clineteIdInvalido)
          .build()
      )

    }
    with(thrown) {
      Assertions.assertEquals(Status.INVALID_ARGUMENT.code, status.code)
      Assertions.assertEquals("Cliente ID não pode ser nulo ou vazio", status.description)
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