package br.com.zupacademy.jpcsik.chavepix

import br.com.zupacademy.jpcsik.*
import br.com.zupacademy.jpcsik.chavepix.ChavePixRepository
import br.com.zupacademy.jpcsik.chavepix.ValidadorChaveRegex
import br.com.zupacademy.jpcsik.clients.BancoCentralClient
import br.com.zupacademy.jpcsik.clients.DeletePixKeyRequest
import io.grpc.Status
import io.grpc.stub.StreamObserver
import io.micronaut.http.client.exceptions.HttpClientResponseException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoverChaveEndpoint(
    @Inject private val repository: ChavePixRepository,
    @Inject private val bancoCentralClient: BancoCentralClient
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

        when {
            //Caso nao seja encontrada retorna not found
            possivelChave.isEmpty -> {
                responseObserver.onError(
                    Status.NOT_FOUND
                        .withDescription("Chave Pix não foi encontrada para o cliente: ${request.clienteId}")
                        .asRuntimeException()
                )
            }

            possivelChave.isPresent -> {
                val pixId = possivelChave.get().pixId!!

                try{
                    //Faz requisicao para deletar chave no banco central
                    bancoCentralClient.deletaChave(pixId, DeletePixKeyRequest(pixId))

                    //Deleta a chave pix selecionada
                    repository.deleteById(request.pixId)

                    responseObserver.onNext(
                        RemoverChaveResponse.newBuilder()
                            .setMensagem("Chave Pix: ${request.pixId} , deletada com sucesso!")
                            .build()
                    )
                    responseObserver.onCompleted()

                }catch (ex: HttpClientResponseException){
                    when(ex.status.code){
                        403 -> responseObserver.onError(Status.PERMISSION_DENIED .withDescription(ex.message) .asRuntimeException())
                        404 -> responseObserver.onError(Status.NOT_FOUND .withDescription(ex.message) .asRuntimeException())
                        else -> responseObserver.onError(Status.INTERNAL .withDescription(ex.message) .asRuntimeException())
                    }
                }
            }
        }
    }
}