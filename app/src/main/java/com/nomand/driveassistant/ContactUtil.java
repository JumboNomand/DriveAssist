package com.nomand.driveassistant;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;

import java.util.ArrayList;
import java.util.List;

public class ContactUtil {

    public static List<Contact> getContactList(Context context){
        List<Contact> contactList = new ArrayList<>();

        Cursor phones = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.
                CONTENT_URI, null,null,null, null);
        while (phones.moveToNext())
        {
            contactList.add(new Contact(phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)),
                    phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                    ));
        }
        phones.close();

        return contactList;
    }

    public static Contact searchContactList(List<Contact> list, String name){

        for (Contact contact:list){
            if (contact.name.equals(name))
                return contact;
        }

        return null;
    }
}

class Contact {
    //private int id;
    public String name;
    public String phoneNum;

    public Contact (String name, String phoneNum){
        this.name = name;
        this.phoneNum = phoneNum;
    }
}
