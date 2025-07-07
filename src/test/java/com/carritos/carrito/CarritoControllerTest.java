package com.carritos.carrito;


import com.carritos.carrito.Controller.CarritoController;
import com.carritos.carrito.Service.CarritoService;
import com.carritos.carrito.Service.UsuarioCliente;
import com.carritos.carrito.Service.WebpayService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CarritoController.class)
public class CarritoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CarritoService carritoService;

    @MockBean
    private WebpayService webpayService;

    @MockBean
    private UsuarioCliente usuarioCliente;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Long usuarioId = 10L;

    @BeforeEach
    void setup() {
        // Configuración general si es necesaria
    }

    @Test
    void testIniciarPagoExitoso() throws Exception {
        when(carritoService.verificarStock(usuarioId)).thenReturn(true);
        when(carritoService.calcularTotalCompra(usuarioId)).thenReturn(5000);
        when(webpayService.crearTransaccion(any())).thenReturn("tokentest");

        mockMvc.perform(post("/api/carrito/pagar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("usuarioId", usuarioId))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data.token").value("tokentest"));
    }

    @Test
    void testIniciarPagoConStockInsuficiente() throws Exception {
        when(carritoService.verificarStock(usuarioId)).thenReturn(false);

        mockMvc.perform(post("/api/carrito/pagar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("usuarioId", usuarioId))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Stock insuficiente para uno o más productos"));
    }

    @Test
    void testIniciarPagoConUsuarioInvalido() throws Exception {
        mockMvc.perform(post("/api/carrito/pagar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("usuarioId", 0))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("UsuarioId inválido"));
    }

    @Test
    void testIniciarPagoConExcepcionEnWebpay() throws Exception {
        when(carritoService.verificarStock(usuarioId)).thenReturn(true);
        when(carritoService.calcularTotalCompra(usuarioId)).thenReturn(5000);
        when(webpayService.crearTransaccion(any())).thenThrow(new RuntimeException("Falla en Webpay"));

        mockMvc.perform(post("/api/carrito/pagar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("usuarioId", usuarioId))))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.status").value("error"));
    }
}
