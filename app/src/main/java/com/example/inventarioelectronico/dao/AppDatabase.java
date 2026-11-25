package com.example.inventarioelectronico.dao;

import android.content.Context;
import androidx.room3.Database;
import androidx.room3.Room;
import androidx.room3.RoomDatabase;

@Database(entities = {Producto.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    // Declaración del DAO que Room implementará automáticamente
    public abstract ProductoDao productoDao();

    // Variable volátil para asegurar cambios visibles entre hilos de forma inmediata
    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "inventario_database")
                            // NUEVO: Esto le dice a Room que si la versión cambia,
                            // borre la base de datos vieja y cree la nueva sin crashearse
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
