package br.com.zupacademy.lincon.pix.registra

import io.micronaut.validation.Validated
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton
import javax.transaction.Transactional
import javax.validation.Valid
import kotlin.IllegalStateException

@Validated
@Singleton
class NovaChavePixService(
  @Inject val repository: ChavePixTepository,
  @Inject val itauClient: ContasDeClientesNoItauClient
) {

  private val LOGGER = LoggerFactory.getLogger(this::class.java)

  @Transactional
  fun registra(@Valid novaChave : NovaChavePix): ChavePix {

    if(repository.existsByChave(novaChave.chave))
      throw ChavePixExistenteException("Chave Pix '${novaChave.chave}' existente")

    val response = itauClient.buscaContaPorTipo(novaChave.clienteId,
      novaChave.tipoDeConta.name)
    val conta = response.body().toModel() ?: throw IllegalStateException
    ("Clinete não encontrado no itaú")

    val chave = novaChave.toModel(conta)

    return chave

  }

}
