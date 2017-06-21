package com.example.android.findthebook;

/**
 * An {@link Book} object contains information related to a single book.
 */

public class Book {


    /**
     * Title of the book
     */
    private String mTitle;

    /**
     * Author of the book
     */
    private String mAuthor;

    /**
     * Description of the book
     */
    private String mDescription;

    /**
     * Google Book URL of book
     */
    private String mUrl;


    /**
     * Constructs a new {@link Book} object.
     *
     * @param title       is the title of the book
     * @param author      is the author of the book
     * @param description is the description of the book
     * @param url         is the website URL to find more details about the book
     */
    public Book(String title, String author, String description, String url) {
        mTitle = title;
        mAuthor = author;
        mDescription = description;
        mUrl = url;

    }

    /**
     * Returns the title of the book.
     */
    public String getTitle() {
        return mTitle;
    }

    /**
     * Returns the author of the book.
     */
    public String getAuthor() {
        return mAuthor;
    }

    /**
     * Returns the description of the book.
     */
    public String getDescription() {
        return mDescription;
    }

    /**
     * Returns the website URL to find more information about the book.
     */
    public String getURL() {
        return mUrl;
    }
}

