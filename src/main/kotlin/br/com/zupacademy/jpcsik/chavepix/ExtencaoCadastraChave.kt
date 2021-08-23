package br.com.zupacademy.jpcsik.chavepix

import br.com.zupacademy.jpcsik.NovaChavePixRequest

//Valida os diversos cenarios da request
@Throws(IllegalArgumentException::class)
fun NovaChavePixRequest.validarRequest() {

    if (this.clienteId == null || this.tipoChave == null || this.tipoConta == null) throw IllegalArgumentException("Valor não pode ser nulo")

    if (!ValidadorChaveRegex.UUID.validar(this.clienteId)) throw IllegalArgumentException("Id do cliente está em formato inválido!")
    if (this.tipoConta.number !in 1..2) throw IllegalArgumentException("Tipo de conta é inválido!")
    if (this.tipoChave.number !in 1..4) throw IllegalArgumentException("Tipo de chave invalido!")
    if (this.valorChave.length > 77) throw IllegalArgumentException("Chave deve ser menor que 77 caracteres")

    when (this.tipoChave.number) {
        1 -> if (!ValidadorChaveRegex.CPF.validar(valorChave)) throw IllegalArgumentException("CPF inválido!")
        2 -> if (!ValidadorChaveRegex.TELEFONE.validar(valorChave)) throw IllegalArgumentException("Numero de telefone inválido!")
        3 -> if (!ValidadorChaveRegex.EMAIL.validar(valorChave)) throw IllegalArgumentException("Email inválido!")
        4 -> if (valorChave.isNotBlank()) throw IllegalArgumentException("Valor não deve ser preenchido!")
        else -> return
    }
    return
}

//Converte a request para a classe de dominio
fun NovaChavePixRequest.toModel(conta: ContaAssociada): ChavePix {
    return ChavePix(
        clienteId = this.clienteId,
        tipoConta = this.tipoConta,
        tipoChave = this.tipoChave,
        conta = conta,
        valorChave = if (this.tipoChave.number == 4) "SEM_VALOR" else this.valorChave
                /*
                A ideia aqui seria salvar as chaves,
                mesmo se houvesse falha ao gerar a chave aleatoria com o servico do banco central,
                para posteriormente o sistema gerar essas chaves automaticamente,
                atraves de um metodo agendado,
                tirando a necessidade do usuario fazer outra requisicao
                 */
    )
}

