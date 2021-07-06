package br.com.zupacademy.lincon.pix.remove

import br.com.zupacademy.lincon.KeymanagerRemoveServiceGrpc
import br.com.zupacademy.lincon.RemoveChavePixRequest
import br.com.zupacademy.lincon.RemoveChavePixResponse
import br.com.zupacademy.lincon.shared.grpc.ErrorHandler
import io.grpc.stub.StreamObserver
import javax.inject.Inject
import javax.inject.Singleton

@ErrorHandler
@Singleton
class RemoveChaveEndpoint(@Inject private val service: RemoveChaveService) :
  KeymanagerRemoveServiceGrpc.KeymanagerRemoveServiceImplBase() {
  override fun remove(
    request: RemoveChavePixRequest,
    responseObserver: StreamObserver<RemoveChavePixResponse>
  ) {
    service.remove(clientId = request.clienteId, pixId = request.pixId)

    responseObserver.onNext(
      RemoveChavePixResponse.newBuilder()
        .setClienteId(request.clienteId)
        .setPixId(request.pixId)
        .build()
    )

    responseObserver.onCompleted()
  }
}