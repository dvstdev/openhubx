
package com.darktile.openhubx.inject.module;

import androidx.annotation.NonNull;

import com.darktile.openhubx.AppApplication;
import com.darktile.openhubx.dao.DaoSession;
import com.darktile.openhubx.dao.OpenHubRoomDatabase;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * AppModule
 * Created by ThirtyDegreesRay on 2016/8/30 13:52
 */
@Module
public class AppModule {

    private AppApplication application;

    public AppModule(AppApplication application) {
        this.application = application;
    }

    @Provides
    @Singleton
    public AppApplication provideApplication() {
        return application;
    }

    @NonNull
    @Provides
    @Singleton
    public DaoSession provideDaoSession() {
        return new DaoSession(OpenHubRoomDatabase.getInstance(application));
    }


}
