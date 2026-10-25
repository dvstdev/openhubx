package com.darktile.openhubx.util;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;

/**
 * Minimal RxJava 3 runtime-permission helper. Replaces
 * com.github.tbruyelle:rxpermissions:0.12, whose jitpack build is broken in this
 * environment and which is absent from Maven Central. Drop-in for the single
 * {@code new RxPermissions(activity).request(perm).subscribe(granted -> ...)}
 * call site: attaches a retained headless fragment, requests the permissions and
 * emits a single Boolean (true only when every requested permission is granted).
 */
public class RxPermissions {

    private static final String TAG = "RxPermissionsFragment";

    private final PermissionFragment fragment;

    public RxPermissions(@NonNull Activity activity) {
        this.fragment = getFragment((FragmentActivity) activity);
    }

    private PermissionFragment getFragment(FragmentActivity activity) {
        FragmentManager fm = activity.getSupportFragmentManager();
        PermissionFragment f = (PermissionFragment) fm.findFragmentByTag(TAG);
        if (f == null) {
            f = new PermissionFragment();
            fm.beginTransaction().add(f, TAG).commitNowAllowingStateLoss();
        }
        return f;
    }

    public Observable<Boolean> request(final String... permissions) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || isGranted(permissions)) {
            return Observable.just(Boolean.TRUE);
        }
        return fragment.requestPermissionsRx(permissions);
    }

    private boolean isGranted(String... permissions) {
        Activity activity = fragment.getActivity();
        if (activity == null) {
            return false;
        }
        for (String permission : permissions) {
            if (activity.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public static class PermissionFragment extends Fragment {

        private static final int REQUEST_CODE = 42;
        private PublishSubject<Boolean> subject;

        public PermissionFragment() {
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
        }

        Observable<Boolean> requestPermissionsRx(String[] permissions) {
            subject = PublishSubject.create();
            requestPermissions(permissions, REQUEST_CODE);
            return subject;
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                               @NonNull int[] grantResults) {
            if (requestCode != REQUEST_CODE || subject == null) {
                return;
            }
            boolean granted = grantResults.length > 0;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }
            subject.onNext(granted);
            subject.onComplete();
            subject = null;
        }
    }
}
