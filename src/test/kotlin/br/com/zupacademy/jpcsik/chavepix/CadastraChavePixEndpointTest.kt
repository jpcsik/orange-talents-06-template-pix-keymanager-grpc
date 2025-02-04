package br.com.zupacademy.jpcsik.chavepix

import br.com.zupacademy.jpcsik.*
import br.com.zupacademy.jpcsik.clients.*
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
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@MicronautTest(transactional = false)
internal class CadastraChavePixEndpointTest(
    @Inject private val repository: ChavePixRepository,
    @Inject private val grpcClient: CadastrarChaveServiceGrpc.CadastrarChaveServiceBlockingStub
) {

    @field:Inject
    private lateinit var itauClient: ItauClient

    @field:Inject
    private lateinit var bancoCentralClient: BancoCentralClient

    private val contaResponsePadrao = ContaResponse(
        tipo = "CONTA_CORRENTE",
        instituicao = mutableMapOf(Pair("nome", "Itau"), Pair("ispb", "60701190")),
        agencia = "00000",
        numero = "11111",
        titular = mutableMapOf(Pair("nome", "Joao"), Pair("cpf", "12312312312"))
    )

    private val requestPadrao = NovaChavePixRequest.newBuilder()
        .setClienteId(UUID.randomUUID().toString())
        .setTipoChave(TipoChave.CPF)
        .setValorChave("12312312312")
        .setTipoConta(TipoConta.CONTA_CORRENTE)
        .build()


    @BeforeEach
    fun setup() {
        repository.deleteAll()
    }


    //Happy Path
    @Test
    fun `Deve cadastrar uma nova chave pix`() {
        //cenario
        val requestPadrao = NovaChavePixRequest.newBuilder()
            .setClienteId(UUID.randomUUID().toString())
            .setTipoChave(TipoChave.ALEATORIA)
            .setValorChave("")
            .setTipoConta(TipoConta.CONTA_CORRENTE)
            .build()

        //acao
        Mockito
            .`when`(itauClient.consultaContas(requestPadrao.clienteId, requestPadrao.tipoConta.name))
            .thenReturn(HttpResponse.ok(contaResponsePadrao))

        Mockito
            .`when`(bancoCentralClient.criarChave(CreatePixKeyRequest(contaResponsePadrao, requestPadrao)))
            .thenReturn(HttpResponse.created(CreatePixKeyResponse(UUID.randomUUID().toString())))

        val response = grpcClient.cadastrar(requestPadrao)

        //validacao
        with(response) {
            assertNotNull(this)
            assertTrue(repository.findById(pixId).isPresent)
            assertTrue(repository.findAll().size == 1)
        }

    }

    //Alternative Path
    @Test
    fun `Nao deve cadastrar chave pix com dados invalidos`() {
        //cenario
        val request = NovaChavePixRequest.newBuilder()
            .setClienteId(UUID.randomUUID().toString())
            .setTipoChave(TipoChave.ALEATORIA)
            .setValorChave("3213n123h12jhds")
            .setTipoConta(TipoConta.CONTA_CORRENTE)
            .build()

        //acao
        Mockito
            .`when`(itauClient.consultaContas(request.clienteId, request.tipoConta.name))
            .thenReturn(HttpResponse.ok(contaResponsePadrao))

        val erro = assertThrows<StatusRuntimeException> {
            grpcClient.cadastrar(request)
        }

        //verificacao
        with(erro) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Valor não deve ser preenchido!", status.description)
        }

        assertTrue(repository.findAll().isEmpty())

    }

    @Test
    fun `Nao deve cadastrar chave pix com email invalido`() {
        //cenario
        val request = NovaChavePixRequest.newBuilder()
            .setClienteId(UUID.randomUUID().toString())
            .setTipoChave(TipoChave.EMAIL)
            .setValorChave("emailinvalido")
            .setTipoConta(TipoConta.CONTA_CORRENTE)
            .build()

        //acao
        Mockito
            .`when`(itauClient.consultaContas(request.clienteId, request.tipoConta.name))
            .thenReturn(HttpResponse.ok(contaResponsePadrao))

        val erro = assertThrows<StatusRuntimeException> {
            grpcClient.cadastrar(request)
        }

        //verificacao
        with(erro) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Email inválido!", status.description)
        }

        assertTrue(repository.findAll().isEmpty())

    }

    @Test
    fun `Nao deve cadastrar chave pix com telefone invalido`() {
        //cenario
        val request = NovaChavePixRequest.newBuilder()
            .setClienteId(UUID.randomUUID().toString())
            .setTipoChave(TipoChave.TELEFONE)
            .setValorChave("telefoneinvalido")
            .setTipoConta(TipoConta.CONTA_CORRENTE)
            .build()

        //acao
        Mockito
            .`when`(itauClient.consultaContas(request.clienteId, request.tipoConta.name))
            .thenReturn(HttpResponse.ok(contaResponsePadrao))

        val erro = assertThrows<StatusRuntimeException> {
            grpcClient.cadastrar(request)
        }

        //verificacao
        with(erro) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Numero de telefone inválido!", status.description)
        }

        assertTrue(repository.findAll().isEmpty())

    }

    @Test
    fun `Nao deve cadastrar chave pix caso chave ja exista`() {
        //cenario
        repository.save(
            ChavePix(
                clienteId = UUID.randomUUID().toString(),
                tipoConta = TipoConta.CONTA_CORRENTE,
                tipoChave = TipoChave.CPF,
                conta = contaResponsePadrao.toModel(),
                valorChave = "12312312312"
            )
        )

        //acao
        Mockito
            .`when`(itauClient.consultaContas(requestPadrao.clienteId, requestPadrao.tipoConta.name))
            .thenReturn(HttpResponse.ok(contaResponsePadrao))

        val erro = assertThrows<StatusRuntimeException> {
            grpcClient.cadastrar(requestPadrao)
        }

        //validacao
        with(erro) {
            assertEquals(Status.ALREADY_EXISTS.code, status.code)
            assertEquals("Chave já cadastrada!", status.description)
        }

        assertTrue(repository.findAll().size == 1)

    }

    @Test
    fun `Nao deve cadastrar caso conta nao seja encontrada`() {
        //acao
        Mockito
            .`when`(itauClient.consultaContas(requestPadrao.clienteId, requestPadrao.tipoConta.name))
            .thenReturn(HttpResponse.notFound())

        val erro = assertThrows<StatusRuntimeException> {
            grpcClient.cadastrar(requestPadrao)
        }

        //validacao
        with(erro){
            assertEquals(Status.NOT_FOUND.code, status.code)
            assertEquals("Conta não foi encontrada!", status.description)
        }

        assertTrue(repository.findAll().isEmpty())

    }

    @Test
    fun `Nao deve cadastrar caso ocorra erro no client`() {
        //acao
        Mockito
            .`when`(itauClient.consultaContas(requestPadrao.clienteId, requestPadrao.tipoConta.name))
            .thenReturn(HttpResponse.ok(contaResponsePadrao))

        Mockito
            .`when`(bancoCentralClient.criarChave(CreatePixKeyRequest(contaResponsePadrao, requestPadrao)))
            .thenReturn(HttpResponse.unprocessableEntity())

        val erro = assertThrows<StatusRuntimeException> {
            grpcClient.cadastrar(requestPadrao)
        }

        //validacao
        with(erro){
            assertEquals(Status.INTERNAL.code, status.code)
            assertEquals("Erro ao registrar chave pix no Banco Central!", status.description)
        }

        assertTrue(repository.findAll().isEmpty())

    }

    @Test
    fun `Nao deve cadastrar uma nova chave pix caso servidor esteja indiponivel`() {
        //cenario
        val requestPadrao = NovaChavePixRequest.newBuilder()
            .setClienteId(UUID.randomUUID().toString())
            .setTipoChave(TipoChave.ALEATORIA)
            .setValorChave("")
            .setTipoConta(TipoConta.CONTA_CORRENTE)
            .build()

        //acao
        Mockito
            .`when`(itauClient.consultaContas(requestPadrao.clienteId, requestPadrao.tipoConta.name))
            .thenReturn(HttpResponse.ok(contaResponsePadrao))

        Mockito
            .`when`(bancoCentralClient.criarChave(CreatePixKeyRequest(contaResponsePadrao, requestPadrao)))
            .thenThrow(HttpClientException("Servidor indisponivel!"))

        val erro = assertThrows<StatusRuntimeException> {
            grpcClient.cadastrar(requestPadrao)
        }

        //validacao
        with(erro) {
            assertEquals(Status.UNAVAILABLE.code, status.code)
            assertEquals("Servidor indisponivel!", status.description)
        }

        assertTrue(repository.findAll().isEmpty())

    }


    @MockBean(ItauClient::class)
    fun itauMock(): ItauClient {
        return Mockito.mock(ItauClient::class.java)
    }


    @MockBean(BancoCentralClient::class)
    fun bancoCentralMock(): BancoCentralClient {
        return Mockito.mock(BancoCentralClient::class.java)
    }


    @Factory
    private class ClientCadastrar {
        @Singleton
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): CadastrarChaveServiceGrpc.CadastrarChaveServiceBlockingStub {
            return CadastrarChaveServiceGrpc.newBlockingStub(channel)
        }

    }

}
