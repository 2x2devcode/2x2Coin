package com.x2xcoin.wallet.ui.wallets;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.x2xcoin.wallet.R;
import com.x2xcoin.wallet.core.chain.NetworkParameters;
import com.x2xcoin.wallet.core.wallet.Amount;
import com.x2xcoin.wallet.core.wallet.WalletAccount;
import com.x2xcoin.wallet.data.WalletRepository;
import com.x2xcoin.wallet.ui.MainActivity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class WalletsFragment extends Fragment {
    private LinearLayout walletList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_wallets, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        walletList = view.findViewById(R.id.walletList);
        Button addWalletButton = view.findViewById(R.id.addWalletButton);
        Button exportWifButton = view.findViewById(R.id.exportWifButton);
        WalletRepository repository = ((MainActivity) requireActivity()).repository();
        addWalletButton.setOnClickListener(v -> {
            repository.addAccount("Conta " + (repository.accounts().size() + 1));
            render(repository, Map.of());
            loadBalances(repository);
            Toast.makeText(requireContext(), "Endereço gerado. A conta anterior continua ativa.", Toast.LENGTH_LONG).show();
        });
        exportWifButton.setOnClickListener(v -> {
            String wif = repository.exportActiveWif();
            if (wif.isEmpty()) {
                Toast.makeText(requireContext(), "Nenhuma conta ativa", Toast.LENGTH_SHORT).show();
                return;
            }
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("x2x-wif", wif));
            Toast.makeText(requireContext(), "WIF copiada. Limpe a área de transferência após o backup.", Toast.LENGTH_LONG).show();
        });
        render(repository, Map.of());
        loadBalances(repository);
    }

    private void loadBalances(WalletRepository repository) {
        repository.runIo(() -> {
            try {
                List<WalletRepository.AccountBalance> balances = repository.refreshAllBalances(false);
                Map<String, String> byId = new HashMap<>();
                for (WalletRepository.AccountBalance row : balances) {
                    String text = Amount.formatDisplay(row.response().balance()) + " " + NetworkParameters.TICKER;
                    if (row.response().scanning()) {
                        text += " · indexando";
                    }
                    byId.put(row.account().id(), text);
                }
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    if (isAdded()) {
                        render(repository, byId);
                    }
                });
            } catch (Exception e) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Saldo: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void render(WalletRepository repository, Map<String, String> balances) {
        walletList.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        int gap = Math.round(10 * getResources().getDisplayMetrics().density);
        String activeId = repository.activeAccount().map(WalletAccount::id).orElse("");
        for (WalletAccount account : repository.accounts()) {
            View row = inflater.inflate(R.layout.item_wallet_account, walletList, false);
            TextView label = row.findViewById(R.id.accountLabel);
            TextView address = row.findViewById(R.id.accountAddress);
            TextView balance = row.findViewById(R.id.accountBalance);
            boolean active = account.id().equals(activeId);
            label.setText(account.label() + (active ? " (ativa)" : ""));
            address.setText(account.address());
            balance.setText(balances.getOrDefault(account.id(), "Saldo: toque para atualizar"));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.bottomMargin = gap;
            row.setLayoutParams(params);
            row.setOnClickListener(v -> {
                repository.setActiveAccount(account.id());
                Toast.makeText(requireContext(), "Conta ativa: " + account.address(), Toast.LENGTH_LONG).show();
                render(repository, balances);
            });
            walletList.addView(row);
        }
    }
}
