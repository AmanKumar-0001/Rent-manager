package com.tenantmanager.auth;

import android.content.Intent;
import com.google.android.gms.auth.api.identity.AuthorizationClient;
import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.internal.ApiKey;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

public class FakeAuthorizationClient implements AuthorizationClient {
    public Task<AuthorizationResult> authorizeTask;
    public AuthorizationResult intentResult;
    public ApiException intentException;
    public AuthorizationRequest lastRequest;

    @Override
    public Task<AuthorizationResult> authorize(AuthorizationRequest request) {
        lastRequest = request;
        return authorizeTask != null ? authorizeTask : Tasks.forResult(null);
    }

    @Override
    public AuthorizationResult getAuthorizationResultFromIntent(Intent intent) throws ApiException {
        if (intentException != null) {
            throw intentException;
        }
        return intentResult;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public ApiKey getApiKey() {
        return null;
    }
}

