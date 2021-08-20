package br.com.zupacademy.jpcsik.chavepix

import br.com.zupacademy.jpcsik.*
import br.com.zupacademy.jpcsik.clients.BancoCentralClient
import br.com.zupacademy.jpcsik.clients.ItauClient
import io.grpc.Status
import io.grpc.stub.StreamObserver
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CadastraChavePixEndpoint(
    @Inject private val processador: ProcessadorNovaChave,
    @Inject private val client: ItauClient,
    @Inject private val bancoCentral: BancoCentralClient
) : CadastrarChaveServiceGrpc.CadastrarChaveServiceImplBase() {

    override fun cadastrar(request: NovaChavePixRequest, responseObserver: StreamObserver<NovaChavePixResponse>) {
        try {
            request.validarRequest()

            //Classe responsavel por processar nova chave pix
            processador.processar(request).let { responseObserver.onNext(it) }
            responseObserver.onCompleted()

        //Tratamentos para as possiveis exceptions
        } catch (ex: Exception) {
            when (ex) {
                is IllegalArgumentException -> responseObserver.onError(
                    Status.INVALID_ARGUMENT.withDescription(ex.message).asRuntimeException()
                )
                is IllegalAccessException -> responseObserver.onError(
                    Status.ALREADY_EXISTS.withDescription(ex.message).asRuntimeException()
                )
                is NoSuchElementException -> responseObserver.onError(
                    Status.NOT_FOUND.withDescription(ex.message).asRuntimeException()
                )
                is IllegalStateException -> responseObserver.onError(
                    Status.INTERNAL.withDescription(ex.message).asRuntimeException()
                )

                else -> responseObserver.onError(Status.UNAVAILABLE.withDescription(ex.message).asRuntimeException())
            }
        }
    }

}
