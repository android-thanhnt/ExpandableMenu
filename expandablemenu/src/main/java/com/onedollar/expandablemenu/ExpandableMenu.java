package com.onedollar.expandablemenu;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;

public class ExpandableMenu extends ViewGroup implements View.OnClickListener {

    /**
     * Default values for XML attributes.
     */
    private static final int DEFAULT_MENU_BUTTON_COLLAPSE_ICON_RES = R.drawable.ic_add_white_24dp;
    private static final int DEFAULT_MENU_BUTTON_EXPAND_ICON_RES = R.drawable.ic_clear_white_24dp;
    private static final int DEFAULT_MENU_BUTTON_BACKGROUND_RES = R.color.default_menu_button_background_color;
    private static final int DEFAULT_MENU_BUTTON_SIZE = 24; //dp

    private static final boolean DEF_COLLAPSE_ON_ITEM_SELECTED = true;
    private static final int DEF_EXPAND_DURATION = 400;
    private static final int DEF_COLLAPSE_DURATION = 400;
    private static final int DEF_EXPAND_ITEM_ROTATION_DEG = 360;
    private static final int DEF_MENU_BTN_ROTATION_DEG = 360;
    private static final int DEF_SPACING_MENU_ITEMS_DP = 8;
    private static final int DEF_DELAY_EXPAND_MENU_ITEM = 100;
    /**
     * Base height adjustment for overshoot interpolator in dp.
     */
    private static final int BASE_HEIGHT_ADJ_FOR_OVERSHOOT_DP = 12;

    /**
     * Width adjustment for overshoot interpolator in dp.
     */
    private static final int WIDTH_ADJ_FOR_OVERSHOOT_DP = 8;

    private static final int DURATION_SHOW_HIDE = 500;

    /**
     * A click listener for menu items.
     */
    private OnExpandableMenuItemClickListener mItemClickListener;
    /**
     * Is this menu expanded?
     */
    private boolean mIsExpanded;
    /**
     * The main menu button, using which the user can cause the menu expand or collapse.
     */
    private ImageView mMenuButton;
    /**
     * A count for keeping track of how many children ( menu items ) were animated
     * as a result of the menu being expanded or collapsed.
     */
    private int mCountAnimatedChildren;
    /**
     * Total children ( menu items ) in this menu.
     */
    private int mChildCount;
    /**
     * A resource id for the {@link #mMenuButton} image.
     */
    private int mMenuButtonExpandIconRes;
    private int mMenuButtonCollapseIconRes;
    private int mMenuButtonBackgroundRes;
    private int mMenuButtonSize;
    /**
     * The size of {@link #mMenuButton}.
     * Available XML attributes are "normal" = {@link FloatingActionButton#SIZE_NORMAL}
     * and "mini" = {@link FloatingActionButton#SIZE_MINI}
     */
    private int mSizeMenuButton;
    /**
     * Whether or not the menu will collapse on menu item selection.
     */
    private boolean mCollapseOnItemSelected;
    /**
     * Animation duration of an expanding menu-item.
     */
    private int mDurationExpand;
    /**
     * Animation duration of a collapsing menu-item.
     */
    private int mDurationCollapse;
    /**
     * Degrees of rotation for expanding menu items.
     */
    private int mExpandItemRotationDeg;
    /**
     * Degrees of rotation of the {@link #mMenuButton}
     */
    private int mExpandMenuBtnRotationDeg;
    /**
     * The spacing between menu items in pixels.
     * {@link #mMenuButton} is not considered a menu item.
     */
    private int mSpacingMenuItemsPx;
    /**
     * Delay between expanding menu items.
     */
    private int mDelayExpandingMenuItems;
    /**
     * Is this menu being currently displayed either expanded or collapsed.
     */
    private boolean mIsVisible = true;
    /**
     * Interpolator for expanding animation.
     */
    private OvershootInterpolator mExpandInterpolator;
    /**
     * Interpolator for collapsing animation.
     */
    private AnticipateOvershootInterpolator mCollapseInterpolator;

    public ExpandableMenu(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        mExpandInterpolator = new OvershootInterpolator();
        mCollapseInterpolator = new AnticipateOvershootInterpolator();

        initAttrs(attrs);

        mIsExpanded = false;

        mMenuButton = new ImageView(getContext());
        mMenuButton.setImageResource(mMenuButtonCollapseIconRes);
        mMenuButton.setBackgroundResource(mMenuButtonBackgroundRes);
        mMenuButton.setOnClickListener(v -> toggle());
        addView(mMenuButton, mMenuButtonSize, mMenuButtonSize);
    }

    private void initAttrs(AttributeSet attrs) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ExpandableMenu, 0, 0);
        try {
            mMenuButtonCollapseIconRes = a.getResourceId(R.styleable.ExpandableMenu_menu_button_collapse_icon_res, DEFAULT_MENU_BUTTON_COLLAPSE_ICON_RES);
            mMenuButtonExpandIconRes = a.getResourceId(R.styleable.ExpandableMenu_menu_button_expand_icon_res, DEFAULT_MENU_BUTTON_EXPAND_ICON_RES);
            mMenuButtonBackgroundRes = a.getResourceId(R.styleable.ExpandableMenu_menu_button_background_res, DEFAULT_MENU_BUTTON_BACKGROUND_RES);
            mMenuButtonSize = a.getDimensionPixelSize(R.styleable.ExpandableMenu_menu_button_size, DensityUtils.dp2px(getContext(), DEFAULT_MENU_BUTTON_SIZE));

            mDurationExpand = a.getInt(R.styleable.ExpandableMenu_expand_duration, DEF_EXPAND_DURATION);
            mDurationCollapse = a.getInt(R.styleable.ExpandableMenu_collapse_duration, DEF_COLLAPSE_DURATION);
            mExpandItemRotationDeg = a.getInt(R.styleable.ExpandableMenu_menu_item_rotation_degree,
                    DEF_EXPAND_ITEM_ROTATION_DEG);
            mExpandMenuBtnRotationDeg = a.getInt(R.styleable.ExpandableMenu_menu_button_expand_rotation_degree,
                    DEF_MENU_BTN_ROTATION_DEG);
            mSpacingMenuItemsPx = a.getDimensionPixelSize(R.styleable.ExpandableMenu_spacing_menu_item,
                    DensityUtils.dp2px(getContext(), DEF_SPACING_MENU_ITEMS_DP));
            mDelayExpandingMenuItems = a.getInt(R.styleable.ExpandableMenu_delay_expanding_menu_item,
                    DEF_DELAY_EXPAND_MENU_ITEM);

            mCollapseOnItemSelected = a.getBoolean(R.styleable.ExpandableMenu_collapse_on_item_selected, DEF_COLLAPSE_ON_ITEM_SELECTED);

        } finally {
            a.recycle();
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mChildCount = getChildCount();

        setChildrenClickListener();
    }

    void toggle() {
        setMenuItemsClickable(false);
        if (mIsExpanded) {
            collapse();
        } else {
            expand();
        }
    }

    public void forceCollapse() {
        if (mIsExpanded) {
            toggle();
        }
    }

    public void setExpandResIcon(int resId) {
        mMenuButtonExpandIconRes = resId;
        mMenuButton.setImageResource(mMenuButtonExpandIconRes);
    }

    public void setCollapseResIcon(int resId) {
        mMenuButtonCollapseIconRes = resId;
        mMenuButton.setImageResource(mMenuButtonCollapseIconRes);
    }

    private void expand() {
        for (int i = 0; i < mChildCount; ++i) {
            animateExpandChild(getChildAt(i), mDurationExpand + i * mDelayExpandingMenuItems);
        }
    }

    private void collapse() {
        for (int i = 0; i < mChildCount; ++i) {
//            animateCollapseChild(getChildAt(i), mDurationCollapse);
            animateCollapseChild(getChildAt(i), 0);
        }
    }

    public void show() {
        if (!mIsVisible) {
            for (int i = 0; i < mChildCount; ++i) {
                animateShowChild(getChildAt(i));
            }
            mIsVisible = true;
            setMenuItemsClickable(true);
        }
    }

    public void hide() {
        if (mIsVisible) {
            for (int i = 0; i < mChildCount; ++i) {
                animateHideChild(getChildAt(i));
            }
            mIsVisible = false;
            setMenuItemsClickable(false);
        }
    }

    private static void animateShowChild(View child) {
        child.animate()
                .scaleX(1)
                .scaleY(1)
                .alpha(1)
                .setDuration(DURATION_SHOW_HIDE);
    }

    private static void animateHideChild(View child) {
        child.animate()
                .scaleX(0)
                .scaleY(0)
                .alpha(0)
                .setDuration(DURATION_SHOW_HIDE);
    }

    private void animateExpandChild(View child, int duration) {

        child.setVisibility(VISIBLE);
        child.animate()
                .setDuration(duration)
                .setInterpolator(mExpandInterpolator)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {

                        /*Not including mMenuButton*/
                        if (++mCountAnimatedChildren == mChildCount) {
                            mCountAnimatedChildren = 0;
                            mIsExpanded = true;
                            /*Re-enable menu item clicks.*/
                            setMenuItemsClickable(true);
                        }

                        if (child == mMenuButton && mMenuButtonExpandIconRes > 0) {
                            mMenuButton.setImageResource(mMenuButtonExpandIconRes);
                        }
                    }
                })
                .translationY(child == mMenuButton ? getMenuTranslationYToCenter(child) : getTranslationYToCenter(child))
                .scaleX(1)
                .scaleY(1)
                .rotation(child == mMenuButton ? mExpandMenuBtnRotationDeg : mExpandItemRotationDeg);
    }

    private void animateCollapseChild(View child, int duration) {
        child.animate()
                .setDuration(duration)
                .setInterpolator(mCollapseInterpolator)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {

                        if (++mCountAnimatedChildren == mChildCount) {
                            /*Not including mMenuButton*/
                            for (int i = mChildCount - 1; i > 0; --i) {
                                getChildAt(i).setVisibility(INVISIBLE);
                            }
                            mCountAnimatedChildren = 0;
                            mIsExpanded = false;
                            /*Re-enable menu item clicks.*/
                            setMenuItemsClickable(true);
                        }

                        if (child == mMenuButton && mMenuButtonCollapseIconRes > 0) {
                            mMenuButton.setImageResource(mMenuButtonCollapseIconRes);
                        }
                    }
                })
                .scaleX(child == mMenuButton ? 1 : 0)
                .scaleY(child == mMenuButton ? 1 : 0)
                .translationY(child == mMenuButton ? getMenuTranslationYToCenter(child) : getTranslationYToCenter(child))
                .rotation(0);
    }

    private void setMenuItemsClickable(boolean enabled) {
        for (int i = 0; i < mChildCount; ++i) {
            View child = getChildAt(i);
            child.setClickable(enabled);
        }
    }

    private float getTranslationYToCenter(View child) {
        return 0;
    }

    private float getMenuTranslationYToCenter(View menu) {
        return 0;
    }


    private void setChildrenClickListener() {
        /*Not including mMenuButton - it has a separate View.OnClickListener*/
        for (int i = mChildCount - 1; i > 0; --i) {
            View child = getChildAt(i);
            child.setOnClickListener(this);
        }
    }

    public void setOnExpandableMenuItemClickListener(OnExpandableMenuItemClickListener listener) {
        mItemClickListener = listener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        setMeasuredDimensionAll(widthMeasureSpec, heightMeasureSpec);
    }

    private void setMeasuredDimensionAll(int widthMeasureSpec, int heightMeasureSpec) {
        int width = 0;
        int height = 0;

        /*Measure all children.*/
        for (int i = 0; i < mChildCount; ++i) {
            View child = getChildAt(i);

            /*Measure child only if it is visible.*/
            if (child != null && child.getVisibility() != GONE) {
                measureChildWithMargins(child, widthMeasureSpec, width, heightMeasureSpec, height);

                MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
                /*The widest child will determine the total width of this View.*/
                width = Math.max(width, child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin);
                height += child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;
                /*Spacing between menu items. mMenuButton is not considered a menu item.*/
                height += child == mMenuButton ? 0 : mSpacingMenuItemsPx;
            }
        }

        /*Adding total padding to width and height.*/
        width += getPaddingLeft() + getPaddingRight() + getWidthAdjustmentForOvershootPx(getContext());
//        height += getPaddingTop() + getPaddingBottom() + getHeightAdjustmentOvershootPx(getContext(), mChildCount);
        height += getPaddingTop() + getPaddingBottom() + DensityUtils.dp2px(getContext(), 16);

        setMeasuredDimension(
                resolveSize(width, widthMeasureSpec),
                resolveSize(height, heightMeasureSpec)
        );
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        layoutAll();

        /*This is a new size or position for this view*/
        if (changed) {
            setInitialViewsPositionAndState();
        }
    }

    private void layoutAll() {
        int x = getPaddingLeft() + getWidthAdjustmentForOvershootPx(getContext()) / 2;
//        int y = getPaddingTop() + getHeightAdjustmentOvershootPx(getContext(), mChildCount) / 2;
        int y = getPaddingTop();
        int l, t, r, b;

        /*Previous child height.*/
        int prevChildHeight = 0;

        for (int i = 0; i < mChildCount; ++i) {
            View child = getChildAt(i);

            if (child != null && child.getVisibility() != GONE) {
                MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();

                y += prevChildHeight;
                prevChildHeight = child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;

                int shiftCenterMenuButton = 0;
                int spacingMenuItems = child == mMenuButton ? 0 : i * mSpacingMenuItemsPx;

                /*Left*/
                l = x + lp.leftMargin + shiftCenterMenuButton;
                /*Top*/
                t = y + lp.topMargin + spacingMenuItems;
                /*Right*/
                r = l + child.getMeasuredWidth();
                /*Bottom*/
                b = t + child.getMeasuredHeight();

                child.layout(l, t, r, b);
            }
        }
    }

    private static int getWidthAdjustmentForOvershootPx(Context context) {
        return DensityUtils.dp2px(context, WIDTH_ADJ_FOR_OVERSHOOT_DP);
    }

    private static int getHeightAdjustmentOvershootPx(Context context, int childCount) {
        /*
         * Overshoot interpolator travel distance is proportional to the number of children,
         * therefore we multiply the *base* height adjustment for this view with the
         * number of menu items + mMenuButton.
         */
        return DensityUtils.dp2px(context, childCount * BASE_HEIGHT_ADJ_FOR_OVERSHOOT_DP);
    }

    private void setInitialViewsPositionAndState() {

        /*Not including mMenuButton*/
        for (int i = mChildCount - 1; i > 0; --i) {
            View child = getChildAt(i);

            child.setTranslationY(getTranslationYToCenter(child));
            child.setScaleX(0);
            child.setScaleY(0);
            child.setVisibility(INVISIBLE);
        }

        mMenuButton.setTranslationY(getMenuTranslationYToCenter(mMenuButton));
    }

    @Override
    public MarginLayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    @Override
    protected MarginLayoutParams generateLayoutParams(LayoutParams p) {
        return generateDefaultLayoutParams();
    }

    @Override
    protected MarginLayoutParams generateDefaultLayoutParams() {
        //noinspection ResourceType
        return new MarginLayoutParams(MarginLayoutParams.WRAP_CONTENT,
                MarginLayoutParams.WRAP_CONTENT);
    }

    @Override
    protected boolean checkLayoutParams(LayoutParams p) {
        return p instanceof MarginLayoutParams;
    }

    @Override
    public void onClick(View v) {
        if (mItemClickListener != null) {
            mItemClickListener.onExpandableMenuItemClick(v);
        }

        if (mCollapseOnItemSelected) {
            toggle();
        }
    }

    public interface OnExpandableMenuItemClickListener {

        void onExpandableMenuItemClick(View view);
    }

    public boolean isVisible() {
        return mIsVisible;
    }

    /**
     * A simple behavior to hide() or show() the menu depending on {@link AppBarLayout} scroll.
     */
    public static class ExpandableMenuBehavior extends CoordinatorLayout.Behavior<ExpandableMenu> {
        /**
         * The scroll limit of a {@link AppBarLayout} from which a ExpandableMenu should be hidden.
         */
        private int mScrollThreshold;

        public ExpandableMenuBehavior(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        @Override
        public boolean layoutDependsOn(CoordinatorLayout parent, ExpandableMenu child, View dependency) {
            if (dependency instanceof AppBarLayout) {
                mScrollThreshold = ((AppBarLayout) dependency).getTotalScrollRange() / 10;
                return true;
            }
            return false;
        }

        @Override
        public boolean onDependentViewChanged(CoordinatorLayout parent, ExpandableMenu child, View dependency) {
            AppBarLayout appBarLayout = (AppBarLayout) dependency;

            float yAbs = Math.abs(appBarLayout.getY());

            /*If AppBarLayout is fully pulled down.*/
            if (yAbs == 0) {
                child.show();
                return true;
            } else if (yAbs > mScrollThreshold) {
                child.hide();
                return true;
            }
            return false;
        }
    }
}
