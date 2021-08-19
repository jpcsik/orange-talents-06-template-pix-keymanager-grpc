package br.com.zupacademy.jpcsik.chavepix

import br.com.zupacademy.jpcsik.TipoChave
import br.com.zupacademy.jpcsik.clients.BankAccount
import br.com.zupacademy.jpcsik.clients.ContaResponse
import br.com.zupacademy.jpcsik.clients.Owner
import br.com.zupacademy.jpcsik.clients.PixKeyDetailsResponse
import com.google.protobuf.Timestamp
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

internal class ExtencaoBuscaChaveKtTest {

    private val contaResponse = ContaResponse(
        tipo = "CONTA_CORRENTE",
        instituicao = mutableMapOf(Pair("nome", "Itau"), Pair("ispb", "60701190")),
        agencia = "00000",
        numero = "11111",
        titular = mutableMapOf(Pair("nome", "Joao"), Pair("cpf", "12312312312"))
    )

    private val criadaEm = LocalDateTime.now()

    @Test
    fun `Deve settar tipo de chave como CPF`() {
        //cenario
        val pixKeyDetailsResponse = PixKeyDetailsResponse(
            keyType = "CPF",
            key = "12312312312",
            bankAccount = BankAccount(contaResponse),
            owner = Owner(contaResponse),
            createdAt = criadaEm.atZone(ZoneId.of("UTC")).toInstant().let {
                Timestamp.newBuilder()
                    .setSeconds(it.epochSecond)
                    .setNanos(it.nano)
                    .build()
            }
        )

        //acao
        val response = respostaComChaveNoBCB(pixKeyDetailsResponse)

        //validacao
        assertEquals(TipoChave.CPF, response.chave.tipo)

    }

    @Test
    fun `Deve settar tipo de chave como TELEFONE`() {
        //cenario
        val pixKeyDetailsResponse = PixKeyDetailsResponse(
            keyType = "PHONE",
            key = "99999999",
            bankAccount = BankAccount(contaResponse),
            owner = Owner(contaResponse),
            createdAt = criadaEm.atZone(ZoneId.of("UTC")).toInstant().let {
                Timestamp.newBuilder()
                    .setSeconds(it.epochSecond)
                    .setNanos(it.nano)
                    .build()
            }
        )

        //acao
        val response = respostaComChaveNoBCB(pixKeyDetailsResponse)

        //validacao
        assertEquals(TipoChave.TELEFONE, response.chave.tipo)

    }

    @Test
    fun `Deve settar tipo de chave como ALEATORIA`() {
        //cenario
        val pixKeyDetailsResponse = PixKeyDetailsResponse(
            keyType = "RANDOM",
            key = UUID.randomUUID().toString(),
            bankAccount = BankAccount(contaResponse),
            owner = Owner(contaResponse),
            createdAt = criadaEm.atZone(ZoneId.of("UTC")).toInstant().let {
                Timestamp.newBuilder()
                    .setSeconds(it.epochSecond)
                    .setNanos(it.nano)
                    .build()
            }
        )

        //acao
        val response = respostaComChaveNoBCB(pixKeyDetailsResponse)

        //validacao
        assertEquals(TipoChave.ALEATORIA, response.chave.tipo)
    }


}