package com.reborn.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class RebornServerApplication

fun main(args: Array<String>) {
    runApplication<RebornServerApplication>(*args)
}
