package ir.hanzodev1375.components.effect;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLExt;
import android.opengl.GLES20;
import android.opengl.GLES31;
import android.opengl.GLUtils;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.Choreographer;
import android.view.TextureView;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import ir.hanzodev1375.components.R;
import ir.hanzodev1375.components.utils.AndroidUtils;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL10;

public class ThanosEffect extends TextureView {

  public static boolean supports() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
  }

  private DrawingThread drawThread;
  private final ArrayList<ToSet> toSet = new ArrayList<>();

  private static class ToSet {
    public final View view;
    public Runnable doneCallback;

    public ToSet(View view, Runnable callback) {
      this.view = view;
      this.doneCallback = callback;
    }
  }

  @Nullable private Runnable whenDone;

  public ThanosEffect(@NonNull Context context) {
    this(context, null);
  }

  public ThanosEffect(@NonNull Context context, @Nullable Runnable whenDoneCallback) {
    super(context);
    this.whenDone = whenDoneCallback;
    setOpaque(false);
    setSurfaceTextureListener(
        new SurfaceTextureListener() {
          @Override
          public void onSurfaceTextureAvailable(
              @NonNull SurfaceTexture surface, int width, int height) {
            if (drawThread != null) {
              drawThread.kill();
              drawThread = null;
            }
            drawThread =
                new DrawingThread(getContext(), surface, ThanosEffect.this::destroy, width, height);
            if (!toSet.isEmpty()) {
              for (int i = 0; i < toSet.size(); ++i) {
                ToSet set = toSet.get(i);
                drawThread.animate(set.view, set.doneCallback);
              }
              toSet.clear();
              Choreographer.getInstance().postFrameCallback(frameCallback);
            }
          }

          @Override
          public void onSurfaceTextureSizeChanged(
              @NonNull SurfaceTexture surface, int width, int height) {
            if (drawThread != null) {
              drawThread.resize(width, height);
            }
          }

          @Override
          public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
            if (drawThread != null) {
              drawThread.kill();
              drawThread = null;
            }
            if (whenDone != null) {
              Runnable runnable = whenDone;
              whenDone = null;
              ensureRunOnUIThread(runnable);
            }
            return false;
          }

          @Override
          public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {}
        });
  }

  public boolean destroyed;

  private void destroy() {
    if (whenDone != null) {
      destroyed = true;
      Runnable runnable = whenDone;
      whenDone = null;
      ensureRunOnUIThread(runnable);
    }
  }

  public void kill() {
    if (destroyed) {
      return;
    }
    destroyed = true;
    for (ToSet set : toSet) {
      if (set.doneCallback != null) {
        ensureRunOnUIThread(set.doneCallback);
        set.doneCallback = null;
      }
    }
    toSet.clear();
    if (drawThread != null) {
      drawThread.kill();
    }
    if (whenDone != null) {
      Runnable runnable = whenDone;
      whenDone = null;
      ensureRunOnUIThread(runnable);
    }
  }

  private final Choreographer.FrameCallback frameCallback =
      new Choreographer.FrameCallback() {
        @Override
        public void doFrame(long frameTimeNanos) {
          if (drawThread != null) {
            drawThread.requestDraw();
            if (drawThread.running) {
              Choreographer.getInstance().postFrameCallback(this);
            }
          }
        }
      };

  public void animate(View view, @Nullable Runnable whenDone) {
    if (view == null) {
      ensureRunOnUIThread(whenDone);
      return;
    }
    if (drawThread != null && drawThread.isThreadAlive()) {
      drawThread.animate(view, whenDone);
      Choreographer.getInstance().postFrameCallback(frameCallback);
    } else if (drawThread == null) {
      toSet.add(new ToSet(view, whenDone));
    } else {
      view.setVisibility(View.GONE);
      ensureRunOnUIThread(whenDone);
    }
  }

  public void cancel() {
    if (drawThread != null) {
      drawThread.kill();
      drawThread = null;
    }
  }

  public static void ensureRunOnUIThread(Runnable runnable) {
    if (runnable == null) return;
    if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
      new Handler(Looper.getMainLooper()).post(runnable);
    } else {
      runnable.run();
    }
  }

  private static class DrawingThread extends HandlerThread implements Handler.Callback {

    private final AtomicBoolean alive = new AtomicBoolean(true);
    @Nullable private final SurfaceTexture surfaceTexture;
    @Nullable private Runnable destroy;
    private final float density;
    @NonNull private final Context appContext;
    private int width, height;
    private Handler glHandler;
    private boolean initialized = false;

    public DrawingThread(
        @NonNull Context context,
        @NonNull SurfaceTexture surfaceTexture,
        Runnable destroy,
        int width,
        int height) {
      super("ThanosEffect.DrawingThread");
      this.appContext = context;
      this.surfaceTexture = surfaceTexture;
      this.destroy = destroy;
      this.density = context.getResources().getDisplayMetrics().density;
      this.width = width;
      this.height = height;
      start();
    }

    public boolean isThreadAlive() {
      return alive.get();
    }

    public static final int DO_DRAW = 0;
    public static final int DO_RESIZE = 1;
    public static final int DO_KILL = 2;
    public static final int DO_ADD_ANIMATION = 3;

    @Override
    public boolean handleMessage(Message msg) {
      switch (msg.what) {
        case DO_DRAW:
          draw();
          return true;
        case DO_RESIZE:
          resizeInternal(msg.arg1, msg.arg2);
          draw();
          return true;
        case DO_KILL:
          killInternal();
          return true;
        case DO_ADD_ANIMATION:
          addAnimationInternal((Animation) msg.obj);
          return true;
      }
      return false;
    }

    @Override
    protected void onLooperPrepared() {
      super.onLooperPrepared();
      glHandler = new Handler(Looper.myLooper(), this);
      if (!alive.get()) {
        Looper.myLooper().quitSafely();
        return;
      }
      try {
        init();
        initialized = true;
      } catch (Throwable t) {
        killInternal();
        return;
      }
      if (!toAddAnimations.isEmpty()) {
        for (int i = 0; i < toAddAnimations.size(); ++i) {
          addAnimationInternal(toAddAnimations.get(i));
        }
        toAddAnimations.clear();
      }
    }

    public void requestDraw() {
      if (glHandler != null && alive.get() && initialized) {
        glHandler.sendMessage(glHandler.obtainMessage(DO_DRAW));
      }
    }

    public void resize(int width, int height) {
      if (glHandler != null && alive.get()) {
        glHandler.sendMessage(glHandler.obtainMessage(DO_RESIZE, width, height));
      }
    }

    private void resizeInternal(int width, int height) {
      if (!alive.get()) return;
      this.width = width;
      this.height = height;
      GLES31.glViewport(0, 0, width, height);
      GLES31.glUniform2f(sizeHandle, width, height);
    }

    public void kill() {
      if (!alive.get()) return;
      if (glHandler != null) {
        glHandler.sendMessage(glHandler.obtainMessage(DO_KILL));
      } else {
        alive.set(false);
        if (destroy != null) {
          Runnable d = destroy;
          destroy = null;
          ensureRunOnUIThread(d);
        }
      }
    }

    private void killInternal() {
      if (!alive.get()) return;
      alive.set(false);
      for (int i = 0; i < pendingAnimations.size(); ++i) {
        pendingAnimations.get(i).done(true);
      }
      pendingAnimations.clear();
      if (drawProgram != 0) {
        GLES31.glDeleteProgram(drawProgram);
        drawProgram = 0;
      }
      if (surfaceTexture != null) {
        try {
          surfaceTexture.release();
        } catch (Throwable ignored) {
        }
      }
      if (destroy != null) {
        Runnable d = destroy;
        destroy = null;
        ensureRunOnUIThread(d);
      }
      Looper looper = Looper.myLooper();
      if (looper != null) {
        looper.quitSafely();
      }
    }

    private EGL10 egl;
    private EGLDisplay eglDisplay;
    private EGLConfig eglConfig;
    private EGLSurface eglSurface;
    private EGLContext eglContext;

    private int drawProgram;

    private int matrixHandle;
    private int resetHandle;
    private int timeHandle;
    private int deltaTimeHandle;
    private int particlesCountHandle;
    private int sizeHandle;
    private int gridSizeHandle;
    private int rectSizeHandle;
    private int seedHandle;
    private int textureHandle;
    private int densityHandle;
    private int longevityHandle;
    private int offsetHandle;
    private int scaleHandle;
    private int uvOffsetHandle;

    public volatile boolean running;
    private final ArrayList<Animation> pendingAnimations = new ArrayList<>();
    private final ArrayList<Animation> toAddAnimations = new ArrayList<>();

    private void init() {
      egl = (EGL10) EGLContext.getEGL();
      eglDisplay = egl.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
      if (eglDisplay == egl.EGL_NO_DISPLAY) {
        throw new RuntimeException("eglDisplay == EGL_NO_DISPLAY");
      }
      int[] version = new int[2];
      if (!egl.eglInitialize(eglDisplay, version)) {
        throw new RuntimeException("failed eglInitialize");
      }

      int[] configAttributes = {
        EGL14.EGL_RED_SIZE, 8,
        EGL14.EGL_GREEN_SIZE, 8,
        EGL14.EGL_BLUE_SIZE, 8,
        EGL14.EGL_ALPHA_SIZE, 8,
        EGL14.EGL_RENDERABLE_TYPE, EGLExt.EGL_OPENGL_ES3_BIT_KHR,
        EGL14.EGL_NONE
      };
      EGLConfig[] eglConfigs = new EGLConfig[1];
      int[] numConfigs = new int[1];
      if (!egl.eglChooseConfig(eglDisplay, configAttributes, eglConfigs, 1, numConfigs)) {
        throw new RuntimeException("failed eglChooseConfig");
      }
      eglConfig = eglConfigs[0];
      if (eglConfig == null) {
        throw new RuntimeException("eglConfig == null");
      }

      int[] contextAttributes = {EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE};
      eglContext =
          egl.eglCreateContext(eglDisplay, eglConfig, egl.EGL_NO_CONTEXT, contextAttributes);
      if (eglContext == null) {
        throw new RuntimeException("eglContext == null");
      }

      eglSurface = egl.eglCreateWindowSurface(eglDisplay, eglConfig, surfaceTexture, null);
      if (eglSurface == null) {
        throw new RuntimeException("eglSurface == null");
      }

      if (!egl.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
        throw new RuntimeException("failed eglMakeCurrent");
      }

      int vertexShader = GLES31.glCreateShader(GLES31.GL_VERTEX_SHADER);
      int fragmentShader = GLES31.glCreateShader(GLES31.GL_FRAGMENT_SHADER);
      if (vertexShader == 0 || fragmentShader == 0) {
        throw new RuntimeException("vertexShader == 0 || fragmentShader == 0");
      }
      String vertexSrc = AndroidUtils.readRes(R.raw.thanos_vertex, appContext);
      String fragmentSrc = AndroidUtils.readRes(R.raw.thanos_fragment, appContext);
      if (vertexSrc == null || fragmentSrc == null) {
        throw new RuntimeException("cannot read thanos shaders");
      }

      GLES31.glShaderSource(vertexShader, vertexSrc);
      GLES31.glCompileShader(vertexShader);
      int[] status = new int[1];
      GLES31.glGetShaderiv(vertexShader, GLES31.GL_COMPILE_STATUS, status, 0);
      if (status[0] != GLES31.GL_TRUE) {
        throw new RuntimeException(
            "compile vertex shader error: " + GLES31.glGetShaderInfoLog(vertexShader));
      }
      GLES31.glShaderSource(fragmentShader, fragmentSrc);
      GLES31.glCompileShader(fragmentShader);
      GLES31.glGetShaderiv(fragmentShader, GLES31.GL_COMPILE_STATUS, status, 0);
      if (status[0] != GLES31.GL_TRUE) {
        throw new RuntimeException(
            "compile fragment shader error: " + GLES31.glGetShaderInfoLog(fragmentShader));
      }
      drawProgram = GLES31.glCreateProgram();
      if (drawProgram == 0) {
        throw new RuntimeException("drawProgram == 0");
      }
      GLES31.glAttachShader(drawProgram, vertexShader);
      GLES31.glAttachShader(drawProgram, fragmentShader);

      String[] feedbackVaryings = {"outUV", "outPosition", "outVelocity", "outTime"};
      GLES31.glTransformFeedbackVaryings(
          drawProgram, feedbackVaryings, GLES31.GL_INTERLEAVED_ATTRIBS);
      GLES31.glLinkProgram(drawProgram);
      GLES31.glGetProgramiv(drawProgram, GLES31.GL_LINK_STATUS, status, 0);
      if (status[0] != GLES31.GL_TRUE) {
        throw new RuntimeException(
            "link program error: " + GLES31.glGetProgramInfoLog(drawProgram));
      }

      matrixHandle = GLES31.glGetUniformLocation(drawProgram, "matrix");
      rectSizeHandle = GLES31.glGetUniformLocation(drawProgram, "rectSize");
      resetHandle = GLES31.glGetUniformLocation(drawProgram, "reset");
      timeHandle = GLES31.glGetUniformLocation(drawProgram, "time");
      deltaTimeHandle = GLES31.glGetUniformLocation(drawProgram, "deltaTime");
      particlesCountHandle = GLES31.glGetUniformLocation(drawProgram, "particlesCount");
      sizeHandle = GLES31.glGetUniformLocation(drawProgram, "size");
      gridSizeHandle = GLES31.glGetUniformLocation(drawProgram, "gridSize");
      textureHandle = GLES31.glGetUniformLocation(drawProgram, "tex");
      seedHandle = GLES31.glGetUniformLocation(drawProgram, "seed");
      densityHandle = GLES31.glGetUniformLocation(drawProgram, "dp");
      longevityHandle = GLES31.glGetUniformLocation(drawProgram, "longevity");
      offsetHandle = GLES31.glGetUniformLocation(drawProgram, "offset");
      scaleHandle = GLES31.glGetUniformLocation(drawProgram, "scale");
      uvOffsetHandle = GLES31.glGetUniformLocation(drawProgram, "uvOffset");

      GLES31.glViewport(0, 0, width, height);
      GLES31.glDisable(GLES31.GL_BLEND);
      GLES31.glClearColor(0f, 0f, 0f, 0f);
      GLES31.glUseProgram(drawProgram);
      GLES31.glUniform2f(sizeHandle, width, height);
    }

    private boolean drawnAnimations = false;
    private final ArrayList<Animation> toRunStartCallback = new ArrayList<>();

    private void draw() {
      if (!alive.get()) return;

      GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT);

      for (int i = 0; i < pendingAnimations.size(); ++i) {
        Animation animation = pendingAnimations.get(i);
        if (animation.firstDraw) {
          animation.calcParticlesGrid();
          if (animation.startCallback != null) {
            toRunStartCallback.add(animation);
          }
        }
        drawnAnimations = true;
        animation.draw();
        if (animation.isDead()) {
          animation.done(true);
          pendingAnimations.remove(i);
          running = !pendingAnimations.isEmpty();
          i--;
        }
      }

      checkGlErrors();

      try {
        egl.eglSwapBuffers(eglDisplay, eglSurface);
      } catch (Throwable t) {
        for (int i = 0; i < pendingAnimations.size(); ++i) {
          pendingAnimations.get(i).done(true);
        }
        pendingAnimations.clear();
        killInternal();
        return;
      }

      for (int i = 0; i < toRunStartCallback.size(); ++i) {
        Animation animation = toRunStartCallback.get(i);
        if (animation.startCallback != null) {
          ensureRunOnUIThread(animation.startCallback);
        }
      }
      toRunStartCallback.clear();

      // Overlay تانوس یک لایه‌ی سراسری و همیشگیه که برای حذف‌های بعدی هم استفاده می‌شه؛ برای همین
      // بعد از اتمام انیمیشن thread رو نمیکشیم (قبلاً kill می‌شد و چون surface برنگشتی بود، حذف بعدی
      // هیچ‌وقت فراخوانی callback نمی‌شد -> `dispatchRemoveFinished` اجرا نمی‌شد -> کل لیست هنگ می‌کرد).
    }

    public void animate(View view, @Nullable Runnable whenDone) {
      if (!alive.get()) {
        if (view != null) {
          view.setVisibility(View.GONE);
        }
        if (whenDone != null) {
          ensureRunOnUIThread(whenDone);
        }
        if (destroy != null) {
          Runnable d = destroy;
          destroy = null;
          ensureRunOnUIThread(d);
        }
        return;
      }
      Animation animation = new Animation(view, whenDone);
      running = true;
      if (glHandler != null) {
        glHandler.sendMessage(glHandler.obtainMessage(DO_ADD_ANIMATION, animation));
      } else {
        toAddAnimations.add(animation);
      }
    }

    private void addAnimationInternal(Animation animation) {
      GLES31.glGenTextures(1, animation.texture, 0);
      GLES20.glBindTexture(GL10.GL_TEXTURE_2D, animation.texture[0]);
      GLES20.glTexParameteri(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
      GLES20.glTexParameteri(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
      GLES20.glTexParameteri(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
      GLES20.glTexParameteri(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
      GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, animation.bitmap, 0);
      GLES20.glBindTexture(GL10.GL_TEXTURE_2D, 0);

      animation.bitmap.recycle();
      animation.bitmap = null;

      pendingAnimations.add(animation);
      running = true;
      animation.ready = true;
    }

    private class Animation {

      public final ArrayList<View> views = new ArrayList<>();
      private long lastDrawTime = -1;
      public float time = 0;
      public boolean firstDraw = true;
      @Nullable public Runnable doneCallback;
      @Nullable public Runnable startCallback;
      public volatile boolean ready;

      public float offsetLeft = 0, offsetTop = 0;
      public float left = 0;
      public float top = 0;
      public float longevity = 1.5f;
      public float timeScale = 1.15f;

      public final float[] glMatrixValues = new float[9];
      public final float[] matrixValues = new float[9];
      public final Matrix matrix = new Matrix();

      public long particlesCount;
      public int viewWidth, viewHeight;
      public int gridWidth, gridHeight;
      public float gridSize;

      public final float seed = (float) (Math.random() * 2.);
      public int currentBuffer;
      public final int[] texture = new int[1];
      public final int[] buffer = new int[2];

      @Nullable private Bitmap bitmap;

      public Animation(View view, @Nullable Runnable whenDone) {
        this.views.add(view);
        viewWidth = Math.max(1, view.getWidth());
        viewHeight = Math.max(1, view.getHeight());
        top = view.getY();
        left = view.getX();
        doneCallback = whenDone;
        startCallback =
            () -> {
              for (int j = 0; j < views.size(); ++j) {
                views.get(j).setVisibility(View.GONE);
              }
            };
        long vw = viewWidth;
        long vh = viewHeight;
        long maxParticles = 120_000;
        long needed = vw * vh;
        particlesCount = (int) Math.max(100, Math.min(maxParticles, needed));

        bitmap = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        int saveCount = canvas.save();
        view.draw(canvas);
        try {
          canvas.restoreToCount(saveCount);
        } catch (Throwable ignored) {
        }
      }

      public void calcParticlesGrid() {
        final float aspectRatio = (float) viewWidth / viewHeight;
        gridHeight = (int) Math.round(Math.sqrt(particlesCount / aspectRatio));
        gridWidth = (int) Math.round((float) particlesCount / gridHeight);
        while (gridWidth * gridHeight < particlesCount) {
          if ((float) gridWidth / gridHeight < aspectRatio) {
            gridWidth++;
          } else {
            gridHeight++;
          }
        }
        particlesCount = gridWidth * gridHeight;
        gridSize = Math.max((float) viewWidth / gridWidth, (float) viewHeight / gridHeight);

        GLES31.glGenBuffers(2, buffer, 0);
        for (int i = 0; i < 2; ++i) {
          var d = (int) particlesCount;
          GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER, buffer[i]);
          GLES31.glBufferData(
              GLES31.GL_ARRAY_BUFFER, (int) (d * 28L), null, GLES31.GL_DYNAMIC_DRAW);
        }
        GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER, 0);
      }

      public void draw() {
        final long now = System.nanoTime();
        final double dt = lastDrawTime < 0 ? 0 : (now - lastDrawTime) / 1_000_000_000.;
        lastDrawTime = now;

        matrix.reset();
        matrix.postScale(viewWidth, viewHeight);
        matrix.postTranslate(left, top);
        matrix.getValues(matrixValues);
        glMatrixValues[0] = matrixValues[0];
        glMatrixValues[1] = matrixValues[3];
        glMatrixValues[2] = matrixValues[6];
        glMatrixValues[3] = matrixValues[1];
        glMatrixValues[4] = matrixValues[4];
        glMatrixValues[5] = matrixValues[7];
        glMatrixValues[6] = matrixValues[2];
        glMatrixValues[7] = matrixValues[5];
        glMatrixValues[8] = matrixValues[8];

        time += dt * timeScale;

        GLES31.glUniformMatrix3fv(matrixHandle, 1, false, glMatrixValues, 0);
        GLES31.glUniform1f(resetHandle, firstDraw ? 1f : 0f);
        GLES31.glUniform1f(timeHandle, time);
        GLES31.glUniform1f(deltaTimeHandle, (float) dt * timeScale);
        GLES31.glUniform1f(particlesCountHandle, particlesCount);
        GLES31.glUniform3f(gridSizeHandle, gridWidth, gridHeight, gridSize);
        GLES31.glUniform2f(offsetHandle, offsetLeft, offsetTop);
        GLES31.glUniform1f(scaleHandle, 1f);
        GLES31.glUniform1f(uvOffsetHandle, 0.6f);

        GLES31.glUniform2f(rectSizeHandle, viewWidth, viewHeight);
        GLES31.glUniform1f(seedHandle, seed);
        GLES31.glUniform1f(densityHandle, density);
        GLES31.glUniform1f(longevityHandle, longevity);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0]);
        GLES31.glUniform1i(textureHandle, 0);

        GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER, buffer[currentBuffer]);
        GLES31.glVertexAttribPointer(0, 2, GLES31.GL_FLOAT, false, 28, 0);
        GLES31.glEnableVertexAttribArray(0);
        GLES31.glVertexAttribPointer(1, 2, GLES31.GL_FLOAT, false, 28, 8);
        GLES31.glEnableVertexAttribArray(1);
        GLES31.glVertexAttribPointer(2, 2, GLES31.GL_FLOAT, false, 28, 16);
        GLES31.glEnableVertexAttribArray(2);
        GLES31.glVertexAttribPointer(3, 1, GLES31.GL_FLOAT, false, 28, 24);
        GLES31.glEnableVertexAttribArray(3);
        GLES31.glBindBufferBase(GLES31.GL_TRANSFORM_FEEDBACK_BUFFER, 0, buffer[1 - currentBuffer]);
        GLES31.glVertexAttribPointer(0, 2, GLES31.GL_FLOAT, false, 28, 0);
        GLES31.glEnableVertexAttribArray(0);
        GLES31.glVertexAttribPointer(1, 2, GLES31.GL_FLOAT, false, 28, 8);
        GLES31.glEnableVertexAttribArray(1);
        GLES31.glVertexAttribPointer(2, 2, GLES31.GL_FLOAT, false, 28, 16);
        GLES31.glEnableVertexAttribArray(2);
        GLES31.glVertexAttribPointer(3, 1, GLES31.GL_FLOAT, false, 28, 24);
        GLES31.glEnableVertexAttribArray(3);

        GLES31.glBeginTransformFeedback(GLES31.GL_POINTS);
        GLES31.glDrawArrays(GLES31.GL_POINTS, 0, (int) particlesCount);
        GLES31.glEndTransformFeedback();

        GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER, 0);
        GLES31.glBindBuffer(GLES31.GL_TRANSFORM_FEEDBACK_BUFFER, 0);

        firstDraw = false;
        currentBuffer = 1 - currentBuffer;
      }

      public boolean isDead() {
        return time > longevity + 0.9f;
      }

      public void done(boolean runCallback) {
        try {
          GLES31.glDeleteBuffers(2, buffer, 0);
        } catch (Throwable ignored) {
        }
        try {
          GLES31.glDeleteTextures(1, texture, 0);
        } catch (Throwable ignored) {
        }
        if (runCallback && doneCallback != null) {
          Runnable cb = doneCallback;
          doneCallback = null;
          ensureRunOnUIThread(cb);
        }
      }
    }

    private void checkGlErrors() {
      int err;
      while ((err = GLES31.glGetError()) != GLES31.GL_NO_ERROR) {}
    }
  }
}
