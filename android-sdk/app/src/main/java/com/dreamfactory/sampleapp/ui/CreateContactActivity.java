package com.dreamfactory.sampleapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.dreamfactory.sampleapp.R;
import com.dreamfactory.sampleapp.models.ContactInfoRecord;
import com.dreamfactory.sampleapp.models.ContactInfoRecords;
import com.dreamfactory.sampleapp.models.ContactRecord;
import dfapi.BaseAsyncRequest;
import com.dreamfactory.sampleapp.utils.AppConstants;
import com.dreamfactory.sampleapp.utils.PrefUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import dfapi.ApiException;
import dfapi.ApiInvoker;
import dfapi.FileRequest;

public class CreateContactActivity extends Activity {

    protected EditText firstNameEditText;
    protected EditText lastNameEditText;
    protected EditText twitterEditText;
    protected EditText skypeEditText;
    protected EditText notesEditText;
    protected Button chooseImageButton;

    protected Button addContactInfoButton;

    protected LinearLayout linearLayout;

    protected List<EditInfoViewGroup> editInfoViewGroupList;

    private AddContactTask addContactTask;
    private int groupId;

    private String profileImagePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // since this gets extended, only do fairly generic stuff in the constructor
        setContentView(R.layout.activity_edit_contact);

        Intent intent = getIntent();
        handleIntent(intent);

        firstNameEditText = (EditText) findViewById(R.id.edit_contact_first_name);
        lastNameEditText = (EditText) findViewById(R.id.edit_contact_last_name);
        twitterEditText = (EditText) findViewById(R.id.edit_contact_twitter);
        skypeEditText = (EditText) findViewById(R.id.edit_contact_skype);
        notesEditText = (EditText) findViewById(R.id.edit_contact_notes);
        chooseImageButton = (Button) findViewById(R.id.edit_contact_info_change_photo);

        addContactInfoButton = (Button) findViewById(R.id.edit_contact_add_info);
        addContactInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditInfoViewGroup editInfoViewGroup = new EditInfoViewGroup(CreateContactActivity.this, null);
                editInfoViewGroupList.add(editInfoViewGroup);
                linearLayout.addView(editInfoViewGroup);

                final ScrollView scrollView = (ScrollView) findViewById(R.id.edit_contact_scroll_view);
                scrollView.post(new Runnable() {
                    @Override
                    public void run() {
                        scrollView.fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        });

        editInfoViewGroupList = new ArrayList<>();
        linearLayout = (LinearLayout) findViewById(R.id.edit_contact_linear_layout);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        buildViews();
        handleButtons();
    }

    protected void handleIntent(Intent intent) {
        // contacts are created from contactListActivity as part of a group
        groupId = intent.getIntExtra("contactGroupId", 0);
    }

    protected void handleButtons() {
        ImageButton back_button = (ImageButton) findViewById(R.id.persistent_back_button);
        ImageButton edit_button = (ImageButton) findViewById(R.id.persistent_edit_button);
        ImageButton save_button = (ImageButton) findViewById(R.id.persistent_save_button);
        ImageButton add_button = (ImageButton) findViewById(R.id.persistent_add_button);
        add_button.setVisibility(View.INVISIBLE);

        back_button.setTag(this);
        back_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Activity tmp = (Activity) v.getTag();
                setResult(Activity.RESULT_CANCELED);
                tmp.finish();
            }
        });

        edit_button.setVisibility(View.INVISIBLE);

        save_button.setTag(this);
        save_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CreateContactActivity tmp = (CreateContactActivity) v.getTag();
                addContactTask = new AddContactTask(tmp);
                addContactTask.execute();
            }
        });

        chooseImageButton.setTag(this);
        chooseImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Activity activity = (Activity) v.getTag();
                activity.startActivityForResult(Intent.createChooser(new Intent(Intent.ACTION_GET_CONTENT).setType("image/*"), "choose an image"), 1);
            }
        });
    }

    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        switch(requestCode) {
            case 1:
                if(resultCode == RESULT_OK){
                    try {

                        Uri uri = imageReturnedIntent.getData();
                        String wholeID = DocumentsContract.getDocumentId(uri);
                        String[] column = {MediaStore.Images.Media.DATA};


                        // Split at colon, use second item in the array
                        String id = wholeID.split(":")[1];

                        // where id is equal to
                        String sel = MediaStore.Images.Media._ID + "=?";

                        Cursor cursor = this.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                column, sel, new String[]{ id }, null);

                        int columnIndex = cursor.getColumnIndex(column[0]);

                        if (cursor.moveToFirst()) {
                            profileImagePath = cursor.getString(columnIndex);
                        }
                        cursor.close();
                    }catch (Exception e){
                        Log.e("createContactActivity", "could not decode image: " + e.toString());
                    }
                }
        }
    }


    protected void buildViews() {
        firstNameEditText = (EditText) findViewById(R.id.edit_contact_first_name);
        lastNameEditText = (EditText) findViewById(R.id.edit_contact_last_name);
        twitterEditText = (EditText) findViewById(R.id.edit_contact_twitter);
        skypeEditText = (EditText) findViewById(R.id.edit_contact_skype);
        notesEditText = (EditText) findViewById(R.id.edit_contact_notes);
    }

    private class AddContactTask extends BaseAsyncRequest {
        private CreateContactActivity createContactActivity;
        private int contactId;
        // once you add the contact and get back the contact id, finish the activity
        public AddContactTask(CreateContactActivity activity){ createContactActivity = activity; }
        @Override
        protected void doSetup() throws ApiException {
            callerName = "AddContactTask";

            serviceName = "db";
            endPoint = "contacts";

            verb = "POST";

            // build contact record, don't have id yet so can't provide one
            ContactRecord contactRecord = new ContactRecord();
            contactRecord.firstName = contactRecord.getNonNull(firstNameEditText.getText().toString());
            contactRecord.lastName = contactRecord.getNonNull(lastNameEditText.getText().toString());
            contactRecord.skype = contactRecord.getNonNull(skypeEditText.getText().toString());
            contactRecord.twitter = contactRecord.getNonNull(twitterEditText.getText().toString());
            contactRecord.notes = contactRecord.getNonNull(notesEditText.getText().toString());
            if(profileImagePath != null && !profileImagePath.isEmpty()){
                contactRecord.imageUrl = "testFile.png";
            }

            requestString = ApiInvoker.serialize(contactRecord);

            applicationName = AppConstants.APP_NAME;
            sessionId = PrefUtil.getString(getApplicationContext(), AppConstants.SESSION_ID);
        }

        @Override
        protected void processResponse(String response) throws ApiException, org.json.JSONException {
            // response has whole contact record, but we just want the contactId
            contactId = new JSONObject(response).getInt("contactId");
        }

        @Override
        protected void onCompletion(boolean success) {
            if(success){
                addContactTask = null;
                if(editInfoViewGroupList.size() > 0) {
                    // build a list of contact_info records to create
                    ContactInfoRecords contactInfoRecords = new ContactInfoRecords();
                    for (EditInfoViewGroup viewGroup : editInfoViewGroupList) {
                        ContactInfoRecord contactInfoRecord = viewGroup.buildToContactInfoRecord();
                        // need to include the contactId
                        contactInfoRecord.contactId = contactId;
                        contactInfoRecords.record.add(contactInfoRecord);
                    }

                    AddContactInfoTask addContactInfoTask =
                            new AddContactInfoTask(contactInfoRecords);
                    addContactInfoTask.execute();
                }

                // since contact is created as part of a group, there is always a relationship to make
                AddContactToGroupTask addContactToGroupTask =
                        new AddContactToGroupTask(groupId, contactId);
                addContactToGroupTask.execute();

                if(profileImagePath != null && !profileImagePath.isEmpty()){
                    new CreateFolderTask(contactId).execute();
                }
                setResult(Activity.RESULT_OK);
            }
            else{
                setResult(Activity.RESULT_CANCELED);
            }

            // let the rest of the contact stuff get uploaded while this view finishes
            // the group in the ContactList activity
            createContactActivity.finish();
        }
    }

    protected class AddContactInfoTask extends BaseAsyncRequest {
        private ContactInfoRecords contactInfoRecords;
        public AddContactInfoTask(ContactInfoRecords records){
            contactInfoRecords = records;
        }

        @Override
        protected void doSetup() throws ApiException {
            callerName = "AddContactInfoTask";

            serviceName = "db";
            endPoint = "contact_info";

            verb = "POST";

            // body is an array of contact_info records to create
            // form is:
            // {
            //      "record":[
            //          { ContactInfoRecord }
            //      ]
            // }

            requestString = ApiInvoker.serialize(contactInfoRecords);

            applicationName = AppConstants.APP_NAME;
            sessionId = PrefUtil.getString(getApplicationContext(), AppConstants.SESSION_ID);
        }
    }

    private class AddContactToGroupTask extends BaseAsyncRequest{
        private int groupId;
        private int contactId;

        public AddContactToGroupTask(int groupId, int contactId){
            this.groupId = groupId;
            this.contactId = contactId;
        }

        @Override
        protected void doSetup() throws ApiException, JSONException {
            callerName = "AddContactToGroup";

            serviceName = "db";
            endPoint = "contact_relationships";

            verb = "POST";

            // request body is { "contactGroupId":cgId, "contactId":cId }
            requestBody = new JSONObject();
            requestBody.put("contactGroupId", groupId);
            requestBody.put("contactId", contactId);

            applicationName = AppConstants.APP_NAME;
            sessionId = PrefUtil.getString(getApplicationContext(), AppConstants.SESSION_ID);
        }
    }

    private class CreateFolderTask extends BaseAsyncRequest {
        private int contactId;
        public CreateFolderTask(int contactId){ this.contactId = contactId; }

        @Override
        protected void doSetup() throws ApiException, JSONException {
            callerName = "CreateFolderTask";

            serviceName = "files";
            applicationName = AppConstants.APP_NAME;

            // build rest path for request, form is <url to DSP>/rest/files/container/application/<folder path>
            // here the folder path is profile_images/contactId/
            // the file path ends in a '/' because we are targeting a folder
            String containerName = "applications";
            String folderPath = "profile_images/" + contactId + "/";
            endPoint = containerName + "/" + applicationName + "/" + folderPath;

            verb = "POST";

            sessionId = PrefUtil.getString(getApplicationContext(), AppConstants.SESSION_ID);
        }

        @Override
        protected void onCompletion(boolean success) {
            if(success){
                new UploadImageTask(contactId).execute();
            }
        }
    }

    private class UploadImageTask extends BaseAsyncRequest{
        private int contactId;
        public UploadImageTask(int contactId){ this.contactId = contactId; }
        @Override
        protected void doSetup() throws ApiException, JSONException {
            callerName = "createImageTask";

            serviceName = "files";
            applicationName = AppConstants.APP_NAME;

            // build rest path for request, form is <url to DSP>/rest/files/container/application/<folder path>/<file name>
            // here the folder path is profile_images/contactId
            String containerName = "applications";
            String folderPath = "profile_images/" + contactId + "/";
            String fileName = "testFile.png";
            endPoint = containerName + "/" + applicationName + "/" + folderPath + "/" + fileName;

            verb = "POST";

            fileRequest = new FileRequest();
            fileRequest.setPath(profileImagePath);

            sessionId = PrefUtil.getString(getApplicationContext(), AppConstants.SESSION_ID);
        }
    }
}