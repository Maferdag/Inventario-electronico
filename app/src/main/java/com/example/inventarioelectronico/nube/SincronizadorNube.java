package com.example.inventarioelectronico.nube;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.Log;

import com.example.inventarioelectronico.dao.AppDatabase;
import com.example.inventarioelectronico.dao.Producto;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SincronizadorNube {

    private static final String TAG = "Sincronizador";
    private static final String PREFS = "sincronizacion";
    private static final String CLAVE_SUBIR = "pendientes_subir";
    private static final String CLAVE_ELIMINAR = "pendientes_eliminar";

    private static final FirestoreRepository repositorio = new FirestoreRepository();
    private static final ExecutorService ejecutor = Executors.newSingleThreadExecutor();
    private static ConnectivityManager.NetworkCallback observador;

    // ---------- 1. ¿Hay internet de verdad? ----------
    public static boolean hayConexion(Context contexto) {
        ConnectivityManager cm = (ConnectivityManager)
                contexto.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        Network red = cm.getActiveNetwork();
        if (red == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(red);
        return caps != null
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    // ---------- 2. Lista de cambios pendientes (se guarda en el teléfono) ----------
    private static synchronized Set<String> leerPendientes(Context c, String clave) {
        SharedPreferences prefs = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return new HashSet<>(prefs.getStringSet(clave, new HashSet<>()));
    }

    private static synchronized void anotarPendiente(Context c, String clave, String id) {
        Set<String> conjunto = leerPendientes(c, clave);
        conjunto.add(id);
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putStringSet(clave, conjunto).apply();
    }

    private static synchronized void quitarPendiente(Context c, String clave, String id) {
        Set<String> conjunto = leerPendientes(c, clave);
        conjunto.remove(id);
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putStringSet(clave, conjunto).apply();
    }

    // ---------- 3. Lo que usa el resto de la app ----------

    // Crear o editar: sube el producto si hay conexión; si no, lo deja pendiente
    public static void subirProducto(Context contexto, Producto producto) {
        Context c = contexto.getApplicationContext();
        String id = String.valueOf(producto.getId());
        quitarPendiente(c, CLAVE_ELIMINAR, id);

        if (!hayConexion(c)) {
            anotarPendiente(c, CLAVE_SUBIR, id);
            Log.w(TAG, "Sin conexión: el producto " + id + " queda pendiente de subir");
            return;
        }
        enviar(c, producto);
    }

    // Eliminar: borra en la nube si hay conexión; si no, lo deja pendiente
    public static void eliminarProducto(Context contexto, String idProducto) {
        Context c = contexto.getApplicationContext();
        String id = String.valueOf(idProducto);
        quitarPendiente(c, CLAVE_SUBIR, id);

        if (!hayConexion(c)) {
            anotarPendiente(c, CLAVE_ELIMINAR, id);
            Log.w(TAG, "Sin conexión: la eliminación del producto " + id + " queda pendiente");
            return;
        }
        borrarEnNube(c, id);
    }

    // ---------- 4. Envío real a Firestore ----------
    private static void enviar(Context c, Producto producto) {
        String id = String.valueOf(producto.getId());
        repositorio.agregar(ProductoNube.desdeRoom(producto), new FirestoreRepository.Resultado() {
            @Override
            public void onExito() {
                quitarPendiente(c, CLAVE_SUBIR, id);
                Log.d(TAG, "Producto " + id + " sincronizado con la nube");
            }

            @Override
            public void onError(String mensaje) {
                anotarPendiente(c, CLAVE_SUBIR, id);
                Log.e(TAG, "Error al subir el producto " + id + ": " + mensaje);
            }
        });
    }

    private static void borrarEnNube(Context c, String id) {
        repositorio.eliminar(id, new FirestoreRepository.Resultado() {
            @Override
            public void onExito() {
                quitarPendiente(c, CLAVE_ELIMINAR, id);
                Log.d(TAG, "Producto " + id + " eliminado de la nube");
            }

            @Override
            public void onError(String mensaje) {
                anotarPendiente(c, CLAVE_ELIMINAR, id);
                Log.e(TAG, "Error al eliminar el producto " + id + ": " + mensaje);
            }
        });
    }

    // ---------- 5. Sincronizar lo pendiente cuando vuelve la conexión ----------
    public static void sincronizarPendientes(Context contexto) {
        Context c = contexto.getApplicationContext();
        if (!hayConexion(c)) return;
        sincronizar(c);
    }

    private static void sincronizar(Context c) {
        ejecutor.execute(() -> {
            Set<String> porSubir = leerPendientes(c, CLAVE_SUBIR);
            Set<String> porEliminar = leerPendientes(c, CLAVE_ELIMINAR);
            if (porSubir.isEmpty() && porEliminar.isEmpty()) return;

            Log.d(TAG, "Conexión disponible: " + porSubir.size()
                    + " cambios por subir y " + porEliminar.size() + " por eliminar");

            if (!porSubir.isEmpty()) {
                // Leemos la base local (Room) y subimos solo los productos pendientes
                List<Producto> locales = AppDatabase.getDatabase(c).productoDao().obtenerTodos();
                Set<String> encontrados = new HashSet<>();
                for (Producto p : locales) {
                    String id = String.valueOf(p.getId());
                    if (porSubir.contains(id)) {
                        encontrados.add(id);
                        enviar(c, p);
                    }
                }
                // Si un pendiente ya no existe en Room, ya no hay nada que subir
                for (String id : porSubir) {
                    if (!encontrados.contains(id)) quitarPendiente(c, CLAVE_SUBIR, id);
                }
            }
            for (String id : porEliminar) {
                borrarEnNube(c, id);
            }
        });
    }

    // ---------- 6. Vigilar la red: avisa cuando vuelve internet ----------
    public static synchronized void iniciarObservador(Context contexto) {
        if (observador != null) return;
        Context c = contexto.getApplicationContext();
        ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return;

        observador = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities caps) {
                if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                    sincronizar(c);
                }
            }
        };
        cm.registerDefaultNetworkCallback(observador);
        sincronizarPendientes(c); // por si quedaron pendientes de antes
    }

    public static synchronized void detenerObservador(Context contexto) {
        if (observador == null) return;
        ConnectivityManager cm = (ConnectivityManager)
                contexto.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            try {
                cm.unregisterNetworkCallback(observador);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "El observador ya estaba desregistrado");
            }
        }
        observador = null;
    }
}