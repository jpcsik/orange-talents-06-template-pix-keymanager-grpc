package br.com.zupacademy.jpcsik.chavepix

import br.com.zupacademy.jpcsik.TipoChave
import br.com.zupacademy.jpcsik.TipoConta
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

internal class AtributosDasClassesTest {

    private val contaResponse = ContaResponse(
        tipo = "CONTA_CORRENTE",
        instituicao = mutableMapOf(Pair("nome", "Itau")),
        agencia = "00000",
        numero = "11111",
        titular = mutableMapOf(Pair("nome", "Joao"), Pair("cpf", "12312312312"))
    )


    @Test
    fun `Deve estar corretos os atributos da conta response`() {

        assertEquals("CONTA_CORRENTE", contaResponse.tipo)
        assertEquals("Itau", contaResponse.instituicao["nome"])
        assertEquals("00000", contaResponse.agencia)
        assertEquals("11111", contaResponse.numero)
        assertEquals("Joao", contaResponse.titular["nome"])
        assertEquals("12312312312", contaResponse.titular["cpf"])

    }

    @Test
    fun `Deve estar corretos os atributos da conta associada`() {
        val contaAssociada = contaResponse.toModel()

        assertEquals("00000", contaAssociada.agencia)
        assertEquals("11111", contaAssociada.numero)
        assertEquals("Itau", contaAssociada.instituicao)
        assertEquals("12312312312", contaAssociada.cpfTitular)
        assertEquals("Joao", contaAssociada.nomeTitular)

    }

    @Test
    fun `Deve estar corretos os atributos da chave pix`() {

        val uuid = UUID.fromString("d5b393b1-2797-4c01-949e-3e7aef9f7455")

        val chavePix = ChavePix(
            clienteId = uuid,
            tipoConta = TipoConta.CONTA_CORRENTE,
            tipoChave = TipoChave.TELEFONE,
            valorChave = "999999999",
            conta = contaResponse.toModel()
        )

        assertEquals(uuid, chavePix.clienteId)
        assertEquals(TipoConta.CONTA_CORRENTE, chavePix.tipoConta)
        assertEquals(TipoChave.TELEFONE, chavePix.tipoChave)
        assertEquals(contaResponse.toModel(), chavePix.conta)

    }

}