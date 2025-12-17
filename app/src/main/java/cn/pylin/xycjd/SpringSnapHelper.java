package cn.pylin.xycjd;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.FloatValueHolder;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

/**
 * A SnapHelper that uses SpringAnimation for a physics-based snapping effect.
 */
public class SpringSnapHelper extends LinearSnapHelper {

    private RecyclerView mRecyclerView;
    private SpringAnimation mSpringAnimX;
    private SpringAnimation mSpringAnimY;
    
    // Configurable spring parameters
    private float mDampingRatio = SpringForce.DAMPING_RATIO_LOW_BOUNCY;
    private float mStiffness = SpringForce.STIFFNESS_LOW;

    private final RecyclerView.OnScrollListener mScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                snapToTargetExistingView();
            }
        }
    };

    @Override
    public void attachToRecyclerView(@Nullable RecyclerView recyclerView) throws IllegalStateException {
        // Call super to ensure initialization of mGravityScroller and other internal states
        super.attachToRecyclerView(recyclerView);
        
        if (mRecyclerView == recyclerView) {
            return;
        }
        
        // super.attachToRecyclerView sets the listener to 'this' (OnFlingListener)
        // and adds its own OnScrollListener.
        // We need to remove its OnScrollListener and add ours.
        
        mRecyclerView = recyclerView;
        
        if (mRecyclerView != null) {
            // Remove SnapHelper's scroll listener using reflection
            try {
                java.lang.reflect.Field scrollListenerField = androidx.recyclerview.widget.SnapHelper.class.getDeclaredField("mScrollListener");
                scrollListenerField.setAccessible(true);
                RecyclerView.OnScrollListener listener = (RecyclerView.OnScrollListener) scrollListenerField.get(this);
                if (listener != null) {
                    mRecyclerView.removeOnScrollListener(listener);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Add our own scroll listener
            mRecyclerView.addOnScrollListener(mScrollListener);
            
            // Initial snap (post to ensure layout is done)
            mRecyclerView.post(new Runnable() {
                @Override
                public void run() {
                    snapToTargetExistingView();
                }
            });
        }
    }

    @Override
    public boolean onFling(int velocityX, int velocityY) {
        RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
        if (layoutManager == null) {
            return false;
        }
        
        int minFlingVelocity = mRecyclerView.getMinFlingVelocity();
        boolean fling = Math.abs(velocityY) > minFlingVelocity || Math.abs(velocityX) > minFlingVelocity;
        
        if (fling) {
            // Reduce fling velocity to prevent scrolling too far/fast
            velocityX = (int) (velocityX * 0.8f);
            velocityY = (int) (velocityY * 0.6f);

            int targetPosition = findTargetSnapPosition(layoutManager, velocityX, velocityY);
            if (targetPosition != RecyclerView.NO_POSITION) {
                View targetView = layoutManager.findViewByPosition(targetPosition);
                if (targetView != null) {
                    // Target is visible, we can spring to it directly
                    int[] distances = calculateDistanceToFinalSnap(layoutManager, targetView);
                    if (distances != null) {
                        startSpringScroll(distances[0], distances[1]);
                        return true;
                    }
                } else {
                    // Target is off-screen. 
                    // We can either:
                    // 1. Let default fling happen (return false), then snap on IDLE.
                    // 2. Scroll to position using smoothScrollToPosition (standard), then spring?
                    // 3. Just let it be.
                    
                    // For a physics feel, letting it fling naturally and then snapping is often best
                    // unless we want to enforce "one page at a time" strictness.
                    // LinearSnapHelper is not strict paging.
                    
                    // However, if we return false, the default LinearSnapHelper logic (if we called super) would run.
                    // But we are NOT calling super methods that trigger scroll.
                    // So we must handle it.
                    
                    // Let's try to just smooth scroll to the position using RecyclerView's default, 
                    // which is decent for long distances.
                    mRecyclerView.smoothScrollToPosition(targetPosition);
                    return true;
                }
            }
        }
        
        return false;
    }

    private void snapToTargetExistingView() {
        if (mRecyclerView == null) {
            return;
        }
        RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
        if (layoutManager == null) {
            return;
        }
        View snapView = findSnapView(layoutManager);
        if (snapView == null) {
            return;
        }
        int[] calculateDistanceToFinalSnap = calculateDistanceToFinalSnap(layoutManager, snapView);
        if (calculateDistanceToFinalSnap != null) {
            int dx = calculateDistanceToFinalSnap[0];
            int dy = calculateDistanceToFinalSnap[1];
            if (dx != 0 || dy != 0) {
                startSpringScroll(dx, dy);
            }
        }
    }

    private int mScrolledX;
    private int mScrolledY;

    private void startSpringScroll(int dx, int dy) {
        if (dx != 0) {
            if (mSpringAnimX != null) {
                mSpringAnimX.cancel();
            }
            mScrolledX = 0;
            mSpringAnimX = new SpringAnimation(new FloatValueHolder(0));
            mSpringAnimX.setSpring(new SpringForce(0)
                    .setDampingRatio(mDampingRatio)
                    .setStiffness(mStiffness));
            mSpringAnimX.setMinimumVisibleChange(DynamicAnimation.MIN_VISIBLE_CHANGE_PIXELS);
            mSpringAnimX.addUpdateListener(new DynamicAnimation.OnAnimationUpdateListener() {
                @Override
                public void onAnimationUpdate(DynamicAnimation animation, float value, float velocity) {
                    int currentScroll = (int) value;
                    int delta = currentScroll - mScrolledX;
                    if (delta != 0) {
                        if (mRecyclerView != null) {
                            mRecyclerView.scrollBy(delta, 0);
                        }
                        mScrolledX += delta;
                    }
                }
            });
            mSpringAnimX.animateToFinalPosition(dx);
        }

        if (dy != 0) {
            if (mSpringAnimY != null) {
                mSpringAnimY.cancel();
            }
            mScrolledY = 0;
            mSpringAnimY = new SpringAnimation(new FloatValueHolder(0));
            mSpringAnimY.setSpring(new SpringForce(0)
                    .setDampingRatio(mDampingRatio)
                    .setStiffness(mStiffness));
            mSpringAnimY.setMinimumVisibleChange(DynamicAnimation.MIN_VISIBLE_CHANGE_PIXELS);
            mSpringAnimY.addUpdateListener(new DynamicAnimation.OnAnimationUpdateListener() {
                @Override
                public void onAnimationUpdate(DynamicAnimation animation, float value, float velocity) {
                    int currentScroll = (int) value;
                    int delta = currentScroll - mScrolledY;
                    if (delta != 0) {
                        if (mRecyclerView != null) {
                            mRecyclerView.scrollBy(0, delta);
                        }
                        mScrolledY += delta;
                    }
                }
            });
            mSpringAnimY.animateToFinalPosition(dy);
        }
    }
}
