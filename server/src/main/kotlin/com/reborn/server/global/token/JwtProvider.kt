package com.reborn.server.global.token

import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtParser
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtProvider(
    @Value("\${jwt.secret}") secret: String,
    @Value("\${jwt.access-token-expiry}") private val accessTokenExpiry: Long,
    @Value("\${jwt.refresh-token-expiry}") private val refreshTokenExpiry: Long,
) {
    private val key: SecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret))
    private val parser: JwtParser = Jwts.parser().verifyWith(key).build()

    fun createAccessToken(userId: Long): String = buildToken(userId, accessTokenExpiry)

    fun createRefreshToken(userId: Long): String = buildToken(userId, refreshTokenExpiry)

    fun parseClaims(token: String): Claims? = runCatching {
        parser.parseSignedClaims(token).payload
    }.getOrNull()

    private fun buildToken(userId: Long, expiry: Long): String {
        val now = Date()
        return Jwts.builder()
            .subject(userId.toString())
            .issuedAt(now)
            .expiration(Date(now.time + expiry))
            .signWith(key)
            .compact()
    }
}
