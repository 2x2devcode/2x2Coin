package com.x2xcoin.wallet.ui.home;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.x2xcoin.wallet.R;
import com.x2xcoin.wallet.api.X2xApiClient;
import com.x2xcoin.wallet.core.chain.NetworkParameters;
import com.x2xcoin.wallet.core.wallet.Amount;
import com.x2xcoin.wallet.core.wallet.WalletAccount;
import com.x2xcoin.wallet.data.WalletRepository;
import com.x2xcoin.wallet.ui.MainActivity;

import java.math.BigDecimal;
import java.util.List;

public final class HomeFragment extends Fragment {
    private static final String TAG = "X2xWallet";
    private static final long BALANCE_POLL_MS = 8_000L;

    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private final Runnable pollRunnable = () -> refresh(false);

    private TextView balanceValue;
    private TextView activeAddress;
    private TextView accountsSummary;
    private TextView networkStatus;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        balanceValue = view.findViewById(R.id.balanceValue);
        activeAddress = view.findViewById(R.id.activeAddress);
        accountsSummary = view.findViewById(R.id.accountsSummary);
        networkStatus = view.findViewById(R.id.networkStatus);
        Button refreshButton = view.findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(v -> refresh(true));
        refresh(false);
    }

    @Override
    public void onDestroyView() {
        pollHandler.removeCallbacks(pollRunnable);
        super.onDestroyView();
    }

    private void refresh(boolean invalidateCache) {
        if (!isAdded()) {
            return;
        }
        pollHandler.removeCallbacks(pollRunnable);
        WalletRepository repository = ((MainActivity) requireActivity()).repository();
        List<WalletAccount> accounts = repository.accounts();
        if (accounts.isEmpty()) {
            balanceValue.setText("Sem conta ativa");
            activeAddress.setText("");
            accountsSummary.setText("");
            return;
        }
        WalletAccount active = repository.activeAccount().orElse(accounts.get(0));
        postToUi(() -> {
            activeAddress.setText("Ativo: " + active.address());
            networkStatus.setText("Sincronizando rede...");
            balanceValue.setText("Carregando saldo...");
        });
        repository.runIo(() -> {
            try {
                X2xApiClient.NetworkStatus network = repository.refreshNetworkStatus();
                postToUi(() -> networkStatus.setText(
                        "Rede: " + network.blocks() + " blocos via " + network.source()
                                + " | peers " + network.peers()
                ));
            } catch (Exception e) {
                Log.e(TAG, "network status failed", e);
                String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                postToUi(() -> {
                    networkStatus.setText("Rede indisponível. Verifique API e explorer.");
                    balanceValue.setText("-- " + NetworkParameters.TICKER);
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                });
                return;
            }

            try {
                List<WalletRepository.AccountBalance> balances = repository.refreshAllBalances(invalidateCache);
                BigDecimal total = BigDecimal.ZERO;
                boolean scanning = false;
                StringBuilder summary = new StringBuilder();
                WalletAccount fundedInactive = null;
                for (WalletRepository.AccountBalance row : balances) {
                    BigDecimal amount = parseAmount(row.response().balance());
                    total = total.add(amount);
                    scanning = scanning || row.response().scanning();
                    boolean isActive = row.account().id().equals(active.id());
                    summary.append(row.account().label())
                            .append(isActive ? " (ativa)" : "")
                            .append("\n")
                            .append(row.account().address())
                            .append("\n")
                            .append(Amount.formatDisplay(row.response().balance()))
                            .append(" ")
                            .append(NetworkParameters.TICKER);
                    if (row.response().scanning()) {
                        summary.append(" · indexando");
                    }
                    summary.append("\n\n");
                    if (!isActive && amount.compareTo(BigDecimal.ZERO) > 0) {
                        fundedInactive = row.account();
                    }
                }
                if (fundedInactive != null && Amount.isZero(balances.stream()
                        .filter(row -> row.account().id().equals(active.id()))
                        .findFirst()
                        .map(row -> row.response().balance())
                        .orElse("0"))) {
                    summary.append("O saldo está em ")
                            .append(fundedInactive.address())
                            .append(". Abra Carteiras e toque nesse endereço para ativá-lo.");
                }
                boolean shouldPoll = scanning;
                BigDecimal displayTotal = total;
                postToUi(() -> {
                    balanceValue.setText(Amount.formatDisplay(displayTotal.toPlainString()) + " " + NetworkParameters.TICKER);
                    accountsSummary.setText(summary.toString().trim());
                    if (shouldPoll) {
                        networkStatus.setText("Indexando saldo na rede...");
                        pollHandler.postDelayed(pollRunnable, BALANCE_POLL_MS);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "balance refresh failed", e);
                String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                postToUi(() -> {
                    balanceValue.setText("-- " + NetworkParameters.TICKER);
                    Toast.makeText(requireContext(), "Saldo: " + message, Toast.LENGTH_LONG).show();
                    pollHandler.postDelayed(pollRunnable, BALANCE_POLL_MS);
                });
            }
        });
    }

    private static BigDecimal parseAmount(String balance) {
        try {
            return new BigDecimal(balance == null ? "0" : balance.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private void postToUi(Runnable action) {
        if (!isAdded()) {
            return;
        }
        requireActivity().runOnUiThread(() -> {
            if (isAdded()) {
                action.run();
            }
        });
    }
}
