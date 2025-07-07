package com.carritos.carrito.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductoCompletoDTO {
    private Long id;
    private String nombre;
    private Double precio;
    private int stock;
    private String descripcion;
}