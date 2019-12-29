package io.github.xausky.unitymodmanager.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.allenliu.versionchecklib.callback.APKDownloadListener;
import com.allenliu.versionchecklib.v2.AllenVersionChecker;
import com.allenliu.versionchecklib.v2.builder.UIData;
import com.lody.virtual.client.core.VirtualCore;
import com.topjohnwu.superuser.Shell;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import io.github.xausky.unitymodmanager.MainActivity;
import io.github.xausky.unitymodmanager.MainApplication;
import io.github.xausky.unitymodmanager.R;
import io.github.xausky.unitymodmanager.adapter.ModsAdapter;
import io.github.xausky.unitymodmanager.domain.Mod;
import io.github.xausky.unitymodmanager.utils.ModUtils;
import io.github.xausky.unitymodmanager.utils.NativeUtils;
import ru.bartwell.exfilepicker.ExFilePicker;
import ru.bartwell.exfilepicker.data.ExFilePickerResult;

/**
 * Created by xausky on 18-3-3.
 */

public class ModFragment extends BaseFragment implements ModsAdapter.OnDataChangeListener {
    private static final int MOD_FILE_PICKER_RESULT = 1;
    private static final int EXTERNAL_MOD_FILE_PICKER_RESULT = 2;
    private static final String NEED_PATCH_PREFERENCES_KEY = "NEED_PATCH_PREFERENCES_KEY";
    private String url = null;
    private String name = null;
    private View view;
    private RecyclerView recyclerView;
    private ModsAdapter adapter;
    private boolean needPatch;
    private Context context;
    private File storage;
    private File externalCache;
    private SharedPreferences settings;
    private boolean showConflict;
    private Handler handler;

    @Override
    public void onDetach() {
        this.context = null;
        super.onDetach();
    }

    @Override
    public void onAttach(Context context) {
        this.context = context;
        super.onAttach(context);
    }

    public void importMod(String url, final String name){
        this.name = name;
        if(context != null){
            AllenVersionChecker
                    .getInstance()
                    .downloadOnly(
                            UIData.create().setDownloadUrl(url).setTitle(getString(R.string.import_mod)).setContent(name == null?url:name)
                    ).setAutoInstall(false).setShowNotification(false).setApkDownloadListener(new APKDownloadListener() {
                @Override
                public void onDownloading(int progress) {
                }

                @Override
                public void onDownloadSuccess(File file) {
                    if(name != null){
                        try {
                            FileUtils.moveFile(file, new File(file.getParentFile().getAbsolutePath() + "/" + name + ".zip"));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    adapter.addMods(file.getParentFile().getAbsolutePath() + "/", Collections.singletonList(name != null?name + ".zip":file.getName()));
                }

                @Override
                public void onDownloadFail() {
                    Log.e(MainApplication.LOG_TAG, "onDownloadFail");
                }
            }).executeMission(context);
            this.url = null;
        } else {
            this.url = url;
        }
    }

    @Override
    public BaseFragment setBase(Context base) {
        handler = new Handler(Looper.getMainLooper());
        storage = base.getExternalFilesDir("mods");
        if(!storage.exists()){
            if(!storage.mkdir()){
                Toast.makeText(base, R.string.store_mkdir_failed, Toast.LENGTH_LONG).show();
            }
        }
        externalCache = base.getExternalFilesDir("caches");
        if(!externalCache.exists()){
            if(!externalCache.mkdir()){
                Toast.makeText(base, R.string.store_mkdir_failed, Toast.LENGTH_LONG).show();
            }
        }
        settings = base.getSharedPreferences(SettingFragment.SETTINGS_PREFERENCE_NAME, Context.MODE_PRIVATE);
        needPatch = settings.getBoolean(NEED_PATCH_PREFERENCES_KEY, false);
        adapter = new ModsAdapter(storage, externalCache, base);
        adapter.setListener(this);
        return super.setBase(base);
    }

    public boolean isNeedPatch() {
        return needPatch;
    }

    public void setNeedPatch(boolean needPatch) {
        this.needPatch = needPatch;
        settings.edit().putBoolean(NEED_PATCH_PREFERENCES_KEY, needPatch).apply();
    }

    public int getItemCount(){
        return adapter.getItemCount();
    }

    public int getEnableItemCount(){
        return adapter.getEnableItemCount();
    }

    public void removeAllMods(){
        adapter.removeAllMods();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        context = inflater.getContext();
        settings = context.getSharedPreferences(SettingFragment.SETTINGS_PREFERENCE_NAME, Context.MODE_PRIVATE);
        showConflict = settings.getBoolean("show_conflict_info", false);
        if(adapter == null){
            setBase(context);
        }
        adapter.setShowConflict(showConflict);
        view = inflater.inflate(R.layout.mod_fragment, container, false);
        recyclerView = view.findViewById(R.id.mod_list);
        adapter.setRecyclerView(recyclerView);
        if(url != null){
            importMod(url, this.name);
        }
        return view;
    }

    @Override
    public int actionButtonVisibility() {
        return View.VISIBLE;
    }

    @Override
    public void OnActionButtonClick() {
        if(ModUtils.effectiveFiles == null){
            Toast.makeText(context, R.string.install_client_and_generate_map_file_first, Toast.LENGTH_LONG).show();
            return;
        }
        ExFilePicker filePicker = new ExFilePicker();
        filePicker.setShowOnlyExtensions("zip", "rar", "7z");
        filePicker.setCanChooseOnlyOneItem(false);
        filePicker.start(this, MOD_FILE_PICKER_RESULT);
    }

    @Override
    public void OnActionButtonLongClick() {
        if(ModUtils.effectiveFiles == null){
            Toast.makeText(context, R.string.install_client_and_generate_map_file_first, Toast.LENGTH_LONG).show();
            return;
        }
        ExFilePicker filePicker = new ExFilePicker();
        filePicker.setShowOnlyExtensions();
        filePicker.setCanChooseOnlyOneItem(true);
        filePicker.start(this, EXTERNAL_MOD_FILE_PICKER_RESULT);
        Toast.makeText(context, R.string.external_mod_import_model, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == MOD_FILE_PICKER_RESULT) {
            ExFilePickerResult result = ExFilePickerResult.getFromIntent(data);
            if(result != null) {
                adapter.addMods(result.getPath(), result.getNames());
            }
        } else if(requestCode == EXTERNAL_MOD_FILE_PICKER_RESULT) {
            ExFilePickerResult result = ExFilePickerResult.getFromIntent(data);
            if(result != null) {
                adapter.addExternalMod(result.getPath(), result.getNames());
            }
        }
    }

    @Override
    public void onDataChange() {
        needPatch = true;
    }

    @Override
    public void onExternalChange() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, R.string.checked_external_mod_changed, Toast.LENGTH_LONG).show();
                HomeFragment fragment = (HomeFragment)BaseFragment.fragment(R.id.nav_home, ModFragment.this.getActivity().getApplication());
                if (fragment.apkModifyModel == HomeFragment.APK_MODIFY_MODEL_VIRTUAL){
                    VirtualCore.get().killApp(fragment.packageName, 0);
                } else {
                    if(Shell.rootAccess()){
                        Shell.su("am force-stop " + fragment.packageName).exec();
                    }
                }
                MainActivity activity = (MainActivity)context;
                needPatch = true;
                activity.launch();
            }
        });
    }

    public int patch(String apkPath, String baseApkPath, String persistentPath, String obbPath, String baseObbPath, String backupPath, int apkModifyModel, boolean persistentSupport, boolean obbSupport){
        if(apkModifyModel == HomeFragment.APK_MODIFY_MODEL_ROOT){
            if(!Shell.rootAccess()){
                return ModUtils.RESULT_STATE_ROOT_ERROR;
            }
            //暂时禁用SELinux，并且修改目标APK权限为666。
            Shell.su("setenforce 0", "chmod 666 " + apkPath).exec();
        }
        try {
            Log.d(MainApplication.LOG_TAG, "patch: apkPath=" + apkPath + ", baseApkPath=" + baseApkPath + ", apkModifyModel=" + apkModifyModel);
            List<Mod> mods = adapter.getMods();
            File fusionFile = new File(getBase().getFilesDir().getAbsolutePath() + "/fusion");
            try {
                FileUtils.deleteDirectory(fusionFile);
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(MainApplication.LOG_TAG, "deleteFile Failed: " + fusionFile);
                return ModUtils.RESULT_STATE_INTERNAL_ERROR;
            }
            if(!fusionFile.mkdir()){
                Log.d(MainApplication.LOG_TAG, "mkdir Failed: " + fusionFile);
                return ModUtils.RESULT_STATE_INTERNAL_ERROR;
            }
            for(Mod mod : mods){
                if(mod.enable){
                    File modFile = new File(storage.getAbsolutePath() + "/" + mod.name);
                    try {
                        if(modFile.isFile()){
                            File externalFile = new File(FileUtils.readFileToString(modFile));
                            int result = ModUtils.Standardization(externalFile.getAbsolutePath(), "", fusionFile);
                            mod.fileCount = result;
                        } else {
                            mod.conflict = new TreeSet<>();
                            mod.conflict.addAll(ModUtils.copyDirectory(modFile, fusionFile));
                        }
                    } catch (IOException e) {
                        Log.d(MainApplication.LOG_TAG, "Copy Mod Directory File Failed: " + e.getMessage());
                        return ModUtils.RESULT_STATE_INTERNAL_ERROR;
                    }
                }
            }
            if(obbSupport){
                int result = NativeUtils.PatchApk(baseObbPath, obbPath, fusionFile.getAbsolutePath());
                if(result != NativeUtils.RESULT_STATE_OK){
                    Log.d(MainApplication.LOG_TAG, "Patch Obb File Failed: " + result + ",obbPath:" + apkPath + ",baseObbPath:" + baseApkPath);
                    return ModUtils.RESULT_STATE_OBB_ERROR;
                }
                try {
                    InputStream inputStream = getBase().getAssets().open("settings.xml");
                    String settings = IOUtils.toString(inputStream);
                    inputStream.close();
                    settings = settings.replace("$CHECKSUM", ModUtils.checkSum(obbPath));
                    File out = new File(fusionFile.getAbsolutePath() + "/assets/bin/Data/settings.xml");
                    out.getParentFile().mkdirs();
                    FileUtils.write(out, settings);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(apkModifyModel == HomeFragment.APK_MODIFY_MODEL_VIRTUAL || apkModifyModel == HomeFragment.APK_MODIFY_MODEL_ROOT){
                int result = NativeUtils.PatchApk(baseApkPath, apkPath, fusionFile.getAbsolutePath());
                if(result != NativeUtils.RESULT_STATE_OK){
                    Log.d(MainApplication.LOG_TAG, "Patch APK File Failed: " + result + ",apkPath:" + apkPath + ",baseApkPath:" + baseApkPath);
                    return result;
                }
            }
            if(persistentSupport){
                int result = NativeUtils.PatchFolder(persistentPath,fusionFile.getAbsolutePath(), backupPath);
                if(result != NativeUtils.RESULT_STATE_OK){
                    Log.d(MainApplication.LOG_TAG, "Patch Persistent Folder Failed: " + result + ",persistentPath=" + persistentPath + ",backupPath=" + backupPath);
                    return result;
                }
            }
            adapter.notifyApply();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    adapter.notifyDataSetChanged();
                }
            });
            return NativeUtils.RESULT_STATE_OK;
        }finally {
            if(apkModifyModel == HomeFragment.APK_MODIFY_MODEL_ROOT){
                //修改目标APK权限回644，并且重新启用SELinux。
                Shell.su("chmod 644 " + apkPath, "setenforce 0").exec();
            }
        }
    }
}
