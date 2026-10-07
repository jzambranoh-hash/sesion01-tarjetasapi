package com.nttdata.tarjetas.controller;

import com.nttdata.tarjetas.model.Tarjeta;
import com.nttdata.tarjetas.repository.TarjetaRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * API REST de tarjetas. Cada método atiende un endpoint.
 */
@RestController
@RequestMapping("/api/v1/tarjetas")
public class TarjetaController {

    private final TarjetaRepository tarjetaRepository;

    public TarjetaController(TarjetaRepository tarjetaRepository) {
        this.tarjetaRepository = tarjetaRepository;
    }

    /** GET /api/v1/tarjetas -> lista todas las tarjetas (200). */
    @GetMapping
    public List<Tarjeta> listarTarjetas() {
        return tarjetaRepository.listarTodas();
    }

    /** GET /api/v1/tarjetas/{numero} -> devuelve la tarjeta o 404 si no existe. */
    @GetMapping("/{numero}")
    public ResponseEntity<Tarjeta> buscarPorNumero(@PathVariable String numero) {
        // Busca la tarjeta en el repositorio.
        Tarjeta tarjeta = tarjetaRepository.buscarPorNumero(numero);

        // Devuelve 404 si no existe y 200 con la tarjeta si se encontró.
        if (tarjeta == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(tarjeta);
    }

    /** POST /api/v1/tarjetas -> valida y registra una tarjeta nueva. */
    @PostMapping
    public ResponseEntity<Tarjeta> registrarTarjeta(@RequestBody Tarjeta tarjeta) {
        if (tarjeta == null) {
            return ResponseEntity.badRequest().build();
        }

        // R1: el número debe tener 16 caracteres y el titular no puede estar vacío.
        if (tarjeta.getNumero() == null || tarjeta.getNumero().length() != 16
                || tarjeta.getTitular() == null || tarjeta.getTitular().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // R2: solo se permiten tarjetas de tipo CREDITO o DEBITO.
        if (!"CREDITO".equals(tarjeta.getTipo()) && !"DEBITO".equals(tarjeta.getTipo())) {
            return ResponseEntity.badRequest().build();
        }

        // R3: una tarjeta de crédito debe tener un límite mayor que cero.
        if ("CREDITO".equals(tarjeta.getTipo()) && !(tarjeta.getLimite() > 0)) {
            return ResponseEntity.badRequest().build();
        }

        // R4: no se permite registrar un número que ya esté en uso.
        if (tarjetaRepository.buscarPorNumero(tarjeta.getNumero()) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        // R5: se guarda siempre como ACTIVA y se responde con la tarjeta creada.
        tarjeta.setEstado("ACTIVA");
        tarjetaRepository.guardar(tarjeta);
        return ResponseEntity.status(HttpStatus.CREATED).body(tarjeta);
    }

    /** PUT /api/v1/tarjetas/{numero}/bloquear -> bloquea una tarjeta existente. */
    @PutMapping("/{numero}/bloquear")
    public ResponseEntity<Tarjeta> bloquearTarjeta(@PathVariable String numero) {
        // Reutiliza la búsqueda para obtener la tarjeta o detectar que no existe.
        Tarjeta tarjeta = tarjetaRepository.buscarPorNumero(numero);

        // Devuelve 404 cuando no se encuentra la tarjeta.
        if (tarjeta == null) {
            return ResponseEntity.notFound().build();
        }

        // Cambia el estado y devuelve la tarjeta actualizada.
        tarjeta.setEstado("BLOQUEADA");
        return ResponseEntity.ok(tarjeta);
    }
}
