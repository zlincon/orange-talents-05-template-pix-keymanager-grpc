package br.com.zupacademy.lincon.pix.registra

import br.com.zupacademy.lincon.KeyManagerRegistraServiceGrpc
import br.com.zupacademy.lincon.RegistraChavePixRequest
import br.com.zupacademy.lincon.TipoDeChave
import br.com.zupacademy.lincon.TipoDeConta
import br.com.zupacademy.lincon.integration.ContasDeClientesNoItauClient
import io.micronaut.http.HttpResponse
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*
import javax.inject.Inject
import org.mockito.Mockito.`when`

@MicronautTest(transactional = false)
internal class RegistraChaveEndpointTest(
  val repository: ChavePixRepository,
  val grpcClient: KeyManagerRegistraServiceGrpc
  .KeyManagerRegistraServiceBlockingStub
) {

  @Inject
  lateinit var itauClient: ContasDeClientesNoItauClient

  companion object {
    val CLIENTE_ID = UUID.randomUUID()
  }

  @BeforeEach
  fun setup() {
    repository.deleteAll()
  }

  @Test
  fun `deve registrar uma nova chave pix`() {
    `when`(itauClient.buscaContaPorTipo(clienteId = CLIENTE_ID.toString(),
      tipo = "CONTA_CORRENTE")).thenReturn(HttpResponse.ok
      (dadosDaContaResponse()))

    val response = grpcClient.registra(RegistraChavePixRequest.newBuilder()
      .setClienteId(CLIENTE_ID.toString())
      .setTipoDeChave(TipoDeChave.EMAIL)
      .setChave("rponde@gmail.com")
      .setTipoDeConta(TipoDeConta.CONTA_CORRENTE)
      .build())

    with(response) {
      assertEquals(CLIENTE_ID.toString(), clienteId)
      assertNotNull(pixId)
    }
  }

  private fun dadosDaContaResponse(): DadosDaContaResponse {
    return DadosDaContaResponse(
      tipo = "CONTA_CORRENTE",
      instituicao = InstituicaoResponse("UNIBANCO ITAU SA", ContaAssociada
        .ITAU_UNIBANCO_ISPB),
      agencia = "1218",
      numero = "291900",
      titular = TitularResponse("Rafaek Ponte", "63657520325")
    )
  }

}