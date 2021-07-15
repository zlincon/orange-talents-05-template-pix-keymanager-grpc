package br.com.zupacademy.lincon.pix.carrega

import br.com.zupacademy.lincon.integration.BancoCentralClient
import br.com.zupacademy.lincon.pix.ChavePixNaoEncontradaException
import br.com.zupacademy.lincon.pix.registra.ChavePixRepository
import br.com.zupacademy.lincon.shared.validation.ValidUUID
import io.micronaut.core.annotation.Introspected
import io.micronaut.http.HttpStatus
import org.slf4j.LoggerFactory
import java.util.*
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

@Introspected
sealed class Filtro {
  abstract fun filtra(
    repository: ChavePixRepository,
    bcbClient: BancoCentralClient
  ): ChavePixInfo

  @Introspected
  data class PorPixId(
    @field:NotBlank @field:ValidUUID val clienteId: String,
    @field:NotBlank @field:ValidUUID val pixId: String
  ) : Filtro() {
    fun pixIdAsUuid() = UUID.fromString(pixId)
    fun clienteIdAsUuid() = UUID.fromString(clienteId)

    override fun filtra(
      repository: ChavePixRepository,
      bcbClient: BancoCentralClient
    ): ChavePixInfo {
      return repository.findById(pixIdAsUuid())
        .filter { it.pertenceAo(clienteIdAsUuid()) }
        .map(ChavePixInfo.Companion::of)
        .orElseThrow { ChavePixNaoEncontradaException("Chave Pix não encontrada") }
    }
  }

  @Introspected
  data class PorChave(@field:NotBlank @Size(max = 77) val chave: String) : Filtro() {

    private val LOGGER = LoggerFactory.getLogger(this::class.java)

    override fun filtra(
      repository: ChavePixRepository,
      bcbClient: BancoCentralClient
    ): ChavePixInfo {
      return repository.findByChave(chave)
        .map(ChavePixInfo.Companion::of)
        .orElseGet {
          LOGGER.info("Consultado chave Pix '$chave' no Banco Central do Brasil (BCB)")

          val response = bcbClient.findByKey(chave)
          when (response.status) {
            HttpStatus.OK -> response.body()?.toModel()
            else -> throw ChavePixNaoEncontradaException("Chave Pix não encontrada")
          }
        }
    }

  }

  @Introspected
  class Invalido : Filtro() {
    override fun filtra(
      repository: ChavePixRepository,
      bcbClient: BancoCentralClient
    ): ChavePixInfo {
      throw IllegalArgumentException("Chave Pix inválida ou não informada")
    }
  }
}
