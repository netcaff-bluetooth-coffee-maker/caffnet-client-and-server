package com.quew8.netcaff;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.quew8.netcaff.lib.access.AccessException;
import com.quew8.netcaff.lib.access.TransferAccess;
import com.quew8.properties.Property;
import com.quew8.properties.deferred.Deferred;
import com.quew8.properties.deferred.Promise;

import java.security.NoSuchAlgorithmException;

import static com.quew8.netcaff.lib.ble.CoffeeServerProfile.*;

/**
 * @author Quew8
 */
public class LoginActivity extends ServerCommunicationActivity {
    public static final String EXTRA_ACCESS_CODE = "extra_user_access_code";

    private TransferAccess transferAccess;
    private ValidEditText usernameField;
    private ValidEditText passwordField;
    private Button loginBtn;
    private ProgressBar loginProgress;
    private View loginError;
    private TextView loginErrorField;

    private Property<String> lastError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        try {
            this.transferAccess = new TransferAccess();
        } catch(NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        this.usernameField = new ValidEditText(findViewById(R.id.login_username_field), this::validateUsername);
        this.passwordField = new ValidEditText(findViewById(R.id.login_password_field), this::validatePassword);
        this.loginBtn = findViewById(R.id.login_btn);
        this.loginProgress = findViewById(R.id.login_progress);
        this.loginError = findViewById(R.id.login_error);
        this.loginErrorField = findViewById(R.id.login_error_field);

        this.lastError = new Property<>(null);

        usernameField.setValue("quew8");
        passwordField.setValue("hello");
        loginBtn.setOnClickListener(this::onLoginBtnClick);

        isWorking().addListener(this::onIsWorkingChange, true);
        usernameField.setOnFocusListener(this::onFieldFocusChange);
        passwordField.setOnFocusListener(this::onFieldFocusChange);
        lastError.addListener(this::onErrorChange, true);
    }

    @Override
    public void onStart() {
        super.onStart();
        lastError.set(null);
    }

    private void onIsWorkingChange(boolean working) {
        loginProgress.setVisibility(working ? View.VISIBLE : View.GONE);
        loginBtn.setEnabled(!working);
    }

    private void onFieldFocusChange(View field, boolean focused) {
        if(focused) {
            lastError.set(null);
        }
    }

    private void onErrorChange(String lastError) {
        if(lastError == null) {
            loginError.setVisibility(View.GONE);
        } else {
            loginError.setVisibility(View.VISIBLE);
            loginErrorField.setText(lastError);
        }
    }

    private String validateUsername(String username) {
        if(username.length() <= 0) {
            return "Username cannot be left blank";
        }
        if(username.contains(" ")) {
            return "Username cannot contain spaces";
        }
        return null;
    }

    private String validatePassword(String password) {
        if(password.length() <= 0) {
            return "Password cannot be left blank";
        }
        return null;
    }

    @Override
    public void onConnectedToManager() {
        super.onConnectedToManager();
    }

    @Override
    public void onDisconnectedFromManager() {
        super.onDisconnectedFromManager();
    }


    private Promise<Void> doRequestReply() {
        Deferred<Void> deferred = new Deferred<>();
        getServerInterface()
                .run(
                        getServerInterface().writeDescriptor(COFFEE_LOGIN_SERVICE_CONFIG, true)
                                .then(getServerInterface().writeCharacteristic(COFFEE_LOGIN_SERVICE_USERNAME))
                                .then(getServerInterface().writeCharacteristic(COFFEE_LOGIN_SERVICE_PASSWORD))
                                .then(getServerInterface().waitForUpdate(COFFEE_LOGIN_SERVICE_ACCESS_CODE))
                                .ifThen(
                                        () -> !getCoffeeServer().getResponseUserAccessCode().isValid(),
                                        getServerInterface().readCharacteristic(COFFEE_LOGIN_SERVICE_ERROR)
                                ).elseThen()
                )
                .done((s) -> runOnUiThread(() -> runOnUiThread(()->deferred.resolve(null))))
                .fail(() -> runOnUiThread(deferred::fail));
        return deferred.promise();
    }

    private void onLoginBtnClick(View v) {
        boolean valid = usernameField.forceValidate();
        valid = passwordField.forceValidate() && valid;
        if(valid) {
            String username = usernameField.getValue();
            String password = passwordField.getValue();
            byte[] passwordHash = transferAccess.hashUser(username, password);
            getCoffeeServer().getUserName().set(username);
            getCoffeeServer().getPassword().set(passwordHash);
            lastError.set(null);
            loginBtn.setText(R.string.logging_in_label);
            loginBtn.setEnabled(false);
            usernameField.setEnabled(false);
            passwordField.setEnabled(false);
            doRequestReply()
                    .always(() -> {
                        loginBtn.setText(R.string.login_label);
                        loginBtn.setEnabled(true);
                        usernameField.setEnabled(true);
                        passwordField.setEnabled(true);
                    })
                    .done(() -> {
                        if(getCoffeeServer().getResponseUserAccessCode().isValid()) {
                            Intent replyIntent = new Intent();
                            replyIntent.putExtra(
                                    EXTRA_ACCESS_CODE,
                                    getCoffeeServer().getResponseUserAccessCode().getAccessCode()
                            );
                            setResult(RESULT_OK, replyIntent);
                            finish();
                        } else {
                            String s = getCoffeeServer().getLoginError().get();
                            AccessException ex = AccessException.fromMessage(s);
                            if(ex != null) {
                                lastError.set(ex.getRawMessage());
                            } else {
                                lastError.set(s);
                            }
                        }
                    })
                    .fail(() -> lastError.set("Communication failed"));
        }
    }

    interface ValidationFunction {
        String validate(String in);
    }

    private static class ValidEditText implements TextWatcher {
        private final EditText field;
        private final ValidationFunction validationFunction;

        ValidEditText(EditText field, ValidationFunction validationFunction) {
            this.field = field;
            this.validationFunction = validationFunction;
            field.addTextChangedListener(this);
        }

        void setOnFocusListener(View.OnFocusChangeListener listener) {
            this.field.setOnFocusChangeListener(listener);
        }

        void setEnabled(boolean enabled) {
            field.setEnabled(enabled);
        }

        void setValue(String value) {
            field.setText(value);
        }

        String getValue() {
            return field.getText().toString();
        }

        boolean forceValidate() {
            String content = getValue();
            String error = validationFunction.validate(content);
            if(error != null) {
                field.setError(error);
                return false;
            }
            return true;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            forceValidate();
        }

        @Override
        public void afterTextChanged(Editable s) {}
    }

}
