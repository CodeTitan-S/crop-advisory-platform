package com.college.cropadvisory.controller;

import com.college.cropadvisory.dto.ApiResponse;
import com.college.cropadvisory.service.FileStorageService;
import com.college.cropadvisory.service.FileStorageService.StoredFile;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Photo upload for disease reports.
 *
 * <p>Uploading is authenticated; reading is not, because the farmer's photo is rendered by an
 * {@code <img>} tag, which cannot attach a bearer token. The stored name is an unguessable UUID, so
 * reads are effectively unlisted rather than public.
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    /** Stores one photo and returns the URL to save on the report. */
    @PostMapping
    public ResponseEntity<ApiResponse<String>> upload(@RequestParam("file") MultipartFile file) {
        String storedName = fileStorageService.store(file);

        // Built from the request so the URL is correct on localhost and on the deployed host alike.
        String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/files/")
                .path(storedName)
                .toUriString();

        return ResponseEntity.ok(new ApiResponse<>(true, "Photo uploaded", url));
    }

    /** Serves a stored photo. */
    @GetMapping("/{fileName}")
    public ResponseEntity<Resource> getFile(@PathVariable String fileName) {
        StoredFile stored = fileStorageService.load(fileName);

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                // The photo is displayed by a page on the frontend's origin, so the default
                // same-origin policy would stop the browser rendering it.
                .header("Cross-Origin-Resource-Policy", "cross-origin")
                .contentType(MediaType.parseMediaType(stored.contentType()))
                .body(stored.resource());
    }
}
