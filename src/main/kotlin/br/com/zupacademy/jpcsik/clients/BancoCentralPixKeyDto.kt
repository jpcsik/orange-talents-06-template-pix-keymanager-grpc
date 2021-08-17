package br.com.zupacademy.jpcsik.clients

import br.com.zupacademy.jpcsik.NovaChavePixRequest

data class CreatePixKeyRequest(
      val keyType: String,
      val key: String,
      val bankAccount: BankAccount,
      val owner: Owner,
) {
    constructor(contaResponse: ContaResponse, request: NovaChavePixRequest) : this(
        keyType = when (request.tipoChave.number) {
            1 -> "CPF"
            2 -> "PHONE"
            3 -> "EMAIL"
            else -> "RANDOM"
        },
        key = request.valorChave,
        bankAccount = BankAccount(contaResponse),
        owner = Owner(contaResponse)
    )
}

data class BankAccount(private val conta: ContaResponse) {
    val participant: String = conta.instituicao["ispb"]!!
    val branch: String = conta.agencia
    val accountNumber: String = conta.numero
    val accountType: String = when (conta.tipo) {
        "CONTA_CORRENTE" -> "CACC"
        else -> "SVGS"
    }
}

data class Owner(private val conta: ContaResponse) {
    val type: String = "NATURAL_PERSON"
    val name: String = conta.titular["nome"]!!
    val taxIdNumber: String = conta.titular["cpf"]!!
}

data class CreatePixKeyResponse(
    val key: String
)

data class DeletePixKeyRequest(val pixId: String) {
    val key: String = pixId
    val participant = "60701190"
}