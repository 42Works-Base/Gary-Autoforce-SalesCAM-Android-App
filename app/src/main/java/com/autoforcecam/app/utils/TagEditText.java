package com.autoforcecam.app.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatEditText;

import com.autoforcecam.app.R;

public class TagEditText extends AppCompatEditText {

    TextWatcher textWatcher;

    String lastString;
    boolean showTagImage = true;

    String separator = " ";

    public TagEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }


    private void init() {
        setMovementMethod(LinkMovementMethod.getInstance());

        textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String thisString = s.toString();
                if (thisString.length() > 0 && !thisString.equals(lastString)) {
                    format();

                }
            }
        };

        addTextChangedListener(textWatcher);
    }

    public void shoeTagImage(boolean status){

        showTagImage = status;

    }


    public void format() {

        SpannableStringBuilder sb = new SpannableStringBuilder();
        String fullString = getText().toString();

        String[] strings = fullString.split(separator);


        for (int i = 0; i < strings.length; i++) {

            String string = strings[i];
            sb.append(string);

            if (fullString.charAt(fullString.length() - 1) != separator.charAt(0) && i == strings.length - 1) {
                break;
            }

            BitmapDrawable bd = (BitmapDrawable) convertViewToDrawable(createTokenView(string));
            bd.setBounds(0, 0, bd.getIntrinsicWidth(), bd.getIntrinsicHeight());

            int startIdx = sb.length() - (string.length());
            int endIdx = sb.length();

            sb.setSpan(new ImageSpan(bd), startIdx, endIdx, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            MyClickableSpan myClickableSpan = new MyClickableSpan(startIdx, endIdx);
            sb.setSpan(myClickableSpan, Math.max(endIdx-2, startIdx), endIdx, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            if (i < strings.length - 1) {
                sb.append(separator);
            } else if (fullString.charAt(fullString.length() - 1) == separator.charAt(0)) {
                sb.append(separator);
            }
        }


        lastString = sb.toString();

        setText(sb);
        setSelection(sb.length());

    }

    public View createTokenView(String text) {

        LinearLayout ll = (LinearLayout) LayoutInflater.from(getContext()).inflate(R.layout.item_tag, null);
        TextView txt_name = ll.findViewById(R.id.txt_name);
        ImageView img_tag = ll.findViewById(R.id.img_tag);

        txt_name.setText(text);
        if(showTagImage){
            img_tag.setVisibility(VISIBLE);
        } else {
            img_tag.setVisibility(GONE);
        }

        return ll;
    }

    public Object convertViewToDrawable(View view) {
        int spec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        view.measure(spec, spec);
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());

        Bitmap b = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);

        Canvas c = new Canvas(b);

        c.translate(-view.getScrollX(), -view.getScrollY());
        view.draw(c);
        view.setDrawingCacheEnabled(true);
        Bitmap cacheBmp = view.getDrawingCache();
        Bitmap viewBmp = cacheBmp.copy(Bitmap.Config.ARGB_8888, true);
        view.destroyDrawingCache();
        return new BitmapDrawable(getContext().getResources(), viewBmp);
    }

    private class MyClickableSpan extends ClickableSpan {

        int startIdx;
        int endIdx;

        public MyClickableSpan(int startIdx, int endIdx) {
            super();
            this.startIdx = startIdx;
            this.endIdx = endIdx;
        }

        @Override
        public void onClick(View widget) {

            String s = getText().toString();

            String s1 = s.substring(0, startIdx);
            String s2 = s.substring(Math.min(endIdx+1, s.length()-1), s.length() );

            TagEditText.this.setText(s1 + s2);
        }

    }
}
