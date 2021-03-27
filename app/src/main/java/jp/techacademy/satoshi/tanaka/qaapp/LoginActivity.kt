package jp.techacademy.satoshi.tanaka.qaapp

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.preference.PreferenceManager
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_login.*


class LoginActivity : AppCompatActivity() {
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mCreateAccoutListener: OnCompleteListener<AuthResult>
    private lateinit var mLoginListener: OnCompleteListener<AuthResult>
    private lateinit var mDateBaseReference: DatabaseReference

    //アカウント作成時にフラグを立てて、ログイン処理時に名前をFirebaseに保存する
    private var mIsCreateAccount = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        mDateBaseReference = FirebaseDatabase.getInstance().reference
        //FirebaseAuthのオブジェクトを取得する
        mAuth = FirebaseAuth.getInstance()

        //アカウント作成処理のリスナー
        mCreateAccoutListener = OnCompleteListener { task ->
            if (task.isSuccessful) {
                //成功した場合➔ログインを行う
                val email = emailText.text.toString()
                val password = PasswordText.text.toString()
                login(email, password)
            } else {
                //失敗した場合➔エラーメッセージを表示
                errorMessage()
                //プログレスバーを非表示
                progressBar.visibility = View.GONE
            }
        }

        //ログイン処理のリスナー
        mLoginListener = OnCompleteListener { task ->
            if (task.isSuccessful) {
                //成功した場合
                val user = mAuth.currentUser
                val userRef = mDateBaseReference.child(UsersPATH).child(user!!.uid)

                if (mIsCreateAccount) {
                    //アカウント作成の時は表示名をFirebaseに保存する
                    val name = nameText.text.toString()
                    val date = HashMap<String, String>()
                    date["name"] = name
                    userRef.setValue(date)
                    //表示名をPreferenceに保存する
                    saveName(name)
                } else {
                    userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val date = snapshot.value as Map<*, *>?
                            saveName(date!!["name"] as String)
                        }

                        override fun onCancelled(firebaseError: DatabaseError) {}
                    })
                }
                //プログレスバーを非表示にする
                progressBar.visibility = View.GONE
                //Activityを閉じる
                finish()
            } else {
                //失敗した場合➔エラーを表示する
                errorMessage()

                //プログレスバーを非表示
                progressBar.visibility = View.GONE
            }
        }
        //タイトルの設定
        title = getString(R.string.login_title)

        createButton.setOnClickListener { v ->
            //キーボードが出てたら閉じる
            val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            im.hideSoftInputFromWindow(v.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)

            val email = emailText.text.toString()
            val password = PasswordText.text.toString()
            val name = nameText.text.toString()

            if (email.length != 0 && password.length >= 6 && name.length != 0) {
                //ログイン時に表示名を保存されるようにフラグを立てる
                mIsCreateAccount = true
                createAccount(email, password)
            } else {
                //エラーを表示
                loginerror()
            }
        }
        loginButton.setOnClickListener { v ->
            val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            im.hideSoftInputFromWindow(v.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)

            val email = emailText.text.toString()
            val password = PasswordText.text.toString()
            val name = nameText.text.toString()

            if (email.length != 0 && password.length >= 6) {
                //フラグを落とす
                mIsCreateAccount = false

                login(email, password)
            } else {
                loginerror()
            }

        }
    }

    private fun saveName(name: String) {
        //Preferenceに保存
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = sp.edit()
        editor.putString(NameKEY, name)
        editor.commit()

    }


    private fun login(email: String, password: String) {
        //プログレスバーを表示
        progressBar.visibility = View.VISIBLE
        //アカウントを作成する
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(mCreateAccoutListener)

    }

    private fun createAccount(email: String, password: String) {
        //プログレスバーを表示
        progressBar.visibility = View.VISIBLE
        //アカウントを作成する
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(mCreateAccoutListener)
    }


    fun errorMessage() {
        val view = findViewById<View>(android.R.id.content)
        Snackbar.make(view, getString(R.string.login_failure_message), Snackbar.LENGTH_LONG)
            .show()
    }

    fun loginerror() {
        Snackbar.make(createButton, getString(R.string.login_error_message), Snackbar.LENGTH_LONG)
            .show()
    }


}