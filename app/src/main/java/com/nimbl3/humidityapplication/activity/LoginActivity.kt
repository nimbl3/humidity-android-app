package com.nimbl3.humidityapplication.activity

import android.app.ProgressDialog
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.nimbl3.humidityapplication.R
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener {

    private val mGso by lazy {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("255969535922-4pd7j60pptut9is33nhib580s9r7mlbq.apps.googleusercontent.com")
                .requestEmail()
                .build()
    }

    private val mApiClient by lazy {
        GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, mGso)
                .build()
    }

    private val mAuth by lazy { FirebaseAuth.getInstance() }

    override fun onConnectionFailed(p0: ConnectionResult) {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        btSignIn.setOnClickListener({
            val signInIntent = Auth.GoogleSignInApi.getSignInIntent(mApiClient)
            startActivityForResult(signInIntent, 100)
        })
    }

    override fun onStart() {
        super.onStart()
        val currentUser = mAuth.currentUser
        if (currentUser != null) {
            val intent = Intent(LoginActivity@ this, ViewChartActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100) {
            ProgressDialog.show(this, "", "Loading ..", true)
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            if (result.isSuccess) {
                val acc = result.signInAccount
                firebaseLogin(acc)
            } else {
                Toast.makeText(LoginActivity@ this, "fail", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseLogin(acc: GoogleSignInAccount?) {

        val credential = GoogleAuthProvider.getCredential(acc?.idToken, null)
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, {
                    if (it.isSuccessful) {
                        Toast.makeText(LoginActivity@ this, "success", Toast.LENGTH_SHORT).show()
                        val intent = Intent(LoginActivity@ this, ViewChartActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(LoginActivity@ this, "fail", Toast.LENGTH_SHORT).show()
                    }
                })

    }

}
