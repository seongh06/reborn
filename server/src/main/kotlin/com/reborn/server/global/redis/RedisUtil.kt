package com.reborn.server.global.redis

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class RedisUtil(
    private val redisTemplate: RedisTemplate<String, String>,
) {
    fun get(key: String): String? =
        redisTemplate.opsForValue().get(key)

    /**
     * 키를 원자적으로 조회 후 삭제한다 (GETDEL). 동시 요청에 의한 중복 소비를 방지할 때 사용.
     */
    fun getAndDelete(key: String): String? =
        redisTemplate.opsForValue().getAndDelete(key)

    fun set(key: String, value: String, ttl: Duration) =
        redisTemplate.opsForValue().set(key, value, ttl)

    fun delete(key: String): Boolean =
        redisTemplate.delete(key) ?: false

    fun exists(key: String): Boolean =
        redisTemplate.hasKey(key) ?: false

    /**
     * 키의 TTL을 갱신한다.
     * @return true = TTL 갱신 성공, false = 키 없음 또는 연산 실패
     */
    fun expire(key: String, ttl: Duration): Boolean =
        redisTemplate.expire(key, ttl) ?: false
}
