package br.com.zupacademy.lincon.shared.grpc.handlers

import br.com.zupacademy.lincon.shared.grpc.ExceptionHandler
import br.com.zupacademy.lincon.shared.grpc.ExceptionHandler.StatusWithDetails
import io.grpc.Status
import io.micronaut.context.MessageSource
import org.hibernate.exception.ConstraintViolationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataIntegrityExceptionHandler(@Inject var messageSource: MessageSource) :
  ExceptionHandler<ConstraintViolationException> {
  override fun handle(exception: ConstraintViolationException): StatusWithDetails {
    val constraintName = exception.constraintName
    if (constraintName.isNullOrBlank()) {
      return internalServerError(exception)
    }
    val message = messageSource.getMessage("data.integrity.error" +
        ".$constraintName", MessageSource.MessageContext.DEFAULT)
    return message.map { alreadyExistsError(it, exception) }
      .orElse(internalServerError(exception))
  }

  override fun supports(e: Exception): Boolean {
    return e is ConstraintViolationException
  }

  private fun alreadyExistsError(message: String?, exception:
  ConstraintViolationException) = StatusWithDetails(Status.ALREADY_EXISTS
    .withDescription("Unexpected internal server error")
    .withCause(exception))

  private fun internalServerError(e: ConstraintViolationException) =
    StatusWithDetails(
      Status.INTERNAL.withDescription(
        "Unexpected internal " +
            "error"
      )
        .withCause(e)
    )


}