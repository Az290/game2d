package com.example.game;

import android.content.Context;
import android.graphics.*;
import android.media.AudioManager;
import android.media.SoundPool;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;

import java.util.ArrayList;
import java.util.List;

public class GameView extends SurfaceView implements Runnable {
    private Thread gameThread;
    private boolean isPlaying;
    private SurfaceHolder holder;
    private Paint paint;

    private Bitmap background;
    private Sprite character;

    private float charX, charY;
    private float speedX = 0, speedY = 0;

    private int screenWidth = 0, screenHeight = 0;
    private boolean initialized = false;

    // Frame timing
    private static final long STEP_NS = 16_666_667L;
    private long prevNs, accNs;

    // Background scroll
    private float bgX = 0;
    private float baseBgSpeed = 2;

    private float dp(float v) { return v * getResources().getDisplayMetrics().density; }

    // Entities
    private List<Projectile> projectiles = new ArrayList<>();
    private List<Enemy> enemies = new ArrayList<>();
    private List<Explosion> explosions = new ArrayList<>();

    // Sound effect
    private SoundPool soundPool;
    private int shootSoundId;
    private int explosionSoundId;

    // BGM bằng ExoPlayer
    private ExoPlayer exoPlayer;

    // Icon âm thanh
    private Bitmap iconVolumeOn, iconVolumeOff;
    private Bitmap iconSfxOn, iconSfxOff;
    private RectF iconVolumeRect, iconSfxRect;
    private boolean isAllSoundOn = true; // trạng thái âm thanh tổng (left icon)
    private boolean isSfxOn = true; // trạng thái hiệu ứng (right icon)

    // Joystick
    private Joystick joystick;
    private int joystickPointerId = -1; // -1 = chưa có ngón nào điều khiển joystick


    // Attack button
    private RectF buttonAttack;
    private Paint buttonPaint, buttonPressedPaint, buttonBorderPaint;
    private Paint arrowPaint;

    public GameView(Context context) {
        super(context);
        holder = getHolder();
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setFilterBitmap(true);

        // Background + player sprite
        background = BitmapFactory.decodeResource(getResources(), R.drawable.bg);
        character = new Sprite(context, R.drawable.character, 1, 8); // plane sprite: 1 row, 8 frames

        initPaints();

        // Load sound effect bằng SoundPool
        soundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
        shootSoundId = soundPool.load(context, R.raw.shoot, 1);
        explosionSoundId = soundPool.load(context, R.raw.explosion, 1);

        // Khởi tạo ExoPlayer
        exoPlayer = new ExoPlayer.Builder(context).build();
        MediaItem music = MediaItem.fromUri("android.resource://" + context.getPackageName() + "/" + R.raw.bg_music);
        exoPlayer.setMediaItem(music);
        exoPlayer.setRepeatMode(Player.REPEAT_MODE_ALL); // lặp vô hạn
        exoPlayer.prepare();
        exoPlayer.setVolume(1.0f);

        // Load icon âm thanh
        iconVolumeOn = BitmapFactory.decodeResource(getResources(), R.drawable.ic_volume_on);
        iconVolumeOff = BitmapFactory.decodeResource(getResources(), R.drawable.ic_volume_off);
        iconSfxOn = BitmapFactory.decodeResource(getResources(), R.drawable.ic_sfx_on);
        iconSfxOff = BitmapFactory.decodeResource(getResources(), R.drawable.ic_sfx_off);
    }

    private void initPaints() {
        buttonPaint = new Paint();
        buttonPaint.setColor(Color.argb(80, 255, 255, 255));
        buttonPaint.setStyle(Paint.Style.FILL);

        buttonPressedPaint = new Paint();
        buttonPressedPaint.setColor(Color.argb(120, 100, 200, 255));
        buttonPressedPaint.setStyle(Paint.Style.FILL);

        buttonBorderPaint = new Paint();
        buttonBorderPaint.setColor(Color.argb(150, 255, 255, 255));
        buttonBorderPaint.setStyle(Paint.Style.STROKE);
        buttonBorderPaint.setStrokeWidth(3);

        arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arrowPaint.setColor(Color.WHITE);
        arrowPaint.setTextSize(50);
        arrowPaint.setTextAlign(Paint.Align.CENTER);
        arrowPaint.setTypeface(Typeface.DEFAULT_BOLD);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        screenWidth = w; screenHeight = h;

        if (background != null) {
            background = Bitmap.createScaledBitmap(background, screenWidth, screenHeight, true);
        }

        if (!initialized && character != null) {
            charX = (screenWidth - character.getFrameWidth()) / 2f;
            charY = screenHeight - character.getFrameHeight() - 30f;
            initialized = true;
            character.setRow(0);
            character.resetAnimation();
        }

        // Setup joystick (thay D-pad)
        joystick = new Joystick(dp(100), screenHeight - dp(150), dp(80), dp(40)); // vị trí góc trái dưới

        // Attack button
        buttonAttack = new RectF(screenWidth - dp(150), screenHeight - dp(150), screenWidth - dp(50), screenHeight - dp(50));

        enemies.add(new Enemy(getContext(), screenWidth, screenHeight));
        enemies.add(new Enemy(getContext(), screenWidth, screenHeight));

        // Vị trí icon âm thanh
        iconVolumeRect = new RectF(20, 20, 20 + dp(50), 20 + dp(50)); // góc trái trên
        iconSfxRect = new RectF(screenWidth - dp(70), 20, screenWidth - 20, 20 + dp(50)); // góc phải trên
    }

    @Override
    public void run() {
        prevNs = System.nanoTime();
        while (isPlaying) {
            long now = System.nanoTime();
            accNs += (now - prevNs);
            prevNs = now;

            while (accNs >= STEP_NS) {
                update();
                accNs -= STEP_NS;
            }
            draw();
            try { Thread.sleep(1); } catch (InterruptedException ignored) {}
        }
    }

    private void update() {
        if (!initialized) return;

        // Background scroll
        bgX -= baseBgSpeed;
        if (bgX <= -screenWidth) bgX += screenWidth;
        if (bgX >= screenWidth) bgX -= screenWidth;

        // Player movement from joystick
        speedX = joystick.getActuatorX() * 5; // tốc độ tối đa 5
        speedY = joystick.getActuatorY() * 5;
        charX += speedX;
        charY += speedY;
        character.update();

        charX = Math.max(0, Math.min(charX, screenWidth - character.getFrameWidth()));
        charY = Math.max(0, Math.min(charY, screenHeight - character.getFrameHeight()));

        // Update projectiles
        for (int i = projectiles.size() - 1; i >= 0; i--) {
            Projectile p = projectiles.get(i);
            p.update();
            if (!p.isActive()) projectiles.remove(i);
        }

        // Update enemies
        for (Enemy e : enemies) e.update();

        // Update explosions
        for (int i = explosions.size() - 1; i >= 0; i--) {
            explosions.get(i).update();
            if (explosions.get(i).isFinished()) explosions.remove(i);
        }

        // Collision
        for (int i = projectiles.size() - 1; i >= 0; i--) {
            Projectile p = projectiles.get(i);
            Rect pRect = p.getRect();
            boolean hit = false;

            for (Enemy e : enemies) {
                if (Rect.intersects(pRect, e.getRect())) {
                    e.takeDamage(p.getDamage());
                    hit = true;
                    if (e.isDead()) {
                        explosions.add(new Explosion(getContext(), R.drawable.explosion_sheet, e.getX(), e.getY(), 9));
                        if (isAllSoundOn && isSfxOn) soundPool.play(explosionSoundId, 1,1,0,0,1);
                        e.reset();
                    }
                    break;
                }
            }
            if (hit) projectiles.remove(i);
        }
    }

    private void draw() {
        if (!initialized) return;
        Canvas canvas = null;
        try {
            if (!holder.getSurface().isValid()) return;
            canvas = holder.lockCanvas();
            if (canvas == null) return;

            if (background != null) {
                canvas.drawBitmap(background, bgX, 0, paint);
                canvas.drawBitmap(background, bgX + screenWidth, 0, paint);
            }

            character.draw(canvas, (int)charX, (int)charY, paint);

            for (Projectile p : projectiles) p.draw(canvas, paint);
            for (Enemy e : enemies) e.draw(canvas, paint);
            for (Explosion ex : explosions) ex.draw(canvas, paint);

            // Draw joystick
            joystick.draw(canvas, paint);

            // Draw attack button
            canvas.drawRoundRect(buttonAttack,25,25,buttonPressedPaint);
            canvas.drawText("⚔", buttonAttack.centerX(),buttonAttack.centerY()+15, arrowPaint);

            // Draw sound icons
            drawSoundIcons(canvas);

        } finally {
            if (canvas != null) holder.unlockCanvasAndPost(canvas);
        }
    }

    private void drawSoundIcons(Canvas canvas) {
        // Icon âm thanh tổng (góc trái)
        Bitmap musicIcon = isAllSoundOn ? iconVolumeOn : iconVolumeOff;
        Rect musicDst = new Rect((int)iconVolumeRect.left, (int)iconVolumeRect.top, (int)iconVolumeRect.right, (int)iconVolumeRect.bottom);
        canvas.drawBitmap(musicIcon, null, musicDst, paint);

        // Icon loa hiệu ứng (góc phải)
        Bitmap sfxIcon = isSfxOn ? iconSfxOn : iconSfxOff;
        Rect sfxDst = new Rect((int)iconSfxRect.left, (int)iconSfxRect.top, (int)iconSfxRect.right, (int)iconSfxRect.bottom);
        canvas.drawBitmap(sfxIcon, null, sfxDst, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!initialized) return true;

        int action = event.getActionMasked();
        int pointerIndex = event.getActionIndex();
        int pointerId = event.getPointerId(pointerIndex);

        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                // Nút volume
                if (iconVolumeRect.contains(x, y)) {
                    isAllSoundOn = !isAllSoundOn;
                    if (isAllSoundOn) {
                        if (exoPlayer != null && !exoPlayer.isPlaying()) exoPlayer.play();
                    } else {
                        if (exoPlayer != null && exoPlayer.isPlaying()) exoPlayer.pause();
                    }
                    return true;
                }

                // Nút SFX
                if (iconSfxRect.contains(x, y)) {
                    isSfxOn = !isSfxOn;
                    return true;
                }

                // Nút Attack
                if (buttonAttack.contains(x, y)) {
                    float projX = charX + character.getFrameWidth() / 2f - 6;
                    float projY = charY - 20;
                    projectiles.add(new Projectile(projX, projY));
                    if (isAllSoundOn && isSfxOn)
                        soundPool.play(shootSoundId, 1, 1, 0, 0, 1);
                    return true;
                }

                // Joystick
                if (joystick.isPressed(x, y)) {
                    joystick.setPressed(true);
                    joystickPointerId = pointerId; // gán pointerId cho joystick
                    joystick.setActuator(x, y);
                }
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (joystickPointerId != -1) {
                    // Tìm index của pointerId đang điều khiển joystick
                    int index = event.findPointerIndex(joystickPointerId);
                    if (index != -1) {
                        float jx = event.getX(index);
                        float jy = event.getY(index);
                        joystick.setActuator(jx, jy);
                    }
                }
                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (pointerId == joystickPointerId) {
                    joystick.setPressed(false);
                    joystick.resetActuator();
                    joystickPointerId = -1;
                }
                break;
            }
        }
        return true;
    }



    public void resume() {
        isPlaying = true;
        gameThread = new Thread(this);
        gameThread.start();

        if (exoPlayer != null && !exoPlayer.isPlaying()) {
            exoPlayer.play();
        }
    }

    public void pause() {
        isPlaying = false;
        try { if (gameThread != null) gameThread.join(); } catch (InterruptedException ignored) {}
        if (exoPlayer != null && exoPlayer.isPlaying()) {
            exoPlayer.pause();
        }
    }

    public void release() {
        if (background != null && !background.isRecycled()) {
            background.recycle();
            background = null;
        }
        if (character != null) {
            character.dispose();
            character = null;
        }
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
    }

    // Class Joystick nội bộ
    private class Joystick {
        private float centerX, centerY;
        private float baseRadius, hatRadius;
        private float actuatorX = 0, actuatorY = 0;
        private boolean isPressed = false;

        public Joystick(float centerX, float centerY, float baseRadius, float hatRadius) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.baseRadius = baseRadius;
            this.hatRadius = hatRadius;
        }

        public void draw(Canvas canvas, Paint paint) {
            // Outer
            paint.setColor(Color.argb(100, 200, 200, 200));
            canvas.drawCircle(centerX, centerY, baseRadius, paint);

            // Inner
            float innerX = centerX + actuatorX * baseRadius;
            float innerY = centerY + actuatorY * baseRadius;
            paint.setColor(Color.argb(200, 100, 200, 255));
            canvas.drawCircle(innerX, innerY, hatRadius, paint);
        }

        public boolean isPressed(float touchX, float touchY) {
            double dist = Math.sqrt((touchX - centerX) * (touchX - centerX) + (touchY - centerY) * (touchY - centerY));
            return dist < baseRadius;
        }

        public void setPressed(boolean pressed) { isPressed = pressed; }

        public boolean isPressed() { return isPressed; }

        public void setActuator(float touchX, float touchY) {
            float dx = touchX - centerX;
            float dy = touchY - centerY;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist > baseRadius) {
                dx = (dx / dist) * baseRadius;
                dy = (dy / dist) * baseRadius;
            }
            actuatorX = dx / baseRadius;
            actuatorY = dy / baseRadius;
        }

        public void resetActuator() {
            actuatorX = 0;
            actuatorY = 0;
        }

        public float getActuatorX() { return actuatorX; }
        public float getActuatorY() { return actuatorY; }
    }
}