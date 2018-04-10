package com.dhis2.usescases.teiDashboard;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;

import com.dhis2.R;
import com.dhis2.data.metadata.MetadataRepository;
import com.dhis2.usescases.teiDashboard.dashboardfragments.TEIDataFragment;
import com.dhis2.usescases.teiDashboard.eventDetail.EventDetailActivity;
import com.dhis2.usescases.teiDashboard.teiDataDetail.TeiDataDetailActivity;
import com.dhis2.usescases.teiDashboard.teiProgramList.TeiProgramListActivity;

import org.hisp.dhis.android.core.program.ProgramModel;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by ppajuelo on 30/11/2017.
 */

public class TeiDashboardPresenter implements TeiDashboardContracts.Presenter {

    private final DashboardRepository dashboardRepository;
    private final MetadataRepository metadataRepository;
    private TeiDashboardContracts.View view;

    private String teUid;
    private String programUid;

    private CompositeDisposable compositeDisposable;

    TeiDashboardPresenter(DashboardRepository dashboardRepository, MetadataRepository metadataRepository) {
        this.dashboardRepository = dashboardRepository;
        this.metadataRepository = metadataRepository;
        compositeDisposable = new CompositeDisposable();
    }

    @Override
    public void init(TeiDashboardContracts.View view, String teiUid, String programUid) {
        this.view = view;
        this.teUid = teiUid;
        this.programUid = programUid;

        getData();
    }

    @SuppressLint("CheckResult")
    private void getData() {
        if (programUid != null)
            Observable.zip(
                    metadataRepository.getTrackedEntityInstance(teUid),
                    dashboardRepository.getEnrollment(programUid, teUid),
                    dashboardRepository.getProgramStages(programUid),
                    dashboardRepository.getTEIEnrollmentEvents(programUid, teUid),
                    metadataRepository.getProgramTrackedEntityAttributes(programUid),
                    dashboardRepository.getTEIAttributeValues(programUid, teUid),
                    metadataRepository.getTeiOrgUnit(teUid),
                    metadataRepository.getTeiActivePrograms(teUid),
                    dashboardRepository.getRelationships(programUid, teUid),
                    DashboardProgramModel::new)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            view::setData,
                            throwable -> Log.d("ERROR", throwable.getMessage()));
        else {
            //TODO: NO SE HA SELECCIONADO PROGRAMA
            Observable.zip(
                    metadataRepository.getTrackedEntityInstance(teUid),
                    metadataRepository.getProgramTrackedEntityAttributes(null),
                    dashboardRepository.getTEIAttributeValues(null, teUid),
                    metadataRepository.getTeiOrgUnit(teUid),
                    metadataRepository.getTeiActivePrograms(teUid),
                    DashboardProgramModel::new)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(view::setDataWithOutProgram,
                            throwable -> Log.d("ERROR", throwable.getMessage()));
        }
    }

    @Override
    public Observable<DashboardProgramModel> getProgram() {
        return Observable.zip(
                metadataRepository.getTrackedEntityInstance(teUid),
                dashboardRepository.getEnrollment(programUid, teUid),
                dashboardRepository.getProgramStages(programUid),
                dashboardRepository.getTEIEnrollmentEvents(programUid, teUid),
                metadataRepository.getProgramTrackedEntityAttributes(programUid),
                dashboardRepository.getTEIAttributeValues(programUid, teUid),
                metadataRepository.getTeiOrgUnit(teUid),
                metadataRepository.getTeiActivePrograms(teUid),
                dashboardRepository.getRelationships(programUid, teUid),
                DashboardProgramModel::new)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public void onBackPressed() {
        view.back();
    }

    @Override
    public void onEnrollmentSelectorClick() {
        Bundle extras = new Bundle();
        extras.putString("TEI_UID", teUid);
        view.startActivity(TeiProgramListActivity.class, extras, false, false, null);
    }

    @Override
    public void setProgram(ProgramModel program) {
        this.programUid = program.uid();
        getData();
    }

    @Override
    public void seeDetails(View sharedView, DashboardProgramModel dashboardProgramModel) {
        Fragment teiFragment = view.getAdapter().getItem(0);
        Intent intent = new Intent(view.getContext(), TeiDataDetailActivity.class);
        Bundle extras = new Bundle();
        extras.putString("TEI_UID", teUid);
        extras.putString("PROGRAM_UID", programUid);
        extras.putString("ENROLLMENT_UID", dashboardProgramModel.getCurrentEnrollment().uid());
        intent.putExtras(extras);
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(view.getAbstractActivity(), sharedView, "user_info");
        teiFragment.startActivityForResult(intent, TEIDataFragment.getRequestCode(), options.toBundle());
    }

    @Override
    public void onEventSelected(String uid, View sharedView) {
        Bundle extras = new Bundle();
        extras.putString("EVENT_UID", uid);
        extras.putString("TOOLBAR_TITLE", view.getToolbarTitle());
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(view.getAbstractActivity(), sharedView, "shared_view");
        view.startActivity(EventDetailActivity.class, extras, false, false, options);
    }

    @Override
    public void onFollowUp(DashboardProgramModel dashboardProgramModel) {
        int success = dashboardRepository.setFollowUp(dashboardProgramModel.getCurrentEnrollment().uid(), !dashboardProgramModel.getCurrentEnrollment().followUp());
        if (success > 0) {
            view.showToast(!dashboardProgramModel.getCurrentEnrollment().followUp() ?
                    view.getContext().getString(R.string.follow_up_enabled) :
                    view.getContext().getString(R.string.follow_up_disabled));
            getData();
        }
    }

    @Override
    public void onDettach() {
        compositeDisposable.clear();
    }
}