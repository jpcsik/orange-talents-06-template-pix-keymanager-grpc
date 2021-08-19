package br.com.zupacademy.jpcsik.chavepix

import br.com.zupacademy.jpcsik.BuscarChaveResponse
import br.com.zupacademy.jpcsik.TipoChave
import br.com.zupacademy.jpcsik.TipoConta
import br.com.zupacademy.jpcsik.clients.PixKeyDetailsResponse
import com.google.protobuf.Timestamp
import io.micronaut.http.HttpStatus
import java.time.ZoneId

//Caso nao ache a chave no banco de dados esse metodo tenta procurar a chave no sistema externo do banco central
fun BuscaChavePixEndpoint.respondePorChave(chave: String): BuscarChaveResponse {

    val possivelChave = repository.findByValorChave(chave)

    return when {
        possivelChave.isPresent -> {
            respostaComChaveNoSistema(possivelChave.get())
        }
        else -> {
            val buscaChaveResponse = bancoCentralClient.buscaChave(chave)

            if(buscaChaveResponse.status == HttpStatus.OK){
                respostaComChaveNoBCB(buscaChaveResponse.body()!!)
            }else throw IllegalArgumentException("chave nao encontrada")
        }
    }

}

//Caso nao ache a chave ou a chave nao tenha valor esse metodo joga uma exception para ser tratada no endpoint
fun BuscaChavePixEndpoint.respondePorId(pixId: String, clienteId: String): BuscarChaveResponse {

    val possivelChave = repository.findByPixIdAndClienteId(pixId, clienteId)

    if(possivelChave.isPresent && possivelChave.get().valorChave != "SEM_VALOR"){
        return respostaComChaveNoSistema(possivelChave.get())
    }else throw IllegalArgumentException("chave nao encontrada")

}

//Cria o objeto de response a partir de uma chave pix do sistema
fun respostaComChaveNoSistema(chavePix: ChavePix): BuscarChaveResponse {

    return BuscarChaveResponse.newBuilder()
        .setPixId(chavePix.pixId)
        .setClientId(chavePix.clienteId)
        .setChave(BuscarChaveResponse.ChavePix.newBuilder()
            .setTipo(chavePix.tipoChave)
            .setChave(chavePix.valorChave)
            .setCriadaEm(chavePix.criadaEm.atZone(ZoneId.of("UTC")).toInstant().let { Timestamp.newBuilder()
                .setSeconds(it.epochSecond)
                .setNanos(it.nano)
                .build()})
            .setConta(BuscarChaveResponse.DadosConta.newBuilder()
                .setInstituicao(chavePix.conta.instituicao)
                .setAgencia(chavePix.conta.agencia)
                .setNumero(chavePix.conta.numero)
                .setTipo(chavePix.tipoConta)
                .setNomeTitular(chavePix.conta.nomeTitular)
                .setCpfDoTitular(chavePix.conta.cpfTitular)
                .build()
            )
        )
        .build()
}

//Cria o objeto de resposta a partir da resposta do sistema externo do banco central
fun respostaComChaveNoBCB(pixKeyDetailsResponse: PixKeyDetailsResponse): BuscarChaveResponse {

    return BuscarChaveResponse.newBuilder()
        .setChave(BuscarChaveResponse.ChavePix.newBuilder()
            .setTipo(when(pixKeyDetailsResponse.keyType){
                "CPF" -> TipoChave.CPF
                "PHONE" -> TipoChave.TELEFONE
                "EMAIL" -> TipoChave.EMAIL
                else -> TipoChave.ALEATORIA
            })
            .setChave(pixKeyDetailsResponse.key)
            .setCriadaEm(pixKeyDetailsResponse.createdAt)
            .setConta(BuscarChaveResponse.DadosConta.newBuilder()
                .setInstituicao("ITAÚ UNIBANCO S.A.")
                .setAgencia(pixKeyDetailsResponse.bankAccount.branch)
                .setNumero(pixKeyDetailsResponse.bankAccount.accountNumber)
                .setTipo(when(pixKeyDetailsResponse.bankAccount.accountType){
                    "CACC" -> TipoConta.CONTA_CORRENTE
                    else -> TipoConta.CONTA_POUPANCA
                })
                .setNomeTitular(pixKeyDetailsResponse.owner.name)
                .setCpfDoTitular(pixKeyDetailsResponse.owner.taxIdNumber)
                .build()
            )
        )
        .build()
}
