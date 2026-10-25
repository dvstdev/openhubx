

package com.darktile.openhubx.inject.component;

import com.darktile.openhubx.AppApplication;
import com.darktile.openhubx.dao.DaoSession;
import com.darktile.openhubx.inject.module.AppModule;

import javax.inject.Singleton;

import dagger.Component;

/**
 * AppComponent
 * Created by ThirtyDegreesRay on 2016/8/30 14:08
 */
@Singleton
@Component(modules = {AppModule.class})
public interface AppComponent {
    /**
     * 获取AppApplication
     * @return
     */
    AppApplication getApplication();

    /**
     * 获取数据库Dao
     * @return
     */
    DaoSession getDaoSession();

}
