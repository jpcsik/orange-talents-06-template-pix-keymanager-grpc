package br.com.zupacademy.jpcsik.chavepix

enum class ValidadorChaveRegex(private val regex: Regex) {

    CPF("^[0-9]{11}$".toRegex()),
    EMAIL("(?:[a-z0-9!#\$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#\$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])".toRegex()),
    TELEFONE("^+[1-9][0-9]\\d{1,14}\$".toRegex()),
    UUID("[a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8}".toRegex());

    //Validacao para cada tipo de chave
    fun validar(value: String): Boolean = value.matches(this.regex)
}