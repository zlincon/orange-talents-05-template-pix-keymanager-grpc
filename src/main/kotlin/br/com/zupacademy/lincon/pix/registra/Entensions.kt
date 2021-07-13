package br.com.zupacademy.lincon.pix.registra

import br.com.zupacademy.lincon.RegistraChavePixRequest
import br.com.zupacademy.lincon.TipoDeChave.*
import br.com.zupacademy.lincon.TipoDeConta.*
import br.com.zupacademy.lincon.pix.TipoDeChave
import br.com.zupacademy.lincon.pix.TipoDeConta

fun RegistraChavePixRequest.toModel(): NovaChavePix {
  return NovaChavePix(
    clienteId = clienteId,
    tipo = when (tipoDeChave) {
      UNKNOWN_TIPO_CHAVE -> null
      else -> TipoDeChave.valueOf(tipoDeChave.name)
    },
    chave = chave,
    tipoDeConta = when (tipoDeConta) {
      UNKNOWN_TIPO_CONTA -> null
      else -> TipoDeConta.valueOf(tipoDeConta.name)
    }
  )
}