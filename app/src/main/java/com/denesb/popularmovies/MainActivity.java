package com.denesb.popularmovies;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.os.AsyncTask;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import com.denesb.popularmovies.data.FavouritesContract;
import com.denesb.popularmovies.databinding.ActivityMainBinding;
import com.denesb.popularmovies.utilities.NetworkUtils;
import com.denesb.popularmovies.utilities.ParseJSONData;

import static com.denesb.popularmovies.data.PopularMoviesPreferences.ID;
import static com.denesb.popularmovies.data.PopularMoviesPreferences.OVERVIEW;
import static com.denesb.popularmovies.data.PopularMoviesPreferences.OrderType;
import static com.denesb.popularmovies.data.PopularMoviesPreferences.IMAGE;
import static com.denesb.popularmovies.data.PopularMoviesPreferences.RELEASE_DATE;
import static com.denesb.popularmovies.data.PopularMoviesPreferences.TITLE;
import static com.denesb.popularmovies.data.PopularMoviesPreferences.VOTE_AVERAGE;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.EnumMap;

public class MainActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor>{
    private static final String SELECTED_SORT_ODER = "selected sort order";
    private static final String JSON_MOVIES_STRING = "json movies string";
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int TASK_LOADER_ID = 0;
    // mMenuSortOrderItems contains the mappings between the type elements and the menu items
    private Map<OrderType, MenuItem> mMenuSortOrderItems = null;
    private OrderType mOderOrderType = OrderType.POPULAR;
    private String mJsonMoviesResponse = null;
    private MoviesAdapter mMoviesAdapter = null;
    private List<MovieListItem> mMoviesData = null;
    private ActivityMainBinding mBinding;
    private List<Integer> mFavourites = new ArrayList<>();
    private boolean firstRun = true;

    /**
     * Creates the Main Activity.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NetworkUtils.setApiValue(getString(R.string.movies_api_key));

        mMoviesAdapter = new MoviesAdapter(this);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        mBinding.moviesList.setAdapter(mMoviesAdapter);

        mBinding.moviesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (mMoviesData != null) {
                    Intent intent = new Intent(MainActivity.this, MovieActivity.class);
                    MovieListItem movieListItem = mMoviesData.get(i);
                    intent.putExtra(ID, movieListItem.getId());
                    intent.putExtra(TITLE, movieListItem.getTitle());
                    intent.putExtra(IMAGE, movieListItem.getImage());
                    intent.putExtra(OVERVIEW, movieListItem.getOverview());
                    intent.putExtra(VOTE_AVERAGE, movieListItem.getVoteAverage());
                    intent.putExtra(RELEASE_DATE, movieListItem.getReleaseDate());
                    startActivity(intent);
                }
            }
        });

        SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        mOderOrderType = OrderType.valueOf(sharedPreferences.getString(SELECTED_SORT_ODER,
                OrderType.POPULAR.toString()));

        // if there is a saved state => load the sort order and the movies JSON string
        if (savedInstanceState != null) {
            try {
                String jsonMoviesResponse = savedInstanceState.getString(JSON_MOVIES_STRING);
                mMoviesData = ParseJSONData.parseMoviesStringFromJSON(jsonMoviesResponse);
                mMoviesAdapter.setMoviesData(mMoviesData);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        getSupportLoaderManager().initLoader(TASK_LOADER_ID, null, this);
    }

    /**
     * Here we save the menu's sort order.
     * @param savedInstanceState
     */
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putString(JSON_MOVIES_STRING, mJsonMoviesResponse);
    }

    @Override
    protected void onResume() {
        // if this is not the first run of the app
        if (!firstRun && mOderOrderType == OrderType.FAVOURITE) {
            getSupportLoaderManager().initLoader(TASK_LOADER_ID, null, this);
        }
        super.onResume();
    }

    /**
     * Query all the favourite movies.
     * @param id
     * @param args
     * @return
     */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new AsyncTaskLoader<Cursor>(this) {
            Cursor mTaskData = null;
            @Override
            protected void onStartLoading() {
//                if (mTaskData != null) {
//                    deliverResult(mTaskData);
//                } else {
                    forceLoad();
//                }
            }

            @Override
            public Cursor loadInBackground() {
                try {
                    return getContentResolver().query(
                            FavouritesContract.FavouritesEntry.CONTENT_URI, null, null, null, null);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to asynchronously load data.");
                    e.printStackTrace();
                    return null;
                }
            }

            public void deliverResult(Cursor data) {
                mTaskData = data;
                super.deliverResult(data);
            }
        };
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mFavourites = new ArrayList<>();
        if (data.moveToFirst()) {
            do {
                final int columnIndex = data.getColumnIndex(
                        FavouritesContract.FavouritesEntry.COLUMN_MOVIE_ID);
                mFavourites.add(data.getInt(columnIndex));
            } while (data.moveToNext());
        }
        if (!firstRun) {
            loadMovies(mOderOrderType);
        }
        firstRun = false;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mFavourites = null;
    }

    /**
     * Creates the Options Menu.
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);

        // create mappings between the sort types and the actual menu items
        if (mMenuSortOrderItems == null) {
            mMenuSortOrderItems = new EnumMap<>(OrderType.class);
            mMenuSortOrderItems.put(OrderType.POPULAR, menu.findItem(R.id.order_popular));
            mMenuSortOrderItems.put(OrderType.TOP_RATED, menu.findItem(R.id.order_top_rated));
            mMenuSortOrderItems.put(OrderType.FAVOURITE, menu.findItem(R.id.order_favourite));

            setSortOrder(mOderOrderType);
        }
        return true;
    }

    /**
     * An item gets selected in the menu.
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.order_popular:
                setSortOrder(OrderType.POPULAR);
                break;
            case R.id.order_top_rated:
                setSortOrder(OrderType.TOP_RATED);
                break;
            case R.id.order_favourite:
                setSortOrder(OrderType.FAVOURITE);
            default:
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Sets the appropriate sort order.
     * @param orderType - the desired order type
     */
    private void setSortOrder(OrderType orderType) {
        if (orderType != mOderOrderType || null == mMoviesData) {
            // save the new preference
            SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
            sharedPreferences.edit().putString(SELECTED_SORT_ODER, orderType.toString()).apply();

            loadMovies(orderType);
        }
        // if (orderType == )
        mOderOrderType = orderType;
        mMenuSortOrderItems.get(orderType).setChecked(true);
    }

    /**
     * Starts a new FetchMovieTask.
     * @param orderType
     */
    private void loadMovies(OrderType orderType) { new FetchMoviesTask().execute(orderType); }

    /**
     * Fetches the Movies Data from the net.
     */
    private class FetchMoviesTask extends AsyncTask<OrderType, Void, List<MovieListItem>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mBinding.pbLoadingIndicator.setVisibility(View.VISIBLE);
            mBinding.tvErrorMessage.setVisibility(View.INVISIBLE);
        }

        @Override
        protected List<MovieListItem> doInBackground(OrderType... params) {
            // if there is no given order type or no internet connection => nothing to do
            if (null == params[0] || !NetworkUtils.isOnline(MainActivity.this)) {
                return null;
            }

            if (params[0] != OrderType.FAVOURITE) {
                URL movieRequestUrl = NetworkUtils.builMovieListsUrl(params[0]);
                try {
                    mJsonMoviesResponse = NetworkUtils.getResponseFromHttpUrl(movieRequestUrl);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    mJsonMoviesResponse = null;
                    return null;
                }
            }
            // favourites section
            else {
                try {
                    mJsonMoviesResponse = "{\"" + ParseJSONData.RESULTS + "\":[";
                    boolean firstItem = true;
                    for (int id : mFavourites) {
                        if (firstItem) {
                            firstItem = false;
                        }
                        else {
                            mJsonMoviesResponse += ",";
                        }
                        URL favouriteMovieRequestUrl = NetworkUtils.builFavouriteMovieUrl(id);
                        mJsonMoviesResponse +=
                                NetworkUtils.getResponseFromHttpUrl(favouriteMovieRequestUrl);
                    }
                    mJsonMoviesResponse += "]}";
                }
                catch (Exception e) {
                    e.printStackTrace();
                    mJsonMoviesResponse = null;
                    return null;
                }
            }

            try {
                return ParseJSONData.parseMoviesStringFromJSON(mJsonMoviesResponse);
            }
            catch (Exception e) {
                e.printStackTrace();
                mJsonMoviesResponse = null;
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<MovieListItem> moviesData) {
            mBinding.pbLoadingIndicator.setVisibility(View.INVISIBLE);
            mMoviesData = moviesData;
            if (moviesData != null) {
                mBinding.moviesList.setVisibility(View.VISIBLE);
                mMoviesAdapter.setMoviesData(moviesData);
            }
            else {
                // show error message...
                mBinding.moviesList.setVisibility(View.INVISIBLE);
                mBinding.tvErrorMessage.setVisibility(View.VISIBLE);
            }
        }
    }
}
