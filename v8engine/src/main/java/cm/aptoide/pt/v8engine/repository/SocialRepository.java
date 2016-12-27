package cm.aptoide.pt.v8engine.repository;

import android.app.Activity;
import android.content.Context;
import cm.aptoide.accountmanager.AptoideAccountManager;
import cm.aptoide.accountmanager.BaseActivity;
import cm.aptoide.pt.dataprovider.DataProvider;
import cm.aptoide.pt.dataprovider.repository.IdsRepositoryImpl;
import cm.aptoide.pt.dataprovider.ws.v7.LikeCardRequest;
import cm.aptoide.pt.dataprovider.ws.v7.SetUserRequest;
import cm.aptoide.pt.dataprovider.ws.v7.ShareCardRequest;
import cm.aptoide.pt.dataprovider.ws.v7.ShareInstallCardRequest;
import cm.aptoide.pt.logger.Logger;
import cm.aptoide.pt.model.v7.timeline.TimelineCard;
import cm.aptoide.pt.preferences.managed.ManagerPreferences;
import cm.aptoide.pt.preferences.secure.SecurePreferencesImplementation;
import cm.aptoide.pt.utils.design.ShowMessage;
import cm.aptoide.pt.v8engine.R;
import cm.aptoide.pt.v8engine.view.recycler.displayable.implementations.appView.AppViewInstallDisplayable;
import rx.schedulers.Schedulers;

/**
 * Created by jdandrade on 21/11/2016.
 */
public class SocialRepository {
  public SocialRepository() {

  }

  public void share(TimelineCard timelineCard, Context context, boolean privacy) {
    String accessToken = AptoideAccountManager.getAccessToken();
    String aptoideClientUUID = new IdsRepositoryImpl(SecurePreferencesImplementation.getInstance(),
        DataProvider.getContext()).getAptoideClientUUID();
    ShareCardRequest.of(timelineCard, accessToken, aptoideClientUUID)
        .observe()
        .subscribe(baseV7Response -> {
          ShowMessage.asSnack((Activity) context, R.string.social_timeline_share_soon);
          final String userAccess = privacy ? BaseActivity.UserAccessState.UNLISTED.toString()
              : BaseActivity.UserAccessState.PUBLIC.toString();
          SetUserRequest.of(aptoideClientUUID, userAccess, accessToken)
              .observe()
              .subscribe(baseV7Response1 -> Logger.d(this.getClass().getSimpleName(),
                  baseV7Response.toString()), throwable -> throwable.printStackTrace());
          ManagerPreferences.setUserAccess(userAccess);
          ManagerPreferences.setUserAccessConfirmed(true);
        }, throwable -> throwable.printStackTrace());
  }

  public void like(TimelineCard timelineCard, String cardType, String ownerHash, int rating) {
    String accessToken = AptoideAccountManager.getAccessToken();
    String aptoideClientUUID = new IdsRepositoryImpl(SecurePreferencesImplementation.getInstance(),
        DataProvider.getContext()).getAptoideClientUUID();
    String email = AptoideAccountManager.getUserEmail();
    LikeCardRequest.of(timelineCard, cardType, ownerHash, accessToken, aptoideClientUUID, email,
        rating)
        .observe()
        .observeOn(Schedulers.io())
        .subscribe(
            baseV7Response -> Logger.d(this.getClass().getSimpleName(), baseV7Response.toString()),
            throwable -> throwable.printStackTrace());
  }

  public void share(AppViewInstallDisplayable displayable, Context context, boolean privacy) {
    String accessToken = AptoideAccountManager.getAccessToken();
    String aptoideClientUUID = new IdsRepositoryImpl(SecurePreferencesImplementation.getInstance(),
        DataProvider.getContext()).getAptoideClientUUID();
    ShareInstallCardRequest.of(
        displayable.getPojo().getNodes().getMeta().getData().getPackageName(), accessToken,
        aptoideClientUUID).observe().subscribe(baseV7Response -> {
      ShowMessage.asSnack((Activity) context, R.string.social_timeline_share_soon);
      final String userAccess = privacy ? BaseActivity.UserAccessState.UNLISTED.toString()
          : BaseActivity.UserAccessState.PUBLIC.toString();
      SetUserRequest.of(aptoideClientUUID, userAccess, accessToken)
          .observe()
          .subscribe(baseV7Response1 -> Logger.d(this.getClass().getSimpleName(),
              baseV7Response.toString()), throwable -> throwable.printStackTrace());
      ManagerPreferences.setUserAccess(userAccess);
      ManagerPreferences.setUserAccessConfirmed(true);
      Logger.d(this.getClass().getSimpleName(), baseV7Response.toString());
    }, throwable -> throwable.printStackTrace());
  }
}

