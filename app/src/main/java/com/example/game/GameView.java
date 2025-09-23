package com.example.game;

import android.content.Context;
import android.graphics.*;
import android.media.AudioManager;
import android.media.SoundPool;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.os.Handler;

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

    private static final long STEP_NS = 16_666_667L;
    private long prevNs, accNs;

    private float bgX = 0;
    private float baseBgSpeed = 2;

    private float dp(float v) { return v * getResources().getDisplayMetrics().density; }

    // Entities
    private List<Projectile> projectiles = new ArrayList<>();
    private List<Enemy> enemies = new ArrayList<>();
    private List<Explosion> explosions = new ArrayList<>();
    private List<LevelUpEffect> levelEffects = new ArrayList<>();

    // Player system
    private GamePlayer player;
    private Paint levelTextPaint, xpBarBgPaint, xpBarFillPaint;

    // Player HP
    private int playerHp = 100;
    private int maxPlayerHp = 100;
    private boolean isGameOver = false;
    private RectF buttonRestart;

    // Sound
    private SoundPool soundPool;
    private int shootSoundId;
    private int explosionSoundId;

    // BGM
    private ExoPlayer exoPlayer;

    // Icons
    private Bitmap iconVolumeOn, iconVolumeOff;
    private Bitmap iconSfxOn, iconSfxOff;
    private RectF iconVolumeRect, iconSfxRect;
    private boolean isAllSoundOn = true;
    private boolean isSfxOn = true;

    // Joystick
    private Joystick joystick;
    private int joystickPointerId = -1;

    // Auto Fire
    private long lastShootTime = 0;
    private long shootCooldown = 300; // ms, càng nhỏ bắn càng nhanh

    // Star power-up
    private Star star;
    private Handler handler = new Handler();

    public GameView(Context context) {
        super(context);
        holder = getHolder();
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setFilterBitmap(true);

        background = BitmapFactory.decodeResource(getResources(), R.drawable.bg);
        character = new Sprite(context, R.drawable.character, 1, 8);

        initPaints();

        player = new GamePlayer();
        levelTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        levelTextPaint.setColor(Color.WHITE);
        levelTextPaint.setTextSize(dp(20));
        levelTextPaint.setTextAlign(Paint.Align.LEFT);

        xpBarBgPaint = new Paint();
        xpBarBgPaint.setColor(Color.GRAY);
        xpBarFillPaint = new Paint();
        xpBarFillPaint.setColor(Color.rgb(0, 230, 118));

        soundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
        shootSoundId = soundPool.load(context, R.raw.shoot, 1);
        explosionSoundId = soundPool.load(context, R.raw.explosion, 1);

        exoPlayer = new ExoPlayer.Builder(context).build();
        MediaItem music = MediaItem.fromUri("android.resource://" + context.getPackageName() + "/" + R.raw.bg_music);
        exoPlayer.setMediaItem(music);
        exoPlayer.setRepeatMode(Player.REPEAT_MODE_ALL);
        exoPlayer.prepare();
        exoPlayer.setVolume(1.0f);

        iconVolumeOn = BitmapFactory.decodeResource(getResources(), R.drawable.ic_volume_on);
        iconVolumeOff = BitmapFactory.decodeResource(getResources(), R.drawable.ic_volume_off);
        iconSfxOn = BitmapFactory.decodeResource(getResources(), R.drawable.ic_sfx_on);
        iconSfxOff = BitmapFactory.decodeResource(getResources(), R.drawable.ic_sfx_off);
    }

    private void initPaints() {
        // không cần vẽ nút Attack nữa
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

        joystick = new Joystick(dp(100), screenHeight - dp(150), dp(80), dp(40));
        buttonRestart = new RectF(screenWidth/2f - dp(100), screenHeight/2f + dp(50),
                screenWidth/2f + dp(100), screenHeight/2f + dp(120));

        enemies.add(new Enemy(getContext(), screenWidth, screenHeight, 0));
        enemies.add(new Enemy(getContext(), screenWidth, screenHeight, 1));
        enemies.add(new Enemy(getContext(), screenWidth, screenHeight, 2));

        star = new Star(getContext(), screenWidth, screenHeight);

        iconVolumeRect = new RectF(20, 20, 20 + dp(50), 20 + dp(50));
        iconSfxRect = new RectF(screenWidth - dp(70), 20, screenWidth - 20, 20 + dp(50));
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
        if (!initialized || isGameOver) return;

        bgX -= baseBgSpeed;
        if (bgX <= -screenWidth) bgX += screenWidth;

        speedX = joystick.getActuatorX() * 5;
        speedY = joystick.getActuatorY() * 5;
        charX += speedX;
        charY += speedY;
        character.update();

        charX = Math.max(0, Math.min(charX, screenWidth - character.getFrameWidth()));
        charY = Math.max(0, Math.min(charY, screenHeight - character.getFrameHeight()));

        // ==== Auto Fire ====
        long now = System.currentTimeMillis();
        if (now - lastShootTime > shootCooldown) {
            fireProjectile();
            lastShootTime = now;
        }

        for (int i = projectiles.size() - 1; i >= 0; i--) {
            Projectile p = projectiles.get(i);
            p.update();
            if (!p.isActive()) projectiles.remove(i);
        }

        for (Enemy e : enemies) e.update();

        for (int i = explosions.size() - 1; i >= 0; i--) {
            explosions.get(i).update();
            if (explosions.get(i).isFinished()) explosions.remove(i);
        }

        for (int i = levelEffects.size() - 1; i >= 0; i--) {
            levelEffects.get(i).update();
            if (levelEffects.get(i).isFinished()) levelEffects.remove(i);
        }

        // Projectile vs Enemy
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

                        boolean leveledUp = player.addXP(e.getXPValue());
                        if (leveledUp) {
                            levelEffects.add(new LevelUpEffect(
                                    charX + character.getFrameWidth()/2,
                                    charY + character.getFrameHeight()/2));
                        }
                        e.reset();
                    }
                    break;
                }
            }
            if (hit) projectiles.remove(i);
        }

        // Character vs Enemy
        Rect charRect = new Rect((int)charX, (int)charY,
                (int)charX + character.getFrameWidth(),
                (int)charY + character.getFrameHeight());

        for (Enemy e : enemies) {
            if (Rect.intersects(charRect, e.getRect())) {
                if (isAllSoundOn && isSfxOn) {
                    soundPool.play(explosionSoundId, 1,1,0,0,1);
                }

                playerHp -= 20;
                e.reset();

                if (playerHp <= 0) {
                    playerHp = 0;
                    isGameOver = true;
                    isPlaying = false;
                }
            }
        }

        // Enemy vs Star
        if (star != null && star.isActive()) {
            for (int i = 0; i < enemies.size(); i++) {
                Enemy e = enemies.get(i);
                if (Rect.intersects(star.getRect(), e.getRect())) {
                    // Nhân bản enemy → thêm 2 con nữa
                    enemies.add(new Enemy(getContext(), screenWidth, screenHeight, e.getType()));
                    enemies.get(enemies.size()-1).setPosition(e.getX() + 30, e.getY());

                    enemies.add(new Enemy(getContext(), screenWidth, screenHeight, e.getType()));
                    enemies.get(enemies.size()-1).setPosition(e.getX() - 30, e.getY());

                    star.deactivate();

                    handler.postDelayed(() -> {
                        if (star != null) star.respawn();
                    }, 3000);
                    break;
                }
            }
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
            for (LevelUpEffect effect : levelEffects) effect.draw(canvas, paint);

            if (star != null) star.draw(canvas, paint);

            joystick.draw(canvas, paint);

            drawSoundIcons(canvas);
            drawPlayerUI(canvas);

            if (isGameOver) {
                paint.setColor(Color.argb(180, 0, 0, 0));
                canvas.drawRect(0, 0, screenWidth, screenHeight, paint);

                paint.setColor(Color.WHITE);
                paint.setTextSize(dp(40));
                paint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText("GAME OVER", screenWidth/2f, screenHeight/2f, paint);

                paint.setTextSize(dp(25));
                canvas.drawRoundRect(buttonRestart, 20, 20, paint);
                canvas.drawText("RESTART", buttonRestart.centerX(), buttonRestart.centerY()+10, paint);
            }

        } finally {
            if (canvas != null) holder.unlockCanvasAndPost(canvas);
        }
    }

    private void drawPlayerUI(Canvas canvas) {
        canvas.drawText("LV: " + player.getLevel(), dp(20), dp(80), levelTextPaint);

        if (player.isMaxLevel()) {
            canvas.drawText("MAX LEVEL", dp(80), dp(80), levelTextPaint);
        } else {
            float xpBarWidth = dp(150);
            float xpBarHeight = dp(10);
            float xpBarX = dp(80);
            float xpBarY = dp(75);
            canvas.drawRect(xpBarX, xpBarY, xpBarX + xpBarWidth, xpBarY + xpBarHeight, xpBarBgPaint);

            float progress = player.getXPProgress();
            canvas.drawRect(xpBarX, xpBarY, xpBarX + xpBarWidth * progress, xpBarY + xpBarHeight, xpBarFillPaint);
        }

        canvas.drawText("HP: " + playerHp, dp(20), dp(120), levelTextPaint);
        float hpBarWidth = dp(150);
        float hpBarHeight = dp(12);
        float hpBarX = dp(80);
        float hpBarY = dp(110);

        paint.setColor(Color.RED);
        canvas.drawRect(hpBarX, hpBarY, hpBarX + hpBarWidth, hpBarY + hpBarHeight, paint);

        paint.setColor(Color.GREEN);
        float hpProgress = (float)playerHp / maxPlayerHp;
        canvas.drawRect(hpBarX, hpBarY, hpBarX + hpBarWidth * hpProgress, hpBarY + hpBarHeight, paint);
    }

    private void drawSoundIcons(Canvas canvas) {
        Bitmap musicIcon = isAllSoundOn ? iconVolumeOn : iconVolumeOff;
        Rect musicDst = new Rect((int)iconVolumeRect.left, (int)iconVolumeRect.top, (int)iconVolumeRect.right, (int)iconVolumeRect.bottom);
        canvas.drawBitmap(musicIcon, null, musicDst, paint);

        Bitmap sfxIcon = isSfxOn ? iconSfxOn : iconSfxOff;
        Rect sfxDst = new Rect((int)iconSfxRect.left, (int)iconSfxRect.top, (int)iconSfxRect.right, (int)iconSfxRect.bottom);
        canvas.drawBitmap(sfxIcon, null, sfxDst, paint);
    }

    private void fireProjectile() {
        float projX = charX + character.getFrameWidth() / 2f - 6;
        float projY = charY - 20;
        int currentDamage = player.getCurrentDamage();
        int bulletCount = player.getBulletCount();

        if (bulletCount == 1) {
            projectiles.add(new Projectile(projX, projY, currentDamage));
        } else if (bulletCount == 2) {
            projectiles.add(new Projectile(projX - 15, projY, currentDamage));
            projectiles.add(new Projectile(projX + 15, projY, currentDamage));
        } else {
            projectiles.add(new Projectile(projX - 30, projY, currentDamage));
            projectiles.add(new Projectile(projX, projY, currentDamage));
            projectiles.add(new Projectile(projX + 30, projY, currentDamage));
        }

        if (isAllSoundOn && isSfxOn)
            soundPool.play(shootSoundId, 1, 1, 0, 0, 1);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!initialized) return true;

        int action = event.getActionMasked();
        int pointerIndex = event.getActionIndex();
        int pointerId = event.getPointerId(pointerIndex);

        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);

        if (isGameOver) {
            if (action == MotionEvent.ACTION_DOWN) {
                if (buttonRestart.contains(x, y)) {
                    resetGame();
                    return true;
                }
            }
            return true;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                if (iconVolumeRect.contains(x, y)) {
                    isAllSoundOn = !isAllSoundOn;
                    if (isAllSoundOn) {
                        if (exoPlayer != null && !exoPlayer.isPlaying()) exoPlayer.play();
                    } else {
                        if (exoPlayer != null && exoPlayer.isPlaying()) exoPlayer.pause();
                    }
                    return true;
                }

                if (iconSfxRect.contains(x, y)) {
                    isSfxOn = !isSfxOn;
                    return true;
                }

                if (joystick.isPressed(x, y)) {
                    joystick.setPressed(true);
                    joystickPointerId = pointerId;
                    joystick.setActuator(x, y);
                }
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (joystickPointerId != -1) {
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

    private void resetGame() {
        playerHp = maxPlayerHp;
        isGameOver = false;
        player = new GamePlayer();
        projectiles.clear();
        enemies.clear();
        enemies.add(new Enemy(getContext(), screenWidth, screenHeight, 0));
        enemies.add(new Enemy(getContext(), screenWidth, screenHeight, 1));
        enemies.add(new Enemy(getContext(), screenWidth, screenHeight, 2));
        explosions.clear();
        levelEffects.clear();
        star.respawn();

        isPlaying = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    public void resume() {
        if (isGameOver) return;
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

    // Joystick nội bộ
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
            paint.setColor(Color.argb(100, 200, 200, 200));
            canvas.drawCircle(centerX, centerY, baseRadius, paint);

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