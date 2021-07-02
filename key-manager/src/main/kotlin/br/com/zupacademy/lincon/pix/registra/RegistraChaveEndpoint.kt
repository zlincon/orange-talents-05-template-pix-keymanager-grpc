package br.com.zupacademy.lincon.pix.registra

import br.com.zupacademy.lincon.*
import io.grpc.stub.StreamObserver
import javax.inject.Singleton

@Singleton
class RegistraChaveEndpoint(private val service: NovaChavePixService) :
  KeyManagerRegistraServiceGrpc.KeyManagerRegistraServiceImplBase() {
  override fun registra(
    request: RegistraChavePixRequest?,
    responseObserver: StreamObserver<RegistraChavePixResponse>?
  ) {
    val novaChave = request.toModel()
    val chaveCriada = service.registra(novaChave)

    responseObserver.onNext(
      RegistraChavePixResponse.newBuilder()
        .setClienteId(chaveCriada.clienteId.toString())
        .setPixId(chaveCriada.id.toString())
        .build()
    )
    responseObserver.onCompleted()
  }
}