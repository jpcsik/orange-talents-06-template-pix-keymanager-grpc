package br.com.zupacademy.jpcsik.chavepix

import br.com.zupacademy.jpcsik.*
import br.com.zupacademy.jpcsik.clients.*
import com.google.protobuf.Timestamp
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.http.HttpResponse
import io.micronaut.http.client.exceptions.HttpClientException
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import java.time.ZoneId
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@MicronautTest(transactional = false)
internal class BuscaChavePixEndpointTest(
    @Inject private val repository: ChavePixRepository,
    @Inject private val grpcClient: BuscarChaveServiceGrpc.BuscarChaveServiceBlockingStub
) {

    @field:Inject
    private lateinit var bancoCentralClient: BancoCentralClient

    private val contaResponse = ContaResponse(
        tipo = "CONTA_POUPANCA",
        instituicao = mutableMapOf(Pair("nome", "Itau"), Pair("ispb", "60701190")),
        agencia = "00000",
        numero = "11111",
        titular = mutableMapOf(Pair("nome", "Joao"), Pair("cpf", "12312312312"))
    )

    private val chavePix = ChavePix(
        clienteId = UUID.randomUUID().toString(),
        tipoConta = TipoConta.CONTA_POUPANCA,
        tipoChave = TipoChave.EMAIL,
        conta = contaResponse.toModel(),
        valorChave = "email@email.com"
    )


    @BeforeEach
    fun setUp() {
        repository.deleteAll()
    }


    //Happy Path
    @Test
    fun `Deve buscar a chave pelo id do cliente e da chave`() {
        //cenario
        repository.save(chavePix)

        val request = BuscarChaveRequest.newBuilder()
            .setPixId(chavePix.pixId)
            .setClienteId(chavePix.clienteId)
            .build()

        //acao
        val response = grpcClient.buscarChave(request)

        //validacao
        with(response) {
            assertNotNull(this)
            assertEquals(chavePix.pixId, pixId)
            assertEquals(chavePix.valorChave, chave.chave)
            assertEquals(chavePix.conta.nomeTitular, chave.conta.nomeTitular)
        }

    }

    @Test
    fun `Deve buscar a chave pelo valor da chave`() {
        //cenario
        repository.save(chavePix)

        val request = BuscarChaveRequest.newBuilder()
            .setChave(chavePix.valorChave)
            .build()

        //acao
        val response = grpcClient.buscarChave(request)

        //validacao
        with(response) {
            assertNotNull(this)
            assertEquals(chavePix.pixId, pixId)
            assertEquals(chavePix.valorChave, chave.chave)
            assertEquals(chavePix.conta.nomeTitular, chave.conta.nomeTitular)
        }
    }

    @Test
    fun `Deve buscar chave no banco central caso a chave nao exista no sistema`() {
        //cenario
        val request = BuscarChaveRequest.newBuilder()
            .setChave(chavePix.valorChave)
            .build()

        val pixKeyDetailsResponse = PixKeyDetailsResponse(
            keyType = "EMAIL",
            key = "email@email.com",
            bankAccount = BankAccount(contaResponse),
            owner = Owner(contaResponse),
            createdAt = chavePix.criadaEm.atZone(ZoneId.of("UTC")).toInstant().let {
                Timestamp.newBuilder()
                    .setSeconds(it.epochSecond)
                    .setNanos(it.nano)
                    .build()
            }
        )

        //acao
        Mockito
            .`when`(bancoCentralClient.buscaChave(request.chave))
            .thenReturn(HttpResponse.ok(pixKeyDetailsResponse))

        val response = grpcClient.buscarChave(request)

        //validacao
        with(response) {
            assertNotNull(this)
            assertTrue(pixId.isEmpty())
            assertEquals("ITAÚ UNIBANCO S.A.", chave.conta.instituicao)
            assertEquals(chavePix.tipoConta, chave.conta.tipo)
            assertEquals(chavePix.valorChave, chave.chave)
            assertEquals(chavePix.conta.nomeTitular, chave.conta.nomeTitular)
        }


    }

    //Alternative Path
    @Test
    fun `Nao deve buscar chave caso os ids estejam em formato invalido e a chave nao esteja preenchida`() {
        //cenario
        val request = BuscarChaveRequest.newBuilder()
            .setPixId("qualquer coisa")
            .setClienteId("qualquer client")
            .build()

        //acao
        val erro = assertThrows<StatusRuntimeException> {
            grpcClient.buscarChave(request)
        }

        //validacao
        with(erro){
            assertEquals(Status.INVALID_ARGUMENT.code , status.code)
            assertEquals("Dados inválidos ou incompletos!", status.description)
        }

    }

    @Test
    fun `Deve retornar NOT_FOUND caso a chave nao seja encontrada`() {
        //cenario
        val request = BuscarChaveRequest.newBuilder()
            .setPixId(UUID.randomUUID().toString())
            .setClienteId(UUID.randomUUID().toString())
            .build()

        //acao
        val erro = assertThrows<StatusRuntimeException> {
            grpcClient.buscarChave(request)
        }

        //validacao
        with(erro){
            assertEquals(Status.NOT_FOUND.code , status.code)
            assertEquals("chave nao encontrada", status.description)
        }
    }

    @Test
    fun `Deve retornar INTERNAL caso ocorra erro ao buscar a chave no banco central`() {
        //cenario
        repository.save(chavePix)

        val request = BuscarChaveRequest.newBuilder()
            .setChave("qualquer chave")
            .build()

        //acao
        Mockito
            .`when`(bancoCentralClient.buscaChave(request.chave))
            .thenThrow(HttpClientException(""))

        val erro = assertThrows<StatusRuntimeException> {
            grpcClient.buscarChave(request)
        }

        //validacao
        with(erro){
            assertEquals(Status.INTERNAL.code , status.code)
            assertEquals("Erro ao buscar chave pix no Banco Central!", status.description)
        }
    }

    @Test
    fun `Deve retornar UNAVAILABLE caso erro inesperado ocorra`() {
        //cenario
        val request = BuscarChaveRequest.newBuilder()
            .setChave("qualquer chave")
            .build()

        //acao
        Mockito
            .`when`(bancoCentralClient.buscaChave(request.chave))
            .thenThrow(IllegalStateException(""))

        val erro = assertThrows<StatusRuntimeException> {
            grpcClient.buscarChave(request)
        }

        //validacao
        with(erro){
            assertEquals(Status.UNAVAILABLE.code , status.code)
        }
    }

    @Test
    fun `Deve lancar exception caso a chave nao seja encontrada nem no sistema nem no banco central`(){
        //cenario
        val request = BuscarChaveRequest.newBuilder()
            .setChave("qualquer chave")
            .build()

        //acao
        Mockito
            .`when`(bancoCentralClient.buscaChave(request.chave))
            .thenReturn(HttpResponse.notFound())

        val erro = assertThrows<IllegalArgumentException> {
            BuscaChavePixEndpoint(repository, bancoCentralClient).respondePorChave(request.chave)
        }

        //validacao
        with(erro){
            assertEquals("chave nao encontrada", message)
        }

    }


    @MockBean(BancoCentralClient::class)
    fun bancoCentralMock(): BancoCentralClient {
        return Mockito.mock(BancoCentralClient::class.java)
    }


    @Factory
    private class ClientBuscar {
        @Singleton
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): BuscarChaveServiceGrpc.BuscarChaveServiceBlockingStub {
            return BuscarChaveServiceGrpc.newBlockingStub(channel)
        }

    }
}