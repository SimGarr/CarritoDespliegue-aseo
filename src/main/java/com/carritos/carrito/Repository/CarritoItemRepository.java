package com.carritos.carrito.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.carritos.carrito.Model.CarritoItem;

public interface CarritoItemRepository extends JpaRepository<CarritoItem, Long> {
    
    List<CarritoItem> findByUsuarioId(Long usuarioId);
    void deleteByUsuarioId(Long usuarioId);


}