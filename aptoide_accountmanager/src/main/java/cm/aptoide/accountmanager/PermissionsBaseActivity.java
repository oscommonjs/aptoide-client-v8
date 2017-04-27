package cm.aptoide.accountmanager;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import cm.aptoide.pt.logger.Logger;
import cm.aptoide.pt.preferences.Application;
import cm.aptoide.pt.utils.AptoideUtils;
import com.jakewharton.rxbinding.view.RxView;
import java.io.File;
import java.util.List;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by pedroribeiro on 02/12/16.
 */

public abstract class PermissionsBaseActivity extends BaseActivity {

  protected static final int CREATE_STORE_REQUEST_CODE = 1;
  protected static final int STORAGE_REQUEST_CODE = 123;
  protected static final int CAMERA_REQUEST_CODE = 124;
  protected static final int USER_PROFILE_CODE = 125;
  static final int GALLERY_CODE = 1046;
  static final int REQUEST_IMAGE_CAPTURE = 1;
  static final String STORAGE_PERMISSION_GIVEN = "storage_permission_given";
  static final String CAMERA_PERMISSION_GIVEN = "camera_permission_given";
  static final String STORAGE_PERMISSION_REQUESTED = "storage_permission_requested";
  static final String CAMERA_PERMISSION_REQUESTED = "camera_permission_requested";
  private static final String TYPE_STORAGE = "storage";
  private static final String TYPE_CAMERA = "camera";
  protected static String photoAvatar = "aptoide_user_avatar.png";
  public boolean result;
  private String TAG = "STORAGE";
  private File avatar;
  private CompositeSubscription mSubscriptions;

  public static String checkAndAskPermission(final AppCompatActivity activity, String type) {

    if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
      if (type.equals(TYPE_STORAGE)) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {

          if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
              Manifest.permission.READ_EXTERNAL_STORAGE)) {

            ActivityCompat.requestPermissions(activity,
                new String[] { Manifest.permission.READ_EXTERNAL_STORAGE }, STORAGE_REQUEST_CODE);
            return STORAGE_PERMISSION_REQUESTED;
          } else {
            ActivityCompat.requestPermissions(activity,
                new String[] { Manifest.permission.READ_EXTERNAL_STORAGE }, STORAGE_REQUEST_CODE);
          }
        } else {
          return STORAGE_PERMISSION_GIVEN;
        }
      } else if (type.equals(TYPE_CAMERA)) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
          if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
              Manifest.permission.CAMERA)) {

            ActivityCompat.requestPermissions(activity, new String[] {
                Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE
            }, CAMERA_REQUEST_CODE);
            return CAMERA_PERMISSION_REQUESTED;
          } else {
            ActivityCompat.requestPermissions(activity, new String[] {
                Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE
            }, CAMERA_REQUEST_CODE);
          }
        } else {
          return CAMERA_PERMISSION_GIVEN;
        }
      }
    } else {
      if (type.equals(TYPE_CAMERA)) {
        return CAMERA_PERMISSION_GIVEN;
      } else if (type.equals(TYPE_STORAGE)) {
        return STORAGE_PERMISSION_GIVEN;
      }
    }
    return "";
  }

  protected static String createAvatarPhotoName(String avatar) {
    //SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-mm-yyyy");
    String output = avatar /*+ simpleDateFormat.toString()*/;
    return output;
  }

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mSubscriptions = new CompositeSubscription();
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    mSubscriptions.clear();
  }

  @Override protected String getActivityTitle() {
    return null;
  }

  @Override int getLayoutId() {
    return 0;
  }

  public void chooseAvatarSource() {
    final Dialog dialog = new Dialog(this);
    dialog.setContentView(R.layout.dialog_choose_avatar_layout);
    mSubscriptions.add(RxView.clicks(dialog.findViewById(R.id.button_camera)).subscribe(click -> {
      callPermissionAndAction(TYPE_CAMERA);
      dialog.dismiss();
    }));
    mSubscriptions.add(RxView.clicks(dialog.findViewById(R.id.button_gallery)).subscribe(click -> {
      callPermissionAndAction(TYPE_STORAGE);
      dialog.dismiss();
    }));
    mSubscriptions.add(
        RxView.clicks(dialog.findViewById(R.id.cancel)).subscribe(click -> dialog.dismiss()));
    dialog.show();
  }

  public void callPermissionAndAction(String type) {
    String result =
        PermissionsBaseActivity.checkAndAskPermission(PermissionsBaseActivity.this, type);
    switch (result) {
      case PermissionsBaseActivity.CAMERA_PERMISSION_GIVEN:
        changePermissionValue(true);
        dispatchTakePictureIntent(getApplicationContext());
        break;
      case PermissionsBaseActivity.STORAGE_PERMISSION_GIVEN:
        changePermissionValue(true);
        callGallery();
        break;
    }
  }

  public void changePermissionValue(boolean b) {
    result = b;
  }

  public void callGallery() {
    if (result) {
      Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
      if (intent.resolveActivity(getPackageManager()) != null) {
        startActivityForResult(intent, GALLERY_CODE);
      }
    }
  }

  public void dispatchTakePictureIntent(Context context) {
    if (result) {
      setFileName();
      Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
      if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          Uri uriForFile = FileProvider.getUriForFile(context,
              Application.getConfiguration().getAppId() + ".provider", new File(getPhotoFileUri(
                  PermissionsBaseActivity.createAvatarPhotoName(photoAvatar)).getPath()));
          takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uriForFile);
        } else {
          takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, getPhotoFileUri(PermissionsBaseActivity.createAvatarPhotoName(photoAvatar)));
        }
        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
      }
    }
  }

  private void setFileName() {
    if (getActivityTitle().equals("Create User Profile")) {
      photoAvatar = "aptoide_user_avatar.jpg";
    } else if (getActivityTitle().equals("Create Your Store")) {
      photoAvatar = "aptoide_store_avatar.jpg";
    }
  }

  public Uri getPhotoFileUri(String fileName) {
    File storageDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath(),
        ".aptoide/user_avatar");
    if (!storageDir.exists() && !storageDir.mkdirs()) {
      Logger.d(TAG, "Failed to create directory");
    }
    avatar = storageDir;
    return Uri.fromFile(new File(storageDir.getPath() + File.separator + fileName));
  }

  protected void checkAvatarRequirements(String avatarPath, Uri avatarUrl) {
    if (!TextUtils.isEmpty(avatarPath)) {
      List<AptoideUtils.IconSizeU.ImageSizeErrors> imageSizeErrors =
          AptoideUtils.IconSizeU.checkIconSizeProperties(avatarPath,
              getResources().getInteger(R.integer.min_avatar_height),
              getResources().getInteger(R.integer.max_avatar_height),
              getResources().getInteger(R.integer.min_avatar_width),
              getResources().getInteger(R.integer.max_avatar_width),
              getResources().getInteger(R.integer.max_avatar_Size));
      if (imageSizeErrors.isEmpty()) {
        loadImage(avatarUrl);
      } else {
        showIconPropertiesError(getErrorsMessage(imageSizeErrors));
      }
    }
  }

  private String getErrorsMessage(List<AptoideUtils.IconSizeU.ImageSizeErrors> imageSizeErrors) {
    StringBuilder message = new StringBuilder();
    message.append(getString(R.string.image_requirements_popup_message));
    for (AptoideUtils.IconSizeU.ImageSizeErrors imageSizeError : imageSizeErrors) {
      switch (imageSizeError) {
        case MIN_HEIGHT:
          message.append(getString(R.string.image_requirements_error_min_height));
          break;
        case MAX_HEIGHT:
          message.append(getString(R.string.image_requirements_error_max_height));
          break;
        case MIN_WIDTH:
          message.append(getString(R.string.image_requirements_error_min_width));
          break;
        case MAX_WIDTH:
          message.append(getString(R.string.image_requirements_error_max_width));
          break;
        case MAX_IMAGE_SIZE:
          message.append(getString(R.string.image_requirements_error_max_file_size));
          break;
      }
    }

    //remove last \n
    int index = message.lastIndexOf("\n");
    if (index > 0) {
      message.delete(index, message.length());
    }

    return message.toString();
  }

  abstract void showIconPropertiesError(String errors);

  abstract void loadImage(Uri imagePath);
}