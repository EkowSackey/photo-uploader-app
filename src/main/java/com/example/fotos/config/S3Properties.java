package com.example.fotos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class S3Properties {

    private String bucketName;
    private String cloudfrontBaseUrl;
    private String awsRegion;

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getCloudfrontBaseUrl() {
        return cloudfrontBaseUrl;
    }

    public void setCloudfrontBaseUrl(String cloudfrontBaseUrl) {
        this.cloudfrontBaseUrl = cloudfrontBaseUrl;
    }

    public String getAwsRegion() {
        return awsRegion;
    }

    public void setAwsRegion(String awsRegion) {
        this.awsRegion = awsRegion;
    }
}
