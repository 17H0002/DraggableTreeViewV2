package com.allyants.draggabletreeview;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import java.util.ArrayList;

/**
 * Created by jbonk on 6/16/2017.
 */

public class DraggableTreeView extends FrameLayout{

    ScrollView mRootLayout;
    LinearLayout mParentLayout;
    private TreeViewAdapter adapter;

    private BitmapDrawable mHoverCell;
    private Rect mHoverCellCurrentBounds;
    private Rect mHoverCellOriginalBounds;

    private TreeNode mobileNode;
    private int sideMargin = 20;

    private int mDownY = -1;
    private int mDownX = -1;
    private int mLastEventX = -1;
    private int mLastEventY = -1;
    private ArrayList<TreeNode> nodeOrder = new ArrayList<>();

    private View mobileView;
    private boolean mCellIsMobile = false;

    public DraggableTreeView(Context context) {
        super(context);
    }

    public DraggableTreeView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DraggableTreeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setAdapter(TreeViewAdapter adapter){
        this.adapter = adapter;
        this.adapter.setDraggableTreeView(this);
        adapter.setTreeViews();
        notifyDataSetChanged();
    }

    public void notifyDataSetChanged(){
        if(adapter != null) {
            mParentLayout.removeAllViews();
            inflateViews(adapter.root);
        }
    }

    private void inflateViews(TreeNode node){
        if(!node.isRoot()) {
            createTreeItem(node.getView(),node);
        }else{
            ((ViewGroup) node.getView()).removeAllViews();
            mParentLayout.addView((LinearLayout)node.getView());
        }
        for (int i = 0; i < node.getChildren().size(); i++) {
            inflateViews(node.getChildren().get(i));
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mRootLayout = new ScrollView(getContext());
        mParentLayout = new LinearLayout(getContext());
        mParentLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        mRootLayout.addView(mParentLayout);
        addView(mRootLayout);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        boolean colValue = handleItemDragEvent(event);
        return colValue || super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean colValue = handleItemDragEvent(event);
        return colValue || super.onTouchEvent(event);
    }

    public boolean handleItemDragEvent(MotionEvent event){
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mDownX = (int)event.getRawX();
                mDownY = (int)event.getRawY();
                break;
            case MotionEvent.ACTION_MOVE:
                if(mDownY == -1){
                    mDownY = (int)event.getRawY();

                }
                if(mDownX == -1){
                    mDownX = (int)event.getRawX();
                }

                mLastEventX = (int) event.getRawX();
                mLastEventY = (int) event.getRawY();
                int deltaX = mLastEventX - mDownX;
                int deltaY = mLastEventY - mDownY;

                if (mCellIsMobile) {
                    int location[] = new int[2];
                    mobileView.getLocationOnScreen(location);
                    int root_location[] = new int[2];
                    mRootLayout.getLocationOnScreen(root_location);
                    int offsetX = deltaX-root_location[0];
                    int offsetY = location[1]+deltaY-root_location[1];
                    mHoverCellCurrentBounds.offsetTo(offsetX,
                            offsetY);
                    mHoverCell.setBounds(rotatedBounds(mHoverCellCurrentBounds,0.0523599f));
                    invalidate();
                    handleItemDrag();
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
                touchEventsCancelled();
                break;
            case MotionEvent.ACTION_CANCEL:
                touchEventsCancelled();
                break;
            case MotionEvent.ACTION_POINTER_UP:
                break;
            default:
                break;
        }
        return false;
    }

    private void handleItemDrag(){
        int position = ((LinearLayout)adapter.root.getView()).indexOfChild(((View)mobileNode.getView().getParent()));

        LinearLayout layout = ((LinearLayout)adapter.root.getView());
        for(int i =0; i< layout.getChildCount(); i++)
        {
            View view = layout.getChildAt(i);

            int[] location = new int[2];
            view.getLocationInWindow(location);
            Rect viewRect = new Rect(location[0], location[1], location[0]+view.getWidth(), location[1]+view.getHeight());
            Rect outRect = new Rect(0, location[1], Resources.getSystem().getDisplayMetrics().widthPixels, location[1]+view.getHeight());

            if(outRect.contains(mLastEventX, mLastEventY))
            {
                //set last position
                Log.e("e", String.valueOf(i));
                if(viewRect.contains(mLastEventX,mLastEventY)) {
                    Log.e("ee",(String)nodeOrder.get(i).getData());
                }
            }
        }
    }

    private void touchEventsCancelled() {
        if(mCellIsMobile && mobileNode != null){
            mobileView.setVisibility(VISIBLE);
            mHoverCell = null;
            if(adapter != null) {
                mParentLayout.removeAllViews();
                inflateViews(adapter.root);
            }
            invalidate();
        }

        mDownX = -1;
        mDownY = -1;
        mCellIsMobile = false;
    }


    public void createTreeItem(View view, final TreeNode node){
        if(view != null) {
            nodeOrder.add(node);
            final LinearLayout mItem = new LinearLayout(getContext());
            mItem.setOrientation(LinearLayout.VERTICAL);
            if(view.getParent() != null) {
                ViewGroup parent = (ViewGroup) view.getParent();
                parent.removeView(view);
            }
            view.setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    mobileNode = node;
                    addToView(mItem,node);
                    mobileView = mItem;
                    mItem.post(new Runnable() {
                        @Override
                        public void run() {
                            mCellIsMobile = true;
                            mHoverCell = getAndAddHoverView(mobileView,1f);
                            mobileView.setVisibility(INVISIBLE);
                        }
                    });

                    return false;
                }
            });
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.setMargins(dpToPx(sideMargin*node.getLevel() ), 0, 0, 0);
            mItem.setLayoutParams(layoutParams);
            mItem.addView(view);
            ((LinearLayout)adapter.root.getView()).addView(mItem);
        }
    }

    private void addToView(LinearLayout linearLayout,TreeNode node){
        for(int i = 0;i < node.getChildren().size();i++) {
            View child = node.getChildren().get(i).getView();
            if(child.getParent().getParent() != null) {
                ((ViewGroup)child.getParent().getParent()).removeView((View) child.getParent());
            }
            linearLayout.addView(((View)child.getParent()));
            addToView(linearLayout,node.getChildren().get(i));
        }
    }

    public int dpToPx(int dp){
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if(mHoverCell != null){
            mHoverCell.draw(canvas);
        }
    }

    private BitmapDrawable getAndAddHoverView(View v, float scale){
        int w = v.getWidth();
        int h = v.getHeight();
        int top = v.getTop();
        int left = v.getLeft();

        Bitmap b = getBitmapWithBorder(v,scale);
        BitmapDrawable drawable = new BitmapDrawable(getResources(),b);
        mHoverCellOriginalBounds = new Rect(left,top,left+w,top+h);
        mHoverCellCurrentBounds = new Rect(mHoverCellOriginalBounds);
        drawable.setBounds(mHoverCellCurrentBounds);
        return drawable;
    }

    private Bitmap getBitmapWithBorder(View v, float scale) {
        Bitmap bitmap = getBitmapFromView(v,0);
        Bitmap b = getBitmapFromView(v,1);
        Canvas can = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setAlpha(150);
        can.scale(scale,scale,mDownX,mDownY);
        can.rotate(3);
        can.drawBitmap(b,0,0,paint);
        return bitmap;
    }

    private Bitmap getBitmapFromView(View v, float scale){
        double radians = 0.0523599f;
        double s = Math.abs(Math.sin(radians));
        double c = Math.abs(Math.cos(radians));
        int width = (int)(v.getHeight()*s + v.getWidth()*c);
        int height = (int)(v.getWidth()*s + v.getHeight()*c);
        Bitmap bitmap = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.scale(scale,scale);
        v.draw(canvas);
        return bitmap;
    }

    private Rect rotatedBounds(Rect tmp,double radians){
        double s = Math.abs(Math.sin(radians));
        double c = Math.abs(Math.cos(radians));
        int width = (int)(tmp.height()*s + tmp.width()*c);
        int height = (int)(tmp.width()*s + tmp.height()*c);

        return new Rect(tmp.left,tmp.top,tmp.left+width,tmp.top+height);
    }

}