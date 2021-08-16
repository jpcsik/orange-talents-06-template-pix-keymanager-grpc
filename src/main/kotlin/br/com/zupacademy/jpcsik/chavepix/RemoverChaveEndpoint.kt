package br.com.zupacademy.jpcsik.chavepix

import br.com.zupacademy.jpcsik.*
import br.com.zupacademy.jpcsik.chavepix.ChavePixRepository
import br.com.zupacademy.jpcsik.chavepix.ValidadorChaveRegex
import io.grpc.Status
import io.grpc.stub.StreamObserver
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoverChaveEndpoint(
    @Inject private val repository: ChavePixRepository
) : RemoverChaveServiceGrpc.RemoverChaveServiceImplBase() {

    override fun removerChave(request: RemoverChaveRequest, responseObserver: StreamObserver<RemoverChaveResponse>) {

        //Verifica se os dados da requisicao sao validos
        when {
            !ValidadorChaveRegex.UUID.validar(request.clienteId) -> {
                responseObserver.onError(
                    Status.INVALID_ARGUMENT.withDescription("Cliente inválido!").asRuntimeException()
                )
            }
            !ValidadorChaveRegex.UUID.validar(request.pixId) -> {
                responseObserver.onError(
                    Status.INVALID_ARGUMENT.withDescription("Chave pix inválida!").asRuntimeException()
                )
            }
        }

        //Procura pela chave pix relacionada ao cliente
        val possivelChave = repository.findByPixIdAndClienteId(request.pixId, request.clienteId)

        //Caso nao seja encontrada retorna not found
        if (possivelChave.isEmpty) {
            responseObserver.onError(
                Status.NOT_FOUND
                    .withDescription("Chave Pix não foi encontrada para o cliente: ${request.clienteId}")
                    .asRuntimeException()
            )
        }

        //Deleta a chave pix selecionada
        repository.deleteById(request.pixId)

        //Resposta para o client
        responseObserver.onNext(RemoverChaveResponse.newBuilder()
            .setMensagem("Chave Pix: ${request.pixId} , deletada com sucesso!")
            .build())

        responseObserver.onCompleted()
    }
}

