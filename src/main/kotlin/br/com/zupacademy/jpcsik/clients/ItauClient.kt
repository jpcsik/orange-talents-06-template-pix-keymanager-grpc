package br.com.zupacademy.jpcsik.clients

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.client.annotation.Client

@Client("\${clients.itau}")
interface ItauClient {

    @Get("/clientes/{clienteId}/contas{?tipo}")
    fun consultaContas(@PathVariable clienteId: String, @QueryValue tipo: String): HttpResponse<ContaResponse>
}