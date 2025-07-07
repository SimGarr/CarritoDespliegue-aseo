package com.carritos.carrito;



import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.carritos.carrito.Service.CorreoService;

@ExtendWith(MockitoExtension.class)
public class CorreoServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private CorreoService correoService;

    private final String destinatarioValido = "cliente@example.com";
    private final String nombreValido = "Juan Pérez";
    private final String detalleHTML = "<table>Detalle de compra</table>";

    @Test
    void enviarCorreo_CorreoValido_EnvioExitoso() {
        // Configurar respuesta simulada
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
            .thenReturn(new ResponseEntity<>("OK", HttpStatus.OK));

        // Ejecutar
        assertDoesNotThrow(() -> 
            correoService.enviarCorreo(destinatarioValido, nombreValido, detalleHTML)
        );

        // Verificar llamada
        verify(restTemplate).postForEntity(
            eq("https://api.mailjet.com/v3.1/send"),
            any(HttpEntity.class),
            eq(String.class)
        );
    }

    @Test
    void enviarCorreo_EmailVacio_ErrorValidacion() {
        // Ejecutar y verificar excepción
        Exception exception = assertThrows(IllegalArgumentException.class, () -> 
            correoService.enviarCorreo("", nombreValido, detalleHTML)
        );

        assertEquals("El email no puede estar vacío", exception.getMessage());
        verify(restTemplate, never()).postForEntity(anyString(), any(), any());
    }

    @Test
    void enviarCorreo_ServidorCaido_ErrorConexion() {
        // Simular error de conexión
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
            .thenThrow(new RuntimeException("Error de conexión"));

        // Ejecutar y verificar excepción
        assertThrows(RuntimeException.class, () -> 
            correoService.enviarCorreo(destinatarioValido, nombreValido, detalleHTML)
        );
    }

    @Test
    void enviarCorreo_FormatoRequest_Correcto() {
        // Capturar request
        ArgumentCaptor<HttpEntity<String>> requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        when(restTemplate.postForEntity(anyString(), requestCaptor.capture(), eq(String.class)))
            .thenReturn(new ResponseEntity<>("OK", HttpStatus.OK));

        // Ejecutar
        correoService.enviarCorreo(destinatarioValido, nombreValido, detalleHTML);

        // Verificar estructura del request
        HttpEntity<String> request = requestCaptor.getValue();
        HttpHeaders headers = request.getHeaders();
        String body = request.getBody();

        // Verificar headers
        assertEquals(MediaType.APPLICATION_JSON, headers.getContentType());
        assertNotNull(headers.get("Authorization"));
        
        // Verificar cuerpo
        assertTrue(body.contains(destinatarioValido));
        assertTrue(body.contains(nombreValido));
        assertTrue(body.contains(detalleHTML));
        assertTrue(body.contains("¡Gracias por tu compra!"));
    }

    @Test
    void enviarCorreo_RespuestaNoExitosa_Error() {
        // Simular respuesta 500
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
            .thenReturn(new ResponseEntity<>("Error", HttpStatus.INTERNAL_SERVER_ERROR));

        // Ejecutar y verificar excepción
        assertThrows(RuntimeException.class, () -> 
            correoService.enviarCorreo(destinatarioValido, nombreValido, detalleHTML)
        );
    }

    @Test
    void enviarCorreo_AutenticacionFallida_Error401() {
        // Simular respuesta 401
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
            .thenReturn(new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED));

        // Ejecutar y verificar excepción
        Exception exception = assertThrows(RuntimeException.class, () -> 
            correoService.enviarCorreo(destinatarioValido, nombreValido, detalleHTML)
        );

        assertTrue(exception.getMessage().contains("401"));
    }
}