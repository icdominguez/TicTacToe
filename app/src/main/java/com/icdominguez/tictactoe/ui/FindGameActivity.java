package com.icdominguez.tictactoe.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.icdominguez.tictactoe.Constants;
import com.icdominguez.tictactoe.R;
import com.icdominguez.tictactoe.model.Game;

import javax.annotation.Nullable;

public class FindGameActivity extends AppCompatActivity {
    private TextView tvLoadingMessage;
    private ProgressBar pbGames;
    private ScrollView layoutProgressBar, layoutGameMenu;
    private Button btnPlay, btnRanking;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore db;
    private FirebaseUser firebaseUser;
    private String uid;
    private String gameId;
    private ListenerRegistration listenerRegistration = null;
    private LottieAnimationView animationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_game);

        findViews();
        events();

        initFirebase();
        initProgressBar();
    }

    private void findViews() {
        tvLoadingMessage = findViewById(R.id.textViewLoading);
        pbGames = findViewById(R.id.progressBarGames);
        btnPlay = findViewById(R.id.buttonPlay);
        btnRanking = findViewById(R.id.buttonRanking);
        layoutProgressBar = findViewById(R.id.layoutProgressBar);
        layoutGameMenu = findViewById(R.id.layoutGameMenu);
    }


    private void initFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        uid = firebaseUser.getUid();
    }

    private void events() {
        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeMenuVisibility(false);
                searchGame();
            }
        });

        btnRanking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
    }

    private void initProgressBar() {
        animationView = findViewById(R.id.animation_view);
        pbGames.setIndeterminate(true);
        tvLoadingMessage.setText("Cargando ...");

        changeMenuVisibility(true);
    }

    private void changeMenuVisibility(boolean showMenu) {
        layoutProgressBar.setVisibility(showMenu ? View.GONE : View.VISIBLE);
        layoutGameMenu.setVisibility(showMenu ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(gameId != null) {
            changeMenuVisibility(false);
            waitPlayer();
        } else {
            changeMenuVisibility(true);
        }
        changeMenuVisibility(true);
    }

    private void searchGame() {
        tvLoadingMessage.setText("Buscando una partida libre ...");
        animationView.playAnimation();

        Log.i("traza", "Searching for a game ...");

        db.collection("games")
                .whereEqualTo("playerTwoId","")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.getResult().size() == 0) {
                            createNewGame();
                        } else {
                            boolean find = false;

                            for(DocumentSnapshot docGame : task.getResult().getDocuments()) {
                                if(!docGame.get("playerOneId").equals(uid)) {
                                    find = true;

                                    gameId = docGame.getId();
                                    Game game = docGame.toObject(Game.class);
                                    game.setPlayerTwoId(uid);

                                    db.collection("games")
                                            .document(gameId)
                                            .set(game)
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    tvLoadingMessage.setText("Partida libre encontrada! ¡Comenzamos!");


                                                    Log.i("traza", "Game found!");


                                                    animationView.setRepeatCount(0);
                                                    animationView.setAnimation("checked_animation.json");
                                                    animationView.playAnimation();

                                                    final Handler handler = new Handler();
                                                    final Runnable r = new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            startGame();
                                                        }
                                                    };

                                                    handler.postDelayed(r, 1500);
                                                }
                                            }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            changeMenuVisibility(true);
                                            Toast.makeText(FindGameActivity.this, "Hubo algún error buscando partida", Toast.LENGTH_SHORT).show();
                                        }
                                    });

                                    break;
                                }

                                if(!find) {
                                    createNewGame();

                                    Log.i("traza", "Game not found. Creating new game ...");
                                }
                            }
                        }
                    }
                });
    }

    private void createNewGame() {
        tvLoadingMessage.setText("Creando una partida nueva ...");
        Game newGame = new Game(uid);

        db.collection("games")
                .add(newGame)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        gameId = documentReference.getId();
                        waitPlayer();

                        Log.i("traza", "Game created. Waiting users ... ");
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                changeMenuVisibility(true);
                Toast.makeText(FindGameActivity.this, "Error al crear la nueva partida", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void waitPlayer() {
        tvLoadingMessage.setText("Esperando a otro jugador ...");

        listenerRegistration = db.collection("games")
                .document(gameId)
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                        if(!documentSnapshot.get("playerTwoId").equals("")) {
                            tvLoadingMessage.setText("¡Ya ha llegado un jugador! Comienza la partida");
                            animationView.setRepeatCount(0);
                            animationView.setAnimation("checked_animation.json");
                            animationView.playAnimation();

                            Log.i("traza", "An user arrives. The game will start");

                            final Handler handler = new Handler();
                            final Runnable r = new Runnable() {
                                @Override
                                public void run() {
                                    startGame();
                                }
                            };

                            handler.postDelayed(r, 1500);
                        }
                    }
                });
    }

    private void startGame() {
        if(listenerRegistration != null) {
            listenerRegistration.remove();
        }
        Intent i = new Intent(FindGameActivity.this, GameActivity.class);
        i.putExtra(Constants.EXTRA_GAME_ID, gameId);
        startActivity(i);
        gameId = "";

        Log.i("traza", "A game started");
    }

    @Override
    protected void onStop() {
        if(listenerRegistration != null) {
            listenerRegistration.remove();
        }

        if(gameId != "") {
            db.collection("games")
                    .document(gameId)
                    .delete()
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            gameId = "";
                        }
                    });
        }
        super.onStop();
    }
}