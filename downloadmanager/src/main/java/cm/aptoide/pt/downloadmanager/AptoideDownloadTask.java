/*
 * Copyright (c) 2016.
 * Modified on 02/09/2016.
 */

package cm.aptoide.pt.downloadmanager;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import cm.aptoide.pt.crashreports.CrashLogger;
import cm.aptoide.pt.crashreports.CrashReport;
import cm.aptoide.pt.logger.Logger;
import cm.aptoide.pt.utils.AptoideUtils;
import cm.aptoide.pt.utils.FileUtils;
import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloadLargeFileListener;
import com.liulishuo.filedownloader.FileDownloader;
import com.liulishuo.filedownloader.exception.FileDownloadHttpException;
import com.liulishuo.filedownloader.exception.FileDownloadOutOfSpaceException;
import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;
import rx.Completable;
import rx.Observable;
import rx.observables.ConnectableObservable;
import rx.schedulers.Schedulers;

/**
 * Created by trinkes on 5/13/16.
 */
class AptoideDownloadTask extends FileDownloadLargeFileListener {

  private static final int RETRY_TIMES = 3;
  private static final int INTERVAL = 1000;    //interval between progress updates
  private static final int APTOIDE_DOWNLOAD_TASK_TAG_KEY = 888;
  private static final int FILE_NOT_FOUND_HTTP_ERROR = 404;
  private static final String TAG = AptoideDownloadTask.class.getSimpleName();
  private final Download download;
  private final DownloadRepository downloadRepository;
  private final FileUtils fileUtils;
  private final AptoideDownloadManager downloadManager;
  private final FilePaths filePaths;

  private ConnectableObservable<Download> observable;
  private Analytics analytics;
  private FileDownloader fileDownloader;
  private final CrashLogger crashLogger;

  AptoideDownloadTask(DownloadRepository downloadRepository, Download download, FileUtils fileUtils,
      Analytics analytics, AptoideDownloadManager downloadManager, FilePaths filePaths, FileDownloader fileDownloader,
      CrashLogger crashLogger) {
    this.analytics = analytics;
    this.download = download;
    this.downloadRepository = downloadRepository;
    this.fileUtils = fileUtils;
    this.downloadManager = downloadManager;
    this.filePaths = filePaths;
    this.fileDownloader = fileDownloader;
    this.crashLogger = crashLogger;
    this.observable = Observable.interval(INTERVAL / 4, INTERVAL, TimeUnit.MILLISECONDS)
        .subscribeOn(Schedulers.io())
        .takeUntil(integer1 -> download.getOverallDownloadStatus() != DownloadStatus.PROGRESS
            && download.getOverallDownloadStatus() != DownloadStatus.IN_QUEUE
            && download.getOverallDownloadStatus() != DownloadStatus.PENDING)
        .filter(aLong1 -> download.getOverallDownloadStatus() == DownloadStatus.PROGRESS
            || download.getOverallDownloadStatus() == DownloadStatus.COMPLETED)
        .map(aLong -> updateProgress())
        .filter(updatedDownload -> {
          if (updatedDownload.getOverallProgress() <= Constants.PROGRESS_MAX_VALUE
              && download.getOverallDownloadStatus() == DownloadStatus.PROGRESS) {
            if (updatedDownload.getOverallProgress() == Constants.PROGRESS_MAX_VALUE
                && download.getOverallDownloadStatus() != DownloadStatus.COMPLETED) {
              setDownloadStatus(DownloadStatus.COMPLETED, download);
              downloadManager.currentDownloadFinished();
            }
            return true;
          } else {
            return false;
          }
        })
        .publish();
  }

  /**
   * Update the overall download progress. It updates the value on database and in memory list
   *
   * @return new current progress
   */
  @NonNull private Download updateProgress() {
    if (download.getOverallProgress() >= Constants.PROGRESS_MAX_VALUE
        || download.getOverallDownloadStatus() != DownloadStatus.PROGRESS) {
      return download;
    }

    int progress = 0;
    for (final DownloadFile fileToDownload : download.getFilesToDownload()) {
      progress += fileToDownload.getProgress();
    }
    download.setOverallProgress((int) Math.floor((float) progress / download.getFilesToDownload()
        .size()));
    saveDownloadInDb(download);
    Logger.d(TAG, "Download: " + download.getHashCode() + " Progress: " + download.getOverallProgress());
    return download;
  }

  private void setDownloadStatus(DownloadStatus status, Download download) {
    setDownloadStatus(status, download, null);
  }

  private synchronized void saveDownloadInDb(Download download) {
    Completable.fromAction(() -> {
      downloadRepository.save(download);
    })
        .subscribeOn(Schedulers.io())
        .subscribe(() -> {
        }, err -> CrashReport.getInstance()
            .log(err));
  }

  private void setDownloadStatus(DownloadStatus status, Download download,
      @Nullable BaseDownloadTask task) {
    if (task != null) {
      for (final DownloadFile fileToDownload : download.getFilesToDownload()) {
        if (fileToDownload.getDownloadId() == task.getId()) {
          fileToDownload.setStatus(status);
        }
      }
    }

    this.download.setOverallDownloadStatus(status);
    saveDownloadInDb(download);
  }

  @Override protected void pending(BaseDownloadTask task, long soFarBytes, long totalBytes) {
    setDownloadStatus(DownloadStatus.PENDING, download, task);
  }

  @Override protected void pending(BaseDownloadTask task, int soFarBytes, int totalBytes) {
    pending(task, (long) soFarBytes, (long) totalBytes);
    setDownloadStatus(DownloadStatus.PENDING, download, task);
  }

  @Override protected void progress(BaseDownloadTask task, long soFarBytes, long totalBytes) {
    for (DownloadFile fileToDownload : download.getFilesToDownload()) {
      if (fileToDownload.getDownloadId() == task.getId()) {
        //sometimes to totalBytes = 0, i believe that's when a 301(Moved Permanently) http error occurs
        if (totalBytes > 0) {
          fileToDownload.setProgress(
              (int) Math.floor((float) soFarBytes / totalBytes * Constants.PROGRESS_MAX_VALUE));
        } else {
          fileToDownload.setProgress(0);
        }
      }
    }
    this.download.setDownloadSpeed(task.getSpeed() * 1024);
    if (download.getOverallDownloadStatus() != DownloadStatus.PROGRESS) {
      setDownloadStatus(DownloadStatus.PROGRESS, download, task);
    }
  }

  @Override protected void progress(BaseDownloadTask task, int soFarBytes, int totalBytes) {
    progress(task, (long) soFarBytes, (long) totalBytes);
  }

  @Override protected void paused(BaseDownloadTask task, long soFarBytes, long totalBytes) {
    setDownloadStatus(DownloadStatus.PAUSED, download, task);
    downloadManager.currentDownloadFinished();
  }

  @Override protected void paused(BaseDownloadTask task, int soFarBytes, int totalBytes) {
    paused(task, (long) soFarBytes, (long) totalBytes);
  }

  @Override protected void blockComplete(BaseDownloadTask task) {

  }

  @Override protected void completed(BaseDownloadTask task) {
    Observable.from(download.getFilesToDownload())
        .filter(file -> file.getDownloadId() == task.getId())
        .flatMap(file -> {
          file.setStatus(DownloadStatus.COMPLETED);
          for (final DownloadFile fileToDownload : download.getFilesToDownload()) {
            if (fileToDownload.getStatus() != DownloadStatus.COMPLETED) {
              file.setProgress(Constants.PROGRESS_MAX_VALUE);
              return Observable.just(null);
            }
          }
          return checkMd5AndMoveFileToRightPlace(download).doOnNext(fileWasMoved -> {
            if (fileWasMoved) {
              Logger.d(TAG, "Expected file hash and downloaded file hash match");
            } else {
              Logger.e(TAG, "Expected file hash and downloaded file hash do not match");
            }
          })
              .doOnNext(fileWasMoved -> {
                if (fileWasMoved) {
                  file.setProgress(Constants.PROGRESS_MAX_VALUE);
                } else {
                  downloadManager.deleteDownloadedFiles(download);
                  download.setDownloadError(DownloadError.GENERIC_ERROR);
                  setDownloadStatus(DownloadStatus.ERROR, download, task);
                }
              });
        })
        .doOnNext(__ -> saveDownloadInDb(download))
        .subscribeOn(Schedulers.io())
        .subscribe(__ -> {
        }, throwable -> {
          setDownloadStatus(DownloadStatus.ERROR, download);
          crashLogger.log(throwable);
        });
    download.setDownloadSpeed(task.getSpeed() * 1024);
  }

  @Override protected void error(BaseDownloadTask task, Throwable e) {
    stopDownloadQueue(download);
    if (e instanceof FileDownloadHttpException
        && ((FileDownloadHttpException) e).getCode() == FILE_NOT_FOUND_HTTP_ERROR) {
      Logger.d(TAG, "File not found on link: " + task.getUrl());
      for (final DownloadFile fileToDownload : download.getFilesToDownload()) {
        if (TextUtils.equals(fileToDownload.getLink(), task.getUrl()) && !TextUtils.isEmpty(
            fileToDownload.getAltLink())) {
          fileToDownload.setLink(fileToDownload.getAltLink());
          fileToDownload.setAltLink(null);
          downloadRepository.save(download);
          startDownload();
          return;
        }
      }
    } else {
      Logger.d(TAG, "Error on download: " + download.getHashCode());
      // Apparently throwable e can be null.
      if (e != null) {
        e.printStackTrace();
      }
      if (analytics != null) {
        analytics.onError(download, e);
      }
    }
    if (e instanceof FileDownloadOutOfSpaceException) {
      download.setDownloadError(DownloadError.NOT_ENOUGH_SPACE_ERROR);
    } else {
      download.setDownloadError(DownloadError.GENERIC_ERROR);
    }
    setDownloadStatus(DownloadStatus.ERROR, download, task);
    downloadManager.currentDownloadFinished();
  }

  @Override protected void warn(BaseDownloadTask task) {
    setDownloadStatus(DownloadStatus.WARNING, download, task);
  }

  /**
   * this method will pause all downloads listed on {@link Download} without change
   * download state, the listener is removed in order to keep the download state, this means that
   * the "virtual" pause will not affect the download state
   */
  private void stopDownloadQueue(Download download) {
    //this try catch sucks
    try {
      for (int i = download.getFilesToDownload()
          .size() - 1; i >= 0; i--) {
        DownloadFile fileToDownload = download.getFilesToDownload()
            .get(i);
        fileDownloader.getStatus(fileToDownload.getDownloadId(), fileToDownload.getPath());
        int taskId = fileDownloader.replaceListener(fileToDownload.getDownloadId(), null);
        if (taskId != 0) {
          fileDownloader.pause(taskId);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * @throws IllegalArgumentException
   */
  public void startDownload() throws IllegalArgumentException {
    observable.connect();
    final List<DownloadFile> filesToDownload = download.getFilesToDownload();
    if (filesToDownload != null) {
      DownloadFile fileToDownload;
      for (int i = 0; i < filesToDownload.size(); i++) {

        fileToDownload = filesToDownload.get(i);

        if (TextUtils.isEmpty(fileToDownload.getLink())) {
          throw new IllegalArgumentException("A link to download must be provided");
        }
        BaseDownloadTask baseDownloadTask = fileDownloader.create(fileToDownload.getLink())
            .setAutoRetryTimes(RETRY_TIMES);
        /*
         * Aptoide - events 2 : download
         * Get X-Mirror and add to the event
         */
        baseDownloadTask.addHeader(Constants.VERSION_CODE,
            String.valueOf(download.getVersionCode()));
        baseDownloadTask.addHeader(Constants.PACKAGE, download.getPackageName());
        baseDownloadTask.addHeader(Constants.FILE_TYPE, String.valueOf(i));
        /*
         * end
         */

        baseDownloadTask.setTag(APTOIDE_DOWNLOAD_TASK_TAG_KEY, this);
        if (fileToDownload.getFileName()
            .endsWith(".temp")) {
          fileToDownload.setFileName(fileToDownload.getFileName()
              .replace(".temp", ""));
        }
        fileToDownload.setDownloadId(baseDownloadTask.setListener(this)
            .setCallbackProgressTimes(Constants.PROGRESS_MAX_VALUE)
            .setPath(filePaths.getDownloadsStoragePath() + fileToDownload.getFileName())
            .asInQueueTask()
            .enqueue());
        fileToDownload.setPath(filePaths.getDownloadsStoragePath());
        fileToDownload.setFileName(fileToDownload.getFileName() + ".temp");
      }

      //if (isSerial) {
        // To form a queue with the same queueTarget and execute them linearly
        fileDownloader.start(this, true);
      //} else {
      //  // To form a queue with the same queueTarget and execute them in parallel
      //  fileDownloader.start(this, false);
      //}
    }
    saveDownloadInDb(download);
  }

  private Observable<Boolean> checkMd5AndMoveFileToRightPlace(Download download) {
    return Observable.fromCallable(() -> {
      for (final DownloadFile fileToDownload : download.getFilesToDownload()) {
        fileToDownload.setFileName(fileToDownload.getFileName()
            .replace(".temp", ""));
        if (!TextUtils.isEmpty(fileToDownload.getMd5())) {
          if (!TextUtils.equals(AptoideUtils.AlgorithmU.computeMd5(
              new File(filePaths.getDownloadsStoragePath() + fileToDownload.getFileName())), fileToDownload.getMd5())) {
            return false;
          }
        }
        String newFilePath = getFilePathFromFileType(fileToDownload);
        fileUtils.copyFile(filePaths.getDownloadsStoragePath(), newFilePath, fileToDownload.getFileName());
        fileToDownload.setPath(newFilePath);
      }
      return true;
    });
  }

  @NonNull private String getFilePathFromFileType(DownloadFile fileToDownload) {
    String path;
    switch (fileToDownload.getFileType()) {
      case APK:
        path = filePaths.getApkPath();
        break;
      case OBB:
        path = filePaths.getObbPath() + fileToDownload.getPackageName() + "/";
        break;
      case GENERIC:
      default:
        path = filePaths.getDownloadsStoragePath();
        break;
    }
    return path;
  }
}