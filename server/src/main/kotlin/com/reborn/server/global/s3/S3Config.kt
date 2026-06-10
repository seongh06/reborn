package com.reborn.server.global.s3

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client

@Configuration
@ConditionalOnExpression(
    "#{T(org.springframework.util.StringUtils).hasText('\${cloud.aws.credentials.access-key:}') " +
        "and T(org.springframework.util.StringUtils).hasText('\${cloud.aws.credentials.secret-key:}')}",
)
class S3Config(
    @param:Value("\${cloud.aws.credentials.access-key}") private val accessKey: String,
    @param:Value("\${cloud.aws.credentials.secret-key}") private val secretKey: String,
    @param:Value("\${cloud.aws.region.static}") private val region: String,
) {

    init {
        require(accessKey.isNotBlank()) { "cloud.aws.credentials.access-key must not be blank" }
        require(secretKey.isNotBlank()) { "cloud.aws.credentials.secret-key must not be blank" }
    }

    @Bean
    fun s3Client(): S3Client = S3Client.builder()
        .region(Region.of(region))
        .credentialsProvider(
            StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)),
        )
        .build()
}
