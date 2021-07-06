package br.com.zupacademy.lincon.pix.registra

import br.com.zupacademy.lincon.KeyManagerRegistraServiceGrpc
import br.com.zupacademy.lincon.RegistraChavePixRequest
import br.com.zupacademy.lincon.TipoDeChave
import br.com.zupacademy.lincon.TipoDeConta
import br.com.zupacademy.lincon.integration.ContasDeClientesNoItauClient
import io.grpc.ManagedChannel
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.http.HttpResponse
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import java.util.*
import javax.inject.Inject

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
        `when`(
            itauClient.buscaContaPorTipo(
                clienteId = CLIENTE_ID.toString(),
                tipo = "CONTA_CORRENTE"
            )
        ).thenReturn(
            HttpResponse.ok
                (dadosDaContaResponse())
        )

        val response = grpcClient.registra(
            RegistraChavePixRequest.newBuilder()
                .setClienteId(CLIENTE_ID.toString())
                .setTipoDeChave(TipoDeChave.EMAIL)
                .setChave("rponde@gmail.com")
                .setTipoDeConta(TipoDeConta.CONTA_CORRENTE)
                .build()
        )

        with(response) {
            assertEquals(CLIENTE_ID.toString(), clienteId)
            assertNotNull(pixId)
        }
    }

    @Factory
    class Clients {
        @Bean
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): KeyManagerRegistraServiceGrpc.KeyManagerRegistraServiceBlockingStub? {
            return KeyManagerRegistraServiceGrpc.newBlockingStub(channel)
        }
    }

    @MockBean(ContasDeClientesNoItauClient::class)
    fun itauClient(): ContasDeClientesNoItauClient? {
        return Mockito.mock(ContasDeClientesNoItauClient::class.java)
    }

    private fun dadosDaContaResponse(): DadosDaContaResponse {
        return DadosDaContaResponse(
            tipo = "CONTA_CORRENTE",
            instituicao = InstituicaoResponse(
                "UNIBANCO ITAU SA", ContaAssociada
                    .ITAU_UNIBANCO_ISPB
            ),
            agencia = "1218",
            numero = "291900",
            titular = TitularResponse("Rafaek Ponte", "63657520325")
        )
    }


}