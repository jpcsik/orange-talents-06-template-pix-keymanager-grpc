package br.com.zupacademy.jpcsik.clients

import br.com.zupacademy.jpcsik.NovaChavePixRequest
import br.com.zupacademy.jpcsik.TipoChave
import br.com.zupacademy.jpcsik.TipoConta
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.IllegalArgumentException
import java.util.*

internal class CreatePixKeyRequestTest {

    @Test
    fun `Deve jogar exception caso tipo de chave nao exista`() {
        //cenario
        val contaResponse = ContaResponse(
            tipo = "CONTA_CORRENTE",
            instituicao = mutableMapOf(Pair("nome", "Itau"), Pair("ispb", "60701190")),
            agencia = "00000",
            numero = "11111",
            titular = mutableMapOf(Pair("nome", "Joao"), Pair("cpf", "12312312312"))
        )

        val request = NovaChavePixRequest.newBuilder()
            .setTipoChave(TipoChave.CHAVE_DESCONHECIDA)
            .setValorChave("12312312312")
            .setTipoConta(TipoConta.CONTA_CORRENTE)
            .setClienteId(UUID.randomUUID().toString())
            .build()

        //acao
        val erro = assertThrows<IllegalArgumentException> {
            CreatePixKeyRequest(contaResponse, request)
        }

        //validacao
        assertEquals("Tipo de chave não existe!", erro.message)

    }

    @Test
    fun `Deve jogar settar keyType corretamente`() {
        //cenario
        val contaResponse = ContaResponse(
            tipo = "CONTA_CORRENTE",
            instituicao = mutableMapOf(Pair("nome", "Itau"), Pair("ispb", "60701190")),
            agencia = "00000",
            numero = "11111",
            titular = mutableMapOf(Pair("nome", "Joao"), Pair("cpf", "12312312312"))
        )

        val request1 = NovaChavePixRequest.newBuilder()
            .setTipoChave(TipoChave.EMAIL)
            .setValorChave("email@email.com")
            .setTipoConta(TipoConta.CONTA_CORRENTE)
            .setClienteId(UUID.randomUUID().toString())
            .build()

        val request2 = NovaChavePixRequest.newBuilder()
            .setTipoChave(TipoChave.ALEATORIA)
            .setValorChave(UUID.randomUUID().toString())
            .setTipoConta(TipoConta.CONTA_CORRENTE)
            .setClienteId(UUID.randomUUID().toString())
            .build()

        val request3 = NovaChavePixRequest.newBuilder()
            .setTipoChave(TipoChave.TELEFONE)
            .setValorChave("999999999")
            .setTipoConta(TipoConta.CONTA_CORRENTE)
            .setClienteId(UUID.randomUUID().toString())
            .build()

        //acao
        val response1 = CreatePixKeyRequest(contaResponse, request1)
        val response2 = CreatePixKeyRequest(contaResponse, request2)
        val response3 = CreatePixKeyRequest(contaResponse, request3)

        //validacao
        with(response1){
            assertEquals("EMAIL", keyType)
            assertEquals("email@email.com", key)
            assertNotNull(bankAccount)
            assertNotNull(owner)
        }

        assertEquals("RANDOM", response2.keyType)
        assertEquals("PHONE", response3.keyType)
    }

    @Test
    fun `Deve jogar exception caso tipo de conta nao exista`() {
        //cenario
        val contaResponse = ContaResponse(
            tipo = "CONTA_DESCONHECIDA",
            instituicao = mutableMapOf(Pair("nome", "Itau"), Pair("ispb", "60701190")),
            agencia = "00000",
            numero = "11111",
            titular = mutableMapOf(Pair("nome", "Joao"), Pair("cpf", "12312312312"))
        )

        //acao
        val erro = assertThrows<IllegalArgumentException> {
            BankAccount(contaResponse)
        }

        //validacao
        assertEquals("Tipo de conta não existe!", erro.message)
    }

}