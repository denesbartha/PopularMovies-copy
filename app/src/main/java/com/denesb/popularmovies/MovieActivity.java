package com.denesb.popularmovies;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.denesb.popularmovies.data.FavouritesContract;
import com.denesb.popularmovies.databinding.ActivityMovieBinding;
import com.denesb.popularmovies.utilities.NetworkUtils;
import com.denesb.popularmovies.utilities.ParseJSONData;
import com.squareup.picasso.Picasso;

import java.net.URL;
import java.util.List;

import static com.denesb.popularmovies.data.PopularMoviesPreferences.ID;
import static com.denesb.popularmovies.data.PopularMoviesPreferences.IMAGE;
import static com.denesb.popularmovies.data.PopularMoviesPreferences.OVERVIEW;
import static com.denesb.popularmovies.data.PopularMoviesPreferences.RELEASE_DATE;
import static com.denesb.popularmovies.data.PopularMoviesPreferences.TITLE;
import static com.denesb.popularmovies.data.PopularMoviesPreferences.VOTE_AVERAGE;

public class MovieActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {
    private ActivityMovieBinding mBinding;
    private String mJsonVideosResponse = null;
    private String mJsonReviewsResponse = null;
    private static final int TASK_LOADER_ID = 1;
    private int mId = -1;
    private boolean mIsFavourite = false;
    private static final String VIDEO_RESPONSE = "video response";
    private static final String REVIEW_RESPONSE = "review response";
    private static final String MARKED_AS_FAVOURITE = "Marked as favourite.";
    private static final String UNMARKED_AS_FAVOURITE = "Unmarked as favourite.";
    private static final String FAVOURITE_ERROR = "An error occurred during save.";
    private static final String TAG = MainActivity.class.getSimpleName();

    /**
     * Creates a MovieActivity instance and fills it with the intent's data.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie);

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_movie);

        Intent parentIntent = getIntent();
        if (parentIntent != null && parentIntent.hasExtra(ID)
                                 && parentIntent.hasExtra(TITLE)
                                 && parentIntent.hasExtra(IMAGE)
                                 && parentIntent.hasExtra(OVERVIEW)
                                 && parentIntent.hasExtra(VOTE_AVERAGE)
                                 && parentIntent.hasExtra(RELEASE_DATE)) {
            mBinding.tvTitle.setText(parentIntent.getStringExtra(TITLE));
            mBinding.tvDate.setText(parentIntent.getStringExtra(RELEASE_DATE));
            URL imageURL = NetworkUtils.buildImageUrl(parentIntent.getStringExtra(IMAGE),
                                                      NetworkUtils.ImageSize.W500);
            Picasso.with(this).load(imageURL.toString()).into(mBinding.ivMoviePoster);
            mBinding.tvRating.setText(parentIntent.getStringExtra(VOTE_AVERAGE) + "/10");
            mBinding.tvOverview.setText(parentIntent.getStringExtra(OVERVIEW));

            mId = parentIntent.getIntExtra(ID, -1);
            if (savedInstanceState != null) {
                mJsonVideosResponse = savedInstanceState.getString(VIDEO_RESPONSE);
                mJsonReviewsResponse = savedInstanceState.getString(REVIEW_RESPONSE);
                fillVideosAndReviews(genVideosAndReviewsData());
            }
            else {
                // load the videos and the reviews and preview them in the lists
                new FetchVideosAndReviewsTask().execute("" + mId);
            }

            getSupportLoaderManager().initLoader(TASK_LOADER_ID, null, this);
        }
        else {
            // An error occurred loading the movie...
            mBinding.trRv.tvMovieErrorMessage.setVisibility(View.VISIBLE);
            mBinding.movieLayout.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Saves the video and review responses.
     * @param out
     */
    @Override
    public void onSaveInstanceState(Bundle out) {
        out.putString(VIDEO_RESPONSE, mJsonVideosResponse);
        out.putString(REVIEW_RESPONSE, mJsonReviewsResponse);

        // call superclass to save any view hierarchy
        super.onSaveInstanceState(out);
    }

    /**
     * Loader for querying the current movie's favourite property in the db.
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
                if (mTaskData != null) {
                    deliverResult(mTaskData);
                } else {
                    forceLoad();
                }
            }

            @Override
            public Cursor loadInBackground() {
                try {
                    return getContentResolver().query(
                            FavouritesContract.FavouritesEntry.CONTENT_URI, null,
                            FavouritesContract.FavouritesEntry.COLUMN_MOVIE_ID + "=?",
                            new String[]{"" + mId}, null);
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

    /**
     * If the load finishes => set the favourite button's text.
     * @param loader
     * @param data
     */
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data != null && data.getCount() > 0) {
            // data.getColumnIndex(FavouritesContract.FavouritesEntry.COLUMN_MOVIE_ID);
            mBinding.btnFavourite.setText(getResources().getString(R.string.unfavourite));
            mIsFavourite = true;
        }
        else {
            mBinding.btnFavourite.setText(getResources().getString(R.string.mark_as_favourite));
            mIsFavourite = false;
        }
    }

    /**
     * If the load got reseted => reset the favourite's button's value to the default
     * @param loader
     */
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mBinding.btnFavourite.setText(getResources().getString(R.string.mark_as_favourite));
        mIsFavourite = false;
    }

    /**
     * Marks the current movie as favourite / unfavourite.
     * @param view
     */
    public void markAsFavourite(View view) {
        if (!mIsFavourite) {
            new AsyncTask<Void, Void, Uri>() {
                @Override
                protected Uri doInBackground(Void... params) {
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(FavouritesContract.FavouritesEntry.COLUMN_MOVIE_ID, mId);
                    return getContentResolver().insert(
                            FavouritesContract.FavouritesEntry.CONTENT_URI, contentValues);
                }

                @Override
                protected void onPostExecute(Uri uri) {
                    if(uri != null) {
                        mIsFavourite = true;
                        Toast.makeText(MovieActivity.this,
                                MARKED_AS_FAVOURITE, Toast.LENGTH_SHORT).show();
                        mBinding.btnFavourite.setText(
                                getResources().getString(R.string.unfavourite));
                    }
                    else {
                        Toast.makeText(MovieActivity.this, FAVOURITE_ERROR,
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }.execute();
        }
        else {
            new AsyncTask<Void, Void, Integer>() {
                @Override
                protected Integer doInBackground(Void... params) {
                    Uri uri = FavouritesContract.FavouritesEntry.CONTENT_URI;
                    uri = uri.buildUpon().appendPath("" + mId).build();
                    return getContentResolver().delete(uri, null, null);
                }

                @Override
                protected void onPostExecute(Integer i) {
                    if(i > 0) {
                        mIsFavourite = false;
                        Toast.makeText(MovieActivity.this,
                                UNMARKED_AS_FAVOURITE, Toast.LENGTH_SHORT).show();
                        mBinding.btnFavourite.setText(
                                getResources().getString(R.string.mark_as_favourite));
                    }
                    else {
                        Toast.makeText(MovieActivity.this, FAVOURITE_ERROR,
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }.execute();
        }
    }

    /**
     * Helper class for storing the videos and reviews lists.
     */
    private class VideosAndReviewsWrapper {
        List<VideoListItem> videos;
        List<ReviewListItem> reviews;

        VideosAndReviewsWrapper(List<VideoListItem> videos, List<ReviewListItem> reviews) {
            this.videos = videos;
            this.reviews = reviews;
        }
    }

    /**
     * Generates lists from the videos and reviews response.
     * @return
     */
    private VideosAndReviewsWrapper genVideosAndReviewsData() {
        if (null == mJsonVideosResponse || null == mJsonReviewsResponse) return null;
        try {
            List<VideoListItem> videos = ParseJSONData.parseVideosFromJSON(mJsonVideosResponse);
            List<ReviewListItem> reviews =
                    ParseJSONData.parseReviewsFromJSON(mJsonReviewsResponse);
            return new VideosAndReviewsWrapper(videos, reviews);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Fills the video and review lists on the UI.
     * @param data
     */
    private void fillVideosAndReviews(VideosAndReviewsWrapper data) {
        mBinding.trRv.pbMovieLoadingIndicator.setVisibility(View.INVISIBLE);
        if (data != null) {
            LayoutInflater inflater = LayoutInflater.from(MovieActivity.this);

            if (data.videos.size() > 0) {
                mBinding.trRv.videosList.setVisibility(View.VISIBLE);
                // inflate the trailers list...
                for (int i = 0; i < data.videos.size(); ++i) {
                    View view = inflater.inflate(
                            R.layout.video_list_item, mBinding.trRv.videosList, false);
                    String trailerId = "Trailer " + (i + 1);
                    ((TextView)view.findViewById(R.id.tv_trailer)).setText(trailerId);
                    final String key_url = data.videos.get(i).getKey();
                    view.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startActivity(new Intent(Intent.ACTION_VIEW,
                                    NetworkUtils.generateYoutubeLink(key_url)));
                        }
                    });
                    // set item content in view
                    mBinding.trRv.videosList.addView(view);
                }
            }
            else {
                mBinding.trRv.videosList.setVisibility(View.INVISIBLE);
            }

            // inflate the reviews list...
            if (data.reviews.size() > 0) {
                mBinding.trRv.reviewsList.setVisibility(View.VISIBLE);
                for (int i = 0; i < data.reviews.size(); ++i) {
                    View view = inflater.inflate(
                            R.layout.review_list_item, mBinding.trRv.reviewsList, false);
                    final String author = data.reviews.get(i).getAuthor();
                    final String content = data.reviews.get(i).getContent();
                    ((TextView)view.findViewById(R.id.tv_author)).setText(author);
                    ((TextView)view.findViewById(R.id.tv_content)).setText(content);
                    // set item content in view
                    mBinding.trRv.reviewsList.addView(view);
                }
            }
            else {
                mBinding.trRv.reviewsList.setVisibility(View.INVISIBLE);
            }
        }
        else {
            // show error message...
            mBinding.trRv.tvMovieErrorMessage.setVisibility(View.VISIBLE);
            mBinding.trRv.videosList.setVisibility(View.INVISIBLE);
            mBinding.trRv.reviewsList.setVisibility(View.INVISIBLE);

        }
    }

    /**
     * Fetches the trailers and reviews from the net.
     */
    private class FetchVideosAndReviewsTask
            extends AsyncTask<String, Void, VideosAndReviewsWrapper> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mBinding.trRv.pbMovieLoadingIndicator.setVisibility(View.VISIBLE);
            mBinding.trRv.tvMovieErrorMessage.setVisibility(View.INVISIBLE);
        }

        @Override
        protected VideosAndReviewsWrapper doInBackground(String... params) {
            // if there is no given order type or no internet connection => nothing to do
            if (null == params[0] || !NetworkUtils.isOnline(MovieActivity.this)) {
                return null;
            }

            URL movieVideosUrl = NetworkUtils.buildMovieVideosUrl(params[0]);
            URL movieReviewsUrl = NetworkUtils.buildMovieReviewsUrl(params[0]);
            try {
                mJsonVideosResponse = NetworkUtils.getResponseFromHttpUrl(movieVideosUrl);
                mJsonReviewsResponse = NetworkUtils.getResponseFromHttpUrl(movieReviewsUrl);
                return genVideosAndReviewsData();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(VideosAndReviewsWrapper data) {
            fillVideosAndReviews(data);
        }
    }
}
