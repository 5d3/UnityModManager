package io.github.xausky.unitymodmanager.fragment;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.lody.virtual.client.core.InstallStrategy;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.remote.InstallResult;

import io.github.xausky.unitymodmanager.MainApplication;
import io.github.xausky.unitymodmanager.R;
import io.github.xausky.unitymodmanager.adapter.AttachesAdapter;
import io.github.xausky.unitymodmanager.dialog.ApplicationChooseDialog;

/**
 * Created by xausky on 18-3-3.
 */

public class AttachFragment extends BaseFragment implements ApplicationChooseDialog.OnApplicationChooseDialogResultListener{
    private static final String ALL_APPLICATION_PACKAGE_REGEX = "^.*$";
    private Context context;
    private ApplicationChooseDialog dialog;
    private ProgressDialog progressDialog;
    private View view;
    private RecyclerView attaches;
    private AttachesAdapter adapter;
    private HomeFragment homeFragment;

    @Override
    public BaseFragment setBase(Context base) {
        homeFragment = (HomeFragment) BaseFragment.fragment(R.id.nav_home, base);
        if(homeFragment.apkModifyModel == HomeFragment.APK_MODIFY_MODEL_VIRTUAL){
            adapter = new AttachesAdapter(homeFragment.packageName);
        } else {
            adapter = new AttachesAdapter( null);
        }
        return super.setBase(base);
    }

    public int getItemCount(){
        return adapter.getItemCount();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.attach_fragment, container, false);
        context = inflater.getContext();
        progressDialog = new ProgressDialog(context);
        progressDialog.setTitle(R.string.progress_dialog_title);
        progressDialog.setMessage(getString(R.string.progress_dialog_message));
        progressDialog.setCancelable(false);
        dialog = new ApplicationChooseDialog(context, this, ALL_APPLICATION_PACKAGE_REGEX, true, false);
        dialog.setListener(this);
        attaches = view.findViewById(R.id.attach_list);
        adapter.setRecyclerView(attaches);
        return view;
    }

    private void appInstall(final String apkPath){
        progressDialog.show();
        new Thread(){
            @Override
            public void run() {
                Log.d(MainApplication.LOG_TAG, apkPath);
                final InstallResult result = VirtualCore.get().installPackage(apkPath, InstallStrategy.UPDATE_IF_EXIST);
                final String resultString;
                if(result.isSuccess){
                    resultString = "Success";
                } else {
                    resultString = result.error;
                }
                AttachFragment.this.view.post(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.hide();
                        adapter.update(homeFragment.packageName);
                        Toast.makeText(context, resultString, Toast.LENGTH_LONG).show();
                    }
                });
            }
        }.start();
    }

    @Override
    public int actionButtonVisibility() {
        return View.VISIBLE;
    }

    @Override
    public void OnActionButtonClick() {
        if(VirtualCore.get().isStartup()){
            AttachFragment.this.dialog.show();
        } else {
            Toast.makeText(context, R.string.not_available_non_virtual, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        dialog.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void OnApplicationChooseDialogResult(String packageName, String apkPath) {
        appInstall(apkPath);
        dialog.hide();
    }
}
