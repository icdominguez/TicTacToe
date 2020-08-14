package com.icdominguez.tictactoe.ui;

import android.content.DialogInterface;
import android.os.Bundle;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.icdominguez.tictactoe.Constants;
import com.icdominguez.tictactoe.R;
import com.icdominguez.tictactoe.model.Game;
import com.icdominguez.tictactoe.model.User;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

public class GameActivity extends AppCompatActivity {

    List<ImageView> cells;
    TextView tvPlayer1, tvPlayer2;
    FirebaseAuth firebaseAuth;
    FirebaseFirestore db;
    String uid, gameId, playerOneName = "", playerTwoName = "", winnerId = "";
    Game game;
    ListenerRegistration gameListener = null;
    FirebaseUser firebaseUser;
    String playerName;
    User userPlayerOne, userPlayerTwo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        initViews();
        initGame();
    }

    private void initViews() {
        tvPlayer1 = findViewById(R.id.textViewPlayer1);
        tvPlayer2 = findViewById(R.id.textViewPlayer2);

        cells = new ArrayList<>();
        cells.add((ImageView) findViewById(R.id.imageView0));
        cells.add((ImageView) findViewById(R.id.imageView1));
        cells.add((ImageView) findViewById(R.id.imageView2));
        cells.add((ImageView) findViewById(R.id.imageView3));
        cells.add((ImageView) findViewById(R.id.imageView4));
        cells.add((ImageView) findViewById(R.id.imageView5));
        cells.add((ImageView) findViewById(R.id.imageView6));
        cells.add((ImageView) findViewById(R.id.imageView7));
        cells.add((ImageView) findViewById(R.id.imageView8));
    }

    private void initGame() {
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        uid = firebaseUser.getUid();

        Bundle extras = getIntent().getExtras();
        gameId = extras.getString(Constants.EXTRA_GAME_ID);
    }

    @Override
    protected void onStart() {
        super.onStart();
        gameListener();
    }

    private void gameListener() {
        gameListener = db.collection("games")
                .document(gameId)
                .addSnapshotListener(GameActivity.this, new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                        if(e != null) {
                            Toast.makeText(GameActivity.this, "Error al obtener datos de la jugada", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String source = documentSnapshot != null && documentSnapshot.getMetadata().hasPendingWrites() ? "Local" : "Server";

                        if(documentSnapshot.exists() && source.equals("Server")) {
                            game = documentSnapshot.toObject(Game.class);
                            if(playerOneName.isEmpty() || playerTwoName.isEmpty()) {
                                getPlayerNames();
                            }
                            updateUI();
                        }
                        updatePlayerUI();
                    }
                });
    }

    private void updatePlayerUI() {
        if(game.isTurnPlayerOne()) {
            tvPlayer1.setTextColor(getResources().getColor(R.color.colorPrimary));
            tvPlayer2.setTextColor(getResources().getColor(R.color.greyColor));
        } else {
            tvPlayer1.setTextColor(getResources().getColor(R.color.greyColor));
            tvPlayer2.setTextColor(getResources().getColor(R.color.colorAccent));
        }

        if(!game.getWinnerId().isEmpty()) {
            winnerId = game.getWinnerId();
            showDialogGameOver();
        }
    }

    private void updateUI() {
        for(int i=0; i < cells.size(); i++) {
            int cell = game.getSelectedCells().get(i);
            ImageView ivCurrentCell = cells.get(i);

            if(cell == 0) {
                ivCurrentCell.setImageResource(R.drawable.empty_square);
            } else if (cell == 1) {
                ivCurrentCell.setImageResource(R.drawable.ic_player_one);
            } else if (cell == 2) {
                ivCurrentCell.setImageResource(R.drawable.ic_player_two);
            }
        }
    }

    private void getPlayerNames() {
        db.collection("users")
                .document(game.getPlayerOneId())
                .get()
                .addOnSuccessListener(GameActivity.this, new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {

                        userPlayerOne = documentSnapshot.toObject(User.class);

                        playerOneName = documentSnapshot.get("name").toString();
                        tvPlayer1.setText(playerOneName);

                        if(game.getPlayerOneId().equals(uid)) {
                            playerName = playerOneName;
                        }
                    }
                });

        db.collection("users")
                .document(game.getPlayerTwoId())
                .get()
                .addOnSuccessListener(GameActivity.this, new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        userPlayerTwo = documentSnapshot.toObject(User.class);

                        playerTwoName = documentSnapshot.get("name").toString();
                        tvPlayer2.setText(playerTwoName);

                        if(game.getPlayerTwoId().equals(uid)) {
                            playerName = playerTwoName;
                        }
                    }
                });
    }

    @Override
    protected void onStop() {
        if(gameListener != null) {
            gameListener.remove();
        }
        super.onStop();
    }

    private void updateGame(String numCell) {
        int cellPosition = Integer.parseInt(numCell);

        if(game.getSelectedCells().get(cellPosition) != 0) {
            Toast.makeText(this, "Seleccione una casilla libre", Toast.LENGTH_SHORT).show();
        } else {
            if(game.isTurnPlayerOne()) {
                cells.get(cellPosition).setImageResource(R.drawable.ic_player_one);
                game.getSelectedCells().set(cellPosition, 1);
            } else {
                cells.get(cellPosition).setImageResource(R.drawable.ic_player_two);
                game.getSelectedCells().set(cellPosition, 2);
            }

            if(solutionExists()) {
                game.setWinnerId(uid);
                Toast.makeText(this, "Hay solucion", Toast.LENGTH_SHORT).show();
            } else if(tieExists()) {
                Toast.makeText(this, "Hay empate", Toast.LENGTH_SHORT).show();
                game.setWinnerId("TIE");
            } else {
                changeTurn();
            }

            db.collection("games")
                    .document(gameId)
                    .set(game)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {

                        }
                    }).addOnFailureListener(GameActivity.this, new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.w("ERROR", "Error al guardar la jugada");
                }
            });
        }
    }

    public void selectedCell(View view) {
        if(!game.getWinnerId().isEmpty()) {
            Toast.makeText(this, "La partida ha terminado",Toast.LENGTH_SHORT).show();
        } else {
            if(game.isTurnPlayerOne() && game.getPlayerOneId().equals(uid)) {
                updateGame(view.getTag().toString());
            } else if (!game.isTurnPlayerOne() && game.getPlayerTwoId().equals(uid)) {
                updateGame(view.getTag().toString());
            } else {
                Toast.makeText(this,"No es tu turno aun", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void changeTurn() {
        game.setTurnPlayerOne(!game.isTurnPlayerOne());
    }

    private boolean tieExists() {
        boolean exists = false;
        boolean freeSquare = false;

        for(int i = 0; i < cells.size(); i++) {
            if (game.getSelectedCells().get(i) == 0) {
                freeSquare = true;
                break;
            }
        }
        if (!freeSquare) {
            exists = true;
        }

        return exists;
    }

    private boolean solutionExists() {
        boolean exists = false;
        List<Integer> selectedCells = game.getSelectedCells();

        if(selectedCells.get(0) == selectedCells.get(1)
                && selectedCells.get(1) == selectedCells.get(2)
                && selectedCells.get(2) != 0) { // 0 - 1 - 2
            exists = true;
        } else if(selectedCells.get(3) == selectedCells.get(4)
                && selectedCells.get(4) == selectedCells.get(5)
                && selectedCells.get(5) != 0) { // 3 - 4 - 5
            exists = true;
        } else if(selectedCells.get(6) == selectedCells.get(7)
                && selectedCells.get(7) == selectedCells.get(8)
                && selectedCells.get(8) != 0) { // 6 - 7 - 8
            exists = true;
        } else if(selectedCells.get(0) == selectedCells.get(3)
                && selectedCells.get(3) == selectedCells.get(6)
                && selectedCells.get(6) != 0) { // 0 - 3 - 6
            exists = true;
        } else if(selectedCells.get(1) == selectedCells.get(4)
                && selectedCells.get(4) == selectedCells.get(7)
                && selectedCells.get(7) != 0) { // 1 - 4 - 7
            exists = true;
        } else if(selectedCells.get(2) == selectedCells.get(5)
                && selectedCells.get(5) == selectedCells.get(8)
                && selectedCells.get(8) != 0) { // 2 - 5 - 8
            exists = true;
        } else if(selectedCells.get(0) == selectedCells.get(4)
                && selectedCells.get(4) == selectedCells.get(8)
                && selectedCells.get(8) != 0) { // 0 - 4 - 8
            exists = true;
        } else if(selectedCells.get(2) == selectedCells.get(4)
                && selectedCells.get(4) == selectedCells.get(6)
                && selectedCells.get(6) != 0) { // 2 - 4 - 6
            exists = true;
        }

        return exists;
    }

    public void showDialogGameOver() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        View v = getLayoutInflater().inflate(R.layout.dialog_game_over, null);

        TextView tvPoints = v.findViewById(R.id.textViewPoints);
        TextView tvInformation = v.findViewById(R.id.textViewInformation);
        LottieAnimationView gameOverAnimation = v.findViewById(R.id.animation_view);

        builder.setTitle("Game over");
        builder.setCancelable(false);
        builder.setView(v);

        if(winnerId == "TIE") {
            updatePuntuation(1);
            tvInformation.setText("¡" + playerName + " has empatado!");
            tvPoints.setText("+1 punto");
        } else if(winnerId.equals(uid)) {
            updatePuntuation(3);
            tvInformation.setText("¡" + playerName + " has ganado!");
            tvPoints.setText("+3 puntos");
        } else {
            updatePuntuation(0);
            tvInformation.setText("¡" + playerName + " has perdido!");
            tvPoints.setText("0 points");
            gameOverAnimation.setAnimation("thumbs_down_animation.json");
        }

        gameOverAnimation.playAnimation();

        builder.setPositiveButton("Salir", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void updatePuntuation(int pointsAchieved) {
        User playerToUpdate = null;

        if(playerOneName.equals(userPlayerOne.getName())) {
            userPlayerOne.setPoints(userPlayerOne.getPoints() + pointsAchieved);
            userPlayerOne.setGamesPlayed(userPlayerOne.getGamesPlayed() + 1);
            playerToUpdate = userPlayerOne;
        } else {
            userPlayerTwo.setPoints(userPlayerTwo.getPoints() + pointsAchieved);
            userPlayerTwo.setPoints(userPlayerTwo.getGamesPlayed() + 1);
            playerToUpdate = userPlayerTwo;
        }

        db.collection("users")
                .document(uid)
                .set(playerToUpdate)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                    }
                }).addOnFailureListener(GameActivity.this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });

    }
}