package br.com.zupacademy.lincon.integration

import br.com.zupacademy.lincon.pix.TipoDeConta
import br.com.zupacademy.lincon.pix.registra.ChavePix
import br.com.zupacademy.lincon.pix.registra.ContaAssociada
import br.com.zupacademy.lincon.pix.registra.TipoDeChave
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.client.annotation.Client
import java.time.LocalDateTime

@Client("\${bcb.pix.url}")
interface BancoCentralClient {

  @Delete(
    "/api/v1/pix/keys/{key}", produces = [MediaType
      .APPLICATION_XML], consumes = [MediaType
      .APPLICATION_XML]
  )
  fun delete(@PathVariable key: String, @Body request: DeletePixKeyRequest):
      HttpResponse<DeletePixKeyResponse>

  @Post(
    "/api/v1/pix/keys", produces = [MediaType
      .APPLICATION_XML], consumes = [MediaType
      .APPLICATION_XML]
  )
  fun create(@Body request: CreatePixKeyRequest):
      HttpResponse<CreatePixKeyResponse>

}

data class CreatePixKeyResponse(
  val keyType: PixKeyType,
  val key: String,
  val bankAccount: BankAccount,
  val owner: Owner,
  val createdAt: LocalDateTime
)

data class CreatePixKeyRequest(
  val keyType: PixKeyType,
  val key: String,
  val bankAccount: BankAccount,
  val owner: Owner
) {
  companion object {
    fun of(chave: ChavePix): CreatePixKeyRequest {
      return CreatePixKeyRequest(
        keyType = PixKeyType.by(chave.tipo),
        key = chave.chave,
        bankAccount = BankAccount(
          participant = ContaAssociada.ITAU_UNIBANCO_ISPB,
          branch = chave.conta.agencia,
          accountNumber = chave.conta.numeroDaConta,
          accountType = BankAccount.AccountType.by(chave.tipoDeConta)
        ),
        owner = Owner(
          type = Owner.OwnerType.NATURAL_PERSON,
          name = chave.conta.nomeDoTitular,
          taxIdNumber = chave.conta.cpfDoTitular
        )
      )
    }
  }
}

enum class PixKeyType(val domainType: TipoDeChave?) {
  CPF(TipoDeChave.CPF),
  PHONE(TipoDeChave.CELULAR),
  EMAIL(TipoDeChave.EMAIL),
  RANDOM(TipoDeChave.ALEATORIA);

  companion object {
    private val mapping =
      PixKeyType.values().associateBy(PixKeyType::domainType)

    fun by(domainType: TipoDeChave): PixKeyType {
      return mapping[domainType] ?: throw IllegalStateException(
        "Tipo de " +
            "chave Pix inválido ou não encontrado. $domainType"
      )
    }
  }
}

data class BankAccount(
  val participant: String,
  val branch: String,
  val accountNumber: String,
  val accountType: AccountType
) {
  enum class AccountType() {
    CACC, SVGS;

    companion object {
      fun by(domainType: TipoDeConta): AccountType {
        return when (domainType) {
          TipoDeConta.CONTA_CORRENTE -> CACC
          TipoDeConta.CONTA_POUPANCA -> SVGS
        }
      }
    }
  }
}


data class Owner(
  val type: OwnerType,
  val name: String,
  val taxIdNumber: String
) {
  enum class OwnerType {
    NATURAL_PERSON,
    LEGAL_PERSON
  }
}

data class DeletePixKeyResponse(
  val key: String,
  val participant: String,
  val deletedAt: LocalDateTime
)

data class DeletePixKeyRequest(
  val key: String,
  val participant: String = ContaAssociada.ITAU_UNIBANCO_ISPB
)
