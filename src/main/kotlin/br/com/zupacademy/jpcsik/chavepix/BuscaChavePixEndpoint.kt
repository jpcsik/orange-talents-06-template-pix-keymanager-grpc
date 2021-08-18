package br.com.zupacademy.jpcsik.chavepix

import br.com.zupacademy.jpcsik.BuscarChaveRequest
import br.com.zupacademy.jpcsik.BuscarChaveResponse
import br.com.zupacademy.jpcsik.BuscarChaveServiceGrpc
import br.com.zupacademy.jpcsik.clients.BancoCentralClient
import io.grpc.Status
import io.grpc.stub.StreamObserver
import io.micronaut.http.client.exceptions.HttpClientException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BuscaChavePixEndpoint(
    @Inject val repository: ChavePixRepository,
    @Inject val bancoCentralClient: BancoCentralClient
) : BuscarChaveServiceGrpc.BuscarChaveServiceImplBase() {

    override fun buscarChave(request: BuscarChaveRequest, responseObserver: StreamObserver<BuscarChaveResponse>) {

        try {
            if (ValidadorChaveRegex.UUID.validar(request.pixId) && ValidadorChaveRegex.UUID.validar(request.clienteId)) {

                respondePorId(request.pixId, request.clienteId).let {
                    responseObserver.onNext(it)
                    responseObserver.onCompleted()
                }

            } else if (request.chave.isNotBlank()) {

                respondePorChave(request.chave).let {
                    responseObserver.onNext(it)
                    responseObserver.onCompleted()
                }

            }

            else responseObserver.onError(Status.INVALID_ARGUMENT .withDescription("Dados inválidos ou incompletos!") .asRuntimeException())

        } catch (ex: Exception) {
            when (ex) {
                is IllegalArgumentException -> responseObserver.onError(
                    Status.NOT_FOUND.withDescription(ex.message).asRuntimeException()
                )
                is HttpClientException -> responseObserver.onError(
                    Status.INTERNAL.withDescription("Erro ao buscar chave pix no Banco Central!").asRuntimeException()
                )
                else -> responseObserver.onError(Status.UNAVAILABLE.withDescription(ex.message).asRuntimeException())
            }
        }

    }

}
