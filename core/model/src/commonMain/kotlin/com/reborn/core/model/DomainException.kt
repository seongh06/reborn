package com.reborn.core.model

interface ErrorCode {
    val code: Long
    val message: String
}

abstract class ApplicationException(
    override val message: String? = null,
    override val cause: Throwable? = null,
    open val errorCode: ErrorCode,
) : Exception(message, cause) {
    override fun toString(): String {
        return "${this::class.simpleName}(" +
                "message=${message ?: "null"}, " +
                "cause=${cause?.toString() ?: "null"}, " +
                "errorCode=${errorCode.code}, " +
                "errorCodeMessage=${errorCode.message}" +
                ")"
    }
}
enum class DomainErrorCode(
    override val code: Long,
    override val message: String,
) : ErrorCode {
    INVALID_OAUTH_TOKEN(1000L, "토큰이 만료되었습니다."),
    UNSUPPORTED_SOCIAL_PROVIDER(1001L, "회원가입을 다시 시도해주세요."),
    KAKAO_FAILED(1002L, "카카오 로그인에 실패했습니다."),
    DUPLICATE_NICKNAME(1003L, "중복된 닉네임입니다."),

    ALREADY_REGISTERED_USER(2000L, "이미 가입된 회원입니다."),
    INVALID_JWT_TOKEN(2001L, "토큰이 만료되었습니다."),
    USER_NOT_REGISTERED(2002L, "가입되지 않은 회원입니다."),

    USER_NOT_FOUND(3000L, "존재하지 않는 회원입니다."),

    IMAGE_UPLOAD_FAILED(9100L, "이미지 업로드에 실패했습니다."),
    IMAGE_DELETE_FAILED(9101L, "이미지 삭제에 실패했습니다."),
    UNSUPPORTED_FILE_TYPE(9102L, "지원하지 않는 파일 형식입니다."),
    UNSUPPORTED_FILE_EXTENSION(9103L, "지원하지 않는 파일 확장자입니다."),

    SERVER_ERROR(9000L, "서버 오류가 발생했습니다."),
    INVALID_INPUT(9001L, "잘못된 입력 값입니다."),

    NETWORK_ERROR(-1L, "네트워크 환경을 확인해주세요."),
    UNKNOWN_ERROR(-2L, "알 수 없는 오류가 발생했습니다.");
}

sealed class DomainException(
    override val cause: Throwable? = null,
    override val errorCode: ErrorCode
) : ApplicationException(cause?.message ?: errorCode.message, cause, errorCode) {

    data class InvalidOAuthTokenException(override val cause: Throwable? = null) : DomainException(cause, DomainErrorCode.INVALID_OAUTH_TOKEN)
    data class UnsupportedSocialProviderException(override val cause: Throwable? = null) : DomainException(cause, DomainErrorCode.UNSUPPORTED_SOCIAL_PROVIDER)
    data class KakaoFailedException(override val cause: Throwable? = null) : DomainException(cause, DomainErrorCode.KAKAO_FAILED)
    data class DuplicateNicknameException(override val cause: Throwable? = null) : DomainException(cause, DomainErrorCode.DUPLICATE_NICKNAME)
    data class AlreadyRegisteredUserException(override val cause: Throwable? = null) : DomainException(cause, DomainErrorCode.ALREADY_REGISTERED_USER)
    data class InvalidJwtTokenException(override val cause: Throwable? = null) : DomainException(cause, DomainErrorCode.INVALID_JWT_TOKEN)

    data class UserNotRegisteredException(val kakaoUserId: String, override val cause: Throwable? = null) : DomainException(cause, DomainErrorCode.USER_NOT_REGISTERED)

    data class UserNotFoundException(override val cause: Throwable? = null) : DomainException(cause, DomainErrorCode.USER_NOT_FOUND)

    data class ImageUploadFailedException(override val cause: Throwable? = null) : DomainException(cause, DomainErrorCode.IMAGE_UPLOAD_FAILED)
    data class ImageDeleteFailedException(override val cause: Throwable? = null) : DomainException(cause, DomainErrorCode.IMAGE_DELETE_FAILED)
    data class UnsupportedFileTypeException(override val cause: Throwable? = null) : DomainException(cause, DomainErrorCode.UNSUPPORTED_FILE_TYPE)
    data class UnsupportedFileExtensionException(override val cause: Throwable? = null) : DomainException(cause, DomainErrorCode.UNSUPPORTED_FILE_EXTENSION)

    data class ServerErrorException(override val cause: Throwable? = null) : DomainException(cause, DomainErrorCode.SERVER_ERROR)
    data class InvalidInputException(override val cause: Throwable? = null) : DomainException(cause, DomainErrorCode.INVALID_INPUT)
    data class NetworkException(override val cause: Throwable? = null) : DomainException(cause, DomainErrorCode.NETWORK_ERROR)
    data class UnknownException(override val cause: Throwable? = null) : DomainException(cause, DomainErrorCode.UNKNOWN_ERROR)
}