package com.college.cropadvisory.service;

import com.college.cropadvisory.exception.BadRequestException;
import com.college.cropadvisory.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

        String storedName = UUID.randomUUID() + "." + extension;
        try {
            file.transferTo(uploadDir.resolve(storedName));
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not store the uploaded photo", ex);
        }
        return storedName;
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
