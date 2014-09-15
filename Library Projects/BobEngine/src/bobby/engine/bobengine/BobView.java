package bobby.engine.bobengine;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;

/**
 * A BobEngine view that contains and renders Rooms with GameObjects. This class
 * should be extended to create custom BobViews that contain Rooms and Graphics.
 * 
 * @author Ben
 * 
 */
public abstract class BobView extends GLSurfaceView {

	// Constants
	private final int INIT_BASE_W = 720;                   // Initial base screen width for correction ratio
	private final int INIT_BASE_H = 1280;                  // Initial base screen height

	// Objects
	private Room currentRoom;
	private Activity myActivity;
	private Touch myTouch;
	private BobRenderer renderer;
	private GraphicsHelper graphicsHelper;

	// Variables
	private Point screen;                                  // The size of the screen in pixels.
	private Point base;                                    // The base screen dimensions to use for calculating correction ratios.
	private double ratioX;                                 // Screen width correction ratio for handling different sized screens
	private double ratioY;                                 // Screen height correction ratio
	private boolean created;                               // Flag that indicates this view has already been created.

	public BobView(Context context, AttributeSet attr) {
		super(context, attr);
		
		created = false;

		init(context);
	}

	public BobView(Context context) {
		super(context);

		created = false;
		
		init(context);
	}

	/**
	 * BobEngine BobView initialization.
	 * 
	 * @param context
	 */
	@SuppressWarnings("deprecation")
	@SuppressLint({ "ClickableViewAccessibility", "NewApi" })
	private void init(Context context) {
		myActivity = (Activity) context;
		
		graphicsHelper = new GraphicsHelper(context);

		renderer = new BobRenderer();
		setRenderer(renderer);

		setOnTouchListener(myTouch = new Touch(this));

		onCreateGraphics();

		this.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

			@Override
			public void onGlobalLayout() {
				if (!created) onCreateRooms();
				created = true;
			}

		});

		/* Screen Size and Correction Ratios */
		WindowManager wm;                                                                // For getting information about the display.
		int rot;                                                                         // The screen rotation

		wm = (WindowManager) myActivity.getSystemService(Context.WINDOW_SERVICE);

		screen = new Point();
		base = new Point();
		
		if (myActivity instanceof BobActivity) {                                         // Best to use a BobActivity but not required
			screen.x = ((BobActivity) myActivity).screenWidth;
			screen.y = ((BobActivity) myActivity).screenHeight;
		} else {                                                                         // Must account for cases where a BobActivity is not used.

			try {
				wm.getDefaultDisplay().getRealSize(screen);
			} catch (NoSuchMethodError e) {
				screen.x = wm.getDefaultDisplay().getWidth();
				screen.y = wm.getDefaultDisplay().getHeight();
			}
		}

		rot = wm.getDefaultDisplay().getRotation();

		if (rot == Surface.ROTATION_0 || rot == Surface.ROTATION_180) {                   // Portrait orientation
			base.x = INIT_BASE_W;
			base.y = INIT_BASE_H;
		} else {                                                                          // Landscape orientation
			base.x = INIT_BASE_H;
			base.y = INIT_BASE_W;
		}

		ratioX = (double) screen.x / (double) base.x;
		ratioY = (double) screen.y / (double) base.y;
	}
	
	/**
	 * Returns the current room.
	 */
	public Room getCurrentRoom() {
		return currentRoom;
	}
	
	/**
	 * Changes the current room to nextRoom.
	 * 
	 * @param nextRoom - the room to switch to.
	 */
	public void goToRoom(Room nextRoom) {
		currentRoom = nextRoom;
	}
	
	/**
	 * @return This BobView's GraphicsHelper
	 */
	public GraphicsHelper getGraphicsHelper() {
		return graphicsHelper;
	}

	/**
	 * <u><i>Use a BobRenderer instead.</u></i>
	 */
	@Override
	public void setRenderer(Renderer renderer) {
		Log.e("BobEngine", "BobRenderer not used!! Use a BobRenderer instead of a Renderer!");
		super.setRenderer(renderer);
	}

	/**
	 * Returns the Activity that contains this BobView.
	 */
	public Activity getMyActivity() {
		return myActivity;
	}

	/**
	 * Returns this BobView's Touch touch listener.
	 */
	public Touch getTouch() {
		return myTouch;
	}

	/**
	 * Sets this BobView's renderer and starts the rendering thread. BobViews
	 * should always use BobRenderers. When a BobView is initialized it automatically
	 * given a renderer.
	 * 
	 * @param renderer
	 *            - The BobRenderer for this view.
	 */
	private void setRenderer(BobRenderer renderer) {
		super.setRenderer(renderer);
		renderer.setOwner(this);
		this.renderer = renderer;
	}

	/**
	 * Gets the screen orientation.
	 * 
	 * @return Surface.ROTATION_0 for no rotation (portrait) <br />
	 *         Surface.ROTATION_90 for 90 degree rotation (landscape) <br />
	 *         Surface.ROTATION_180 for 180 degree rotation (reverse
	 *         portrait/upside down portrait) <br />
	 *         Surface.ROTATION_270 for 270 degree rotation (reverse landscape)
	 */
	public float getScreenRotation() {
		return myActivity.getWindowManager().getDefaultDisplay().getRotation();
	}

	/**
	 * Returns true if the oriention is portrait. (Including reverse portrait)
	 */
	public boolean isPortrait() {
		if (getScreenRotation() == Surface.ROTATION_0 || getScreenRotation() == Surface.ROTATION_180) {
			return true;
		}

		return false;
	}

	/**
	 * Returns true if the device orientation is landscape.
	 */
	public boolean isLandscape() {
		if (getScreenRotation() == Surface.ROTATION_90 || getScreenRotation() == Surface.ROTATION_270) {
			return true;
		}
		
		return false;
	}
	
	/**
	 * Returns the screen width correction ratio for dealing with different size screens.
	 * This ratio is based off of the initial orientation of the device when the BobView is initialized!
	 */
	public double getRatioX() {
		return ratioX;
	}
	
	/**
	 * Returns the screen height correction ratio for dealing with different size screens.
	 * This ratio is based off of the initial orientation of the device when the BobView is initialized!
	 */
	public double getRatioY() {
		return ratioY;
	}

	/**
	 * Sets the background color for the BobView.
	 * 
	 * @param red
	 *            - Red value from 0 to 1
	 * @param green
	 *            - Green value from 0 to 1
	 * @param blue
	 *            - Blue value from 0 to 1
	 * @param alpha
	 *            - Alpha value from 0 to 1
	 */
	public void setBackgroundColor(float red, float green, float blue, float alpha) {
		renderer.setBackgroundColor(red, green, blue, alpha);
	}
	
	protected abstract void onCreateGraphics();

	protected abstract void onCreateRooms();
}