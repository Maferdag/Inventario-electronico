package com.example.inventarioelectronico.dao;

import androidx.room3.Dao;
import androidx.room3.Delete;
import androidx.room3.Insert;
import androidx.room3.Query;
import androidx.room3.Update;

import java.util.List;

@Dao
public interface ProductoDao {

    @Query("SELECT * FROM inventario")
    List<Producto> obtenerTodos();

    @Insert
    long insertar(Producto producto);

    @Update
    void actualizar(Producto producto);

    @Delete
    void eliminar(Producto producto);
}
