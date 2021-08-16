package br.com.zupacademy.jpcsik.chavepix

data class ContaResponse(
    val tipo: String,
    val instituicao: Map<String, String>,
    val agencia: String,
    val numero: String,
    val titular: Map<String, String>
) {

    fun toModel(): ContaAssociada {
        return ContaAssociada(
            instituicao = this.instituicao["nome"]!!,
            agencia = this.agencia,
            numero = this.numero,
            nomeTitular = this.titular["nome"]!!,
            cpfTitular = this.titular["cpf"]!!
        )
    }

}
