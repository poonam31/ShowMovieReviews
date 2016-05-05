package com.example.poonam.showmoviereviews;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class MovieReviewsFragment extends Fragment {

    private ArrayAdapter<String> mMovieReviewsAdapter;

    public MovieReviewsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add this line in order for this fragment to handle menu events.
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.moviereviewsfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_refresh) {

            FetchMovieReviewsTask movieReviewsTask = new FetchMovieReviewsTask();
            movieReviewsTask.execute();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        String[] movieArray = {};
        List<String> movienames = new ArrayList<String>(Arrays.asList(movieArray));

        mMovieReviewsAdapter = new ArrayAdapter<String>(getActivity(),
                R.layout.list_item_movie_review, R.id.list_item_movie_review_textview,
                movienames);

        FetchMovieReviewsTask movieReviewsTask = new FetchMovieReviewsTask();
        movieReviewsTask.execute();

        ListView listView = (ListView) rootView.findViewById(R.id.listview_movie);
        listView.setAdapter(mMovieReviewsAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                String movie1 = mMovieReviewsAdapter.getItem(position);
                Toast.makeText(getActivity(), movie1, Toast.LENGTH_SHORT).show();
            }
        });

        return rootView;

    }

    public class FetchMovieReviewsTask extends AsyncTask<Void, Void, String[]> {

        private final String LOG_TAG = FetchMovieReviewsTask.class.getSimpleName();

        private String[] getMovieReviewsDataFromJson(String movieReviewJsonStr, int numReviews)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String MR_RESULTS = "results";
            final String MR_DISPLAY_TITLE = "display_title";

            JSONObject movieReviewJson = new JSONObject(movieReviewJsonStr);
            JSONArray movieReviewArray = movieReviewJson.getJSONArray(MR_RESULTS);

            String[] resultStrs = new String[numReviews];
            for (int i = 0; i < movieReviewArray.length(); i++) {

                String movie_name;

                JSONObject movieObject = movieReviewArray.getJSONObject(i);
                movie_name = movieObject.getString(MR_DISPLAY_TITLE);

                resultStrs[i] = movie_name;

                System.out.print(resultStrs[i]);

            }

            for (String s : resultStrs) {
                Log.v(LOG_TAG, "Movie entry: " + s);
            }
            return resultStrs;

        }

        @Override
        protected String[] doInBackground(Void... params) {


            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String movieReviewJsonStr = null;
            int numReviews = 20;

            try {

                final String MOVIE_REVIEW_BASE_URL =
                        "http://api.nytimes.com/svc/movies/v2/reviews/search.json?";

                final String OFFSET_PARAM = "offset";
                final String APIKEY_PARAM = "api-key";

                Uri builtUri = Uri.parse(MOVIE_REVIEW_BASE_URL).buildUpon()
                        .appendQueryParameter(OFFSET_PARAM, Integer.toString(numReviews))
                        .appendQueryParameter(APIKEY_PARAM, BuildConfig.MOVIE_REVIEWS_API_KEY)
                        .build();

                URL url = new URL(builtUri.toString());

                Log.v(LOG_TAG, "Built URI " + builtUri.toString());


                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                movieReviewJsonStr = buffer.toString();

                Log.v(LOG_TAG, "Movie Review JSON String: " + movieReviewJsonStr);

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);

                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                return getMovieReviewsDataFromJson(movieReviewJsonStr, numReviews);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }



            return null;
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (result != null) {
                mMovieReviewsAdapter.clear();
                for (String movieReviewStr : result) {
                    mMovieReviewsAdapter.add(movieReviewStr);
                }
                // New data is back from the server.  Hooray!
            }
        }

    }

}
