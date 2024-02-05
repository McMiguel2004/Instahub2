package com.example.instahub;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Map;

public class HomeFragment extends Fragment {

    private AppViewModel appViewModel;
    private NavController navController;
    private RecyclerView postsRecyclerView;

    private EditText editTextBusqueda;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        view.findViewById(R.id.gotoNewPostFragmentButton).setOnClickListener(v -> {
            navController.navigate(R.id.newPostFragment);
            appViewModel = new ViewModelProvider(requireActivity()).get(AppViewModel.class);
        });

        postsRecyclerView = view.findViewById(R.id.postsRecyclerView);
        editTextBusqueda = view.findViewById(R.id.editTextBusqueda);

        appViewModel = new ViewModelProvider(requireActivity()).get(AppViewModel.class);

        // Configurar el botón de búsqueda
        view.findViewById(R.id.btnFiltrarAutor).setOnClickListener(v -> filtrarPosts());

        Query query = FirebaseFirestore.getInstance().collection("posts").limit(50);

        FirestoreRecyclerOptions<Post> options = new FirestoreRecyclerOptions.Builder<Post>()
                .setQuery(query, Post.class)
                .setLifecycleOwner(this)
                .build();

        postsRecyclerView.setAdapter(new PostsAdapter(options));
    }
    private void filtrarPosts() {
        // Obtener el correo electrónico del usuario actualmente autenticado
        String autor = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        // Filtrar los posts por autor utilizando el ViewModel
        appViewModel.filtrarPorAutor(autor);

        // Obtener el contenido de búsqueda desde editTextBusqueda
        String contenido = editTextBusqueda.getText().toString().trim();

        // Verificar si el campo de búsqueda está vacío
        if (!contenido.isEmpty()) {

            // Si hay contenido de búsqueda, filtrar los posts por contenido
            Query query = FirebaseFirestore.getInstance().collection("posts")
                    .whereEqualTo("content", contenido).limit(50);
            FirestoreRecyclerOptions<Post> options = new FirestoreRecyclerOptions.Builder<Post>()
                    .setQuery(query, Post.class)
                    .setLifecycleOwner(this)
                    .build();
            postsRecyclerView.setAdapter(new PostsAdapter(options));
        } else {
            // Si el campo de búsqueda está vacío, mostrar todos los posts
            Query query = FirebaseFirestore.getInstance().collection("posts").limit(50);
            FirestoreRecyclerOptions<Post> options = new FirestoreRecyclerOptions.Builder<Post>()
                    .setQuery(query, Post.class)
                    .setLifecycleOwner(this)
                    .build();
            postsRecyclerView.setAdapter(new PostsAdapter(options));
        }
    }




    class PostsAdapter extends FirestoreRecyclerAdapter<Post, PostsAdapter.PostViewHolder> {
        public PostsAdapter(@NonNull FirestoreRecyclerOptions<Post> options) {
            super(options);
        }


        @NonNull
        @Override
        public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new PostViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_post, parent, false));
        }

        @Override
        protected void onBindViewHolder(@NonNull PostViewHolder holder, int position, @NonNull final Post post) {
            if (post.authorPhotoUrl == null) {
                holder.authorPhotoImageView.setImageResource(R.drawable.user);
            } else {
                Glide.with(getContext()).load(post.authorPhotoUrl).circleCrop().into(holder.authorPhotoImageView);
            }
            holder.authorTextView.setText(post.author);
            holder.contentTextView.setText(post.content);

            // Gestion de likes
            final String postKey = getSnapshots().getSnapshot(position).getId();
            final String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            if (post.likes.containsKey(uid)) {
                holder.likeImageView.setImageResource(R.drawable.like_on);
            } else {
                holder.likeImageView.setImageResource(R.drawable.like_off);
            }
            holder.numLikesTextView.setText(String.valueOf(post.likes.size()));
            holder.likeImageView.setOnClickListener(v -> {
                FirebaseFirestore.getInstance().collection("posts")
                        .document(postKey)
                        .update("likes." + uid, post.likes.containsKey(uid) ?
                                FieldValue.delete() : true);
            });

            // Gestion de comentarios
            holder.commentImageView.setOnClickListener(v -> {
                // Mostrar un cuadro de diálogo para ingresar el comentario
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                builder.setTitle("Agregar Comentario");

                // Setear el layout del cuadro de diálogo
                View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_comment, null);
                builder.setView(dialogView);

                EditText commentEditText = dialogView.findViewById(R.id.commentEditText);

                builder.setPositiveButton("Publicar", (dialog, which) -> {
                    String commentContent = commentEditText.getText().toString().trim();

                    if (!commentContent.isEmpty()) {
                        // Guardar el ID del post al que se está añadiendo el comentario
                        String postId = getSnapshots().getSnapshot(position).getId();

                        // Crear un nuevo documento en la colección "comentarios" de Firestore
                        Map<String, Object> comentarioMap = new HashMap<>();
                        comentarioMap.put("postId", postId); // ID del post al que se está añadiendo el comentario
                        comentarioMap.put("contenido", commentContent);
                        comentarioMap.put("timestamp", FieldValue.serverTimestamp());

                        // Agregar el comentario a la colección de comentarios en Firestore
                        FirebaseFirestore.getInstance().collection("comentarios")
                                .add(comentarioMap)
                                .addOnSuccessListener(documentReference -> {
                                    // El comentario se guardó exitosamente en Firestore
                                    // Puedes mostrar un mensaje o realizar alguna otra acción si lo deseas
                                })
                                .addOnFailureListener(e -> {
                                    // Ocurrió un error al guardar el comentario en Firestore
                                    Toast.makeText(getContext(), "Error al publicar el comentario", Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Toast.makeText(getContext(), "Por favor, ingresa un comentario", Toast.LENGTH_SHORT).show();
                    }
                });

                builder.setNegativeButton("Cancelar", (dialog, which) -> {
                    // El usuario canceló la acción, no hacemos nada
                });

                AlertDialog dialog = builder.create();
                dialog.show();
            });

            // Miniatura de media
            if (post.mediaUrl != null) {
                holder.mediaImageView.setVisibility(View.VISIBLE);
                if ("audio".equals(post.mediaType)) {
                    Glide.with(requireView()).load(R.drawable.audio).centerCrop().into(holder.mediaImageView);
                } else {
                    Glide.with(requireView()).load(post.mediaUrl).centerCrop().into(holder.mediaImageView);
                }
                holder.mediaImageView.setOnClickListener(view -> {
                    appViewModel.postSeleccionado.setValue(post);
                    navController.navigate(R.id.mediaFragment);
                });
            } else {
                holder.mediaImageView.setVisibility(View.GONE);
            }

            // Verificar la autoría del post antes de permitir eliminar
            holder.deleteButton.setOnClickListener(view -> {
                String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                if (post.uid.equals(currentUserId)) {
                    FirebaseFirestore.getInstance().collection("posts")
                            .document(postKey)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                // Éxito al eliminar
                            })
                            .addOnFailureListener(e -> {
                                // Manejar fallo en la eliminación
                            });
                } else {
                    Toast.makeText(getContext(), "No tienes permisos para eliminar este post", Toast.LENGTH_SHORT).show();
                }
            });
        }

        class PostViewHolder extends RecyclerView.ViewHolder {
            ImageView authorPhotoImageView, likeImageView, mediaImageView, commentImageView, deleteButton;
            TextView authorTextView, contentTextView, numLikesTextView;

            public PostViewHolder(@NonNull View itemView) {
                super(itemView);
                authorPhotoImageView = itemView.findViewById(R.id.photoImageView);
                likeImageView = itemView.findViewById(R.id.likeImageView);
                mediaImageView = itemView.findViewById(R.id.mediaImage);
                authorTextView = itemView.findViewById(R.id.authorTextView);
                contentTextView = itemView.findViewById(R.id.contentTextView);
                numLikesTextView = itemView.findViewById(R.id.numLikesTextView);
                commentImageView = itemView.findViewById(R.id.commentButton);
                deleteButton = itemView.findViewById(R.id.deleteButton); // Nuevo botón para eliminar el post
            }
        }
    }
}