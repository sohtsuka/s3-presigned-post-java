package com.example.demo;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link UploaderService}用の設定オブジェクトです。
 * 
 * <p>application.properties などにプレフィックス app.uploader で指定した設定を格納します。例:</p>
 * <code><pre>
 * app.uploader.bucket=my-example-bucket
 * </pre></code>
 * 
 * @param bucket バケット名
 * @param expirationSeconds 署名付きPOSTの有効期限(秒数)
 * @param contentLengthMin content-lengthの最小バイト数
 * @param contentLengthMax content-lengthの最大バイト数
 */
@ConfigurationProperties(prefix = "app.uploader")
public record UploaderServiceConfig(
    String bucket, int expirationSeconds, int contentLengthMin, int contentLengthMax) {}
