package com.darktile.openhubx.mvp.presenter;

import com.darktile.openhubx.dao.DaoSession;
import com.darktile.openhubx.dao.LocalRepo;
import com.darktile.openhubx.dao.LocalUser;
import com.darktile.openhubx.dao.Trace;
import com.darktile.openhubx.dao.TraceDao;
import com.darktile.openhubx.mvp.contract.ITraceContract;
import com.darktile.openhubx.mvp.model.Repository;
import com.darktile.openhubx.mvp.model.TraceExt;
import com.darktile.openhubx.mvp.model.User;
import com.darktile.openhubx.mvp.presenter.base.BasePresenter;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

/**
 * Created by ThirtyDegreesRay on 2017/11/23 10:55:30
 */

public class TracePresenter extends BasePresenter<ITraceContract.View>
        implements ITraceContract.Presenter {

    private ArrayList<TraceExt> traceList;
    private TraceExt removedTrace;
    private int removedPosition;

    @Inject
    public TracePresenter(DaoSession daoSession) {
        super(daoSession);
    }

    @Override
    public void onViewInitialized() {
        super.onViewInitialized();
        loadTraceList(1);
    }

    @Override
    public void loadTraceList(int page) {
        mView.showLoading();
        final ArrayList<TraceExt> tempTraceList = new ArrayList<>();
        daoSession.rxTx()
                .run(() -> {
                    List<Trace> traceListDb = daoSession.getTraceDao()
                            .loadPaged(page * 30, (page - 1) * 30);
                    for (Trace trace : traceListDb) {
                        TraceExt ext = TraceExt.generate(trace);
                        if ("user".equals(trace.getType())) {
                            LocalUser localUser = daoSession.getLocalUserDao().load(ext.getUserId());
                            ext.setUser(User.generateFromLocalUser(localUser));
                        } else {
                            LocalRepo localRepo = daoSession.getLocalRepoDao().load(ext.getRepoId());
                            ext.setRepository(Repository.generateFromLocalRepo(localRepo));
                        }
                        tempTraceList.add(ext);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aVoid -> {
                    if (traceList == null || page == 1) {
                        traceList = tempTraceList;
                    } else {
                        traceList.addAll(tempTraceList);
                    }
                    mView.showTraceList(traceList);
                    mView.hideLoading();
                });
    }

    @Override
    public void removeTrace(int position) {
        removedTrace = traceList.remove(position);
        removedPosition = position;
        rxDBExecute(() -> daoSession.getTraceDao().deleteByKey(removedTrace.getId()));
    }

    @Override
    public void undoRemoveTrace() {
        traceList.add(removedPosition, removedTrace);
        mView.notifyItemAdded(removedPosition);
        rxDBExecute(() -> daoSession.getTraceDao().insert(removedTrace));
    }

    @Override
    public int getFirstItemByDate(long dateTime) {
        for (TraceExt trace : traceList) {
            if (trace.getLatestDate().getTime() == dateTime) {
                return traceList.indexOf(trace);
            }
        }
        return 0;
    }

}
