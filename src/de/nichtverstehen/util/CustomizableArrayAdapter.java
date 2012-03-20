package de.nichtverstehen.util;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.util.List;

public class CustomizableArrayAdapter<T> extends ArrayAdapter<T> {

    protected LayoutInflater mInflater;
    protected int mResource;
    protected int mDropDownResource;
    protected int mFieldId;

    public CharSequence getItemText(T item) {
        if (item instanceof CharSequence) {
            return (CharSequence) item;
        } else {
            return item.toString();
        }
    }

    public void prepareItemView(View view, T item) {
        TextView text;
        try {
            if (mFieldId == 0) {
                //  If no custom field is assigned, assume the whole resource is a TextView
                text = (TextView) view;
            } else {
                //  Otherwise, find the TextView field within the layout
                text = (TextView) view.findViewById(mFieldId);
            }
        } catch (ClassCastException e) {
            Log.e("ArrayAdapter", "You must supply a resource ID for a TextView");
            throw new IllegalStateException(
                    "ArrayAdapter requires the resource ID to be a TextView", e);
        }

        CharSequence itemText = getItemText(item);
        text.setText(itemText);
    }

    private View getCustomView(int position, View convertView, ViewGroup parent,
                                        int resource) {
        View view;

        if (convertView == null) {
            view = mInflater.inflate(resource, parent, false);
        } else {
            view = convertView;
        }

        T item = getItem(position);
        prepareItemView(view, item);

        return view;
    }

    private void init(Context context, int resource, int textViewResourceId) {
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mResource = mDropDownResource = resource;
        mFieldId = textViewResourceId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, convertView, parent, mResource);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDropDownViewResource(int resource) {
        super.setDropDownViewResource(resource);
        this.mDropDownResource = resource;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, convertView, parent, mDropDownResource);
    }

    /**
     * {@inheritDoc}
     */
    public CustomizableArrayAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
        init(context, textViewResourceId, 0);
    }

    /**
     * {@inheritDoc}
     */
    public CustomizableArrayAdapter(Context context, int resource, int textViewResourceId) {
        super(context, resource, textViewResourceId);
        init(context, resource, textViewResourceId);
    }

    /**
     * {@inheritDoc}
     */
    public CustomizableArrayAdapter(Context context, int textViewResourceId, T[] objects) {
        super(context, textViewResourceId, objects);
        init(context, textViewResourceId, 0);
    }

    /**
     * {@inheritDoc}
     */
    public CustomizableArrayAdapter(Context context, int resource, int textViewResourceId, T[] objects) {
        super(context, resource, textViewResourceId, objects);
        init(context, resource, textViewResourceId);
    }

    /**
     * {@inheritDoc}
     */
    public CustomizableArrayAdapter(Context context, int textViewResourceId, List<T> objects) {
        super(context, textViewResourceId, objects);
        init(context, textViewResourceId, 0);
    }

    /**
     * {@inheritDoc}
     */
    public CustomizableArrayAdapter(Context context, int resource, int textViewResourceId, List<T> objects) {
        super(context, resource, textViewResourceId, objects);
        init(context, resource, textViewResourceId);
    }
}
