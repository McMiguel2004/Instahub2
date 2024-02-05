package com.example.instahub;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.LiveData;
import java.util.ArrayList;
import java.util.List;

public class AppViewModel extends AndroidViewModel {

    // Clase interna para representar la información de los medios seleccionados
    public static class Media {
        public Uri uri;
        public String tipo;

        public Media(Uri uri, String tipo) {
            this.uri = uri;
            this.tipo = tipo;
        }
    }

    // LiveData para el post seleccionado y el medio seleccionado
    public MutableLiveData<Post> postSeleccionado = new MutableLiveData<>();
    public MutableLiveData<Media> mediaSeleccionado = new MutableLiveData<>();

    // LiveData para la lista de posts filtrados
    private MutableLiveData<List<Post>> filteredPosts = new MutableLiveData<>();
    private List<Post> allPosts = new ArrayList<>();

    public AppViewModel(@NonNull Application application) {
        super(application);
        // Aquí puedes inicializar cualquier cosa que necesites para tu ViewModel
    }

    // Método para establecer el medio seleccionado
    public void setMediaSeleccionado(Uri uri, String tipo) {
        mediaSeleccionado.setValue(new Media(uri, tipo));
    }

    // Método para establecer la lista completa de posts
    public void setAllPosts(List<Post> posts) {
        allPosts = posts;
        // Cuando se establece la lista completa de posts, inicialmente mostramos todos los posts
        filteredPosts.setValue(posts);
    }

    // Método para obtener la lista de posts filtrados
    public LiveData<List<Post>> getFilteredPosts() {
        return filteredPosts;
    }

    public void filtrarPorAutor(String autor) {
        List<Post> postsFiltrados = new ArrayList<>();
        for (Post post : allPosts) {
            if (post.getAuthor().equals(autor)) {
                postsFiltrados.add(post);
            }
        }
        filteredPosts.setValue(postsFiltrados);
    }


}
