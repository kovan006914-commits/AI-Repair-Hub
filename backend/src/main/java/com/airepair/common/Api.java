package com.airepair.common;
public record Api<T>(boolean success, String message, T data) {
 public static <T> Api<T> ok(T data){ return new Api<>(true,"ok",data); }
 public static Api<Void> error(String message){ return new Api<>(false,message,null); }
}
