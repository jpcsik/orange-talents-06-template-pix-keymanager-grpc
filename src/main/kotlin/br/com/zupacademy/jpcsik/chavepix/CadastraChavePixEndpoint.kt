package br.com.zupacademy.jpcsik.chavepix

import br.com.zupacademy.jpcsik.KeyManagerGrpcServiceGrpc
import br.com.zupacademy.jpcsik.NovaChavePixRequest
import br.com.zupacademy.jpcsik.NovaChavePixResponse
import io.grpc.Status
import io.grpc.stub.StreamObserver
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CadastraChavePixEndpoint(
    @Inject val processador: ProcessadorNovaChaveRequest
) : KeyManagerGrpcServiceGrpc.KeyManagerGrpcServiceImplBase() {

    override fun cadastrar(request: NovaChavePixRequest, responseObserver: StreamObserver<NovaChavePixResponse>) {

        try {

            //Metodo que valida os dados da requisicao
            request.validarRequest()

            //Classe responsavel por processar a requisicao
            val response = processador.processar(request)

            //Resposde o client com a nova chave pix
            responseObserver.onNext(response)

            //Fecha o stream
            responseObserver.onCompleted()

            //Tratamentos para as possiveis exceptions
        } catch (ex: IllegalArgumentException) {

            responseObserver.onError(
                Status.INVALID_ARGUMENT
                    .withDescription(ex.message)
                    .asRuntimeException()
            )

        } catch (ex: IllegalStateException) {

            responseObserver.onError(
                Status.ALREADY_EXISTS
                    .withDescription(ex.message)
                    .asRuntimeException()
            )

        } catch (ex: NoSuchElementException) {

            responseObserver.onError(
                Status.NOT_FOUND
                    .withDescription(ex.message)
                    .asRuntimeException()
            )
        } catch (ex: Exception) {

            responseObserver.onError(
                Status.INTERNAL
                    .withDescription(ex.message)
                    .withCause(ex.cause)
                    .asRuntimeException()
            )

        }

    }

}
