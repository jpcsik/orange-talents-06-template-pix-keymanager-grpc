package br.com.zupacademy.jpcsik.chavepix

import br.com.zupacademy.jpcsik.NovaChavePixRequest
import br.com.zupacademy.jpcsik.NovaChavePixResponse
import io.micronaut.http.HttpResponse
import javax.inject.Inject
import javax.inject.Singleton
import javax.transaction.Transactional

@Singleton
open class ProcessadorNovaChaveRequest(
    @Inject private val repository: ChavePixRepository
) {

    @Throws(
        IllegalStateException::class,
        IllegalArgumentException::class,
        NoSuchElementException::class
    )
    @Transactional
    open fun processar(request: NovaChavePixRequest, contaResponse: HttpResponse<ContaResponse>): NovaChavePixResponse {

        //Cria conta para ser associada a chave pix
        val conta = contaResponse.body()?.toModel() ?: throw NoSuchElementException("Cliente não encontrado!")

        //Cria uma nova chave pix
        val novaChave = request.toModel(conta)

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
