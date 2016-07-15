/*
 * Copyright (c) 2016.
 * Modified by Neurophobic Animal on 25/05/2016.
 */

package cm.aptoide.pt.v8engine.fragment.implementations;

import android.os.Bundle;

import com.trello.rxlifecycle.FragmentEvent;

import java.util.LinkedList;

import cm.aptoide.pt.database.Database;
import cm.aptoide.pt.database.realm.Store;
import cm.aptoide.pt.v8engine.fragment.GridRecyclerFragmentWithDecorator;
import cm.aptoide.pt.v8engine.view.recycler.displayable.Displayable;
import cm.aptoide.pt.v8engine.view.recycler.displayable.implementations.grid.AddMoreStoresDisplayable;
import cm.aptoide.pt.v8engine.view.recycler.displayable.implementations.grid.SubscribedStoreDisplayable;
import io.realm.RealmResults;
import rx.Observable;

/**
 * Created by neuro on 11-05-2016.
 */
public class SubscribedStoresFragment extends GridRecyclerFragmentWithDecorator {

	public static SubscribedStoresFragment newInstance() {
		SubscribedStoresFragment fragment = new SubscribedStoresFragment();
		return fragment;
	}

	@Override
	public void load(boolean refresh, Bundle savedInstanceState) {

		Observable<RealmResults<Store>> realmResultsObservable = Database.StoreQ.getAll(realm).asObservable();

		realmResultsObservable.compose(bindUntilEvent(FragmentEvent.DESTROY_VIEW))
				.subscribe(stores -> {

					LinkedList<Displayable> displayables = new LinkedList<>();

					for (Store store : stores) {
						displayables.add(new SubscribedStoreDisplayable(store));
					}

					// Add the final row as a button
					displayables.add(new AddMoreStoresDisplayable());

					setDisplayables(displayables);
				});
	}
}
