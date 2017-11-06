package cm.aptoide.pt.search;

import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.view.MenuItem;
import android.view.View;
import cm.aptoide.pt.search.view.QueryResultRepository;
import cm.aptoide.pt.search.view.UnableToSearchAction;
import cm.aptoide.pt.search.websocket.WebSocketManager;

public class SearchActionsHandler
    implements SearchView.OnQueryTextListener, SearchView.OnSuggestionListener,
    View.OnFocusChangeListener, View.OnClickListener {

  private final WebSocketManager searchWebSocket;
  private final MenuItem menuItem;
  private final SearchNavigator searchNavigator;
  private final UnableToSearchAction unableToSearchAction;
  private final QueryResultRepository webSocketQueryResultRepository;
  private final QueryResultRepository trendingQueryResultRepository;
  private final String lastQuery;

  public SearchActionsHandler(WebSocketManager searchWebSocket, MenuItem menuItem,
      SearchNavigator searchNavigator, UnableToSearchAction unableToSearchAction,
      QueryResultRepository webSocketQueryResultRepository, QueryResultRepository trendingQueryResultRepository, String lastQuery) {
    this.searchWebSocket = searchWebSocket;
    this.menuItem = menuItem;
    this.searchNavigator = searchNavigator;
    this.unableToSearchAction = unableToSearchAction;
    this.webSocketQueryResultRepository = webSocketQueryResultRepository;
    this.trendingQueryResultRepository = trendingQueryResultRepository;
    this.lastQuery = lastQuery;
  }

  @Override public boolean onQueryTextSubmit(String query) {
    MenuItemCompat.collapseActionView(menuItem);
    if (query.length() > 1) {
      searchNavigator.navigate(query);
    } else {
      unableToSearchAction.call();
    }
    return true;
  }

  @Override public boolean onQueryTextChange(String s) {
    return false;
  }

  @Override public boolean onSuggestionSelect(int position) {
    return false;
  }

  @Override public boolean onSuggestionClick(int position) {
    final String query = webSocketQueryResultRepository.getQueryAt(position);
    searchNavigator.navigate(query);
    return true;
  }

  @Override public void onFocusChange(View v, boolean hasFocus) {
    if (hasFocus && lastQueryIsEmpty()) {
      SearchView searchView = (SearchView) v;
      searchView.setQuery(lastQuery, false);
    } else {
      MenuItemCompat.collapseActionView(menuItem);
      searchWebSocket.disconnect();
    }
  }

  private boolean lastQueryIsEmpty() {
    return lastQuery != null && !lastQuery.trim()
        .equals("");
  }

  @Override public void onClick(View v) {
    searchWebSocket.connect();

    if(lastQueryIsEmpty()){

    }
  }
}
