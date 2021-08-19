package br.com.zupacademy.jpcsik.chavepix

import br.com.zupacademy.jpcsik.ListaChavesRequest
import br.com.zupacademy.jpcsik.ListaChavesResponse
import br.com.zupacademy.jpcsik.ListarChavesServiceGrpc
import com.google.protobuf.Timestamp
import io.grpc.Status
import io.grpc.stub.StreamObserver
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListaChavesEndpoint(
    @Inject private val repository: ChavePixRepository
) : ListarChavesServiceGrpc.ListarChavesServiceImplBase() {

    override fun listarChaves(request: ListaChavesRequest, responseObserver: StreamObserver<ListaChavesResponse>) {

        //Valida o id do cliente que vem na request
        if (!ValidadorChaveRegex.UUID.validar(request.clienteId)) {
            responseObserver.onError(
                Status.INVALID_ARGUMENT.withDescription("Id do cliente inválido!").asRuntimeException()
            )
        }

        //Mapeia as chaves encontradas para o clientId informado
        val chaves = repository.findAllByClienteId(request.clienteId).map {
            ListaChavesResponse.ChavePix.newBuilder()
                .setPixId(it.pixId)
                .setTipoChave(it.tipoChave)
                .setValorChave(it.valorChave)
                .setTipoConta(it.tipoConta)
                .setCriadaEm(it.criadaEm.atZone(ZoneId.of("UTC")).toInstant().let { data ->
                    Timestamp.newBuilder()
                        .setSeconds(data.epochSecond)
                        .setNanos(data.nano)
                        .build()
                })
                .build()
        }

        //Cria o objeto de resposta adicionando as chaves encontradas
        responseObserver.onNext(
            ListaChavesResponse.newBuilder()
                .setClienteId(request.clienteId)
                .addAllChaves(chaves)
                .build()
        )

        responseObserver.onCompleted()

    }

}