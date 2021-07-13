package br.com.zupacademy.lincon.carrega

import br.com.zupacademy.lincon.CarregaChavePixRequest
import br.com.zupacademy.lincon.CarregaChavePixRequest.FiltroCase.*
import br.com.zupacademy.lincon.CarregaChavePixResponse
import br.com.zupacademy.lincon.KeymanagerCarregaServiceGrpc
import br.com.zupacademy.lincon.integration.BancoCentralClient
import br.com.zupacademy.lincon.pix.registra.ChavePixRepository
import br.com.zupacademy.lincon.shared.grpc.ErrorHandler
import io.grpc.stub.StreamObserver
import javax.inject.Inject
import javax.inject.Singleton
import javax.validation.ConstraintViolationException
import javax.validation.Validator

@ErrorHandler
@Singleton
class CarregaChaveEndpoint(
    @Inject private val repository: ChavePixRepository,
    @Inject private val bcbClient: BancoCentralClient,
    @Inject private val validator: Validator
) : KeymanagerCarregaServiceGrpc.KeymanagerCarregaServiceImplBase() {
    override fun carrega(
        request: CarregaChavePixRequest,
        responseObserver: StreamObserver<CarregaChavePixResponse>
    ) {
        val filtro = request.toModel(validator)
        val chaveInfo = filtro.filtra(repository = repository, bcbClient = bcbClient)

        responseObserver.onNext(CarregaChavePixResponseConverter().convert(chaveInfo))
        responseObserver.onCompleted()
    }
}

private fun CarregaChavePixRequest.toModel(validator: Validator): Filtro {
    val filtro = when(filtroCase!!) {
        PIXID -> pixId.let {
            Filtro.PorPixId(clienteId = it.clienteId, pixId = it.pixId)
        }
        CHAVE -> Filtro.PorChave(chave)
        FILTRO_NOT_SET -> Filtro.Invalido()
    }

    val violations = validator.validate(filtro)
    if(violations.isNotEmpty()) {
        throw ConstraintViolationException(violations)
    }

    return filtro
}
