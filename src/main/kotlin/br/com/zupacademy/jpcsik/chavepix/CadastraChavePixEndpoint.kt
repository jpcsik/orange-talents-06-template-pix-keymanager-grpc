package br.com.zupacademy.jpcsik.chavepix

import br.com.zupacademy.jpcsik.KeyManagerGrpcServiceGrpc
import br.com.zupacademy.jpcsik.NovaChavePixRequest
import br.com.zupacademy.jpcsik.NovaChavePixResponse
import br.com.zupacademy.jpcsik.clients.ItauClient
import io.grpc.Status
import io.grpc.stub.StreamObserver
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CadastraChavePixEndpoint(
    @Inject private val processador: ProcessadorNovaChaveRequest,
    @Inject private val client: ItauClient
) : KeyManagerGrpcServiceGrpc.KeyManagerGrpcServiceImplBase() {

    override fun cadastrar(request: NovaChavePixRequest, responseObserver: StreamObserver<NovaChavePixResponse>) {

        try {

            //Metodo que valida os dados da requisicao
            request.validarRequest()

            //Faz a requisicao para capturar os dados da conta no servico externo
            val contaResponse = client.consultaContas(request.clienteId, request.tipoConta.name)

            //Classe responsavel por processar a requisicao
            val response = processador.processar(request, contaResponse)

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
                    .withDescription("Erro interno do servidor!")
                    .asRuntimeException()
            )

        }

    }

}
