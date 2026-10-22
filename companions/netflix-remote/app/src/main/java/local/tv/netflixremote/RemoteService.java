package local.tv.netflixremote;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.graphics.*;
import android.os.*;
import android.util.Log;
import android.view.*;
import android.view.accessibility.*;
import java.util.List;

public final class RemoteService extends AccessibilityService {
 private static final String TARGET="com.netflix.mediaclient", TAG="NetflixRemote";
 private final Handler handler=new Handler(Looper.getMainLooper());
 private WindowManager wm; private Cursor cursor; private WindowManager.LayoutParams params;
 private boolean netflix, mouse=true, attached, injecting; private int width,height,dx,dy,ticks;
 private float x,y;
 private final Runnable mover=new Runnable(){public void run(){
  if(!netflix || !mouse || (dx==0 && dy==0))return;
  int speed=Math.min(24,5+ticks++/3);
  x=Math.max(12,Math.min(width-12,x+dx*speed)); y=Math.max(12,Math.min(height-12,y+dy*speed));
  cursor.invalidate(); handler.postDelayed(this,24);
 }};
 @Override protected void onServiceConnected(){
  wm=(WindowManager)getSystemService(Context.WINDOW_SERVICE);
  android.util.DisplayMetrics m=new android.util.DisplayMetrics();wm.getDefaultDisplay().getRealMetrics(m);
  width=m.widthPixels;height=m.heightPixels;x=width/2f;y=height/2f;
  cursor=new Cursor(this);
  params=new WindowManager.LayoutParams(width,height,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
   WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
   PixelFormat.TRANSLUCENT);
  params.gravity=Gravity.TOP|Gravity.LEFT;
  refresh(); Log.i(TAG,"Service ready");
 }
 @Override public void onAccessibilityEvent(AccessibilityEvent event){refresh();}
 private void refresh(){
  if(wm==null)return;
  boolean found=false, isNetflix=false;
  for(AccessibilityWindowInfo w:getWindows()){
   if(w.getType()!=AccessibilityWindowInfo.TYPE_APPLICATION || !w.isActive())continue;
   AccessibilityNodeInfo r=w.getRoot();if(r==null)continue;
   CharSequence p=r.getPackageName();if(p!=null){found=true;isNetflix=TARGET.contentEquals(p);}r.recycle();
  }
  if(!found){AccessibilityNodeInfo r=getRootInActiveWindow();if(r!=null){
   CharSequence p=r.getPackageName();if(p!=null && TARGET.contentEquals(p)){found=true;isNetflix=true;}r.recycle();
  }}
  if(found && netflix!=isNetflix){netflix=isNetflix;stopMove();Log.i(TAG,netflix?"Netflix active":"Netflix inactive");}
  updateOverlay();
 }
 private void updateOverlay(){
  if(cursor==null)return;
  boolean want=netflix&&mouse&&!injecting;
  if(want&&!attached){try{wm.addView(cursor,params);attached=true;}catch(RuntimeException e){Log.e(TAG,"Overlay unavailable",e);}}
  if(!want&&attached){wm.removeViewImmediate(cursor);attached=false;}
 }
 private void stopMove(){dx=dy=ticks=0;handler.removeCallbacks(mover);}
 @Override protected boolean onKeyEvent(KeyEvent e){
  int key=e.getKeyCode();
  if(key==KeyEvent.KEYCODE_HOME || key==KeyEvent.KEYCODE_TV_INPUT){netflix=false;stopMove();updateOverlay();return false;}
  if(!netflix)return false;
  if(key==KeyEvent.KEYCODE_MENU){if(e.getAction()==KeyEvent.ACTION_UP){mouse=!mouse;stopMove();updateOverlay();Log.i(TAG,"Mouse enabled="+mouse);}return true;}
  if(!mouse)return false;
  if(key==KeyEvent.KEYCODE_DPAD_CENTER || key==KeyEvent.KEYCODE_ENTER){
   if(e.getAction()==KeyEvent.ACTION_UP)click();return true;
  }
  int nx=0,ny=0;
  switch(key){case KeyEvent.KEYCODE_DPAD_LEFT:nx=-1;break;case KeyEvent.KEYCODE_DPAD_RIGHT:nx=1;break;
   case KeyEvent.KEYCODE_DPAD_UP:ny=-1;break;case KeyEvent.KEYCODE_DPAD_DOWN:ny=1;break;default:return false;}
  if(e.getAction()==KeyEvent.ACTION_UP)stopMove();
  else if(dx!=nx || dy!=ny){stopMove();dx=nx;dy=ny;handler.post(mover);}
  return true;
 }
 private boolean clickNode(AccessibilityNodeInfo n,int px,int py){
  if(n==null)return false;
  Rect r=new Rect();n.getBoundsInScreen(r);if(!r.contains(px,py))return false;
  for(int i=n.getChildCount()-1;i>=0;i--){AccessibilityNodeInfo child=n.getChild(i);if(child==null)continue;
   boolean clicked=clickNode(child,px,py);child.recycle();if(clicked)return true;}
  if(n.isEnabled() && n.isClickable()){
   n.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
   boolean ok=n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
   if(ok)Log.i(TAG,"Node click accepted");return ok;
  }
  return false;
 }
 private void click(){
  if(injecting)return;
  stopMove();
  for(AccessibilityWindowInfo w:getWindows()){
   if(w.getType()==AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY)continue;
   AccessibilityNodeInfo root=w.getRoot();if(root==null)continue;
   boolean ok=clickNode(root,(int)x,(int)y);root.recycle();if(ok)return;
  }
  // Remove even the trusted overlay before a touch, for apps rejecting obscured input.
  injecting=true;
  if(attached){wm.removeViewImmediate(cursor);attached=false;}
  final float clickX=x,clickY=y;
  handler.postDelayed(()->{
   refresh();
   if(!netflix || !mouse){injecting=false;updateOverlay();return;}
   Path path=new Path();path.moveTo(clickX,clickY);
   GestureDescription g=new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription(path,0,90)).build();
   boolean accepted=dispatchGesture(g,new GestureResultCallback(){
    @Override public void onCompleted(GestureDescription g){Log.i(TAG,"Touch completed");injecting=false;updateOverlay();}
    @Override public void onCancelled(GestureDescription g){Log.i(TAG,"Touch cancelled");injecting=false;updateOverlay();}
   },handler);
   if(!accepted){Log.i(TAG,"Touch rejected");injecting=false;updateOverlay();}
  },70);
 }
 @Override public void onInterrupt(){stopMove();}
 @Override public void onDestroy(){handler.removeCallbacksAndMessages(null);if(attached){wm.removeViewImmediate(cursor);attached=false;}super.onDestroy();}
 private final class Cursor extends View{
  private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
  Cursor(Context c){super(c);}
  @Override protected void onDraw(Canvas c){
   p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(5);p.setColor(Color.BLACK);c.drawCircle(x,y,13,p);
   p.setStrokeWidth(2.5f);p.setColor(Color.WHITE);c.drawCircle(x,y,13,p);
   c.drawLine(x-20,y,x-6,y,p);c.drawLine(x+6,y,x+20,y,p);c.drawLine(x,y-20,x,y-6,p);c.drawLine(x,y+6,x,y+20,p);
  }
 }
}
