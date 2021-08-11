package br.com.zupacademy.jpcsik

import io.micronaut.runtime.Micronaut.*
fun main(args: Array<String>) {
	build()
	    .args(*args)
		.packages("br.com.zupacademy.jpcsik")
		.start()
}

