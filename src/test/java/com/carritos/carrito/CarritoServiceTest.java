package com.carritos.carrito;


import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;

import com.carritos.carrito.Model.CarritoItem;
import com.carritos.carrito.Model.ProductoCompletoDTO;
import com.carritos.carrito.Repository.CarritoItemRepository;
import com.carritos.carrito.Service.CarritoService;
import com.carritos.carrito.Service.CorreoService;

public class CarritoServiceTest {

    @Mock
    private CarritoItemRepository carritoItemRepo;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private CorreoService correoService;

    @InjectMocks
    private CarritoService carritoService;

    private CarritoItem item;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        item = new CarritoItem();
        item.setId(1L);
        item.setUsuarioId(10L);
        item.setProductoId(100L);
        item.setCantidad(2);
    }

    @Test
    void testAgregarProductoValido() {
        carritoService.agregarProducto(10L, 100L, 2);
        verify(carritoItemRepo).save(any(CarritoItem.class));
    }

    @Test
    void testAgregarProductoConCantidadInvalida() {
        assertThrows(IllegalArgumentException.class, () -> {
            carritoService.agregarProducto(10L, 100L, 0);
        });
    }

    @Test
    void testObtenerCarritoPorUsuario() {
        when(carritoItemRepo.findByUsuarioId(10L)).thenReturn(List.of(item));
        List<CarritoItem> items = carritoService.obtenerCarritoPorUsuario(10L);
        assertEquals(1, items.size());
    }

    @Test
    void testCalcularTotalCompra() {
        when(carritoItemRepo.findByUsuarioId(10L)).thenReturn(List.of(item));
        when(restTemplate.getForObject(anyString(), eq(ProductoCompletoDTO.class)))
            .thenReturn(new ProductoCompletoDTO(100L, "Producto", 2000.0, 10, "desc"));

        int total = carritoService.calcularTotalCompra(10L);
        assertEquals(4000, total);
    }

    @Test
    void testVerificarStockSuficiente() {
        when(carritoItemRepo.findByUsuarioId(10L)).thenReturn(List.of(item));
        when(restTemplate.getForObject(anyString(), eq(ProductoCompletoDTO.class)))
            .thenReturn(new ProductoCompletoDTO(100L, "Producto", 2000.0, 10, "desc"));

        boolean ok = carritoService.verificarStock(10L);
        assertTrue(ok);
    }

    @Test
    void testVerificarStockInsuficiente() {
        when(carritoItemRepo.findByUsuarioId(10L)).thenReturn(List.of(item));
        when(restTemplate.getForObject(anyString(), eq(ProductoCompletoDTO.class)))
            .thenReturn(new ProductoCompletoDTO(100L, "Producto", 2000.0, 1, "desc"));

        boolean ok = carritoService.verificarStock(10L);
        assertFalse(ok);
    }

    @Test
    void testVaciarCarrito() {
        carritoService.vaciarCarrito(10L);
        verify(carritoItemRepo).deleteByUsuarioId(10L);
    }
}
