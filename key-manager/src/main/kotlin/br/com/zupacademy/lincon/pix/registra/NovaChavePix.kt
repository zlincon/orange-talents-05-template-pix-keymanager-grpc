package br.com.zupacademy.lincon.pix.registra

import br.com.zupacademy.lincon.TipoDeChave
import br.com.zupacademy.lincon.TipoDeConta
import io.micronaut.core.annotation.Introspected
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@Introspected
data class NovaChavePix(
  @field:NotBlank
  val clienteId: String,
  @field:NotNull
  val tipo: TipoDeChave,
  @field:Size(max = 77)
  val chave: String,
  @field:NotNull
  val tipoDeConta: TipoDeConta
) {

}
