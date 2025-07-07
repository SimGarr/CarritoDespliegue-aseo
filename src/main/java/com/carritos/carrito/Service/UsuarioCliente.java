package com.carritos.carrito.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.carritos.carrito.Model.UsuarioDTO;

@Component
public class UsuarioCliente {

    @Autowired
    private RestTemplate restTemplate;

    public UsuarioDTO obtenerUsuarioPorId(Long id) {
        String url = "http://localhost:8081/api/usuarios/" + id; 
        return restTemplate.getForObject(url, UsuarioDTO.class);
    }
}