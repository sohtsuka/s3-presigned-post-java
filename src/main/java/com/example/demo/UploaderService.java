package com.example.demo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.auth.aws.internal.signer.CredentialScope;
import software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerUtils;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProviderChain;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.utils.BinaryUtils;

/**
 * ファイルアップロードの処理を行うサービスです。
 */
@Component
public class UploaderService {
    private static final DateTimeFormatter AMZ_DATE_FORMATTER = DateTimeFormatter
            .ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneId.of("UTC"));

    private static final DateTimeFormatter EXPIRATION_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneId.of("UTC"));

    private final UploaderServiceConfig config;

    private final AwsCredentialsProvider credentialsProvider;

    private final AwsRegionProviderChain regionProviderChain;

    private final Region region;

    private final String endpointUrl;

    private final Clock signingClock;

    public UploaderService(UploaderServiceConfig config) {
        this.config = config;
        this.credentialsProvider = DefaultCredentialsProvider.create();
        this.regionProviderChain = DefaultAwsRegionProviderChain.builder().build();
        this.region = regionProviderChain.getRegion();
        this.endpointUrl = String.format("https://%s.s3.amazonaws.com/", config.bucket());
        this.signingClock = Clock.systemUTC();
    }

    /**
     * ブラウザから直接S3にアップロードするためにフォームデータに設定すべき値を生成します。
     * 
     * <p>返却される {@link PresignedPost} のデータを以下のように使用してPOST送信することで、
     * ブラウザからS3への直接ファイルアップロードが可能になります。</p>
     * <ul>
     * <li>{@code url}をPOST先のURLにする</li>
     * <li>エンコードは "multipart/form-data" 形式にする</li>
     * <li>{@code fields}の名前・値の各ペアをフォームフィールドに設定</li>
     * <li>ファイルデータを {@code file}という名前のフィールドに設定</li>
     * </ul>
     * 
     * @param key アップロード先として使用するS3オブジェクトのキー(ファイルパス)
     * @return フォームに設定すべき値を保持するオブジェクト
     * @see https://zaccharles.medium.com/s3-uploads-proxies-vs-presigned-urls-vs-presigned-posts-9661e2b37932
     * @see https://docs.aws.amazon.com/ja_jp/AmazonS3/latest/API/sigv4-HTTPPOSTForms.html
     * @see https://docs.aws.amazon.com/ja_jp/AmazonS3/latest/API/sigv4-UsingHTTPPOST.html
     * @see https://docs.aws.amazon.com/ja_jp/IAM/latest/UserGuide/aws-signing-authentication-methods.html
     */
    public PresignedPost createPresignedPost(String key) {
        AwsCredentials credentials = credentialsProvider.resolveCredentials();
        
        Instant signingInstant = signingClock.instant();
        String signingDate = AMZ_DATE_FORMATTER.format(signingInstant);

        // TODO: AWSのinternalなAPIを使用してしまっている
        CredentialScope scope = new CredentialScope(region.id(), "s3", signingInstant);
        String siginingCredential = scope.scope(credentials);

        // ポリシーとフォームパラメータの共通データ
        List<Field> fields = new ArrayList<>();
        fields.add(new Field("bucket", config.bucket()));
        fields.add(new Field("key", key));
        fields.add(new Field("X-Amz-Algorithm", "AWS4-HMAC-SHA256"));
        fields.add(new Field("X-Amz-Credential", siginingCredential));
        fields.add(new Field("X-Amz-Date", signingDate));

        Instant expiringInstant = signingInstant.plusSeconds(config.expirationSeconds());
        String policyJson = createPolicyJson(expiringInstant, fields, config.contentLengthMin(), config.contentLengthMax());
        String policyB64 = BinaryUtils.toBase64(policyJson.getBytes(StandardCharsets.UTF_8));

        // TODO: AWSのinternalなAPIを使用してしまっている
        byte[] signingKey = SignerUtils.deriveSigningKey(credentials, scope);
        byte[] signature = SignerUtils.computeSignature(policyB64, signingKey);

        Map<String, String> fieldsMap = fields.stream().collect(Collectors.toMap(f -> f.name(), f -> f.value()));
        fieldsMap.put("Policy", policyB64);
        fieldsMap.put("X-Amz-Signature", BinaryUtils.toHex(signature));

        return new PresignedPost(endpointUrl, fieldsMap);
    }

    /**
     * POSTポリシーのJSON文字列を作成します。
     * @see https://docs.aws.amazon.com/ja_jp/AmazonS3/latest/API/sigv4-HTTPPOSTConstructPolicy.html
     */
    private static String createPolicyJson(
            Instant expiringInstant, List<Field> fields, int contentLengthMin, int contentLengthMax) {
        ObjectMapper om = new ObjectMapper();

        ObjectNode policy = om.createObjectNode();
        policy.put("expiration", EXPIRATION_FORMATTER.format(expiringInstant));

        ArrayNode conds = om.createArrayNode();
        conds.add(om.createArrayNode().add("content-length-range")
            .add(contentLengthMin).add(contentLengthMax));
        for (Field field : fields) {
            conds.add(om.createObjectNode().put(field.name(), field.value()));
        }
        policy.set("conditions", conds);

        try {
            return om.writeValueAsString(policy);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private record Field(String name, String value) {
    }
}
