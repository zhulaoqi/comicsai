package com.comicsai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private final Path basePath;
    private final String comicImagesDir;
    private final String novelTextsDir;
    private final String coversDir;
    private final String accessUrlPrefix;

    public FileStorageService(
            @Value("${app.file-storage.base-path:./uploads}") String basePath,
            @Value("${app.file-storage.comic-images:comic-images}") String comicImagesDir,
            @Value("${app.file-storage.novel-texts:novel-texts}") String novelTextsDir,
            @Value("${app.file-storage.covers:covers}") String coversDir,
            @Value("${app.file-storage.access-url-prefix:/files}") String accessUrlPrefix) {
        this.basePath = Paths.get(basePath).toAbsolutePath().normalize();
        this.comicImagesDir = comicImagesDir;
        this.novelTextsDir = novelTextsDir;
        this.coversDir = coversDir;
        this.accessUrlPrefix = accessUrlPrefix;
        initDirectories();
    }

    private void initDirectories() {
        try {
            Files.createDirectories(basePath.resolve(comicImagesDir));
            Files.createDirectories(basePath.resolve(novelTextsDir));
            Files.createDirectories(basePath.resolve(coversDir));
        } catch (IOException e) {
            log.error("Failed to create storage directories", e);
            throw new RuntimeException("无法创建文件存储目录", e);
        }
    }

    public String storeCoverImage(MultipartFile file) throws IOException {
        return storeFile(file, coversDir);
    }

    public String storeComicImage(MultipartFile file) throws IOException {
        return storeFile(file, comicImagesDir);
    }

    public String storeNovelText(String text, String filename) throws IOException {
        String uniqueName = UUID.randomUUID() + "_" + filename;
        Path targetPath = basePath.resolve(novelTextsDir).resolve(uniqueName);
        Files.writeString(targetPath, text);
        return accessUrlPrefix + "/" + novelTextsDir + "/" + uniqueName;
    }

    public String storeCoverImage(byte[] imageData, String originalFilename) throws IOException {
        return storeBytes(imageData, originalFilename, coversDir);
    }

    public String storeComicImage(byte[] imageData, String originalFilename) throws IOException {
        return storeBytes(imageData, originalFilename, comicImagesDir);
    }

    public String readNovelText(String url) throws IOException {
        Path filePath = resolveUrlToPath(url);
        return Files.readString(filePath);
    }

    public byte[] readFile(String url) throws IOException {
        Path filePath = resolveUrlToPath(url);
        return Files.readAllBytes(filePath);
    }

    public boolean fileExists(String url) {
        try {
            Path filePath = resolveUrlToPath(url);
            return Files.exists(filePath);
        } catch (Exception e) {
            return false;
        }
    }

    public void deleteFile(String url) throws IOException {
        Path filePath = resolveUrlToPath(url);
        Files.deleteIfExists(filePath);
    }

    private String storeFile(MultipartFile file, String subDir) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);
        String uniqueName = UUID.randomUUID() + extension;
        Path targetPath = basePath.resolve(subDir).resolve(uniqueName);
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
        return accessUrlPrefix + "/" + subDir + "/" + uniqueName;
    }

    private String storeBytes(byte[] data, String originalFilename, String subDir) throws IOException {
        String extension = getExtension(originalFilename);
        String uniqueName = UUID.randomUUID() + extension;
        Path targetPath = basePath.resolve(subDir).resolve(uniqueName);
        Files.write(targetPath, data);
        return accessUrlPrefix + "/" + subDir + "/" + uniqueName;
    }

    private Path resolveUrlToPath(String url) {
        // Remove the access URL prefix to get the relative path
        String relativePath = url.startsWith(accessUrlPrefix)
                ? url.substring(accessUrlPrefix.length() + 1)
                : url;
        return basePath.resolve(relativePath).normalize();
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex >= 0 ? filename.substring(dotIndex) : "";
    }

    /**
     * Health check: verifies that all storage sub-directories are accessible and writable.
     *
     * @return true if storage is healthy
     */
    public boolean isStorageHealthy() {
        try {
            for (String subDir : List.of(comicImagesDir, novelTextsDir, coversDir)) {
                Path dir = basePath.resolve(subDir);
                if (!Files.isDirectory(dir) || !Files.isWritable(dir)) {
                    log.warn("Storage directory not accessible: {}", dir);
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.error("Storage health check failed", e);
            return false;
        }
    }

    /**
     * Validates that all provided URLs point to existing files.
     *
     * @param urls collection of file URLs to validate
     * @return list of URLs that are missing (broken references)
     */
    public List<String> findMissingFiles(Collection<String> urls) {
        List<String> missing = new ArrayList<>();
        for (String url : urls) {
            if (url != null && !url.isBlank() && !fileExists(url)) {
                missing.add(url);
            }
        }
        return missing;
    }

    // Visible for testing
    Path getBasePath() {
        return basePath;
    }
}
