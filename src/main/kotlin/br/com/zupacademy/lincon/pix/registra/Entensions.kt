package br.com.zupacademy.lincon.pix.registra

import br.com.zupacademy.lincon.RegistraChavePixRequest
import br.com.zupacademy.lincon.TipoDeChave.*
import br.com.zupacademy.lincon.TipoDeConta

fun RegistraChavePixRequest.toModel(): NovaChavePix {
  return NovaChavePix(
    clienteId = clienteId,
    tipo = when (tipoDeChave) {
      UNKNOWN_TIPO_CHAVE -> null
      else -> TipoDeChave.valueOf(tipoDeChave.name)
    },
    chave = chave,
    tipoDeConta = when (tipoDeConta) {
      TipoDeConta.UNKNOWN_TIPO_CONTA -> null
      else -> TipoDeConta.valueOf(tipoDeConta.name)
    }
  )
}