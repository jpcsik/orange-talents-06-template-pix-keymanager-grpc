package br.com.zupacademy.jpcsik.chavepix

import br.com.zupacademy.jpcsik.*
import br.com.zupacademy.jpcsik.clients.ContaResponse
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@MicronautTest(transactional = false)
internal class ListaChavesEndpointTest(
    @Inject private val repository: ChavePixRepository,
    @Inject private val grpcClient: ListarChavesServiceGrpc.ListarChavesServiceBlockingStub
) {


    @BeforeEach
    fun setUp() {
        repository.deleteAll()
    }


    //Happy Path
    @Test
    fun `Deve buscar uma lista com as chaves do cliente`() {
        //cenario
        val contaResponse = ContaResponse(
            tipo = "CONTA_POUPANCA",
            instituicao = mutableMapOf(Pair("nome", "Itau"), Pair("ispb", "60701190")),
            agencia = "00000",
            numero = "11111",
            titular = mutableMapOf(Pair("nome", "Joao"), Pair("cpf", "12312312312"))
        )

        val chavePix1 = ChavePix(
            clienteId = UUID.randomUUID().toString(),
            tipoConta = TipoConta.CONTA_POUPANCA,
            tipoChave = TipoChave.EMAIL,
            conta = contaResponse.toModel(),
            valorChave = "email@email.com"
        )

        val chavePix2 = ChavePix(
            clienteId = chavePix1.clienteId,
            tipoConta = TipoConta.CONTA_POUPANCA,
            tipoChave = TipoChave.CPF,
            conta = contaResponse.toModel(),
            valorChave = "12312312312"
        )

        repository.save(chavePix1)
        repository.save(chavePix2)

        //acao
        val request = ListaChavesRequest.newBuilder()
            .setClienteId(chavePix1.clienteId)
            .build()

        val response = grpcClient.listarChaves(request)

        //validacao
        with(response){
            assertNotNull(this)
            assertTrue(chavesList.isNotEmpty())
            assertEquals(chavesList.size, 2)
            assertEquals("email@email.com", chavesList[0].valorChave)
            assertEquals("12312312312", chavesList[1].valorChave)
            assertEquals(chavePix1.clienteId,clienteId)
        }

    }

    @Test
    fun `Deve retornar uma lista vazia caso nao encontre nenhuma chave`() {
        //cenario
        val request = ListaChavesRequest.newBuilder()
            .setClienteId(UUID.randomUUID().toString())
            .build()

        //acao
        val response = grpcClient.listarChaves(request)

        //validacao
        with(response){
            assertTrue(chavesList.isEmpty())
            assertEquals(chavesList.size, 0)
        }
    }

    //Alternative Path
    @Test
    fun `Nao deve buscar uma lista quando o id do cliente for invalido`() {
        //cenario
        val request = ListaChavesRequest.newBuilder()
            .setClienteId("client id invalido")
            .build()

        //acao
        val erro = assertThrows<StatusRuntimeException> {
            grpcClient.listarChaves(request)
        }

        //validacao
        with(erro){
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Id do cliente inválido!", status.description)
        }

    }


    @Factory
    private class ClientListar {
        @Singleton
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): ListarChavesServiceGrpc.ListarChavesServiceBlockingStub {
            return ListarChavesServiceGrpc.newBlockingStub(channel)
        }

    }
}