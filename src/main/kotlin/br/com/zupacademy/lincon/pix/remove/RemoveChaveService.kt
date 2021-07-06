package br.com.zupacademy.lincon.pix.remove

import br.com.zupacademy.lincon.pix.ChavePixNaoEncontradaException
import br.com.zupacademy.lincon.integration.BancoCentralClient
import br.com.zupacademy.lincon.integration.DeletePixKeyRequest
import br.com.zupacademy.lincon.pix.registra.ChavePixRepository
import br.com.zupacademy.lincon.shared.validation.ValidUUID
import io.micronaut.http.HttpStatus
import io.micronaut.validation.Validated
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import javax.transaction.Transactional
import javax.validation.constraints.NotBlank

@Validated
@Singleton
class RemoveChaveService(
  @Inject val repository: ChavePixRepository,
  @Inject val bcbClient: BancoCentralClient
) {

  @Transactional
  fun remove(
    @NotBlank @ValidUUID clientId: String?, @NotBlank @ValidUUID pixId:
    String?
  ) {
    val uuidPixId = UUID.fromString(pixId)
    val uuidClienteId = UUID.fromString(clientId)

    val chave = repository.findByIdAndClienteId(
      uuidPixId,
      uuidClienteId
    )
      .orElseThrow {
        ChavePixNaoEncontradaException(
          "Chave Pix não encontrda " +
              "ou não pertence ao cliente informado"
        )
      }

    repository.delete(chave)

    val request = DeletePixKeyRequest(chave.chave)

    val bcbResponse = bcbClient.delete(key = chave.chave, request = request)
    if (bcbResponse.status != HttpStatus.OK) {
      throw IllegalStateException(
        "Erro ao remover chave Pix no Banco Central" +
            " do Brasil (BCB)"
      )
    }
  }

}
