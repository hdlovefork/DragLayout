package com.dean.dragview;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * Created by Administrator on 2016/3/1.
 */
public class DragLayout extends FrameLayout {

    private static final String TAG = "DragViewLayout";
    private ViewDragHelper mViewDragHelper;
    private ViewGroup mMenu;
    private ViewGroup mMain;
    //菜单面板打开后主面板是否禁止触摸事件
    private boolean mMainPanelDisableEventAfterMenuOpen = true;
    private ViewGroup mOldMain;
    /*菜单打开状态*/

    /**
     * 获取菜单面板状态
     *
     * @return
     */
    public Status getStatus() {
        return mStatus;
    }

    private Status mStatus = Status.Close;

    /**
     * 水平方向拖拽面板可移动的最大像素距离
     */
    private int mDragHorizontalRange;

    public DragLayout(Context context) {
        this(context, null);
    }

    public DragLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mViewDragHelper = ViewDragHelper.create(this, new DragCallback());
    }

    /**
     * 指示当前菜单面板的状态
     */
    public enum Status {
        Close, Open, Draging
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (mMenu == null || mMain == null) {
            if (getChildCount() != 2) {
                throw new IllegalStateException("有且仅有两个子View");
            }
            if (!(getChildAt(0) instanceof ViewGroup && getChildAt(1) instanceof ViewGroup)) {
                throw new IllegalStateException("子View必须是ViewGroup");
            }
            mMenu = (ViewGroup) getChildAt(0);
            mMain = (ViewGroup) getChildAt(1);
            mDragHorizontalRange = (int) (w * 0.6f);
            decorInterceptLayout();
        }
    }

    //根据mMainPanelDisableEventAfterMenuOpen判断是否拦截主面板事件
    //mMainPanelDisableEventAfterMenuOpen为true时添加一个InterceptLayout布局（装饰模式）
    private void decorInterceptLayout() {
        if (mMainPanelDisableEventAfterMenuOpen && mMain != null && !(mMain instanceof InterceptLayout)) {
            mOldMain = mMain;
            removeView(mMain);
            InterceptLayout interceptLayout = new InterceptLayout(getContext());
            interceptLayout.addView(mMain);
            mMain = interceptLayout;
            addView(interceptLayout);
        }
    }


    class DragCallback extends ViewDragHelper.Callback {
        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return true;
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            int newLeft = child.getLeft();//默认不改变孩子的位置
            if (dx > 100) {//手指向右滑动距离，这里是为了降低向右滑动的灵敏度，防止用户轻易的拉出左侧菜单面板
                newLeft = fixLeft(child, left);
            }
            return newLeft;
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            int newLeft = fixLeft(mMain, mMain.getLeft() + dx);
            if (changedView == mMenu) {
                //菜单不允许移动，但监听到移动消息后需要改变主界面位置
                mMenu.layout(0, 0, mMenu.getWidth(), mMenu.getHeight());
                mMain.layout(newLeft, 0, newLeft + mMain.getWidth(), mMain.getHeight());
            }
            float percent = newLeft * 1.0f / mDragHorizontalRange;
            //改变主面板透明度
            mMain.setAlpha(1 - percent + 0.3f);
            //处理事件响应
            dispatchEvent(percent);
            //动画效果
            anim(percent);
        }

        private void anim(float percent) {
            //菜单面板从0.5-1之间缩放
            mMenu.setScaleX(evaluate(percent, 0.5f, 1));
            mMenu.setScaleY(evaluate(percent, 0.5f, 1));
            //菜单面板负宽度的一半到0之间平移
            mMenu.setTranslationX(evaluate(percent, -(mMenu.getWidth() >> 1), 0));
            mMenu.setAlpha(evaluate(percent, 0.5f, 1));
            //主面板在1-0.8之间做缩放动画
            mMain.setScaleX(evaluate(percent, 1, 0.8f));
            mMain.setScaleY(evaluate(percent, 1, 0.8f));
            //在背景上蒙上一层能改变透明颜色的纸
            int color = (int) evaluateColor(percent, Color.BLACK, Color.TRANSPARENT);
            getBackground().setColorFilter(color, PorterDuff.Mode.SRC_OVER);
        }

        /**
         * 获取一个值，该值按fraction比例计算出来（公式：return start + (end - start) * fraction ）
         */
        private float evaluate(float fraction, float start, float end) {
            return start + (end - start) * fraction;
        }

        /**
         * 获取一个颜色值，该颜色值的每个分量通过fraction按比例计算出来的
         */
        public Object evaluateColor(float fraction, Object startValue, Object endValue) {
            int startInt = (Integer) startValue;
            int startA = (startInt >>> 24);
            int startR = (startInt >> 16) & 0xff;
            int startG = (startInt >> 8) & 0xff;
            int startB = startInt & 0xff;

            int endInt = (Integer) endValue;
            int endA = (endInt >>> 24);
            int endR = (endInt >> 16) & 0xff;
            int endG = (endInt >> 8) & 0xff;
            int endB = endInt & 0xff;

            return ((startA + (int) (fraction * (endA - startA))) << 24) |
                    ((startR + (int) (fraction * (endR - startR))) << 16) |
                    ((startG + (int) (fraction * (endG - startG))) << 8) |
                    (startB + (int) (fraction * (endB - startB)));
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            return (int) (child.getWidth() * 0.6);
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            if (xvel > 200) {
                open();
            } else if (xvel < -100) {
                close();
            } else {
                if (mMain.getLeft() < mDragHorizontalRange / 2) {
                    close();
                } else {
                    open();
                }
            }
        }
    }

    /**
     * 菜单面板打开时主面板是否禁用触摸事件
     *
     * @param disable
     */
    public void setDisableEventAfterMenuClose(boolean disable) {
        mMainPanelDisableEventAfterMenuOpen = disable;
        decorInterceptLayout();
    }

    public void setOnStatusChangedListener(OnStatusChangedListener onStatusChangedListener) {
        mOnStatusChangedListener = onStatusChangedListener;
    }

    private OnStatusChangedListener mOnStatusChangedListener;

    public interface OnStatusChangedListener {
        void onOpened(View view);

        void onClosed(View view);

        void onStatusChanging(View view, float percent);
    }

    private void dispatchEvent(float percent) {
        Status prevStatus = mStatus;
        mStatus = updateStatus(percent);
        //触发拖拽事件
        if (mOnStatusChangedListener != null) {
            mOnStatusChangedListener.onStatusChanging(this, percent);
        }
        //触发开关事件
        if (prevStatus != mStatus && mOnStatusChangedListener != null) {
            if (mStatus == Status.Close) {
                mOnStatusChangedListener.onClosed(this);
            } else if (mStatus == Status.Open) {
                mOnStatusChangedListener.onOpened(this);
            }
        }
    }

    private Status updateStatus(float percent) {
        if (percent == 0) {
            return Status.Close;
        } else if (percent == 1) {
            return Status.Open;
        } else {
            return Status.Draging;
        }
    }

    public void close() {
        close(true);
    }

    public void close(boolean smooth) {
        changeStatusTo(smooth, Status.Close);
    }

    public void open() {
        open(true);
    }

    public void open(boolean smooth) {
        changeStatusTo(smooth, Status.Open);
    }

    public void togglePanel() {
        if (mStatus == Status.Open) {
            close();
        } else if (mStatus == Status.Close) {
            open();
        }
    }

    //开关菜单面板实际上就是调用ViewDragHelper.smoothSlideViewTo方法实现
    public void changeStatusTo(boolean smooth, Status targetStatus) {
        if (targetStatus == Status.Open && mMain.getLeft() == mDragHorizontalRange ||
                targetStatus == Status.Close && mMain.getLeft() == 0) {
            return;
        }
        //主面板目标左边距
        int finalLeft = targetStatus == Status.Open ? mDragHorizontalRange : 0;
        if (smooth) {//平滑移动
            if (mViewDragHelper.smoothSlideViewTo(mMain, finalLeft, 0)) {
                invalidate();
            }
        } else {//直接绘制到目标状态的位置
            mMain.layout(mDragHorizontalRange, 0, finalLeft + mMain.getWidth(), mMain
                    .getHeight());
        }
    }

    @Override
    public void computeScroll() {
        if (mViewDragHelper.continueSettling(true)) {
            invalidate();
        }
    }

    /**
     * 得到面板有效的左边距，主面板的左边距是屏幕宽度的60%，而菜单面板的左边距永远固定在0，0的位置
     *
     * @param child 待修正左边距的面板
     * @param left  希望移动到的左边距
     * @return 有效的左边距
     */
    private int fixLeft(View child, int left) {
        if (child == mMain) {//
            left = left < 0 ? 0 : left;
            left = left > mDragHorizontalRange ? mDragHorizontalRange : left;
        }
        return left;
    }

    //下面2个方法是使用ViewDragHelper必须调用的方法，将事件交给它去处理，它将引发ViewDragHelper.Callback回调方法
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mViewDragHelper.shouldInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mViewDragHelper.processTouchEvent(event);
        return true;
    }

    /**
     * 用于拦截主面板触摸事件的内部类（装饰模式）
     */
    class InterceptLayout extends FrameLayout {

        public InterceptLayout(Context context) {
            super(context);
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            //菜单是关闭状态交给原布局处理
            if (mStatus == Status.Close) {
                return mOldMain != null ? mOldMain.onInterceptTouchEvent(ev) : super
                        .onInterceptTouchEvent(ev);
            } else {
                return true;//菜单是打开状态拦截触摸事件
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            //菜单是关闭状态交给原布局处理
            if (mStatus == Status.Close) {
                return mOldMain != null ? mOldMain.onTouchEvent(event) : super
                        .onTouchEvent(event);
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                togglePanel();//关闭菜单
            }
            return true;
        }
    }
}
