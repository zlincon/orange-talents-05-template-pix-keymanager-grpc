package br.com.zupacademy.lincon.pix.registra

import br.com.zupacademy.lincon.integration.BancoCentralClient
import br.com.zupacademy.lincon.integration.ContasDeClientesNoItauClient
import br.com.zupacademy.lincon.integration.CreatePixKeyRequest
import br.com.zupacademy.lincon.pix.ChavePix
import io.micronaut.http.HttpStatus
import io.micronaut.validation.Validated
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton
import javax.transaction.Transactional
import javax.validation.Valid

@Validated
@Singleton
class NovaChavePixService(
  @Inject val repository: ChavePixRepository,
  @Inject val itauClient: ContasDeClientesNoItauClient,
  @Inject val bcbClient: BancoCentralClient
) {

  private val LOGGER = LoggerFactory.getLogger(this::class.java)

  @Transactional
  fun registra(@Valid novaChave: NovaChavePix): ChavePix {

    if (repository.existsByChave(novaChave.chave))
      throw ChavePixExistenteException("Chave Pix '${novaChave.chave}' existente")

    val response = itauClient.buscaContaPorTipo(
      novaChave.clienteId!!,
      novaChave.tipoDeConta!!.name
    )
    val conta = response.body()?.toModel()
      ?: throw IllegalStateException("Cliente não encontrado no Itaú")

    val chave = novaChave.toModel(conta)
    repository.save(chave)

    val bcbRequest = CreatePixKeyRequest.of(chave).also {
      LOGGER.info("Registrando chave Pix no Banco Central do Brasil: $it")
    }

    val bcbResponse = bcbClient.create(bcbRequest)
    if (bcbResponse.status != HttpStatus.CREATED)
      throw IllegalStateException(
        "Erro ao registrar chave Pix no Banco " +
            "Central"
      )

    chave.atualiza(bcbResponse.body()!!.key)

    return chave

  }

}
