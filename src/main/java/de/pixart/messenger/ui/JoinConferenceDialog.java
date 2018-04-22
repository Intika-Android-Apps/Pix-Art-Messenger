package de.pixart.messenger.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.pixart.messenger.R;
import de.pixart.messenger.databinding.DialogJoinConferenceBinding;
import de.pixart.messenger.services.XmppConnectionService;
import de.pixart.messenger.ui.adapter.KnownHostsAdapter;
import de.pixart.messenger.ui.interfaces.OnBackendConnected;
import de.pixart.messenger.ui.util.DelayedHintHelper;

public class JoinConferenceDialog extends DialogFragment implements OnBackendConnected {

    private static final String PREFILLED_JID_KEY = "prefilled_jid";
    private static final String ACCOUNTS_LIST_KEY = "activated_accounts_list";
    private static final String MULTIPLE_ACCOUNTS = "multiple_accounts_enabled";
    public XmppConnectionService xmppConnectionService;
    private JoinConferenceDialogListener mListener;
    private KnownHostsAdapter knownHostsAdapter;

    public static JoinConferenceDialog newInstance(String prefilledJid, List<String> accounts, boolean multipleAccounts) {
        JoinConferenceDialog dialog = new JoinConferenceDialog();
        Bundle bundle = new Bundle();
        bundle.putString(PREFILLED_JID_KEY, prefilledJid);
        bundle.putBoolean(MULTIPLE_ACCOUNTS, multipleAccounts);
        bundle.putStringArrayList(ACCOUNTS_LIST_KEY, (ArrayList<String>) accounts);

        dialog.setArguments(bundle);
        return dialog;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setRetainInstance(true);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.join_conference);
        DialogJoinConferenceBinding binding = DataBindingUtil.inflate(getActivity().getLayoutInflater(), R.layout.dialog_join_conference, null, false);
        DelayedHintHelper.setHint(R.string.conference_address_example, binding.jid);
        this.knownHostsAdapter = new KnownHostsAdapter(getActivity(), R.layout.simple_list_item);
        binding.jid.setAdapter(knownHostsAdapter);
        String prefilledJid = getArguments().getString(PREFILLED_JID_KEY);
        if (prefilledJid != null) {
            binding.jid.append(prefilledJid);
        }
        if (getArguments().getBoolean(MULTIPLE_ACCOUNTS)) {
            binding.yourAccount.setVisibility(View.VISIBLE);
            binding.account.setVisibility(View.VISIBLE);
        } else {
            binding.yourAccount.setVisibility(View.GONE);
            binding.account.setVisibility(View.GONE);
        }
        StartConversationActivity.populateAccountSpinner(getActivity(), getArguments().getStringArrayList(ACCOUNTS_LIST_KEY), binding.account);
        builder.setView(binding.getRoot());
        builder.setPositiveButton(R.string.join, null);
        builder.setNegativeButton(R.string.cancel, null);
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(view -> mListener.onJoinDialogPositiveClick(dialog, binding.account, binding.jid, binding.bookmark.isChecked()));
        return dialog;
    }

    @Override
    public void onBackendConnected() {
        refreshKnownHosts();
    }

    private void refreshKnownHosts() {
        Activity activity = getActivity();
        if (activity instanceof XmppActivity) {
            Collection<String> hosts = ((XmppActivity) activity).xmppConnectionService.getKnownConferenceHosts();
            this.knownHostsAdapter.refresh(hosts);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (JoinConferenceDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement JoinConferenceDialogListener");
        }
    }

    @Override
    public void onDestroyView() {
        Dialog dialog = getDialog();
        if (dialog != null && getRetainInstance()) {
            dialog.setDismissMessage(null);
        }
        super.onDestroyView();
    }

    @Override
    public void onStart() {
        super.onStart();
        final Activity activity = getActivity();
        if (activity instanceof XmppActivity && ((XmppActivity) activity).xmppConnectionService != null) {
            refreshKnownHosts();
        }
    }

    public interface JoinConferenceDialogListener {
        void onJoinDialogPositiveClick(Dialog dialog, Spinner spinner, AutoCompleteTextView jid, boolean isBookmarkChecked);
    }
}