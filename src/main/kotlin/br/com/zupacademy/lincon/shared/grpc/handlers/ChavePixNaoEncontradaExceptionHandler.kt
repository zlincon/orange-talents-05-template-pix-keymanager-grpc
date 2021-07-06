package br.com.zupacademy.lincon.shared.grpc.handlers

import br.com.zupacademy.lincon.pix.ChavePixNaoEncontradaException
import br.com.zupacademy.lincon.shared.grpc.ExceptionHandler
import br.com.zupacademy.lincon.shared.grpc.ExceptionHandler.StatusWithDetails
import io.grpc.Status
import javax.inject.Singleton

@Singleton
class ChavePixNaoEncontradaExceptionHandler :
  ExceptionHandler<ChavePixNaoEncontradaException> {
  override fun handle(exception: ChavePixNaoEncontradaException):
      StatusWithDetails {
    return StatusWithDetails(
      Status.NOT_FOUND
        .withDescription(exception.message)
        .withCause(exception)
    )
  }

  override fun supports(e: Exception): Boolean {
    return e is ChavePixNaoEncontradaException
  }
}