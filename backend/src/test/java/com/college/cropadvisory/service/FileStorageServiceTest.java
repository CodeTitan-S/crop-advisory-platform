package com.college.cropadvisory.service;

import com.college.cropadvisory.exception.BadRequestException;
import com.college.cropadvisory.exception.NotFoundException;
import com.college.cropadvisory.service.FileStorageService.StoredFile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link FileStorageService}.
 * Covers upload validation, the generated name, and the read path's refusal to touch anything this
 * service did not store itself.
 */
class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService service() {
        return new FileStorageService(tempDir.toString());
    }

    private MockMultipartFile upload(String contentType, byte[] content) {
        return new MockMultipartFile("file", "photo", contentType, content);
    }

    @Test
    @DisplayName("store – saves the bytes under a generated uuid name")
    void store_savesImageWithGeneratedName() throws IOException {
        byte[] content = {1, 2, 3, 4};

        String storedName = service().store(upload("image/jpeg", content));

        assertTrue(storedName.matches("[0-9a-f-]{36}\\.jpg"), "unexpected name: " + storedName);
        assertArrayEquals(content, Files.readAllBytes(tempDir.resolve(storedName)));
    }

    @Test
    @DisplayName("store – refuse: empty file")
    void store_rejectsEmptyUpload() {
        assertThrows(BadRequestException.class, () -> service().store(upload("image/png", new byte[0])));
    }

    @Test
    @DisplayName("store – refuse: not an image")
    void store_rejectsNonImageType() {
        assertThrows(BadRequestException.class,
                () -> service().store(upload("application/pdf", new byte[] {1})));
    }

    @Test
    @DisplayName("store – refuse: svg, which can carry script")
    void store_rejectsSvg() {
        assertThrows(BadRequestException.class,
                () -> service().store(upload("image/svg+xml", "<svg/>".getBytes())));
    }

    @Test
    @DisplayName("store – refuse: image over 2 MB")
    void store_rejectsOversizedImage() {
        byte[] tooBig = new byte[2 * 1024 * 1024 + 1];
        assertThrows(BadRequestException.class, () -> service().store(upload("image/png", tooBig)));
    }

    @Test
    @DisplayName("load – returns the stored resource with its content type")
    void load_returnsResourceAndContentType() throws IOException {
        FileStorageService service = service();
        String storedName = service.store(upload("image/png", new byte[] {9, 9}));

        StoredFile stored = service.load(storedName);

        assertEquals("image/png", stored.contentType());
        assertTrue(stored.resource().exists());
        assertEquals(2, stored.resource().contentLength());
    }

    @Test
    @DisplayName("load – refuse: names we did not generate, including traversal attempts")
    void load_rejectsUnrecognisedNames() {
        FileStorageService service = service();

        for (String name : new String[] {"../application.properties", "secret.png", "abc.txt", ""}) {
            assertThrows(NotFoundException.class, () -> service.load(name), "should reject: " + name);
        }
    }

    @Test
    @DisplayName("load – refuse: a generated name that is not on disk")
    void load_rejectsMissingFile() {
        assertThrows(NotFoundException.class,
                () -> service().load("11111111-2222-3333-4444-555555555555.png"));
    }
}
