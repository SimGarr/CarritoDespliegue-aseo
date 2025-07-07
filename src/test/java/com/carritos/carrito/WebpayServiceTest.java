package com.carritos.carrito;


import com.carritos.carrito.Model.TransaccionRequest;
import com.carritos.carrito.Service.WebpayService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

public class WebpayServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private WebpayService webpayService;

    private final String mockUrl = "https://webpay.api/mock";
    private final String mockCode = "commerce-code";
    private final String mockKey = "secret-key";

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        // Inyectar valores simulando @Value
        webpayService.getClass().getDeclaredFields();

        webpayService = new WebpayService(restTemplate);
        webpayService.getClass().getDeclaredFields();
        try {
            var urlField = WebpayService.class.getDeclaredField("webpayUrl");
            urlField.setAccessible(true);
            urlField.set(webpayService, mockUrl);

            var codeField = WebpayService.class.getDeclaredField("commerceCode");
            codeField.setAccessible(true);
            codeField.set(webpayService, mockCode);

            var keyField = WebpayService.class.getDeclaredField("apiKey");
            keyField.setAccessible(true);
            keyField.set(webpayService, mockKey);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testCrearTransaccionExito() throws Exception {
        TransaccionRequest request = new TransaccionRequest("123", "session", 1000, "http://retorno");

        String fakeResponse = "{\"token\":\"abc123\"}";

        when(restTemplate.exchange(
                eq(mockUrl),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(new ResponseEntity<>(fakeResponse, HttpStatus.OK));

        String token = webpayService.crearTransaccion(request);

        assertEquals("abc123", token);
    }

    @Test
    void testCrearTransaccionFalla() {
        TransaccionRequest request = new TransaccionRequest("123", "session", 1000, "http://retorno");

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class)))
                .thenThrow(new RuntimeException("error externo"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            webpayService.crearTransaccion(request);
        });

        assertTrue(ex.getMessage().contains("Error al crear transacci\u00f3n Webpay"));
    }

    @Test
    void testConfirmarTransaccionExito() throws Exception {
        String token = "abc123";
        String fakeResponse = "{\"response_code\":0,\"amount\":5000,\"session_id\":\"10\"}";

        when(restTemplate.exchange(
                eq(mockUrl + "/" + token),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(new ResponseEntity<>(fakeResponse, HttpStatus.OK));

        JsonNode json = webpayService.confirmarTransaccion(token);

        assertEquals(0, json.get("response_code").asInt());
        assertEquals(5000, json.get("amount").asInt());
    }

    @Test
    void testConfirmarTransaccionFalla() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(), eq(String.class)))
                .thenThrow(new RuntimeException("error externo"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            webpayService.confirmarTransaccion("abc123");
        });

        assertTrue(ex.getMessage().contains("Error al confirmar transacci\u00f3n Webpay"));
    }
}
