package com.example.inventarioelectronico.nube;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class FirestoreRepository {

    // Colección (como una "tabla") donde se guardan los productos
    private final CollectionReference coleccion =
            FirebaseFirestore.getInstance().collection("productos");

    // Avisos para saber si una operación salió bien o mal
    public interface Resultado {
        void onExito();
        void onError(String mensaje);
    }

    public interface ResultadoLista {
        void onDatos(List<ProductoNube> productos);
        void onError(String mensaje);
    }

    // CREAR: guarda el producto con su id como nombre del documento
    public void agregar(ProductoNube p, Resultado r) {
        coleccion.document(p.getId()).set(p)
                .addOnSuccessListener(v -> r.onExito())
                .addOnFailureListener(e -> r.onError(e.getMessage()));
    }

    // LEER todos los productos (una sola vez)
    public void obtenerTodos(ResultadoLista r) {
        coleccion.get()
                .addOnSuccessListener(snapshot ->
                        r.onDatos(convertir(snapshot.getDocuments())))
                .addOnFailureListener(e -> r.onError(e.getMessage()));
    }

    // LEER uno solo (consulta individual)
    public void obtenerPorId(String id, ResultadoLista r) {
        coleccion.document(id).get()
                .addOnSuccessListener(doc -> {
                    List<ProductoNube> lista = new ArrayList<>();
                    ProductoNube p = doc.toObject(ProductoNube.class);
                    if (p != null) { p.setId(doc.getId()); lista.add(p); }
                    r.onDatos(lista);
                })
                .addOnFailureListener(e -> r.onError(e.getMessage()));
    }

    // ACTUALIZAR: set() sobre el mismo id reemplaza los datos
    public void actualizar(ProductoNube p, Resultado r) {
        agregar(p, r);
    }

    // ELIMINAR
    public void eliminar(String id, Resultado r) {
        coleccion.document(id).delete()
                .addOnSuccessListener(v -> r.onExito())
                .addOnFailureListener(e -> r.onError(e.getMessage()));
    }

    // TIEMPO REAL: se ejecuta cada vez que alguien cambia algo en la nube.
    // Devuelve un "registro" para poder dejar de escuchar con remove().
    public ListenerRegistration escucharCambios(ResultadoLista r) {
        return coleccion.addSnapshotListener((snapshot, error) -> {
            if (error != null) { r.onError(error.getMessage()); return; }
            if (snapshot != null) r.onDatos(convertir(snapshot.getDocuments()));
        });
    }

    private List<ProductoNube> convertir(
            List<? extends com.google.firebase.firestore.DocumentSnapshot> docs) {
        List<ProductoNube> lista = new ArrayList<>();
        for (com.google.firebase.firestore.DocumentSnapshot d : docs) {
            ProductoNube p = d.toObject(ProductoNube.class);
            if (p != null) { p.setId(d.getId()); lista.add(p); }
        }
        return lista;
    }
}