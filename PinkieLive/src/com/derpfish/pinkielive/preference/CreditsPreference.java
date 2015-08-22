package com.derpfish.pinkielive.preference;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.preference.DialogPreference;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import com.derpfish.pinkielive.R;

public class CreditsPreference extends DialogPreference {

    private View view;

    public CreditsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public CreditsPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize();
    }

    private void initialize() {
        this.setWidgetLayoutResource(0);
    }

    @Override
    public void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);

        if (view == null) {
            TextView textView = new TextView(getContext());
            textView.setBackgroundColor(Color.WHITE);
            textView.setTextSize(18.0f);
            textView.setText(Html.fromHtml(getContext().getString(R.string.credits_string)));
            textView.setMovementMethod(LinkMovementMethod.getInstance());

            ScrollView scrollView = new ScrollView(getContext());
            scrollView.setFillViewport(true);
            scrollView.addView(textView);

            view = scrollView;
        }

        builder.setView(view);
        builder.setIcon(0);
        builder.setPositiveButton(null, null);
        builder.setNegativeButton(null, null);
    }
}
