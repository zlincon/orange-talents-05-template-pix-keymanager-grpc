package br.com.zupacademy.lincon.pix.registra

import br.com.zupacademy.lincon.*
import br.com.zupacademy.lincon.TipoDeChave
import br.com.zupacademy.lincon.integration.*
import com.google.rpc.BadRequest
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.protobuf.StatusProto
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.http.HttpResponse
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import java.time.LocalDateTime
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

  @Inject
  lateinit var bcbClient: BancoCentralClient

  companion object {
    val CLIENTE_ID = UUID.randomUUID()
  }

  @BeforeEach
  fun setup() {
    repository.deleteAll()
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

//  @MockBean(BancoCentralClient::class)
//  fun bcbClient(): BancoCentralClient {
//    return Mockito.mock(BancoCentralClient::class.java)
//  }

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

//    `when`(bcbClient.create(createPixKeyRequest()))
//      .thenReturn(HttpResponse.created(createPixKeyResponse()))

    val response = grpcClient.registra(
      RegistraChavePixRequest.newBuilder()
        .setClienteId(CLIENTE_ID.toString())
        .setTipoDeChave(TipoDeChave.EMAIL)
        .setChave("rponte@gmail.com")
        .setTipoDeConta(TipoDeConta.CONTA_CORRENTE)
        .build()
    )

    with(response) {
      assertEquals(CLIENTE_ID.toString(), clienteId)
      assertNotNull(pixId)
    }
  }

  @Test
  fun `nao deve registrar chave pix quando chave existente`() {
    repository.save(
      chave(
        tipo = br.com.zupacademy.lincon.pix.registra.TipoDeChave.CPF,
        chave = "63657520325",
        clientId = CLIENTE_ID
      )
    )

    val thrown = assertThrows<StatusRuntimeException> {
      grpcClient.registra(
        RegistraChavePixRequest.newBuilder()
          .setClienteId(CLIENTE_ID.toString())
          .setTipoDeChave(TipoDeChave.CPF)
          .setChave("63657520325")
          .setTipoDeConta(TipoDeConta.CONTA_CORRENTE)
          .build()
      )
    }

    with(thrown) {
      assertEquals(Status.ALREADY_EXISTS.code, status.code)
      assertEquals("Chave Pix '63657520325' existente", status.description)
    }
  }

  @Test
  fun `nao deve registrar chave pix quando nao encontrar dados da conta cliente`() {
    `when`(
      itauClient.buscaContaPorTipo(
        clienteId = CLIENTE_ID.toString(),
        tipo = "CONTA_CORRENTE"
      )
    )
      .thenReturn(HttpResponse.notFound())

    val thrown = assertThrows<StatusRuntimeException> {
      grpcClient.registra(
        RegistraChavePixRequest.newBuilder()
          .setClienteId(CLIENTE_ID.toString())
          .setTipoDeChave(TipoDeChave.EMAIL)
          .setChave("rponte@gmail.com")
          .setTipoDeConta(TipoDeConta.CONTA_CORRENTE)
          .build()
      )
    }

    with(thrown) {
      assertEquals(Status.FAILED_PRECONDITION.code, status.code)
      assertEquals("Cliente não encontrado no Itaú", status.description)
    }
  }

  @Test
  fun `nao deve registrar chave pix quando nao for possivel registrar chave no BCB`() {
    `when`(
      itauClient.buscaContaPorTipo(
        clienteId = CLIENTE_ID.toString(),
        tipo = "CONTA_CORRENTE"
      )
    )
      .thenReturn(HttpResponse.ok(dadosDaContaResponse()))

    `when`(bcbClient.create(createPixKeyRequest()))
      .thenReturn(HttpResponse.badRequest())

    val thrown = assertThrows<StatusRuntimeException> {
      grpcClient.registra(
        RegistraChavePixRequest.newBuilder()
          .setClienteId(CLIENTE_ID.toString())
          .setTipoDeChave(TipoDeChave.EMAIL)
          .setChave("rponte@gmail.com")
          .setTipoDeConta(TipoDeConta.CONTA_CORRENTE)
          .build()
      )
    }

    with(thrown) {
      assertEquals(Status.FAILED_PRECONDITION.code, status.code)
      assertEquals("Erro ao registrar Pix no Banco Central", status.description)
    }
  }

  @Test
  fun `nao deve registrar chave pix quando parametros forem invalidos`() {
    val thrown = assertThrows<StatusRuntimeException> {
      grpcClient.registra(RegistraChavePixRequest.newBuilder().build())
    }

    with(thrown) {
      assertEquals(Status.INVALID_ARGUMENT.code, status.code)
      assertEquals("Dados inválidos", status.description)
      assertThat(
        violations(), containsInAnyOrder(
          Pair("clienteId", "must not be blank"),
          Pair("clienteId", "não é um formato válido de UUID"),
          Pair("tipoDeConta", "must not be null"),
          Pair("tipo", "must not be null"),
        )
      )
    }
  }

  @Test
  fun `nao deve registrar chave pix quando parametros forem invalidos - chave inválida`() {
    val thrown = assertThrows<StatusRuntimeException> {
      grpcClient.registra(
        RegistraChavePixRequest.newBuilder()
          .setClienteId(CLIENTE_ID.toString())
          .setTipoDeChave(TipoDeChave.CPF)
          .setChave("378.930.cpf-inválido.389-73")
          .setTipoDeConta(TipoDeConta.CONTA_POUPANCA)
          .build()
      )
    }

    with(thrown) {
      assertEquals(Status.INVALID_ARGUMENT.code, status.code)
      assertEquals(
        violations(), containsInAnyOrder(
          Pair("chave", "chave Pix inválida (CPF)")
        )
      )
    }
  }

  private fun chave(
    tipo: br.com.zupacademy.lincon.pix.registra.TipoDeChave,
    chave: String = UUID.randomUUID().toString(),
    clientId: UUID = UUID.randomUUID()
  ): ChavePix {
    return ChavePix(
      clienteId = clientId,
      tipo = tipo,
      chave = chave,
      tipoDeConta = br.com.zupacademy.lincon.pix.TipoDeConta.CONTA_CORRENTE,
      conta = ContaAssociada(
        instituicao = "UNIBANCO ITAU",
        nomeDoTitular = "Rafael Ponte",
        cpfDoTitular = "63657520325",
        agencia = "1218",
        numeroDaConta = "291900"
      )
    )
  }


  private fun createPixKeyResponse(): CreatePixKeyResponse {
    return CreatePixKeyResponse(
      keyType = PixKeyType.EMAIL,
      key = "rponte@gmail.com",
      bankAccount = bankAccount(),
      owner = owner(),
      createdAt = LocalDateTime.now()
    )
  }


  private fun createPixKeyRequest(): CreatePixKeyRequest {
    return CreatePixKeyRequest(
      keyType = PixKeyType.EMAIL,
      key = "rponte@gmail.com",
      bankAccount = bankAccount(),
      owner = owner()
    )
  }

  private fun owner(): Owner {
    return Owner(
      type = Owner.OwnerType.NATURAL_PERSON,
      name = "Rafael Ponte",
      taxIdNumber = "63657520325"
    )
  }

  private fun bankAccount(): BankAccount {
    return BankAccount(
      participant = ContaAssociada.ITAU_UNIBANCO_ISPB,
      branch = "1218",
      accountNumber = "291900",
      accountType = BankAccount.AccountType.CACC
    )
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