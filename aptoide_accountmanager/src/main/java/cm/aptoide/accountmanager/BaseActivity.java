package cm.aptoide.accountmanager;

import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;

/**
 * Created by trinkes on 4/18/16.
 */
public abstract class BaseActivity extends AppCompatActivity {

	private static final String TAG = BaseActivity.class.getSimpleName();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle(getActivityTitle());
		Log.d(TAG, "testeonCreate: " + getClass().getSimpleName());
	}

	protected abstract String getActivityTitle();

	@LayoutRes
	abstract int getLayoutId();

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int i = item.getItemId();
		if (i == android.R.id.home) {
			finish();
		} else if (i == R.id.home) {
			finish();
		}
		return super.onOptionsItemSelected(item);
	}
}
