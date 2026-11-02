package com.airepair.common;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DuplicateKeyException;
@RestControllerAdvice public class Errors {
 @ExceptionHandler(BusinessException.class) ResponseEntity<Api<Void>> business(BusinessException e){return ResponseEntity.status(e.status).body(Api.error(e.getMessage()));}
 @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<Api<Void>> validation(MethodArgumentNotValidException e){return ResponseEntity.badRequest().body(Api.error(e.getBindingResult().getFieldErrors().isEmpty()?"参数无效":e.getBindingResult().getFieldErrors().get(0).getDefaultMessage()));}
 @ExceptionHandler({org.springframework.http.converter.HttpMessageNotReadableException.class,org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class}) ResponseEntity<Api<Void>> malformed(){return ResponseEntity.badRequest().body(Api.error("请求格式无效"));}
 @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class) ResponseEntity<Api<Void>> tooLarge(){return ResponseEntity.status(413).body(Api.error("图片超过大小限制"));}
 @ExceptionHandler(DuplicateKeyException.class) ResponseEntity<Api<Void>> duplicate(){return ResponseEntity.status(409).body(Api.error("记录已存在"));}
 @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class) ResponseEntity<Api<Void>> denied(){return ResponseEntity.status(403).body(Api.error("无权执行此操作"));}
 @ExceptionHandler(Exception.class) ResponseEntity<Api<Void>> unexpected(Exception e){org.slf4j.LoggerFactory.getLogger(Errors.class).error("Request failed",e);return ResponseEntity.internalServerError().body(Api.error("服务器内部错误"));}
}
