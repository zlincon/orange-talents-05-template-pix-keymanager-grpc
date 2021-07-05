package br.com.zupacademy.lincon

import io.micronaut.runtime.Micronaut.build

fun main(args: Array<String>) {
  build()
    .args(*args)
    .packages("br.com.zupacademy.lincon")
    .start()
}

