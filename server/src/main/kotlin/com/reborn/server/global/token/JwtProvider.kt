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
    private val key: SecretKey

    init {
        require(secret.isNotBlank()) {
            "JWT_SECRET 환경 변수가 설정되지 않았거나 비어 있습니다. 보안을 위해 필수 설정이 필요합니다."
        }

        val keyBytes = Decoders.BASE64.decode(secret)
        this.key = Keys.hmacShaKeyFor(keyBytes)
    }

    private val parser: JwtParser = Jwts.parser().verifyWith(key).build()

    fun createAccessToken(userId: Long): String = buildToken(userId, accessTokenExpiry, ACCESS_TYPE)

    fun createRefreshToken(userId: Long): String = buildToken(userId, refreshTokenExpiry, REFRESH_TYPE)

    fun parseClaims(token: String): Claims? = runCatching {
        parser.parseSignedClaims(token).payload
    }.getOrNull()

    private fun buildToken(userId: Long, expiry: Long, type: String): String {
        val now = Date()
        return Jwts.builder()
            .subject(userId.toString())
            .claim(TYPE_KEY, type)
            .issuedAt(now)
            .expiration(Date(now.time + expiry))
            .signWith(key)
            .compact()
    }

    companion object {
        const val TYPE_KEY = "type"
        const val ACCESS_TYPE = "access"
        const val REFRESH_TYPE = "refresh"
    }
}