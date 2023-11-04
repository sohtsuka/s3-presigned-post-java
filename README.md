# Presigned POST in Java

こちらに記載されている Presigned POST をJava (Spring Boot) で実装したものです。

https://zaccharles.medium.com/s3-uploads-proxies-vs-presigned-urls-vs-presigned-posts-9661e2b37932

## 必要な準備

### S3へのアップロード権限付与

`~/.aws/credentials` や環境変数・インスタンスロールなどにより、Spring BootアプリケーションがS3にアクセスできる権限を与える。
（`DefaultCredentialsProvider` で取得できるようにクレデンシャルを設定する）

最低限必要な権限は以下の通り。
（バケット名は適宜変更すること。 `application.properties` もあわせて変更）

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "s3:PutObject",
            ],
            "Resource": [
                "arn:aws:s3:::example-bucket/*"
            ]
        }
    ]
}
```

### CORS設定を行う

対象のS3に、アップロード元アプリからのアクセスを許可するCORS設定を行う。

```json
[
    {
        "AllowedHeaders": [
            "*"
        ],
        "AllowedMethods": [
            "POST"
        ],
        "AllowedOrigins": [
            "http://localhost:8080"
        ],
        "ExposeHeaders": []
    }
]
```
