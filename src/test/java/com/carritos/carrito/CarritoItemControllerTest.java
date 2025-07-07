package com.carritos.carrito;



import com.carritos.carrito.Controller.CarritoItemController;
import com.carritos.carrito.Model.CarritoItem;
import com.carritos.carrito.Repository.CarritoItemRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CarritoItemController.class)
public class CarritoItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CarritoItemRepository carritoItemRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private CarritoItem item;

    @BeforeEach
    void setUp() {
        item = new CarritoItem(1L, 10L, 100L, 2);
    }

    @Test
    void testAgregarItemExitoso() throws Exception {
        when(carritoItemRepository.save(any())).thenReturn(item);

        mockMvc.perform(post("/api/carrito/items/agregar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(item)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.productoId").value(100L));
    }

    @Test
    void testAgregarItemSinProductoId() throws Exception {
        item.setProductoId(null);

        mockMvc.perform(post("/api/carrito/items/agregar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(item)))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("El productoId es obligatorio"));
    }

    @Test
    void testAgregarItemConCantidadCero() throws Exception {
        item.setCantidad(0);

        mockMvc.perform(post("/api/carrito/items/agregar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(item)))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("La cantidad debe ser mayor que cero"));
    }

    @Test
    void testAgregarItemSinUsuarioId() throws Exception {
        item.setUsuarioId(null);

        mockMvc.perform(post("/api/carrito/items/agregar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(item)))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("El usuarioId es obligatorio"));
    }

    @Test
    void testObtenerItemsPorUsuario() throws Exception {
        when(carritoItemRepository.findByUsuarioId(10L)).thenReturn(List.of(item));

        mockMvc.perform(get("/api/carrito/items/usuario/10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].cantidad").value(2));
    }

    @Test
    void testEliminarItem() throws Exception {
        doNothing().when(carritoItemRepository).deleteById(1L);

        mockMvc.perform(delete("/api/carrito/items/eliminar/1"))
            .andExpect(status().isOk())
            .andExpect(content().string("Item eliminado"));
    }

    @Test
    void testVaciarCarrito() throws Exception {
        doNothing().when(carritoItemRepository).deleteByUsuarioId(10L);

        mockMvc.perform(delete("/api/carrito/items/vaciar/10"))
            .andExpect(status().isOk())
            .andExpect(content().string("Carrito vaciado"));
    }
}
