package br.com.zupacademy.jpcsik.chavepix

import br.com.zupacademy.jpcsik.*
import br.com.zupacademy.jpcsik.clients.BancoCentralClient
import br.com.zupacademy.jpcsik.clients.CreatePixKeyRequest
import br.com.zupacademy.jpcsik.clients.CreatePixKeyResponse
import br.com.zupacademy.jpcsik.clients.DeletePixKeyRequest
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.http.HttpResponse
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@MicronautTest(transactional = false)
internal class RemoverChaveEndpointTest {

    @Inject
    private lateinit var repository: ChavePixRepository

    @field:Inject
    private lateinit var bancoCentralClient: BancoCentralClient

    @Inject
    private lateinit var grpcClient: RemoverChaveServiceGrpc.RemoverChaveServiceBlockingStub

    private lateinit var chavePix: ChavePix


    @BeforeEach
    fun setup() {
        repository.deleteAll()

        chavePix = ChavePix(
            clienteId = UUID.randomUUID().toString(),
            tipoConta = TipoConta.CONTA_CORRENTE,
            tipoChave = TipoChave.CPF,
            conta = ContaAssociada(
                instituicao = "Itau",
                agencia = "00000",
                numero = "11111",
                nomeTitular = "Joao",
                cpfTitular = "12312312312"
            ),
            valorChave = "12312312312",
        )

        repository.save(chavePix)
    }


    //Happy Path
    @Test
    fun `Deve deletar a chave pix de um cliente`() {
        //cenario
        val request = RemoverChaveRequest.newBuilder()
            .setClienteId(chavePix.clienteId)
            .setPixId(chavePix.pixId)
            .build()

        //acao
        Mockito
            .`when`(bancoCentralClient.deletaChave(request.pixId, DeletePixKeyRequest(request.pixId)))
            .thenReturn(HttpResponse.ok())

        val response = grpcClient.removerChave(request)

        //verificacao
        assertNotNull(response)
        assertTrue(repository.findById(request.pixId).isEmpty)
        assertTrue(repository.findAll().isEmpty())
        assertEquals(response.mensagem, "Chave Pix: ${request.pixId} , deletada com sucesso!")

    }

    //Alternative Path
    @Test
    fun `Nao deve deletar chave pix com pix id invalido`() {
        //cenario
        val request = RemoverChaveRequest.newBuilder()
            .setClienteId(chavePix.clienteId)
            .setPixId("qualquer formato")
            .build()

        //acao
        val erro = assertThrows<StatusRuntimeException> {
            grpcClient.removerChave(request)
        }

        //validacao
        with(erro) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Chave pix inválida!", status.description)
        }

        assertTrue(repository.findAll().size == 1)

    }

    @Test
    fun `Nao deve deletar chave pix com cliente id invalido`() {
        //cenario
        val request = RemoverChaveRequest.newBuilder()
            .setClienteId("qualquer cliente id")
            .setPixId(chavePix.pixId)
            .build()

        //acao
        val erro = assertThrows<StatusRuntimeException> {
            grpcClient.removerChave(request)
        }

        //validacao
        with(erro) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Cliente inválido!", status.description)
        }

        assertTrue(repository.findAll().size == 1)

    }

    @Test
    fun `Nao deve deletar chave pix se nao encontrar chave por cliente`() {
        //cenario
        val outraChavePix = ChavePix(
            clienteId = UUID.randomUUID().toString(),
            tipoConta = TipoConta.CONTA_CORRENTE,
            tipoChave = TipoChave.EMAIL,
            conta = ContaAssociada(
                instituicao = "Itau",
                agencia = "00000",
                numero = "11111",
                nomeTitular = "Joao",
                cpfTitular = "32132132132"
            ),
            valorChave = "email@email.com",
        )

        repository.save(outraChavePix)

        val request = RemoverChaveRequest.newBuilder()
            .setClienteId(outraChavePix.clienteId)
            .setPixId(chavePix.pixId)
            .build()

        //acao
        val erro = assertThrows<StatusRuntimeException> {
            grpcClient.removerChave(request)
        }

        //validacao
        with(erro) {
            assertEquals(Status.NOT_FOUND.code, status.code)
            assertEquals("Chave Pix não foi encontrada para o cliente: ${request.clienteId}", status.description)
        }

        assertTrue(repository.findAll().size == 2)

    }

    @Test
    fun `Nao deve deletar chave pix inexistente`(){
        //cenario
        val request = RemoverChaveRequest.newBuilder()
            .setClienteId(chavePix.clienteId)
            .setPixId(UUID.randomUUID().toString())
            .build()

        //acao
        val erro = assertThrows<StatusRuntimeException>{
            grpcClient.removerChave(request)
        }

        //validacao
        with(erro){
            assertEquals(Status.NOT_FOUND.code, status.code)
            assertEquals("Chave Pix não foi encontrada para o cliente: ${request.clienteId}", status.description)
        }

        assertTrue(repository.findAll().size == 1)

    }

    @Test
    fun `Nao deve deletar a chave pix de um cliente caso ocorra erro no servico externo`() {
        //cenario
        val request = RemoverChaveRequest.newBuilder()
            .setClienteId(chavePix.clienteId)
            .setPixId(chavePix.pixId)
            .build()

        //acao
        Mockito
            .`when`(bancoCentralClient.deletaChave(request.pixId, DeletePixKeyRequest(request.pixId)))
            .thenReturn(HttpResponse.serverError())

        val erro = assertThrows<StatusRuntimeException> {
            grpcClient.removerChave(request)
        }

        //verificacao
        with(erro){
            assertEquals(Status.INTERNAL.code, erro.status.code)
            assertEquals("Chave pix não pode ser deletada pelo Banco Central!", status.description)
        }

        assertTrue(repository.findById(chavePix.pixId!!).isPresent)

    }


    @MockBean(BancoCentralClient::class)
    fun bancoCentralMock(): BancoCentralClient {
        return Mockito.mock(BancoCentralClient::class.java)
    }


    @Factory
    private class ClientRemover {
        @Singleton
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): RemoverChaveServiceGrpc.RemoverChaveServiceBlockingStub {
            return RemoverChaveServiceGrpc.newBlockingStub(channel)
        }

    }
}