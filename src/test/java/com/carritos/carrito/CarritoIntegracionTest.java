package com.carritos.carrito;

import com.carritos.carrito.Model.CarritoItem;
import com.carritos.carrito.Repository.CarritoItemRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.util.Map;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CarritoIntegracionTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private CarritoItemRepository carritoItemRepo;

    private String baseUrl;

    @BeforeEach
    void setup() {
        baseUrl = "http://localhost:" + port + "/api/carrito";
        carritoItemRepo.deleteAll();
    }

    @Test
    void testAgregarYObtenerItemsDeCarrito() {
        // Crear item
        CarritoItem item = new CarritoItem(null, 1L, 101L, 3);
        ResponseEntity<CarritoItem> response = rest.postForEntity(baseUrl + "/items/agregar", item, CarritoItem.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().getId());

        // Obtener por usuario
        ResponseEntity<CarritoItem[]> itemsResponse = rest.getForEntity(baseUrl + "/items/usuario/1", CarritoItem[].class);
        assertEquals(1, itemsResponse.getBody().length);
    }

    @Test
    void testVaciarCarrito() {
        carritoItemRepo.save(new CarritoItem(null, 2L, 101L, 1));
        rest.delete(baseUrl + "/items/vaciar/2");

        List<CarritoItem> resultado = carritoItemRepo.findByUsuarioId(2L);
        assertTrue(resultado.isEmpty());
    }

    @Test
    void testIniciarPagoFallaPorStockInsuficiente() {
        // Sin stock (porque microservicio de productos no responde correctamente en test)
        carritoItemRepo.save(new CarritoItem(null, 3L, 999L, 10));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Long>> body = new HttpEntity<>(Map.of("usuarioId", 3L), headers);

        ResponseEntity<String> response = rest.postForEntity(baseUrl + "/pagar", body, String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testAgregarProductoInvalido() {
        CarritoItem item = new CarritoItem(null, 1L, null, 3);
        ResponseEntity<String> response = rest.postForEntity(baseUrl + "/items/agregar", item, String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testEliminarItemEspecifico() {
        CarritoItem item = carritoItemRepo.save(new CarritoItem(null, 1L, 101L, 2));
        rest.delete(baseUrl + "/items/eliminar/" + item.getId());

        assertFalse(carritoItemRepo.findById(item.getId()).isPresent());
    }

    @Test
    void testObtenerCarritoVacio() {
        ResponseEntity<CarritoItem[]> response = rest.getForEntity(baseUrl + "/items/usuario/999", CarritoItem[].class);
        assertEquals(0, response.getBody().length);
    }
}
