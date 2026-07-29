package com.reborn.server.global.storage

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.util.UUID
import javax.imageio.ImageIO

data class LocalUploadResponse(
    val key: String,
    val url: String,
)

// 프로필 이미지 등 사용자 업로드 파일을 로컬 디스크에 저장한다 - AWS 계정 없이 바로 동작하도록
// S3Uploader 대신 채택(#217). 홈서버(docker-compose)의 /app/uploads는 호스트 디렉토리에
// 바인드 마운트돼 있어(mysql/redis와 동일 패턴) 컨테이너가 매 배포마다 재생성돼도 파일이 유지된다.
// WebMvcConfig가 이 uploadDir을 "/uploads/**" 경로로 정적 서빙한다.
@Component
class LocalFileStorage(
    @param:Value("\${app.upload-dir:/app/uploads}") private val uploadDir: String,
    @param:Value("\${app.public-base-url:https://www.reborn-energy.com}") private val publicBaseUrl: String,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    // originalFilename과 Content-Type 헤더는 둘 다 클라이언트가 마음대로 지정 가능한 값이라, 그걸로
    // 저장 확장자를 정하면 image/jpeg라고 우기면서 payload.html을 올려 공개 /uploads 아래에 .html로
    // 저장시키는 공격이 가능했음 - 확장자는 서버가 정한 허용 목록에서만 고르고, 실제 바이트도 디코딩
    // 가능한 이미지인지 검증한다.
    fun upload(file: MultipartFile, directory: String = "uploads"): LocalUploadResponse {
        val extension = file.contentType?.let { IMAGE_EXTENSION_BY_CONTENT_TYPE[it] }
            ?: throw IllegalArgumentException("지원하지 않는 이미지 형식입니다: ${file.contentType}")
        if (!isDecodableImage(file)) {
            throw IllegalArgumentException("올바른 이미지 파일이 아닙니다.")
        }

        val key = "$directory/${UUID.randomUUID()}.$extension"
        val target = File(uploadDir, key)
        target.parentFile?.mkdirs()
        file.inputStream.use { input -> target.outputStream().use { output -> input.copyTo(output) } }

        val url = "$publicBaseUrl/uploads/$key"
        log.info("local upload success: key={}, url={}", key, url)
        return LocalUploadResponse(key = key, url = url)
    }

    // 이전 이미지가 이 서버가 직접 서빙하는 파일이 맞는지 확인 후에만 key를 돌려준다 - 소셜 로그인
    // 프로바이더가 내려준 외부 URL(카카오/구글 프로필 이미지)을 실수로 삭제 시도하지 않기 위한 안전장치.
    fun extractKeyIfOwned(url: String): String? {
        val prefix = "$publicBaseUrl/uploads/"
        return url.takeIf { it.startsWith(prefix) }?.removePrefix(prefix)
    }

    fun delete(key: String) {
        val target = File(uploadDir, key)
        val deleted = runCatching { target.delete() }.getOrDefault(false)
        if (deleted) {
            log.info("local delete success: key={}", key)
        } else {
            log.warn("local delete failed or file not found: key={}", key)
        }
    }

    private fun isDecodableImage(file: MultipartFile): Boolean =
        runCatching { file.inputStream.use { ImageIO.read(it) != null } }.getOrDefault(false)

    companion object {
        private val IMAGE_EXTENSION_BY_CONTENT_TYPE = mapOf(
            "image/jpeg" to "jpg",
            "image/png" to "png",
            "image/webp" to "webp",
        )
    }
}
