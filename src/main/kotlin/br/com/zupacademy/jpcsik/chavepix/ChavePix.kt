package br.com.zupacademy.jpcsik.chavepix

import br.com.zupacademy.jpcsik.TipoChave
import br.com.zupacademy.jpcsik.TipoConta
import org.hibernate.annotations.GenericGenerator
import java.time.LocalDateTime
import javax.persistence.*
import javax.validation.constraints.PastOrPresent
import javax.validation.constraints.Size

@Entity
class ChavePix(
    @Column(nullable = false)
    val clienteId: String,

    @Column(nullable = false)
    val tipoConta: TipoConta,

    @Column(nullable = false)
    val tipoChave: TipoChave,

    @Embedded
    @Column(nullable = false)
    val conta: ContaAssociada,

    @field:Size(max = 77)
    @Column(nullable = false, unique = true)
    val valorChave: String
) {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    val pixId: String? = null

    @Column(nullable = false)
    @field:PastOrPresent
    val criadaEm: LocalDateTime = LocalDateTime.now()
}