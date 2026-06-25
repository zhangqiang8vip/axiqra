package com.axiqra.core.adapter;

import io.minio.*;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 证据存储适配器
 * 用于存储 Trace 的证据文件（日志、diff、截图等）
 *
 * @author Axiqra Team
 */
@Slf4j
@Component
public class EvidenceStorageAdapter {

    @Value("${axiqra.minio.bucket:axiqra-evidence}")
    private String bucketName;

    @Value("${axiqra.minio.enabled:false}")
    private boolean minioEnabled;

    @Value("${axiqra.minio.endpoint:http://localhost:9000}")
    private String endpoint;

    @Value("${axiqra.minio.access-key:}")
    private String accessKey;

    @Value("${axiqra.minio.secret-key:}")
    private String secretKey;

    private MinioClient getMinioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    /**
     * 上传证据文件
     *
     * @param objectName 对象名称 (例如: workspace/123/trace/456/log.txt)
     * @param inputStream 文件输入流
     * @param contentType 内容类型
     * @param size 文件大小
     * @return 文件的访问 URL
     */
    public String uploadEvidence(String objectName, InputStream inputStream, String contentType, long size) {
        if (!minioEnabled) {
            log.warn("MinIO 存储未启用，无法上传证据文件: {}", objectName);
            return null;
        }

        try {
            ensureBucketExists();
            getMinioClient().putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, size, -1)
                            .contentType(contentType)
                            .build()
            );
            log.info("证据文件上传成功: bucket={}, object={}", bucketName, objectName);
            return getPresignedUrl(objectName);
        } catch (Exception e) {
            log.error("证据文件上传失败: object={}, error={}", objectName, e.getMessage());
            throw new RuntimeException("证据文件上传失败", e);
        }
    }

    /**
     * 下载证据文件
     *
     * @param objectName 对象名称
     * @return 文件输入流
     */
    public InputStream downloadEvidence(String objectName) {
        if (!minioEnabled) {
            log.warn("MinIO 存储未启用");
            return null;
        }

        try {
            return getMinioClient().getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            log.error("证据文件下载失败: object={}, error={}", objectName, e.getMessage());
            throw new RuntimeException("证据文件下载失败", e);
        }
    }

    /**
     * 获取预签名 URL（用于临时访问）
     *
     * @param objectName 对象名称
     * @param expiryMinutes 过期时间（分钟）
     * @return 预签名 URL
     */
    public String getPresignedUrl(String objectName, int expiryMinutes) {
        if (!minioEnabled) {
            log.warn("MinIO 存储未启用");
            return null;
        }

        try {
            return getMinioClient().getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(expiryMinutes, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            log.error("获取预签名 URL 失败: object={}, error={}", objectName, e.getMessage());
            throw new RuntimeException("获取预签名 URL 失败", e);
        }
    }

    /**
     * 获取预签名 URL（默认 60 分钟）
     */
    public String getPresignedUrl(String objectName) {
        return getPresignedUrl(objectName, 60);
    }

    /**
     * 删除证据文件
     *
     * @param objectName 对象名称
     */
    public void deleteEvidence(String objectName) {
        if (!minioEnabled) {
            log.warn("MinIO 存储未启用");
            return;
        }

        try {
            getMinioClient().removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            log.info("证据文件删除成功: bucket={}, object={}", bucketName, objectName);
        } catch (Exception e) {
            log.error("证据文件删除失败: object={}, error={}", objectName, e.getMessage());
            throw new RuntimeException("证据文件删除失败", e);
        }
    }

    /**
     * 检查文件是否存在
     *
     * @param objectName 对象名称
     * @return 是否存在
     */
    public boolean exists(String objectName) {
        if (!minioEnabled) {
            return false;
        }

        try {
            getMinioClient().statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 确保存储桶存在
     */
    private void ensureBucketExists() throws Exception {
        boolean exists = getMinioClient().bucketExists(
                BucketExistsArgs.builder()
                        .bucket(bucketName)
                        .build()
        );
        if (!exists) {
            getMinioClient().makeBucket(
                    MakeBucketArgs.builder()
                            .bucket(bucketName)
                            .build()
            );
            log.info("创建 MinIO 存储桶: {}", bucketName);
        }
    }
}
