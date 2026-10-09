package com.college.cropadvisory.service;

import com.college.cropadvisory.exception.BadRequestException;
import com.college.cropadvisory.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Stores one uploaded photo per call on the service filesystem and reads it back for display.
 *
 * <p>Files are named with a random UUID plus an extension derived from the validated content type,
 * so a request can never steer a write or a read to a path of its choosing.
 *
 * <p>The bytes live on the instance's disk, which is the simplest thing that works and needs no
 * third-party account. On a host with an ephemeral filesystem (Render's free tier) they do not
 * survive a redeploy or restart — see RENDER_DEPLOYMENT.md. Point {@code app.upload.dir} at a
 * mounted disk, or replace this class with an object-store client, to make them durable.
 */
@Service
public class FileStorageService {

    /**
     * Accepted image types. SVG is deliberately absent: it can carry script and would be served
     * back from our own origin.
     */
    private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/gif", "gif");

    private static final Map<String, String> CONTENT_TYPE_BY_EXTENSION = Map.of(
            "jpg", "image/jpeg",
            "png", "image/png",
            "webp", "image/webp",
            "gif", "image/gif");

    /** Kept in step with spring.servlet.multipart.max-file-size; checked here so it is testable. */
    private static final long MAX_BYTES = 2L * 1024 * 1024;

    /**
     * Magic-byte signatures that identify each accepted image format. The client-supplied
     * Content-Type header is untrusted, so every upload's leading bytes are matched against
     * these instead — a polyglot/script labelled "image/png" is rejected here.
     */
    private static final Map<String, List<byte[]>> MAGIC_BYTES_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", List.of(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
            "image/png", List.of(new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A}),
            "image/gif", List.of(
                    "GIF87a".getBytes(StandardCharsets.US_ASCII),
                    "GIF89a".getBytes(StandardCharsets.US_ASCII)),
            // RIFF....WEBP: format marker sits 8 bytes in.
            "image/webp", List.of(new byte[] {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'}));

    /** Longest signature above; reads this many bytes (at most) for the sniff. */
    private static final int MAX_SIGNATURE_LENGTH = 12;

    /** Matches only names this class generates, which rules out traversal in the read path. */
    private static final Pattern STORED_NAME = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp|gif)$");

    private final Path uploadDir;

    public FileStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not create upload directory " + this.uploadDir, ex);
        }
    }

    /**
     * Validates and stores one image.
     *
     * @return the generated stored name, which the controller turns into a public URL
     */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Choose a photo to upload");
        }
        String extension = EXTENSION_BY_CONTENT_TYPE.get(file.getContentType());
        if (extension == null) {
            throw new BadRequestException("Only JPEG, PNG, WebP or GIF photos are accepted");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BadRequestException("Photo must be 2 MB or smaller");
        }
        if (!hasAcceptedImageSignature(file)) {
            throw new BadRequestException(
                "This file is not a valid " + extension.toUpperCase(Locale.ROOT) + " photo");
        }

        String storedName = UUID.randomUUID() + "." + extension;
        try {
            file.transferTo(uploadDir.resolve(storedName));
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not store the uploaded photo", ex);
        }
        return storedName;
    }

    /**
     * Sniffs the upload's leading bytes and requires one of the accepted signatures for the
     * claimed content type. Reading the stream (rather than an in-memory copy) keeps this safe
     * for the 2 MB cap; Spring's multipart support re-reads the stream on transferTo.
     */
    private boolean hasAcceptedImageSignature(MultipartFile file) {
        List<byte[]> signatures = MAGIC_BYTES_BY_CONTENT_TYPE.get(file.getContentType());
        if (signatures == null) {
            return false;
        }
        byte[] head = new byte[MAX_SIGNATURE_LENGTH];
        int read;
        try (InputStream in = file.getInputStream()) {
            read = in.readNBytes(head, 0, head.length);
        } catch (IOException ex) {
            return false;
        }
        for (byte[] signature : signatures) {
            if (matchesSignature(head, read, signature)) {
                return true;
            }
        }
        return false;
    }

    /** The WEBP entry has wildcard bytes (0) in the RIFF size field; other signatures are exact. */
    private boolean matchesSignature(byte[] head, int read, byte[] signature) {
        if (read < signature.length) {
            return false;
        }
        boolean isWebp = signature.length == 12;
        for (int i = 0; i < signature.length; i++) {
            if (isWebp && i >= 4 && i < 8) {
                continue; // RIFF chunk-size field: four bytes we cannot predict, so skip them.
            }
            if (head[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }

    /** Reads a previously stored photo back, rejecting any name this class did not generate. */
    public StoredFile load(String fileName) {
        if (fileName == null || !STORED_NAME.matcher(fileName).matches()) {
            throw new NotFoundException("Photo not found");
        }

        Resource resource;
        try {
            resource = new UrlResource(uploadDir.resolve(fileName).toUri());
        } catch (MalformedURLException ex) {
            throw new NotFoundException("Photo not found");
        }
        if (!resource.exists()) {
            throw new NotFoundException("Photo not found");
        }

        String extension = fileName.substring(fileName.lastIndexOf('.') + 1);
        return new StoredFile(resource, CONTENT_TYPE_BY_EXTENSION.get(extension));
    }

    /** A stored photo and the content type to serve it with. */
    public record StoredFile(Resource resource, String contentType) {
    }
}
