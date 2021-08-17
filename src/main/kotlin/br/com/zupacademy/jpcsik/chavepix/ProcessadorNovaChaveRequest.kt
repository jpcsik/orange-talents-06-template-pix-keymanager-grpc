package br.com.zupacademy.jpcsik.chavepix

import br.com.zupacademy.jpcsik.NovaChavePixRequest
import br.com.zupacademy.jpcsik.NovaChavePixResponse
import br.com.zupacademy.jpcsik.clients.BancoCentralClient
import br.com.zupacademy.jpcsik.clients.ContaResponse
import br.com.zupacademy.jpcsik.clients.CreatePixKeyRequest
import br.com.zupacademy.jpcsik.clients.CreatePixKeyResponse
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientException
import javax.inject.Inject
import javax.inject.Singleton
import javax.transaction.Transactional
import kotlin.jvm.Throws

@Singleton
open class ProcessadorNovaChaveRequest(
    @Inject private val repository: ChavePixRepository
) {

    @Throws(IllegalStateException::class)
    @Transactional
    open fun processar(
        request: NovaChavePixRequest,
        contaResponse: ContaResponse,
        bancoCentralResponse: HttpResponse<CreatePixKeyResponse>
    ): NovaChavePixResponse {
        //Chave aleatoria gerada pelo serviço externo
        val key = bancoCentralResponse.body()!!.key

        //Cria uma nova chave pix
        val novaChave = request.toModel(contaResponse.toModel(), key)

        //Verifica se chave já existe
        repository.existsByValorChave(novaChave.valorChave)
            .let { if (it) throw IllegalStateException("Chave já cadastrada!") }

        //Salva chave no banco de dados
        repository.save(novaChave)

        //Cria a resposta
        return NovaChavePixResponse.newBuilder()
            .setPixId(novaChave.pixId)
            .build()

    }

}

