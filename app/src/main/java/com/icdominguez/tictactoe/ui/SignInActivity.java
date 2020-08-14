package com.icdominguez.tictactoe.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.icdominguez.tictactoe.R;
import com.icdominguez.tictactoe.model.User;

public class SignInActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPassword;
    private Button btnSignIn;
    private ProgressBar pbSingIn;
    private ScrollView formSignIn;
    FirebaseAuth firebaseAuth;
    FirebaseFirestore db;
    String name, email, password;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        findViews();
        firebaseAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        events();
        changeSignInFormVisibility(true);
    }

    private void findViews() {
        etName = findViewById(R.id.editTextName);
        etEmail = findViewById(R.id.editTextEmail);
        etPassword = findViewById(R.id.editTextPassword);
        btnSignIn = findViewById(R.id.buttonSignIn);
        pbSingIn = findViewById(R.id.progressBarSignIn);
        formSignIn = findViewById(R.id.formSignIn);
    }

    private void events() {
        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                name = etName.getText().toString();
                email = etEmail.getText().toString();
                password = etPassword.getText().toString();

                if(name.isEmpty()) {
                    etName.setError("El nombre es obligatorio");
                } else if(email.isEmpty()) {
                    etEmail.setError("El email es obligatorio");
                } else if(password.isEmpty()) {
                    etPassword.setError("La password es obligatoria");
                } else {
                    createUser();
                }
            }
        });
    }

    private void createUser() {
        changeSignInFormVisibility(false);
        firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()) {
                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    updateUI(user);
                } else {
                    Log.w("TAG", "createUserWithEmail: failure", task.getException());
                    Toast.makeText(SignInActivity.this, "Error el registro de usuario. Intentelo de nuevo mas tarde", Toast.LENGTH_SHORT).show();
                    updateUI(null);
                }
            }
        });
    }

    private void updateUI(FirebaseUser user) {
        if(user != null) {
            User newUser = new User(name, 0 ,0);

            db.collection("users")
                    .document(user.getUid())
                    .set(newUser)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            finish();
                            Intent i = new Intent(SignInActivity.this, FindGameActivity.class);
                            startActivity(i);
                        }
                    });
        } else {
            changeSignInFormVisibility(true);
            etPassword.setError("Nombre, EMail y/o contraseña incorrectos");
            etPassword.requestFocus();
        }
    }

    private void changeSignInFormVisibility(boolean showForm) {
        pbSingIn.setVisibility(showForm ? View.GONE : View.VISIBLE);
        formSignIn.setVisibility(showForm ? View.VISIBLE : View.GONE);
    }
}