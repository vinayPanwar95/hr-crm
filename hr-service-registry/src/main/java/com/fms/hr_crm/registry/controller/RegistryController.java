package com.fms.hr_crm.registry.controller;

import com.fms.hr_crm.registry.model.RegisterRequest;
import com.fms.hr_crm.registry.model.ServiceInstance;
import com.fms.hr_crm.registry.service.RegistryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

/**
 * REST API for the service registry.
 *
 * <ul>
 *   <li>{@code POST   /registry/register}         — register or re-register</li>
 *   <li>{@code PUT    /registry/{name}/heartbeat}  — signal still-alive</li>
 *   <li>{@code DELETE /registry/{name}}            — deregister on shutdown</li>
 *   <li>{@code GET    /registry/{name}}            — resolve a single service URL</li>
 *   <li>{@code GET    /registry}                   — list all registered services</li>
 * </ul>
 */
@RestController
@RequestMapping("/registry")
@RequiredArgsConstructor
@Slf4j
public class RegistryController {

    private final RegistryService registryService;

    @PostMapping("/register")
    public ResponseEntity<ServiceInstance> register(@RequestBody RegisterRequest request) {
        log.info("register() — name={}, url={}", request.name(), request.url());
        return ResponseEntity.ok(registryService.register(request));
    }

    @PutMapping("/{name}/heartbeat")
    public ResponseEntity<ServiceInstance> heartbeat(@PathVariable String name) {
        log.debug("heartbeat() — name={}", name);
        return registryService.heartbeat(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<Void> deregister(@PathVariable String name) {
        log.info("deregister() — name={}", name);
        return registryService.deregister(name)
                ? ResponseEntity.noContent().<Void>build()
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/{name}")
    public ResponseEntity<ServiceInstance> find(@PathVariable String name) {
        log.debug("find() — name={}", name);
        return registryService.find(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public Collection<ServiceInstance> findAll() {
        log.debug("findAll() — returning {} entries", registryService.findAll().size());
        return registryService.findAll();
    }
}