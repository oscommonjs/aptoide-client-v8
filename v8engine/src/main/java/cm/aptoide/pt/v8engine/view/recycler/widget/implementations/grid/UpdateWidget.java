/*
 * Copyright (c) 2016.
 * Modified by Neurophobic Animal on 08/07/2016.
 */

package cm.aptoide.pt.v8engine.view.recycler.widget.implementations.grid;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import cm.aptoide.pt.database.Database;
import cm.aptoide.pt.database.realm.Download;
import cm.aptoide.pt.downloadmanager.AptoideDownloadManager;
import cm.aptoide.pt.downloadmanager.DownloadServiceHelper;
import cm.aptoide.pt.imageloader.ImageLoader;
import cm.aptoide.pt.utils.AptoideUtils;
import cm.aptoide.pt.v8engine.R;
import cm.aptoide.pt.v8engine.fragment.implementations.AppViewFragment;
import cm.aptoide.pt.v8engine.util.DownloadFactory;
import cm.aptoide.pt.v8engine.util.FragmentUtils;
import cm.aptoide.pt.v8engine.view.recycler.displayable.implementations.grid.UpdateDisplayable;
import cm.aptoide.pt.v8engine.view.recycler.widget.Displayables;
import cm.aptoide.pt.v8engine.view.recycler.widget.Widget;
import io.realm.Realm;
import lombok.Cleanup;

/**
 * Created by neuro on 17-05-2016.
 */
@Displayables({UpdateDisplayable.class})
public class UpdateWidget extends Widget<UpdateDisplayable> {

	private View updateRowRelativeLayout;
	private TextView labelTextView;
	private ImageView iconImageView;
	private TextView installedVernameTextView;
	private TextView updateVernameTextView;
	private ViewGroup updateButtonLayout;

	public UpdateWidget(View itemView) {
		super(itemView);
	}

	@Override
	protected void assignViews(View itemView) {
		updateRowRelativeLayout = itemView.findViewById(R.id.updateRowRelativeLayout);
		labelTextView = (TextView) itemView.findViewById(R.id.name);
		iconImageView = (ImageView) itemView.findViewById(R.id.icon);
		installedVernameTextView = (TextView) itemView.findViewById(R.id.app_installed_version);
		updateVernameTextView = (TextView) itemView.findViewById(R.id.app_update_version);
		updateButtonLayout = (ViewGroup) itemView.findViewById(R.id.updateButtonLayout);
	}

	@Override
	public void bindView(UpdateDisplayable updateDisplayable) {
		@Cleanup Realm realm = Database.get();

		labelTextView.setText(updateDisplayable.getLabel());
		installedVernameTextView.setText(Database.InstalledQ.get(updateDisplayable.getPackageName(), realm).getVersionName());
		updateVernameTextView.setText(updateDisplayable.getUpdateVersionName());
		ImageLoader.load(updateDisplayable.getIcon(), iconImageView);

		updateRowRelativeLayout.setOnClickListener(v -> FragmentUtils.replaceFragmentV4(getContext(), AppViewFragment.newInstance(updateDisplayable.getAppId())));

		updateButtonLayout.setOnClickListener(view -> {
			new DownloadServiceHelper(AptoideDownloadManager.getInstance()).startDownload(new DownloadFactory().create(updateDisplayable))
					.subscribe(download -> {
				if (download.getOverallDownloadStatus() == Download.COMPLETED) {
					AptoideUtils.SystemU.installApp(download.getFilesToDownload().get(0).getFilePath());
				}
			});
		});
	}
}
