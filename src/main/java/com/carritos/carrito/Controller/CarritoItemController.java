package com.carritos.carrito.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.carritos.carrito.Model.CarritoItem;
import com.carritos.carrito.Repository.CarritoItemRepository;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/carrito/items")
@CrossOrigin(origins = "*")
public class CarritoItemController {

    @Autowired
    private CarritoItemRepository carritoItemRepository;

    @Operation(summary = "Agregar un nuevo ítem al carrito")
@PostMapping("/agregar")
public ResponseEntity<?> agregarItem(@RequestBody CarritoItem item) {
    if (item.getProductoId() == null) {
        return ResponseEntity.badRequest().body("El productoId es obligatorio");
    }
    if (item.getCantidad() <= 0) {
        return ResponseEntity.badRequest().body("La cantidad debe ser mayor que cero");
    }
    if (item.getUsuarioId() == null) {
        return ResponseEntity.badRequest().body("El usuarioId es obligatorio");
    }
    
    CarritoItem guardado = carritoItemRepository.save(item);
    return ResponseEntity.ok(guardado);
}
    @Operation(summary = "Obtener todos los ítems del carrito para un usuario específico")
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<CarritoItem>> obtenerItemsPorUsuario(@PathVariable Long usuarioId) {
        List<CarritoItem> items = carritoItemRepository.findByUsuarioId(usuarioId);
        return ResponseEntity.ok(items);
    }

    @Operation(summary = "Eliminar un ítem específico del carrito por su ID")
    @DeleteMapping("/eliminar/{itemId}")
    public ResponseEntity<String> eliminarItem(@PathVariable Long itemId) {
        carritoItemRepository.deleteById(itemId);
        return ResponseEntity.ok("Item eliminado");
    }

    @Operation(summary = "Vaciar el carrito completo de un usuario")
    @DeleteMapping("/vaciar/{usuarioId}")
    public ResponseEntity<String> vaciarCarrito(@PathVariable Long usuarioId) {
        carritoItemRepository.deleteByUsuarioId(usuarioId);
        return ResponseEntity.ok("Carrito vaciado");
    }
}
