package com.example.android.pets.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import static android.R.attr.defaultHeight;
import static android.R.attr.fingerprintAuthDrawable;
import static android.R.attr.id;
import static com.example.android.pets.data.PetContract.CONTENT_AUTHORITY;
import static com.example.android.pets.data.PetContract.PATH_PETS;

public class PetProvider extends ContentProvider {

    private static final int PETS = 100;
    private static final int PET_ID = 101;

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static{
        sUriMatcher.addURI(CONTENT_AUTHORITY,PATH_PETS,PETS);
        sUriMatcher.addURI(CONTENT_AUTHORITY,PATH_PETS + "/#",PET_ID);
    }

    private PetDbHelper mDbHelper;
    public static final String LOG_TAG = PetProvider.class.getSimpleName();

    @Override
    public boolean onCreate(){
        mDbHelper = new PetDbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder){

        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        Cursor cursor;

        int match = sUriMatcher.match(uri);
        switch (match){
            case PETS:
                cursor = database.query(PetContract.PetEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case PET_ID:
                selection = PetContract.PetEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                cursor = database.query(PetContract.PetEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

        cursor.setNotificationUri(getContext().getContentResolver(),uri);


        return cursor;
    }

    @Override
    public String getType(Uri uri){
        final int match = sUriMatcher.match(uri);
        switch (match){
            case PETS:
                return PetContract.PetEntry.CONTENT_LIST_TYPE;
            case PET_ID:
                return PetContract.PetEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unkown uri"+uri);
        }
    }


    @Override
    public Uri insert(Uri uri, ContentValues contentValues){

        final int match = sUriMatcher.match(uri);
        switch(match){
            case PETS:
                return insertPet(uri, contentValues);
            default:
                throw new IllegalArgumentException("Insertion is not suported for "+uri);
        }
    }

    private Uri insertPet(Uri uri, ContentValues values){

        isValid(values);

        SQLiteDatabase database = mDbHelper.getReadableDatabase();
        Long id = database.insert(PetContract.PetEntry.TABLE_NAME, null, values);
        if(id==-1){
            Log.e(LOG_TAG, "Failed to insert data at " + uri);
        }

        getContext().getContentResolver().notifyChange(uri,null);

        return ContentUris.withAppendedId(uri,id);
    }


    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs){

        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        final int match = sUriMatcher.match(uri);
        switch (match){
            case PETS:
                int rowsDeleted = database.delete(PetContract.PetEntry.TABLE_NAME, selection, selectionArgs);
                if(rowsDeleted!=0){
                    getContext().getContentResolver().notifyChange(uri,null);
                }
                return rowsDeleted ;
            case PET_ID:
                selection = PetContract.PetEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                int rowsDeletedB = database.delete(PetContract.PetEntry.TABLE_NAME, selection, selectionArgs);
                if(rowsDeletedB!=0){
                    getContext().getContentResolver().notifyChange(uri,null);
                }
                return rowsDeletedB;
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }

    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs){
        isValid(contentValues);

        final int match = sUriMatcher.match(uri);
        switch (match){
            case PETS:
                return updatePet(uri, contentValues, selection, selectionArgs);
            case PET_ID:
                selection = PetContract.PetEntry._ID + "=?";
                selectionArgs = new String[] {String.valueOf(ContentUris.parseId(uri))};
                return updatePet(uri, contentValues, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " +uri);
        }
    }

    private int updatePet(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs){

        if(contentValues.size() == 0){
            return 0;
        }
        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        int rowsUpdated = database.update(PetContract.PetEntry.TABLE_NAME,contentValues,selection,selectionArgs);
        if(rowsUpdated!=0){
            getContext().getContentResolver().notifyChange(uri, null);
        }
       return rowsUpdated;
    }

    private void isValid(ContentValues values){

        String name = values.getAsString(PetContract.PetEntry.COLUMN_PET_NAME);
        String breed = values.getAsString((PetContract.PetEntry.COLUMN_PET_BREED));
        Integer weight = values.getAsInteger(PetContract.PetEntry.COLUMN_PET_WEIGHT);
        Integer gender = values.getAsInteger(PetContract.PetEntry.COLUMN_PET_GENDER);

        if(name==null){
            throw new IllegalArgumentException("Pet Requires a Name");
        }

        if(breed==null){
            throw new IllegalArgumentException("Pet Requires a Breed");
        }

        if(weight<=0 && weight!=null){
            throw new IllegalArgumentException("Pet Requires valid weight");
        }

        if(gender==null||!PetContract.PetEntry.isGenderValid(gender)){
            throw new IllegalArgumentException("Pet Requires valid gender");
        }

    }
}
