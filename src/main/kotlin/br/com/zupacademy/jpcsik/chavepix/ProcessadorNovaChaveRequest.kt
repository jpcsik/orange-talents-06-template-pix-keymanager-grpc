package br.com.zupacademy.jpcsik.chavepix

import br.com.zupacademy.jpcsik.NovaChavePixRequest
import br.com.zupacademy.jpcsik.NovaChavePixResponse
import br.com.zupacademy.jpcsik.clients.ItauClient
import javax.inject.Inject
import javax.inject.Singleton
import javax.transaction.Transactional
import javax.validation.ConstraintViolationException

@Singleton
open class ProcessadorNovaChaveRequest(
    @Inject val repository: ChavePixRepository,
    @Inject val client: ItauClient
) {

    @Throws(
        IllegalStateException::class,
        IllegalArgumentException::class,
        NoSuchElementException::class
    )
    @Transactional
    open fun processar(request: NovaChavePixRequest): NovaChavePixResponse {

        //Faz a requisicao para capturar os dados da conta no servico externo
        val response = client.consultaContas(request.clienteId, request.tipoConta.name)

        //Cria conta para ser associada a chave pix
        val conta = response.body()?.toModel() ?: throw NoSuchElementException("Cliente não encontrado!")

        //Criar uma nova chave pix
        val novaChave = request.toModel(conta)

        //Verifica se chave já existe
        repository.existsByValorChave(novaChave.valorChave)
            .let { if (it) throw IllegalStateException("Chave já cadastrada!") }

        //Salva chave no banco de dados
        try {
            repository.save(novaChave)
        } catch (ex: ConstraintViolationException) {
            throw IllegalArgumentException(ex.message)
        }

        //Cria a resposta
        return NovaChavePixResponse.newBuilder()
            .setPixId(novaChave.pixId)
            .build()
    }

}
