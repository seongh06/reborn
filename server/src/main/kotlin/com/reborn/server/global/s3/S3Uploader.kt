package com.reborn.server.global.s3

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.util.UUID

data class S3UploadResponse(
    val key: String,
    val url: String
)

@Component
@ConditionalOnBean(S3Client::class)
class S3Uploader(
    private val s3Client: S3Client,
    @param:Value("\${cloud.aws.s3.bucket}") private val bucket: String,
    @param:Value("\${cloud.aws.region.static}") private val region: String,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun upload(file: MultipartFile, directory: String = "uploads"): S3UploadResponse {
        val extension = file.originalFilename
            ?.substringAfterLast('.', "")
            ?.takeIf { it.matches(SAFE_EXTENSION_PATTERN) }
            ?.let { ".$it" }
            ?: ""

        val key = "$directory/${UUID.randomUUID()}$extension"
        val url = "https://$bucket.s3.$region.amazonaws.com/$key"

        file.inputStream.use { inputStream ->
            s3Client.putObject(
                PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.contentType)
                    .contentLength(file.size)
                    .build(),
                RequestBody.fromInputStream(inputStream, file.size),
            )
        }

        log.info("S3 upload success: key={}, url={}", key, url)
        return S3UploadResponse(key = key, url = url)
    }

    fun delete(key: String) {
        try {
            s3Client.deleteObject(
                DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build(),
            )
            log.info("S3 delete success: key={}", key)
        } catch (e: Exception) {
            log.error("S3 delete failed: key={}", key, e)
            throw e
        }
    }

    companion object {
        private val SAFE_EXTENSION_PATTERN = Regex("^[a-zA-Z0-9]{1,10}$")
    }
}