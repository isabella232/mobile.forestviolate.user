/*
 * Project: Forest violations
 * Purpose: Mobile application for registering facts of the forest violations.
 * Author:  Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:  NikitaFeodonit, nfeodonit@yandex.com
 * *****************************************************************************
 * Copyright (c) 2015-2015. NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextgis.safeforest.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import com.nextgis.safeforest.R;


public class YesNoDialog
        extends DialogFragment
{
    protected Integer mIconId;

    protected Integer mTitleId;
    protected Integer mMessageId;
    protected Integer mPositiveTextId;
    protected Integer mNegativeTextId;

    protected CharSequence mTitleText;
    protected CharSequence mMessageText;
    protected CharSequence mPositiveText;
    protected CharSequence mNegativeText;

    protected ImageView    mIcon;
    protected TextView     mTitle;
    protected LinearLayout mDialogLayout;
    protected TextView     mMessage;
    protected View         mView;
    protected TableLayout  mButtons;
    protected Button       mBtnPositive;
    protected Button       mBtnNegative;

    protected OnPositiveClickedListener mOnPositiveClickedListener;
    protected OnNegativeClickedListener mOnNegativeClickedListener;
    protected OnCancelListener          mOnCancelListener;
    protected OnDismissListener         mOnDismissListener;

    protected boolean mKeepInstance = false;


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setRetainInstance(mKeepInstance);
    }


    @Override
    public void onDestroyView()
    {
        if (getDialog() != null && getRetainInstance()) {
            getDialog().setOnDismissListener(null);
        }

        mDialogLayout.removeAllViews();

        super.onDestroyView();
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        // Idea from here
        // http://thanhcs.blogspot.ru/2014/10/android-custom-dialog-fragment.html
        Dialog dialog = new Dialog(getActivity());

        Window window = dialog.getWindow();
        window.requestFeature(Window.FEATURE_NO_TITLE);
        window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        dialog.setContentView(R.layout.dialog_yes_no);

        mIcon = (ImageView) dialog.findViewById(R.id.dialog_icon_yn);
        mTitle = (TextView) dialog.findViewById(R.id.dialog_title_yn);
        mDialogLayout = (LinearLayout) dialog.findViewById(R.id.dialog_layout_yn);
        mButtons = (TableLayout) dialog.findViewById(R.id.dialog_buttons_yn);
        mBtnPositive = (Button) dialog.findViewById(R.id.dialog_btn_positive_yn);
        mBtnNegative = (Button) dialog.findViewById(R.id.dialog_btn_negative_yn);

        if (null != mIconId) {
            mIcon.setVisibility(View.VISIBLE);
            mIcon.setImageResource(mIconId);
        }

        if (null != mTitleId) {
            mTitle.setText(mTitleId);
        }
        if (null != mTitleText) {
            mTitle.setText(mTitleText);
        }

        if (null != mMessageId) {
            setMessageView();
            mMessage.setText(mMessageId);
        }
        if (null != mMessageText) {
            setMessageView();
            mMessage.setText(mMessageText);
        }

        if (null != mView) {
            mDialogLayout.setVisibility(View.VISIBLE);
            mDialogLayout.addView(mView);
        }

        if (null != mPositiveTextId) {
            mButtons.setVisibility(View.VISIBLE);
            mBtnPositive.setVisibility(View.VISIBLE);
            mBtnPositive.setText(mPositiveTextId);
        }
        if (null != mPositiveText) {
            mButtons.setVisibility(View.VISIBLE);
            mBtnPositive.setVisibility(View.VISIBLE);
            mBtnPositive.setText(mPositiveText);
        }

        if (null != mNegativeTextId) {
            mButtons.setVisibility(View.VISIBLE);
            mBtnNegative.setVisibility(View.VISIBLE);
            mBtnNegative.setText(mNegativeTextId);
        }
        if (null != mNegativeText) {
            mButtons.setVisibility(View.VISIBLE);
            mBtnNegative.setVisibility(View.VISIBLE);
            mBtnNegative.setText(mNegativeText);
        }


        if (null != mOnPositiveClickedListener) {
            mButtons.setVisibility(View.VISIBLE);
            mBtnPositive.setVisibility(View.VISIBLE);
            mBtnPositive.setOnClickListener(
                    new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View v)
                        {
                            if (null != mOnPositiveClickedListener) {
                                mOnPositiveClickedListener.onPositiveClicked();
                                return;
                            }
                            dismiss();
                        }
                    });
        }

        if (null != mOnNegativeClickedListener) {
            mButtons.setVisibility(View.VISIBLE);
            mBtnNegative.setVisibility(View.VISIBLE);
            mBtnNegative.setOnClickListener(
                    new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View v)
                        {
                            if (null != mOnNegativeClickedListener) {
                                mOnNegativeClickedListener.onNegativeClicked();
                                return;
                            }
                            dismiss();
                        }
                    });
        }

        return dialog;
    }


    protected void setMessageView()
    {
        LinearLayout layout =
                (LinearLayout) View.inflate(getActivity(), R.layout.dialog_yes_no_message, null);
        mMessage = (TextView) layout.findViewById(R.id.dialog_message_yn);
        mDialogLayout.setVisibility(View.VISIBLE);
        mDialogLayout.addView(layout);
    }


    public void setView(View view)
    {
        mView = view;
    }


    @Override
    public void onCancel(DialogInterface dialog)
    {
        if (null != mOnCancelListener) {
            mOnCancelListener.onCancel();
        }
        super.onCancel(dialog);
    }


    @Override
    public void onDismiss(DialogInterface dialog)
    {
        if (null != mOnDismissListener) {
            mOnDismissListener.onDismiss();
        }
        super.onDismiss(dialog);
    }


    public YesNoDialog setKeepInstance(boolean keepInstance)
    {
        mKeepInstance = keepInstance;
        return this;
    }


    public YesNoDialog setIcon(int iconId)
    {
        mIconId = iconId;
        return this;
    }


    public YesNoDialog setTitle(int titleId)
    {
        mTitleId = titleId;
        return this;
    }


    public YesNoDialog setTitle(CharSequence titleText)
    {
        mTitleText = titleText;
        return this;
    }


    public YesNoDialog setMessage(int messageId)
    {
        mMessageId = messageId;
        return this;
    }


    public YesNoDialog setMessage(CharSequence messageText)
    {
        mMessageText = messageText;
        return this;
    }


    public YesNoDialog setPositiveText(int positiveTextId)
    {
        mPositiveTextId = positiveTextId;
        return this;
    }


    public YesNoDialog setPositiveText(CharSequence positiveText)
    {
        mPositiveText = positiveText;
        return this;
    }


    public YesNoDialog setNegativeText(int negativeTextId)
    {
        mNegativeTextId = negativeTextId;
        return this;
    }


    public YesNoDialog setNegativeText(CharSequence negativeText)
    {
        mNegativeText = negativeText;
        return this;
    }


    public YesNoDialog setOnPositiveClickedListener(OnPositiveClickedListener onPositiveClickedListener)
    {
        mOnPositiveClickedListener = onPositiveClickedListener;
        return this;
    }


    public YesNoDialog setOnNegativeClickedListener(OnNegativeClickedListener onNegativeClickedListener)
    {
        mOnNegativeClickedListener = onNegativeClickedListener;
        return this;
    }


    public YesNoDialog setOnCancelListener(OnCancelListener onCancelListener)
    {
        mOnCancelListener = onCancelListener;
        return this;
    }


    public YesNoDialog setOnDismissListener(OnDismissListener onDismissListener)
    {
        mOnDismissListener = onDismissListener;
        return this;
    }


    public interface OnPositiveClickedListener
    {
        void onPositiveClicked();
    }


    public interface OnNegativeClickedListener
    {
        void onNegativeClicked();
    }


    public interface OnCancelListener
    {
        void onCancel();
    }


    public interface OnDismissListener
    {
        void onDismiss();
    }
}
