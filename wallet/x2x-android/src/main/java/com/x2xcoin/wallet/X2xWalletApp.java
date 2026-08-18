package com.x2xcoin.wallet;

import android.app.Application;

import com.x2xcoin.wallet.api.ApiEndpoints;
import com.x2xcoin.wallet.api.X2xApiClient;
import com.x2xcoin.wallet.api.X2xHttpClientFactory;
import com.x2xcoin.wallet.data.WalletRepository;
import com.x2xcoin.wallet.security.BiometricVault;
import com.x2xcoin.wallet.security.NativePinProvider;

public final class X2xWalletApp extends Application {
    private WalletRepository walletRepository;
    private X2xApiClient apiClient;
    private BiometricVault biometricVault;

    @Override
    public void onCreate() {
        super.onCreate();
        NativePinProvider pinProvider = new NativePinProvider();
        apiClient = new X2xApiClient(
                ApiEndpoints.OFFICIAL_BASE_URLS,
                X2xHttpClientFactory.create(pinProvider, "2x2Coin-Wallet/1.1.7")
        );
        walletRepository = new WalletRepository(this, apiClient);
        try {
            biometricVault = new BiometricVault(this);
        } catch (Exception e) {
            throw new IllegalStateException("failed to initialize biometric vault", e);
        }
    }

    public WalletRepository walletRepository() {
        return walletRepository;
    }

    public X2xApiClient apiClient() {
        return apiClient;
    }

    public BiometricVault biometricVault() {
        return biometricVault;
    }
}
