/*
 * Copyright (C) 2016-2025 crDroid Android Project
 * Copyright (C) 2024-2025 The Clover Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.settings.custom.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.ViewCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.custom.R;
import com.android.settingslib.widget.SliderPreference;

import com.google.android.material.slider.LabelFormatter;
import com.google.android.material.slider.Slider;

public class CustomSeekBarPreference extends SliderPreference {

    private static final String TAG = "CustomSeekBarPreference";
    private static final String SETTINGS_NS = "http://schemas.android.com/apk/res/com.android.settings";
    private static final String ANDROIDNS = "http://schemas.android.com/apk/res/android";

    private boolean mShowSign;
    @Nullable
    private String mUnits = "";
    @Nullable
    private String mDefaultValueText;
    private boolean mDefaultValueTextExists;
    private boolean mDefaultValueExists;
    private int mDefaultValue;

    private CharSequence mUserSummary;  

    private boolean mInUserDrag = false;
    private boolean mShowButtons = true;

    public CustomSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        readLegacyAttrs(context, attrs);
        initDefaults();
        mUserSummary = super.getSummary();
        updateSummaryNow();
    }

    public CustomSeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        readLegacyAttrs(context, attrs);
        initDefaults();
        mUserSummary = super.getSummary();
        updateSummaryNow();
    }

    public CustomSeekBarPreference(Context context) {
        super(context, null);
        initDefaults();
        mUserSummary = super.getSummary();
        updateSummaryNow();
    }

    private void initDefaults() {
        setShowSliderValue(true);
        setHapticFeedbackMode(HAPTIC_FEEDBACK_MODE_ON_TICKS);
        setLabelFormater(new LabelFormatter() {
            @Override public String getFormattedValue(float value) {
                return formatValueForSummary((int) value);
            }
        });
    }

    private void readLegacyAttrs(Context c, AttributeSet attrs) {
        if (attrs == null) return;
        final TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.CustomSeekBarPreference);
        try {
            mShowSign = a.getBoolean(R.styleable.CustomSeekBarPreference_showSign, false);
            final String units = a.getString(R.styleable.CustomSeekBarPreference_units);
            if (units != null) mUnits = units;

            final boolean continuous = a.getBoolean(
                    R.styleable.CustomSeekBarPreference_continuousUpdates, false);
            setUpdatesContinuously(continuous);

            mShowButtons = a.getBoolean(R.styleable.CustomSeekBarPreference_showButtons, true);

            mDefaultValueText = a.getString(
                    R.styleable.CustomSeekBarPreference_defaultValueText);
            mDefaultValueTextExists = mDefaultValueText != null && !mDefaultValueText.isEmpty();

            String defaultValue = attrs.getAttributeValue(ANDROIDNS, "defaultValue");
            if (defaultValue == null) {
                defaultValue = attrs.getAttributeValue(SETTINGS_NS, "defaultValue");
            }
            if (defaultValue != null && !defaultValue.isEmpty()) {
                try {
                    mDefaultValue = Integer.parseInt(defaultValue);
                    mDefaultValueExists = true;
                } catch (NumberFormatException ignored) {
                    mDefaultValueExists = false;
                }
            }

            int interval = attrs.getAttributeIntValue(SETTINGS_NS, "interval", 0);
            if (interval == 0) {
                interval = attrs.getAttributeIntValue(ANDROIDNS, "interval", 0);
            }
            if (interval > 0) {
                setSliderIncrement(interval);
            }

            // Guard against improper slider increment
            int min = getMin();
            int max = getMax();
            int span = Math.max(0, max - min);

            int step = getSliderIncrement();
            if (step <= 0 || span == 0) {
                setSliderIncrement(1);
            } else if ((span % step) != 0) {
                int commonDivisor = gcd(span, step);
                setSliderIncrement(Math.max(1, commonDivisor));
                Log.w(TAG, "Adjusted interval to " + getSliderIncrement() + " to perfectly divide range " + span);
            }
            
            // Initial snap for safety
            setValue(getRoundedValue(getValue()));
            
        } catch (Throwable ignored) {
            // keep safe defaults
        } finally {
            a.recycle();
        }
    }

    @Override
    public void setSummary(CharSequence summary) {
        mUserSummary = summary;
        updateSummaryNow();
    }

    @Override
    public void setValue(int sliderValue) {
        // Enforce snapping to interval
        int roundedValue = getRoundedValue(sliderValue);
        super.setValue(roundedValue);
        if (!mInUserDrag) updateSummaryNow();
    }

    private void updateSummaryNow() {
        CharSequence composed = composeSummary(mUserSummary, getValue());
        super.setSummary(composed);
    }

    private String formatValueForSummary(int v) {
        if (mDefaultValueExists && mDefaultValueTextExists && v == mDefaultValue) {
            return mDefaultValueText;
        }
        String s = String.valueOf(v);
        if (mShowSign && v > 0) s = "+" + s;
        if (mUnits != null && !mUnits.isEmpty()) s = s + " " + mUnits;
        return s;
    }

    private CharSequence composeSummary(CharSequence userSummary, int v) {
        final String valueText = formatValueForSummary(v);
        if (userSummary == null || userSummary.length() == 0) return valueText;
        return valueText + " \u2022 " + userSummary;
    }

    @Override
    public void setDefaultValue(Object defaultValue) {
        if (defaultValue instanceof Integer) {
            mDefaultValueExists = true;
            mDefaultValue = (Integer) defaultValue;
        }
        super.setDefaultValue(defaultValue);
        updateSummaryNow();
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        final TextView summaryView = (TextView) holder.findViewById(android.R.id.summary);
        if (summaryView != null) {
            summaryView.setText(composeSummary(mUserSummary, getValue()));
        }

        final View labelFrame = holder.findViewById(
                com.android.settingslib.widget.preference.slider.R.id.label_frame);
        final TextView startText = (TextView) holder.findViewById(android.R.id.text1);
        final TextView endText = (TextView) holder.findViewById(android.R.id.text2);

        if (labelFrame != null) {
            boolean hasStart = startText != null && startText.getText() != null
                    && startText.getText().length() > 0;
            boolean hasEnd = endText != null && endText.getText() != null
                    && endText.getText().length() > 0;
            boolean parentWantsLabels = hasStart || hasEnd;

            labelFrame.setVisibility((parentWantsLabels || mDefaultValueExists) ? View.VISIBLE : View.GONE);
        }

        if (endText != null) {
            attachResetIcon(endText);
        }

        ViewGroup minusFrame = (ViewGroup) holder.findViewById(
                com.android.settingslib.widget.preference.slider.R.id.icon_start_frame);
        ImageView minusIcon = (ImageView) holder.findViewById(
                com.android.settingslib.widget.preference.slider.R.id.icon_start);

        ViewGroup plusFrame = (ViewGroup) holder.findViewById(
                com.android.settingslib.widget.preference.slider.R.id.icon_end_frame);
        ImageView plusIcon = (ImageView) holder.findViewById(
                com.android.settingslib.widget.preference.slider.R.id.icon_end);

        final Slider slider = (Slider) holder.findViewById(
                com.android.settingslib.widget.preference.slider.R.id.slider);

        int stepForClicks = Math.max(1, getSliderIncrement());

        if (minusFrame != null && minusIcon != null) {
            if (mShowButtons) {
                minusFrame.setVisibility(View.VISIBLE);
            } else {
                minusFrame.setVisibility(View.GONE);
            }
            minusIcon.setImageResource(R.drawable.ic_custom_seekbar_minus);
            minusFrame.setOnClickListener(v -> {
                if (!isEnabled()) return;
                int base = slider != null ? Math.round(slider.getValue()) : getValue();
                int newVal = Math.max(getMin(), base - stepForClicks);
                applyUserValue(newVal, slider);
                updatePlusMinusEnabledStates(holder);
            });
        }

        if (plusFrame != null && plusIcon != null) {
            if (mShowButtons) {
                plusFrame.setVisibility(View.VISIBLE);
            } else {
                plusFrame.setVisibility(View.GONE);
            }
            plusIcon.setImageResource(R.drawable.ic_custom_seekbar_plus);
            plusFrame.setOnClickListener(v -> {
                if (!isEnabled()) return;
                int base = slider != null ? Math.round(slider.getValue()) : getValue();
                int newVal = Math.min(getMax(), base + stepForClicks);
                applyUserValue(newVal, slider);
                updatePlusMinusEnabledStates(holder);
            });
        }

        updatePlusMinusEnabledStates(holder);

        if (slider != null && summaryView != null) {
            // Isolation Fix: Clear old listeners
            slider.clearOnChangeListeners();
            slider.clearOnSliderTouchListeners();

            slider.addOnChangeListener((s, value, fromUser) -> {
                if (fromUser) {
                    summaryView.setText(composeSummary(mUserSummary, (int) value));
                    updatePlusMinusEnabledStates(holder);
                }
            });
            slider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
                @Override
                public void onStartTrackingTouch(@NonNull Slider s) {
                    mInUserDrag = true;
                }

                @Override
                public void onStopTrackingTouch(@NonNull Slider s) {
                    mInUserDrag = false;
                    applyUserValue(Math.round(s.getValue()), s);
                    updatePlusMinusEnabledStates(holder);
                }
            });
        }
    }

    @Override
    public void onDependencyChanged(@NonNull Preference dependency, boolean disableDependent) {
        super.onDependencyChanged(dependency, disableDependent);
        notifyChanged();
    }

    private void applyUserValue(int newVal, @Nullable Slider slider) {
        newVal = getRoundedValue(newVal);
        if (newVal == getValue()) return;
        if (!callChangeListener(newVal)) {
            if (slider != null) slider.setValue(getValue());
            return;
        }
        setValue(newVal);
        updateSummaryNow();
        notifyChanged();
    }

    private static int gcd(int a, int b) {
        a = Math.abs(a); b = Math.abs(b);
        if (a == 0) return b;
        if (b == 0) return a;
        while (b != 0) {
            int t = b; b = a % b; a = t;
        }
        return a;
    }

    private int getRoundedValue(int value) {
        int interval = getSliderIncrement();
        if (interval <= 1) {
            return value;
        }
        int relativeValue = value - getMin();
        int remainder = relativeValue % interval;
        if (remainder == 0) {
            return value;
        }
        if (remainder < (float) interval / 2) {
            return value - remainder;
        } else {
            return value + (interval - remainder);
        }
    }

    private void updatePlusMinusEnabledStates(PreferenceViewHolder holder) {
        View minusFrame = holder.findViewById(
                com.android.settingslib.widget.preference.slider.R.id.icon_start_frame);
        ImageView minusIcon = (ImageView) holder.findViewById(
                com.android.settingslib.widget.preference.slider.R.id.icon_start);
        View plusFrame = holder.findViewById(
                com.android.settingslib.widget.preference.slider.R.id.icon_end_frame);
        ImageView plusIcon = (ImageView) holder.findViewById(
                com.android.settingslib.widget.preference.slider.R.id.icon_end);
        boolean enabled = isEnabled();
        int value = getValue();

        if (minusFrame != null && minusIcon != null) {
            int min = getMin();
            minusFrame.setEnabled(enabled && (value > min));
            minusIcon.setEnabled(enabled && (value > min));
        }
        if (plusFrame  != null && plusIcon != null) {
            int max = getMax();
            plusFrame.setEnabled(enabled && (value < max));
            plusIcon.setEnabled(enabled && (value < max));
        }
    }

    private void attachResetIcon(TextView tv) {
        if (!mDefaultValueExists) {
            tv.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, null);
            tv.setOnTouchListener(null);
            tv.setClickable(false);
            return;
        }

        final Drawable icon = ResourcesCompat.getDrawable(
                tv.getResources(), R.drawable.ic_custom_seekbar_reset, tv.getContext().getTheme());
        if (icon == null) return;

        tv.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, icon, null);
        tv.setCompoundDrawablePadding(dp(tv, 6));
        tv.setClickable(isEnabled());
        tv.setFocusable(isEnabled());

        final int tapSlop = dp(tv, 8);

        tv.setOnTouchListener((v, ev) -> {
            if (!isEnabled() || ev.getAction() != MotionEvent.ACTION_UP) return false;

            final boolean isRtl = ViewCompat.getLayoutDirection(tv) == ViewCompat.LAYOUT_DIRECTION_RTL;
            final Drawable[] drs = tv.getCompoundDrawablesRelative();
            final Drawable end = drs[2];
            if (end == null) return false;

            final int iconW = end.getIntrinsicWidth();
            final int x = (int) ev.getX();

            if (!isRtl) {
                final int left = tv.getWidth() - ViewCompat.getPaddingEnd(tv) - iconW - tapSlop;
                if (x >= left) { performReset(); return true; }
            } else {
                final int right = ViewCompat.getPaddingStart(tv) + iconW + tapSlop;
                if (x <= right) { performReset(); return true; }
            }
            return false;
        });
    }

    private void performReset() {
        if (mDefaultValueExists) {
            applyUserValue(mDefaultValue, null);
        }
    }

    private static int dp(TextView v, int dp) {
        return Math.round(dp * v.getResources().getDisplayMetrics().density);
    }

    // Compatibility methods
    public void refresh(int newValue) {
        setValue(newValue);
    }
}
