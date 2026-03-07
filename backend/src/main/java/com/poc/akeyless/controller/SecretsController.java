package com.poc.akeyless.controller;

import com.poc.akeyless.model.SecretRequest;
import com.poc.akeyless.model.SecretResponse;
import com.poc.akeyless.service.AkeylessService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller exposing Akeyless secret operations.
 *
 * POST   /api/secrets          — create or update a static secret
 * GET    /api/secrets/{name}   — retrieve a secret value by path (URL-encoded)
 * GET    /api/secrets          — list secrets under a given path prefix
 */
@RestController
@RequestMapping("/api/secrets")
public class SecretsController {

    private final AkeylessService akeylessService;

    public SecretsController(AkeylessService akeylessService) {
        this.akeylessService = akeylessService;
    }

    /**
     * Save (create or update) a key-value secret.
     * Body: { "name": "/my-app/key", "value": "secret-value", "description": "optional" }
     */
    @PostMapping
    public ResponseEntity<SecretResponse> saveSecret(@Valid @RequestBody SecretRequest req) {
        SecretResponse result = akeylessService.createSecret(req.getName(), req.getValue(), req.getDescription());
        return result.isSuccess()
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }

    /**
     * Retrieve a secret value.
     * The path-encoded secret name is passed as a request param to avoid
     * ambiguity with slashes in path variables.
     *
     * GET /api/secrets/value?name=%2Fmy-app%2Fkey
     */
    @GetMapping("/value")
    public ResponseEntity<SecretResponse> getSecret(@RequestParam String name) {
        SecretResponse result = akeylessService.getSecret(name);
        return result.isSuccess()
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }

    /**
     * List static secrets under a path prefix.
     * GET /api/secrets/list?path=/my-app
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listSecrets() {
        List<String> secrets = akeylessService.listSecrets();
        return ResponseEntity.ok(Map.of("secrets", secrets, "count", secrets.size()));
    }

    /**
     * List all secrets under /manishpoc and return their values.
     * GET /api/secrets/all-values
     */
    @GetMapping("/all-values")
    public ResponseEntity<Map<String, Object>> getAllValues() {
        Map<String, String> values = akeylessService.getAllValues();
        return ResponseEntity.ok(Map.of("values", values, "count", values.size()));
    }
}
