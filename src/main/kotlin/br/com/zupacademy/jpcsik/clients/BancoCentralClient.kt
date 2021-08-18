package br.com.zupacademy.jpcsik.clients

import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.http.client.annotation.Client

@Client("\${clients.banco-central}")
interface BancoCentralClient {

    @Get("/keys/{key}")
    @Produces(MediaType.APPLICATION_XML)
    fun buscaChave(@PathVariable key: String): HttpResponse<PixKeyDetailsResponse>

    @Post("/keys")
    @Produces(MediaType.APPLICATION_XML)
    fun criarChave(@Body request: CreatePixKeyRequest): HttpResponse<CreatePixKeyResponse>

    @Delete("/keys/{key}")
    @Produces(MediaType.APPLICATION_XML)
    fun deletaChave(@PathVariable key: String, @Body request: DeletePixKeyRequest): HttpResponse<Any>

}
