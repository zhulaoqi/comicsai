package com.comicsai.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService(
                tempDir.toString(),
                "comic-images",
                "novel-texts",
                "covers",
                "/files"
        );
    }

    @Test
    void initDirectories_shouldCreateSubDirectories() {
        assertTrue(Files.exists(tempDir.resolve("comic-images")));
        assertTrue(Files.exists(tempDir.resolve("novel-texts")));
        assertTrue(Files.exists(tempDir.resolve("covers")));
    }

    @Test
    void storeCoverImage_bytes_shouldStoreAndReturnUrl() throws IOException {
        byte[] imageData = "fake-image-data".getBytes();
        String url = fileStorageService.storeCoverImage(imageData, "cover.jpg");

        assertNotNull(url);
        assertTrue(url.startsWith("/files/covers/"));
        assertTrue(url.endsWith(".jpg"));
        assertTrue(fileStorageService.fileExists(url));
    }

    @Test
    void storeComicImage_bytes_shouldStoreAndReturnUrl() throws IOException {
        byte[] imageData = "fake-comic-image".getBytes();
        String url = fileStorageService.storeComicImage(imageData, "page1.png");

        assertNotNull(url);
        assertTrue(url.startsWith("/files/comic-images/"));
        assertTrue(url.endsWith(".png"));
        assertTrue(fileStorageService.fileExists(url));
    }

    @Test
    void storeNovelText_shouldStoreAndBeReadable() throws IOException {
        String text = "Once upon a time in a faraway land...";
        String url = fileStorageService.storeNovelText(text, "chapter1.txt");

        assertNotNull(url);
        assertTrue(url.startsWith("/files/novel-texts/"));

        String readBack = fileStorageService.readNovelText(url);
        assertEquals(text, readBack);
    }

    @Test
    void readFile_shouldReturnStoredBytes() throws IOException {
        byte[] data = "binary-content".getBytes();
        String url = fileStorageService.storeCoverImage(data, "test.jpg");

        byte[] readBack = fileStorageService.readFile(url);
        assertArrayEquals(data, readBack);
    }

    @Test
    void fileExists_shouldReturnFalseForNonExistent() {
        assertFalse(fileStorageService.fileExists("/files/covers/nonexistent.jpg"));
    }

    @Test
    void deleteFile_shouldRemoveFile() throws IOException {
        byte[] data = "to-delete".getBytes();
        String url = fileStorageService.storeCoverImage(data, "delete-me.jpg");

        assertTrue(fileStorageService.fileExists(url));
        fileStorageService.deleteFile(url);
        assertFalse(fileStorageService.fileExists(url));
    }

    @Test
    void storeComicImage_noExtension_shouldWork() throws IOException {
        byte[] data = "no-ext".getBytes();
        String url = fileStorageService.storeComicImage(data, "noext");

        assertNotNull(url);
        assertTrue(fileStorageService.fileExists(url));
    }
}
