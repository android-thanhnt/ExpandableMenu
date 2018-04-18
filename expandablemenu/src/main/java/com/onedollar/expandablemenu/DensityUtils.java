package com.onedollar.expandablemenu;

import android.content.Context;

final class DensityUtils {

    private DensityUtils() {
    }

    static int dp2px(Context context, float dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }
}
