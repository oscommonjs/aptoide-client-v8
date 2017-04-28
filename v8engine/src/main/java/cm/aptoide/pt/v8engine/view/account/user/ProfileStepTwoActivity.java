/*
 * Copyright (c) 2017.
 * Modified by Marcelo Benites on 09/02/2017.
 */

package cm.aptoide.pt.v8engine.view.account.user;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.widget.Button;
import cm.aptoide.accountmanager.Account;
import cm.aptoide.accountmanager.AptoideAccountManager;

import cm.aptoide.pt.v8engine.networking.IdsRepository;
import cm.aptoide.pt.utils.GenericDialogs;
import cm.aptoide.pt.utils.design.ShowMessage;
import cm.aptoide.pt.v8engine.R;
import cm.aptoide.pt.v8engine.V8Engine;
import cm.aptoide.pt.v8engine.analytics.Analytics;
import cm.aptoide.pt.v8engine.view.account.AccountBaseActivity;
import com.jakewharton.rxbinding.view.RxView;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by pedroribeiro on 15/12/16.
 */

public class ProfileStepTwoActivity extends AccountBaseActivity {

  private static final String TAG = ProfileStepTwoActivity.class.getSimpleName();

  private IdsRepository idsRepository;

  private Button mContinueButton;
  private Button mPrivateProfile;
  private CompositeSubscription mSubscriptions;
  private Toolbar mToolbar;
  private ProgressDialog pleaseWaitDialog;
  private AptoideAccountManager accountManager;
  private boolean externalLogin;

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(getLayoutId());
    accountManager = ((V8Engine) getApplicationContext()).getAccountManager();
    pleaseWaitDialog = GenericDialogs.createGenericPleaseWaitDialog(this,
        getApplicationContext().getString(cm.aptoide.accountmanager.R.string.please_wait));
    idsRepository = ((V8Engine) getApplicationContext()).getIdsRepository();
    mSubscriptions = new CompositeSubscription();
    externalLogin = getIntent().getBooleanExtra(AptoideAccountManager.IS_FACEBOOK_OR_GOOGLE, false);
    bindViews();
    setupToolbar();
    setupListeners();
  }

  @Override public String getActivityTitle() {
    return getString(R.string.create_profile_logged_in_activity_title);
  }

  @Override public int getLayoutId() {
    return R.layout.logged_in_second_screen;
  }

  private void bindViews() {
    mToolbar = (Toolbar) findViewById(R.id.toolbar);
    mContinueButton = (Button) findViewById(R.id.logged_in_continue);
    mPrivateProfile = (Button) findViewById(R.id.logged_in_private_button);
    mToolbar = (Toolbar) findViewById(R.id.toolbar);
  }

  private void setupToolbar() {
    setSupportActionBar(mToolbar);
    getSupportActionBar().setTitle(getActivityTitle());
  }

  private void setupListeners() {
    mSubscriptions.add(RxView.clicks(mContinueButton)
        .doOnNext(click -> pleaseWaitDialog.show())
        .flatMap(click -> accountManager.updateAccount(Account.Access.PUBLIC)
            .doOnCompleted(
                () -> showContinueSuccessMessage(Analytics.Account.ProfileAction.CONTINUE))
            .doOnError(throwable -> showErrorMessage())
            .onErrorComplete()
            .doOnTerminate(() -> navigateToCreateStoreViewOrDismiss())
            .toObservable())
        .retry()
        .subscribe());

    mSubscriptions.add(RxView.clicks(mPrivateProfile)
        .doOnNext(click -> pleaseWaitDialog.show())
        .flatMap(click -> accountManager.updateAccount(Account.Access.UNLISTED)
            .doOnCompleted(
                () -> showContinueSuccessMessage(Analytics.Account.ProfileAction.PRIVATE_PROFILE))
            .doOnError(throwable -> showErrorMessage())
            .onErrorComplete()
            .doOnTerminate(() -> navigateToCreateStoreViewOrDismiss())
            .toObservable())
        .retry()
        .subscribe());
  }

  private void showErrorMessage() {
    ShowMessage.asSnack(this, R.string.unknown_error);
  }

  private void showContinueSuccessMessage(Analytics.Account.ProfileAction action) {
    ShowMessage.asSnack(this, R.string.successful);
    Analytics.Account.accountProfileAction(2, action);
  }

  private void navigateToCreateStoreViewOrDismiss() {
    if (externalLogin) {
      dismiss();
    } else {
      startActivity(new Intent(this, CreateStoreActivity.class));
      dismiss();
    }
  }

  private void dismiss() {
    pleaseWaitDialog.dismiss();
    finish();
  }
}
