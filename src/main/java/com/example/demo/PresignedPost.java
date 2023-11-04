package com.example.demo;

import java.util.Map;

/**
 * 署名付きPOSTでのS3アップロードに必要なデータを保持します。
 * 
 * @param url 送信先URL
 * @param fields ファイルと共に送信が必要なフォームフィールド
 */
public record PresignedPost(String url, Map<String, String> fields) {}
