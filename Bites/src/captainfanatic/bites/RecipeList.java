package captainfanatic.bites;

/*
 * Send Text message using built in sms message app
 * 
 *  Intent sendIntent = new Intent(Intent.ACTION_VIEW);
 *  sendIntent.putExtra("sms_body", "Content of the SMS goes here...");
 *  sendIntent.setType("vnd.android-dir/mms-sms");
 *  startActivity(sendIntent);
 *  
 *  from http://mobiforge.com/developing/story/sms-messaging-android
*/
import captainfanatic.bites.RecipeBook.Ingredients;
import captainfanatic.bites.RecipeBook.Methods;
import captainfanatic.bites.RecipeBook.Recipes;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class RecipeList extends ListActivity {
	
	private static final String TAG = "RecipeList";
	// Menu item ids
	public static final int MENU_ITEM_EDIT = Menu.FIRST;
	public static final int MENU_ITEM_DELETE = Menu.FIRST + 1;
    public static final int MENU_ITEM_INSERT = Menu.FIRST + 2;
    public static final int MENU_ITEM_SEND = Menu.FIRST + 3;
    
    /**
     * Case selections for the type of dialog box displayed
     */
    private static final int DIALOG_EDIT = 1;
    private static final int DIALOG_DELETE = 2;
    private static final int DIALOG_INSERT = 3;
    private static final int DIALOG_RECVD = 4;
    
    /**
     * Column indexes
     */
    private static final int COLUMN_INDEX_ID = 0;
    private static final int COLUMN_INDEX_TITLE = 1;

	/**
     * The columns we are interested in from the database
     */
    private static final String[] PROJECTION = new String[] {
            Recipes._ID, // 0
            Recipes.TITLE, // 1
    };
    	
    private Cursor mCursor;
    
    private Uri mUri;
    
    //Use private members for dialog textview to prevent weird persistence problem
	private EditText mDialogEdit;
	private View mDialogView;
	private TextView mDialogText;
    private TextView mHeader;
    
    /**
     * Custom adapter for recipe list.
     * Allows filtering by recipe name.
     * @author Ben
     *
     */
    private class RecipeAdapter extends SimpleCursorAdapter implements Filterable
    {
    	private ContentResolver mContent;   
    	
		public RecipeAdapter(Context context, int layout, Cursor c,
				String[] from, int[] to) {
			super(context, layout, c, from, to);
			mContent = context.getContentResolver();
		}

		@Override
        public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
            if (getFilterQueryProvider() != null) {
                return getFilterQueryProvider().runQuery(constraint);
            }

            StringBuilder buffer = null;
            String[] args = null;
            if (constraint != null) {
                buffer = new StringBuilder();
                buffer.append("UPPER(");
                buffer.append(Recipes.TITLE);
                buffer.append(") GLOB ?");
                args = new String[] { constraint.toString().toUpperCase() + "*" };
            }

            return mContent.query(Recipes.CONTENT_URI, PROJECTION,
                    buffer == null ? null : buffer.toString(), args,
                    null);
        }
    }
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		Intent intent = getIntent();
		if (intent.getData() == null) {
            intent.setData(Recipes.CONTENT_URI);
        }
        
        setContentView(R.layout.recipes);
        
        mHeader = (TextView)findViewById(R.id.recipeheader);
	        
        getListView().setOnCreateContextMenuListener(this);
	}
	
	
	
	@Override
	protected void onResume() {
		super.onResume();
			
		mCursor = managedQuery(Recipes.CONTENT_URI, PROJECTION, null, null,
                Recipes.DEFAULT_SORT_ORDER);

        // Used to map notes entries from the database to views
        RecipeAdapter adapter = new RecipeAdapter(this, R.layout.recipelist_item, mCursor,
                new String[] { Recipes.TITLE }, new int[] { R.id.recipetitle});
        setListAdapter(adapter);
        if (mCursor.moveToFirst())
        {
	        Bites.mRecipeId = mCursor.getLong(COLUMN_INDEX_ID);
	        Bites.mRecipeName = mCursor.getString(COLUMN_INDEX_TITLE);
        }
        else
        {
        	Bites.mRecipeId = 0;
        	Bites.mRecipeName = "";
        }
        mHeader.setText(Bites.mRecipeName);
	}



	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		// Insert a new recipe into the list
        menu.add(0, MENU_ITEM_INSERT, 0, "insert")
                .setShortcut('3', 'a')
                .setIcon(android.R.drawable.ic_menu_add);
        menu.add(0, MENU_ITEM_EDIT, 0, "edit")
        .setIcon(android.R.drawable.ic_menu_edit);
        menu.add(0, MENU_ITEM_SEND, 0, "send")
        .setIcon(android.R.drawable.ic_menu_send);
        
     // Generate any additional actions that can be performed on the
        // overall list.  In a normal install, there are no additional
        // actions found here, but this allows other applications to extend
        // our menu with their own actions.
        Intent intent = new Intent(null, getIntent().getData());
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, RecipeList.class), null, intent, 0, null);
       
        menu.add(0, MENU_ITEM_DELETE, 0, "delete")
        .setIcon(android.R.drawable.ic_menu_delete);
        
        return true;
	}

	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_ITEM_INSERT:
            // Insert a new item
        	showDialog(DIALOG_INSERT);
        	mDialogEdit.setText("");
        	break;
	    case MENU_ITEM_EDIT:
	        // Edit an existing item
			showDialog(DIALOG_EDIT);
			mDialogEdit.setText(mCursor.getString(1));
			break;
	    case MENU_ITEM_DELETE:
	        // Delete an existing item
			showDialog(DIALOG_DELETE);
			mDialogText.setText(mCursor.getString(1));
			break;
	    case MENU_ITEM_SEND:
	    	// Send a recipe
	    	SendRecipe();
	    	break;
	    }
        return super.onOptionsItemSelected(item);
    }

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		AdapterView.AdapterContextMenuInfo info;
        try {
             info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return;
        }
		Cursor cursor = (Cursor)getListAdapter().getItem(info.position);
		if (cursor == null) {
            // For some reason the requested item isn't available, do nothing
            return;
        }
        // Setup the menu header
        menu.setHeaderTitle(cursor.getString(COLUMN_INDEX_TITLE));
        // Add a menu item to delete the note
        menu.add(0, MENU_ITEM_EDIT, 0, R.string.edit_recipe);
        menu.add(0, MENU_ITEM_DELETE, 0, R.string.delete_recipe);
        menu.add(0, MENU_ITEM_SEND, 0, R.string.send_recipe);
	}
	
	

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info;
        try {
             info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return false;
        }
        
        Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
        if (cursor == null) {
            // For some reason the requested item isn't available, do nothing
            return false;
        }
        
        mUri = ContentUris.withAppendedId(getIntent().getData(), cursor.getLong(COLUMN_INDEX_ID));
        Bites.mRecipeId = mCursor.getLong(COLUMN_INDEX_ID);
        Bites.mRecipeName = mCursor.getString(COLUMN_INDEX_TITLE);

        switch (item.getItemId()) {
	        case MENU_ITEM_EDIT: {
                // Edit the ingredient that the context menu is for
	        	showDialog(DIALOG_EDIT);
				mDialogEdit.setText(cursor.getString(COLUMN_INDEX_TITLE));
                return true;	        	
	        }    
	        case MENU_ITEM_DELETE: {
                // Delete the note that the context menu is for
	        	showDialog(DIALOG_DELETE);
				mDialogText.setText(cursor.getString(COLUMN_INDEX_TITLE));
                return true;
            }
	        
	        case MENU_ITEM_SEND: {
	        	//Send the recipe via sms
	        	SendRecipe();
	        	return true;
	        }
        }
        return false;
	}

	private void SendRecipe(){
		Intent sendIntent = new Intent(Intent.ACTION_VIEW);
		
		String msg;
		//Get an ingredient cursor
    	Cursor cIngredient = getContentResolver().query(Ingredients.CONTENT_URI, 
    											new String[] {Ingredients._ID, Ingredients.TEXT}, 
    											Ingredients.RECIPE + "=" + Bites.mRecipeId , 
    											null, 
    											null);
    	Cursor cMethod = getContentResolver().query(Methods.CONTENT_URI, 
				new String[] {Methods._ID, Methods.STEP, Methods.TEXT}, 
				Methods.RECIPE + "=" + Bites.mRecipeId , 
				null, 
				null);
    	
    	msg =  "***Bites Recipe***\n";
    	msg = msg + Bites.mRecipeName + "\n";
    	msg = msg + "**Ingredients**\n";
    	//Get ingredients
    	int colIng = cIngredient.getColumnIndex(Ingredients.TEXT);
    	cIngredient.moveToFirst();
    	while (!cIngredient.isLast() && !cIngredient.isNull(0) )
    	{
    		
    		msg = msg + cIngredient.getString(colIng) + "\n";
    		cIngredient.moveToNext();
    	}
    	msg = msg + "**Method**\n";
    	//Get methods
    	int colStep = cMethod.getColumnIndex(Methods.STEP);
    	int colMethod = cMethod.getColumnIndex(Methods.TEXT);
    	cMethod.moveToFirst();
    	while (!cMethod.isLast())
    	{
    		msg = msg + cMethod.getString(colStep) + "." + cMethod.getString(colMethod) + "\n";
    		cMethod.moveToNext();
    	}
    	msg = msg + "***";
    	sendIntent.putExtra("sms_body", msg);
    	sendIntent.setType("vnd.android-dir/mms-sms");
    	startActivity(sendIntent);
	}


	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		mUri = ContentUris.withAppendedId(getIntent().getData(), id);
		Bites.mRecipeId = id;
		//Get a temp cursor to the uri of the clicked item
		Cursor c = getContentResolver().query(mUri, PROJECTION, null, null, null);
		c.moveToLast();
		Bites.mRecipeName = c.getString(COLUMN_INDEX_TITLE);
		//Update the header text with the current recipe name
		mHeader.setText(Bites.mRecipeName);
	}
	
	
	@Override
	protected Dialog onCreateDialog(int id) {
		LayoutInflater factory = LayoutInflater.from(this);
		switch (id) {
		case DIALOG_INSERT:
			mDialogView = factory.inflate(R.layout.dialog_recipename, null);
			mDialogEdit = (EditText)mDialogView.findViewById(R.id.recipename_edit);
            return new AlertDialog.Builder(this)
                .setTitle(R.string.recipe_name)
                .setView(mDialogView)
                .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                	public void onClick(DialogInterface dialog, int whichButton) {
                    	/* User clicked OK so do some stuff */
                    	ContentValues values = new ContentValues();
                    	values.put(Recipes.TITLE, mDialogEdit.getText().toString());
                    	mUri = getContentResolver().insert(Recipes.CONTENT_URI,values);
                    	getListView().performItemClick(getListView(), 0, Long.parseLong(mUri.getLastPathSegment()));
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        /* User clicked cancel so do some stuff */
                    }
                })
                .create();
		case DIALOG_EDIT:
			mDialogView = factory.inflate(R.layout.dialog_recipename, null);
			mDialogEdit = (EditText)mDialogView.findViewById(R.id.recipename_edit);
            return new AlertDialog.Builder(this)
                .setTitle(R.string.recipe_name)
                .setView(mDialogView)
                .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                	public void onClick(DialogInterface dialog, int whichButton) {
                    	/* User clicked OK so do some stuff */
                    	ContentValues values = new ContentValues();
                        values.put(Recipes.TITLE, mDialogEdit.getText().toString());
                        getContentResolver().update(mUri, values, null, null);
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        /* User clicked cancel so do some stuff */
                    }
                })
                .create();
		case DIALOG_DELETE:
			mDialogView = factory.inflate(R.layout.dialog_confirm, null);
			mDialogText = (TextView)mDialogView.findViewById(R.id.dialog_confirm_prompt);
            return new AlertDialog.Builder(this)
                .setTitle(R.string.delete_recipe)
                .setView(mDialogView)
                .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                	public void onClick(DialogInterface dialog, int whichButton) {
                    	/* User clicked OK so do some stuff */
                        getContentResolver().delete(mUri, null, null);
                        //Requery cursor to update with removed row
                        mCursor.requery();
                        if (mCursor.moveToFirst()) 
                        {
                        	getListView().performItemClick(null, 0, mCursor.getLong(0));
                        }
                        else //empty list - clear the recipe title header
                        {
                        	Bites.mRecipeId = 0;
                        	Bites.mRecipeName = "";
                        	mHeader.setText(Bites.mRecipeName);
                        }
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        /* User clicked cancel so do some stuff */
                    }
                })
                .create();
		}
		return null;
	}
}