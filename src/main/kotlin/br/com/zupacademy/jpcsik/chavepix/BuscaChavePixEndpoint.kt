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
            //Valida se request veio com os ids validos
            if (ValidadorChaveRegex.UUID.validar(request.pixId) && ValidadorChaveRegex.UUID.validar(request.clienteId)) {

                //Metodo que faz as devidas verificacoes e cria a resposta
                respondePorId(request.pixId, request.clienteId).let {
                    responseObserver.onNext(it)
                    responseObserver.onCompleted()
                }

            //Valida se request veio com a chave, caso nao tenha ids
            } else if (request.chave.isNotBlank()) {

                //Metodo que faz as devidas verificacoes e cria a resposta
                respondePorChave(request.chave).let {
                    responseObserver.onNext(it)
                    responseObserver.onCompleted()
                }

            }

            //Responde com um erro caso nada venha preenchido na request ou os dados sejam invalidos
            else responseObserver.onError(Status.INVALID_ARGUMENT .withDescription("Dados inválidos ou incompletos!") .asRuntimeException())

        //Trata as possiveis exceptions esperadas pelo sistema
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
