package br.com.zupacademy.jpcsik.chavepix

import br.com.zupacademy.jpcsik.TipoChave
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import java.util.*

@Repository
interface ChavePixRepository : JpaRepository<ChavePix, String> {

    fun existsByValorChave(valorChave: String): Boolean

    fun findByPixIdAndClienteId(pixId: String, clienteId: String): Optional<ChavePix>

    fun findByValorChave(chave: String): Optional<ChavePix>

    fun findAllByClienteId(clienteId: String): List<ChavePix>

    fun existsByClienteIdAndTipoChave(clienteId: String, tipoChave: TipoChave): Boolean

}