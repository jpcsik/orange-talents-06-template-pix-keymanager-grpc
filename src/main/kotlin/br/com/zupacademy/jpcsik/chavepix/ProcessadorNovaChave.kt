package br.com.zupacademy.jpcsik.chavepix

import br.com.zupacademy.jpcsik.NovaChavePixRequest
import br.com.zupacademy.jpcsik.NovaChavePixResponse
import br.com.zupacademy.jpcsik.clients.BancoCentralClient
import br.com.zupacademy.jpcsik.clients.CreatePixKeyRequest
import br.com.zupacademy.jpcsik.clients.ItauClient
import io.micronaut.http.HttpStatus
import javax.inject.Inject
import javax.inject.Singleton
import javax.transaction.Transactional

@Singleton
open class ProcessadorNovaChave(
    @Inject private val repository: ChavePixRepository,
    @Inject private val client: ItauClient,
    @Inject private val bancoCentral: BancoCentralClient
) {

    @Throws(IllegalStateException::class, NoSuchElementException::class, IllegalAccessException::class)
    @Transactional
    open fun processar(
        request: NovaChavePixRequest
    ): NovaChavePixResponse {

        //Faz a requisicao para capturar os dados da conta no servico externo
        val contaResponse = client.consultaContas(request.clienteId, request.tipoConta.name)
        val contaBody = contaResponse.body() ?: throw NoSuchElementException("Conta não foi encontrada!")

        //Cria uma nova chave pix
        val novaChave = request.toModel(contaBody.toModel())

        //Verifica se chave já existe
        repository.existsByValorChave(novaChave.valorChave)
            .let { if (it && novaChave.valorChave != "SEM_VALOR") throw IllegalAccessException("Chave já cadastrada!") }

        //Verifica se existe ja existe uma chave para o tipo
        repository.existsByClienteIdAndTipoChave(novaChave.clienteId, novaChave.tipoChave)
            .let { if(it) throw IllegalAccessException("Já existe uma chave para esse tipo!") }

        //Salva chave no banco de dados
        repository.save(novaChave)

        //Cria chave pix no servico externo do banco central
        val bancoCentralResponse = bancoCentral.criarChave(CreatePixKeyRequest(contaBody, request))
        if (bancoCentralResponse.status != HttpStatus.CREATED) throw IllegalStateException("Erro ao registrar chave pix no Banco Central!")

        //Chave aleatoria gerada pelo serviço externo
        val key = bancoCentralResponse.body()!!.key

        //Atualiza chave aleatoria com valor gerado pelo servico externo
        if (novaChave.tipoChave.number == 4) {
            novaChave.valorChave = key
            repository.save(novaChave)
        }

        //Cria a resposta
        return NovaChavePixResponse.newBuilder()
            .setPixId(novaChave.pixId)
            .build()

    }

}

