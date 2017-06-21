package com.example.android.findthebook;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static android.view.View.GONE;
import static com.example.android.findthebook.R.id.loading_indicator;

public class BookActivity extends AppCompatActivity {

    private static final String LOG_TAG = BookActivity.class.getName();

    /**
     * URL for books data from the Google Books API
     */
    private static final String GOOGLE_REQUEST_URL =
            "https://www.googleapis.com/books/v1/volumes?q=";

    /**
     * Adapter for the list of books
     */
    private BookAdapter mAdapter;

    /**
     * ProgressBar that is displayed when the list is loading
     */
    private ProgressBar mLoadingIndicator;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book);

        // Set and make the progress bar GONE
        mLoadingIndicator = (ProgressBar) findViewById(loading_indicator);
        mLoadingIndicator.setVisibility(GONE);

        // Find a reference to the {@link ListView} in the layout
        final ListView bookListView = (ListView) findViewById(R.id.book_list_view);

        // TextView to give information and when there is no Internet connection
        final TextView informationTextView = (TextView) findViewById(R.id.information_text);
        bookListView.setEmptyView(informationTextView);

        // Create a new adapter that takes an empty list of earthquakes as input
        mAdapter = new BookAdapter(this, new ArrayList<Book>());

        // Set the adapter on the {@link ListView}
        // so the list can be populated in the user interface
        bookListView.setAdapter(mAdapter);

        // Find a reference to the search_field and search_image_button in the layout
        final EditText searchField = (EditText) findViewById(R.id.search_field);
        final ImageButton searchImageButton = (ImageButton) findViewById(R.id.search_image_button);

        // Set an item click listener on the ImageButton, to execute when clicked
        searchImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Hide the TextView make visible the progress bar
                mLoadingIndicator.setVisibility(View.VISIBLE);
                if (searchField == null) {
                    mLoadingIndicator.setVisibility(GONE);
                } else {
                    mLoadingIndicator.setVisibility(View.VISIBLE);
                }

                mAdapter.clear();
                assert searchField != null;
                // Get the searched word and insert it in the Google request url
                // to find the corresponding books
                String searchedWord = searchField.getText().toString();
                String searchUrl = GOOGLE_REQUEST_URL + searchedWord;

                // Get a reference to the ConnectivityManager to check state of network connectivity
                ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

                // Get details on the currently active default data network
                NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

                // Check if there is a network connection
                if (networkInfo != null && networkInfo.isConnected()) {
                    new BookLoader().execute(searchUrl);
                } else {
                    // Otherwise, display error
                    // First, hide loading indicator so error message will be visible
                    View loadingIndicator = findViewById(loading_indicator);
                    loadingIndicator.setVisibility(GONE);

                    // Make the information TextView visible and display the no Internet connection
                    informationTextView.setVisibility(View.VISIBLE);
                    informationTextView.setText(R.string.no_internet);
                }
            }
        });

        // Set an item click listener on the ListView, which sends an intent to a web browser
        // to open a website with more information about the selected book.
        bookListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // Find the current earthquake that was clicked on
                Book currentBook = mAdapter.getItem(position);

                // Convert the String URL into a URI object (to pass into the Intent constructor)
                Uri bookUri = null;
                if (currentBook != null) {
                    bookUri = Uri.parse(currentBook.getURL());
                }

                // Create a new intent to view the book URI
                Intent websiteIntent = new Intent(Intent.ACTION_VIEW, bookUri);

                // Send the intent to launch a new activity
                startActivity(websiteIntent);
            }
        });
    }

    /**
     * Clear the adapter of previous book data and display the Book info in the UI
     */
    private void updateUi(List<Book> books) {
        mAdapter.clear();
        mAdapter.addAll(books);
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Loads a list of books by using an AsyncTask to perform the
     * network request to the given URL.
     */
    private class BookLoader extends AsyncTask<String, Void, List<Book>> {

        @Override
        protected List<Book> doInBackground(String... urls) {
            // Create URL object
            URL url = createUrl(urls[0]);

            if (urls.length < 1) {
                return null;
            }

            // Perform HTTP request to the URL and receive a JSON response back
            String jsonResponse = "";
            try {
                jsonResponse = makeHttpRequest(url);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Problem making the HTTP request.", e);
            }

            // Extract relevant fields from the JSON response and create a list of {@link Book}s
            List<Book> books = extractFeatureFromJson(jsonResponse);

            // Return the list of {@link Book}s
            return books;
        }

        /**
         * Here we update the UI with the new researched book list.
         */
        @Override
        protected void onPostExecute(List<Book> books) {
            View loading = findViewById(loading_indicator);
            loading.setVisibility(GONE);
            if (books == null) {
                return;
            }
            mAdapter.clear();
            updateUi(books);
        }

        /**
         * Returns new URL object from the given string URL.
         */
        private URL createUrl(String stringUrl) {
            URL url = null;
            try {
                url = new URL(stringUrl);
            } catch (MalformedURLException exception) {
                Log.e(LOG_TAG, "Problem building the URL ");
            }
            return url;
        }

        /**
         * Make an HTTP request to the given URL and return a String as the response.
         */
        private String makeHttpRequest(URL url) throws IOException {
            String jsonResponse = "";

            // If the URL is null, then return early.
            if (url == null) {
                return jsonResponse;
            }

            HttpURLConnection urlConnection = null;
            InputStream inputStream = null;
            try {
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setReadTimeout(10000/* time in milliseconds*/);
                urlConnection.setConnectTimeout(15000/* time in milliseconds*/);
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // If the request was successful (response code 200),
                // then read the input stream and parse the response.
                if (urlConnection.getResponseCode() == 200) {
                    inputStream = urlConnection.getInputStream();
                    jsonResponse = readFromStream(inputStream);
                } else {
                    Log.e(LOG_TAG, "Error Response Code: " + urlConnection.getResponseCode());
                }

            } catch (IOException e) {
                Log.e(LOG_TAG, "Problem retrieving the earthquake JSON results.", e);
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }

                if (inputStream != null) {
                    // Closing the input stream could throw an IOException, which is why
                    // the makeHttpRequest(URL url) method signature specifies than an IOException
                    // could be thrown.
                    inputStream.close();
                }
            }

            return jsonResponse;
        }

        /**
         * Convert the {@link InputStream} into a String which contains the
         * whole JSON response from the server.
         */
        private String readFromStream(InputStream inputStream) throws IOException {
            StringBuilder output = new StringBuilder();
            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));

                BufferedReader reader = new BufferedReader(inputStreamReader);
                String line = reader.readLine();
                while (line != null) {
                    output.append(line);
                    line = reader.readLine();
                }
            }

            return output.toString();
        }

        /**
         * Return a list of {@link Book} objects that has been built up from
         * parsing the given JSON response.
         */
        private List<Book> extractFeatureFromJson(String bookJSON) {
            // If the JSON string is empty or null, then return early.
            if (TextUtils.isEmpty(bookJSON)) {
                return null;
            }

            // Create an empty ArrayList that we can start adding books to
            List<Book> books = new ArrayList<>();
            // Try to parse the JSON response string. If there's a problem with the way the JSON
            // is formatted, a JSONException exception object will be thrown.
            // Catch the exception so the app doesn't crash, and print the error message to the logs.
            try {

                // Create a JSONObject from the JSON response string
                JSONObject baseJsonResponse = new JSONObject(bookJSON);


                int numberOfBooks = baseJsonResponse.optInt("totalItems");
                if (numberOfBooks == 0) {
                    Log.i(LOG_TAG, "No Items Found :(");
                    return null;
                }

                // Extract the JSONArray associated with the key called "items",
                // which represents a list of features (or books).
                JSONArray items = baseJsonResponse.optJSONArray("items");

                // For each book in the bookArray, create an {@link Book} object
                for (int i = 0; i < items.length(); i++) {
                    JSONObject currentBook = items.getJSONObject(i);
                    JSONObject volumeInfo = currentBook.getJSONObject("volumeInfo");

                    // Check if title is available & extract the value for the key called "title"
                    String title;
                    if (volumeInfo.has("title")) {
                        title = volumeInfo.getString("title");
                    } else {
                        title = getString(R.string.no_title);
                    }

                    // Check if author(s) is available & extract the value for the key called "authors"
                    String authors = "";
                    JSONArray authorAry = null;
                    if (volumeInfo.has("authors")) {
                        authorAry = volumeInfo.getJSONArray("authors");
                        for (int j = 0; j < authorAry.length(); j++) {
                            authors += authorAry.getString(j) + ", ";
                        }
                    } else {
                        authors = getString(R.string.no_author);
                    }

                    // Check if description is available & extract the value for the key called "description"
                    String description;
                    if (volumeInfo.has("description")) {
                        description = volumeInfo.getString("description");
                    } else {
                        description = getString(R.string.no_description);
                    }

                    // Extract the value for the key called "infoLink"
                    String url = volumeInfo.getString("infoLink");

                    // Create a new {@link Book} object with the title, author, description,
                    // and url from the JSON response.
                    Book booksList = new Book(title, authors, description, url);

                    // Add the new {@link Book} to the list of books.
                    books.add(booksList);
                }

            } catch (JSONException e) {
                // If an error is thrown when executing any of the above statements in the "try" block,
                // catch the exception here, so the app doesn't crash. Print a log message
                // with the message from the exception.
                Log.e("LOG_TAG", "Problem parsing the book JSON results", e);
            }

            // Return the list of books
            return books;
        }
    }
}




