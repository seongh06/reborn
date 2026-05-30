package com.reborn.server.global.s3

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.ObjectCannedACL
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.util.UUID

@Component
@ConditionalOnBean(S3Client::class)
class S3Uploader(
    private val s3Client: S3Client,
    @Value("\${cloud.aws.s3.bucket}") private val bucket: String,
    @Value("\${cloud.aws.region.static}") private val region: String,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun upload(file: MultipartFile, directory: String = "uploads"): String {
        val key = "$directory/${UUID.randomUUID()}-${file.originalFilename}"
        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.contentType)
                .contentLength(file.size)
                .build(),
            RequestBody.fromInputStream(file.inputStream, file.size),
        )
        log.info("S3 upload success: {}", key)
        return "https://$bucket.s3.$region.amazonaws.com/$key"
    }

    fun delete(key: String) {
        s3Client.deleteObject(
            DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build(),
        )
        log.info("S3 delete success: {}", key)
    }
}
