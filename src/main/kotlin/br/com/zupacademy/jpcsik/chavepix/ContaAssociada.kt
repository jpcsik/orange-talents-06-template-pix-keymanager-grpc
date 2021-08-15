package br.com.zupacademy.jpcsik.chavepix

import javax.persistence.Embeddable

@Embeddable
data class ContaAssociada(
    val instituicao: String,
    val agencia: String,
    val numero: String,
    val nomeTitular: String,
    val cpfTitular: String
)
