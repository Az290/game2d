package com.example.game;

import android.content.Context;
import android.graphics.*;
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

    private static final long STEP_NS = 16_666_667L; // ~60fps
    private long prevNs, accNs;

    private float bgX = 0;
    private float baseBgSpeed = 2;

    private float dp(float v) { return v * getResources().getDisplayMetrics().density; }

    // Entities
    private List<Projectile> projectiles = new ArrayList<>();
    private List<Explosion> explosions = new ArrayList<>();
    private List<LevelUpEffect> levelEffects = new ArrayList<>();
    private List<Missile> missiles = new ArrayList<>();
    private List<Laser> lasers = new ArrayList<>();

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
    private int shootSoundId, explosionSoundId;

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
    private long shootCooldown = 300; // ms

    // Star
    private Star star;

    // Defense
    private Shield shield = new Shield();
    private ArmorBuff armorBuff = new ArmorBuff();
    private Barrier barrier = new Barrier();

    // WaveManager
    private WaveManager waveManager;

    // === Skill buttons ===
    private RectF btnMissile, btnLaser, btnShield, btnArmor, btnBarrier;
    private long cdMissile=5000, cdLaser=7000, cdShield=8000, cdArmor=8000, cdBarrier=9000;
    private long lastMissile=0, lastLaser=0, lastShield=0, lastArmor=0, lastBarrier=0;
    // Skill icons
    private Bitmap iconMissile, iconLaser, iconShield, iconArmor, iconBarrier;
    // Game Over UI icons
    private Bitmap bmpYouLose, bmpGameOver;
    private RectF buttonReplay, buttonHome, buttonExit;

    // Nút icon
    private Bitmap iconReplay, iconHome, iconExit;
    public GameView(Context context) {
        super(context);
        holder = getHolder();
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setFilterBitmap(true);

        background = BitmapFactory.decodeResource(getResources(), R.drawable.bg);
        character = new Sprite(context, R.drawable.character, 1, 8);

        player = new GamePlayer();
        levelTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        levelTextPaint.setColor(Color.WHITE);
        levelTextPaint.setTextSize(dp(20));
        levelTextPaint.setTextAlign(Paint.Align.LEFT);

        xpBarBgPaint = new Paint();
        xpBarBgPaint.setColor(Color.GRAY);
        xpBarFillPaint = new Paint();
        xpBarFillPaint.setColor(Color.rgb(0, 230, 118));

        soundPool = new SoundPool(5, android.media.AudioManager.STREAM_MUSIC, 0);
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
        // load your skill icons from drawable
        iconMissile = BitmapFactory.decodeResource(getResources(), R.drawable.ic_missile);
        iconLaser   = BitmapFactory.decodeResource(getResources(), R.drawable.ic_laser);
        iconShield  = BitmapFactory.decodeResource(getResources(), R.drawable.ic_shield);
        iconArmor   = BitmapFactory.decodeResource(getResources(), R.drawable.ic_armor);
        iconBarrier = BitmapFactory.decodeResource(getResources(), R.drawable.ic_barrier);

        bmpYouLose = BitmapFactory.decodeResource(getResources(), R.drawable.youlose);
        bmpGameOver = BitmapFactory.decodeResource(getResources(), R.drawable.gameover);

        iconReplay = BitmapFactory.decodeResource(getResources(), R.drawable.ic_replay);
        iconHome   = BitmapFactory.decodeResource(getResources(), R.drawable.ic_home);
        iconExit   = BitmapFactory.decodeResource(getResources(), R.drawable.ic_exit);
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
        star = new Star(getContext(), screenWidth, screenHeight);
        iconVolumeRect = new RectF(20, 20, 20 + dp(50), 20 + dp(50));
        iconSfxRect = new RectF(screenWidth - dp(70), 20, screenWidth - 20, 20 + dp(50));

        waveManager = new WaveManager(getContext(), screenWidth, screenHeight);

        // Skill buttons (phía phải màn hình như cũ)
        float btnSize = dp(60), margin = dp(20);
        btnMissile = new RectF(screenWidth - btnSize - margin, screenHeight - (btnSize*5 + margin), screenWidth - margin, screenHeight - (btnSize*4 + margin));
        btnLaser   = new RectF(screenWidth - btnSize - margin, screenHeight - (btnSize*4 + margin), screenWidth - margin, screenHeight - (btnSize*3 + margin));
        btnShield  = new RectF(screenWidth - btnSize - margin, screenHeight - (btnSize*3 + margin), screenWidth - margin, screenHeight - (btnSize*2 + margin));
        btnArmor   = new RectF(screenWidth - btnSize - margin, screenHeight - (btnSize*2 + margin), screenWidth - margin, screenHeight - (btnSize*1 + margin));
        btnBarrier = new RectF(screenWidth - btnSize - margin, screenHeight - (btnSize*1 + margin), screenWidth - margin, screenHeight - (0 + margin));

        // === Nút GameOver ===
        float goBtnSize = dp(80);
        float goSpacing = dp(40);
        float topY = screenHeight/2f + dp(120); // đặt nút dưới logo

        buttonReplay = new RectF(screenWidth/2f - goBtnSize - goSpacing, topY,
                screenWidth/2f - goSpacing, topY + goBtnSize);
        buttonHome   = new RectF(screenWidth/2f - goBtnSize/2, topY,
                screenWidth/2f + goBtnSize/2, topY + goBtnSize);
        buttonExit   = new RectF(screenWidth/2f + goSpacing, topY,
                screenWidth/2f + goSpacing + goBtnSize, topY + goBtnSize);
    }

    @Override
    public void run() {
        prevNs = System.nanoTime();
        while(isPlaying){
            long now=System.nanoTime();
            accNs += (now-prevNs); prevNs=now;
            while(accNs>=STEP_NS){
                update();
                accNs-=STEP_NS;
            }
            draw();
            try{ Thread.sleep(1); }catch(Exception ignored){}
        }
    }

    private void update() {
        if (!initialized || isGameOver) return;
        if (joystick == null || waveManager == null) return;

        // Bg
        bgX -= baseBgSpeed;
        if (bgX <= -screenWidth) bgX += screenWidth;

        // Player movement
        speedX = joystick.getActuatorX() * 5;
        speedY = joystick.getActuatorY() * 5;
        charX += speedX; charY += speedY;
        character.update();
        charX = Math.max(0, Math.min(charX, screenWidth - character.getFrameWidth()));
        charY = Math.max(0, Math.min(charY, screenHeight - character.getFrameHeight()));

        // Auto fire
        long now = System.currentTimeMillis();
        if (now - lastShootTime > shootCooldown) {
            fireProjectile();
            lastShootTime = now;
        }

        // Update projectiles
        for (int i=projectiles.size()-1;i>=0;i--){
            Projectile p = projectiles.get(i); p.update();
            if(!p.isActive()) projectiles.remove(i);
        }

        waveManager.update();
        for (Enemy e : new ArrayList<>(waveManager.getActiveEnemies())) {
            if (e.hasEscaped()) {
                int dmg=20; dmg=armorBuff.reduceDamage(dmg);
                playerHp-=dmg;
                explosions.add(new Explosion(getContext(),R.drawable.explosion_sheet,e.getX(),e.getY(),9));
                waveManager.getActiveEnemies().remove(e);
                if(playerHp<=0){ playerHp=0; isGameOver=true; isPlaying=false; }
            }
        }

        // Explosions
        for(int i=explosions.size()-1;i>=0;i--){
            explosions.get(i).update();
            if(explosions.get(i).isFinished()) explosions.remove(i);
        }

        // Level effects
        for(int i=levelEffects.size()-1;i>=0;i--){
            levelEffects.get(i).update();
            if(levelEffects.get(i).isFinished()) levelEffects.remove(i);
        }

        // === Player projectiles vs enemies ===
        for(int i=projectiles.size()-1;i>=0;i--){
            Projectile p = projectiles.get(i); boolean hit=false;
            for(Enemy e : new ArrayList<>(waveManager.getActiveEnemies())){
                if(!e.isDead() && Rect.intersects(p.getRect(), e.getRect())){
                    e.takeDamage(p.getDamage()); hit=true;
                    if(e.isDead()){
                        explosions.add(new Explosion(getContext(),R.drawable.explosion_sheet,e.getX(),e.getY(),9));
                        if(isAllSoundOn&&isSfxOn) soundPool.play(explosionSoundId,1,1,0,0,1);
                        if(player.addXP(e.getXPValue())){
                            levelEffects.add(new LevelUpEffect(charX+character.getFrameWidth()/2,charY));
                        }
                        waveManager.getActiveEnemies().remove(e);
                    }
                    break;
                }
            }
            if(hit) projectiles.remove(i);
        }

        // Missile vs Enemy
        for(int i=missiles.size()-1;i>=0;i--){
            Missile m=missiles.get(i);
            Enemy nearest=null; float best=Float.MAX_VALUE;
            for(Enemy e: waveManager.getActiveEnemies()){
                if(e.isDead()) continue;
                float dx=e.getX()-m.getRect().centerX();
                float dy=e.getY()-m.getRect().centerY();
                float d=(float)Math.sqrt(dx*dx+dy*dy);
                if(d<best){nearest=e;best=d;}
            }
            m.update(nearest);
            if(nearest!=null && !nearest.isDead() && Rect.intersects(m.getRect(),nearest.getRect())){
                nearest.takeDamage(m.getDamage());
                explosions.add(new Explosion(getContext(),R.drawable.explosion_sheet,nearest.getX(),nearest.getY(),9));
                if(isAllSoundOn&&isSfxOn) soundPool.play(explosionSoundId,1,1,0,0,1);
                m.deactivate();
                if(nearest.isDead()){
                    player.addXP(nearest.getXPValue());
                    waveManager.getActiveEnemies().remove(nearest);
                }
            }
            if(!m.isActive()) missiles.remove(i);
        }

        // Laser vs Enemy
        for(int i=lasers.size()-1;i>=0;i--){
            Laser l=lasers.get(i); l.update();
            if(!l.isActive()){lasers.remove(i); continue;}
            for(Enemy e: new ArrayList<>(waveManager.getActiveEnemies())){
                if(!e.isDead() && l.hit(e)){
                    e.takeDamage(l.getDamage());
                    explosions.add(new Explosion(getContext(),R.drawable.explosion_sheet,e.getX(),e.getY(),9));
                    if(e.isDead()){
                        player.addXP(e.getXPValue());
                        waveManager.getActiveEnemies().remove(e);
                    }
                }
            }
        }

        // Shield follow
        if(shield.isActive())
            shield.update(charX+character.getFrameWidth()/2,charY+character.getFrameHeight()/2);

        // Armor buff
        armorBuff.update();

        // Barrier collision
        if(barrier.isActive()){
            Rect bRect=barrier.getRect();
            for(Enemy e: new ArrayList<>(waveManager.getActiveEnemies())){
                if(!e.isDead() && Rect.intersects(bRect, e.getRect())){
                    e.takeDamage(999);
                    barrier.deactivate();
                    explosions.add(new Explosion(getContext(),R.drawable.explosion_sheet,e.getX(),e.getY(),9));
                    if(e.isDead()) waveManager.getActiveEnemies().remove(e);
                    break;
                }
            }
        }

        // === Player vs Enemy + bullets ===
        Rect cRect=new Rect((int)charX,(int)charY,
                (int)(charX+character.getFrameWidth()),
                (int)(charY+character.getFrameHeight()));
        for(Enemy e: new ArrayList<>(waveManager.getActiveEnemies())){
            if(!e.isDead() && Rect.intersects(cRect,e.getRect())){
                if(shield.isActive()){ shield.hit(); }
                else {
                    int dmg=20; dmg=armorBuff.reduceDamage(dmg); playerHp-=dmg;
                }
                explosions.add(new Explosion(getContext(),R.drawable.explosion_sheet,e.getX(),e.getY(),9));
                waveManager.getActiveEnemies().remove(e);
                if(playerHp<=0){playerHp=0;isGameOver=true;isPlaying=false;}
            }
            for(EnemyBullet b: e.getBullets()){
                if(b.isActive() && Rect.intersects(cRect,b.getRect())){
                    b.active=false;
                    if(shield.isActive()) shield.hit();
                    else {
                        int dmg=10; dmg=armorBuff.reduceDamage(dmg); playerHp-=dmg;
                    }
                    if(playerHp<=0){playerHp=0;isGameOver=true;isPlaying=false;}
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

            // Clear màn hình
            canvas.drawColor(Color.BLACK);

            // Background
            canvas.drawBitmap(background, bgX, 0, paint);
            canvas.drawBitmap(background, bgX + screenWidth, 0, paint);

            // Player
            character.draw(canvas,(int)charX,(int)charY,paint);

            for(Projectile p: projectiles) p.draw(canvas,paint);
            for(Missile m: missiles) m.draw(canvas,paint);
            for(Laser l: lasers)     l.draw(canvas,paint);

            for(Enemy e: new ArrayList<>(waveManager.getActiveEnemies())) e.draw(canvas, paint);

            for(Explosion ex: explosions) ex.draw(canvas,paint);
            for(LevelUpEffect eff: levelEffects) eff.draw(canvas,paint);

            if (armorBuff.isActive()) {
                paint.setColor(Color.argb(120,200,0,200));
                canvas.drawCircle(charX+character.getFrameWidth()/2f,
                        charY+character.getFrameHeight()/2f,
                        character.getFrameWidth(),paint);
            }

            if(star != null) star.draw(canvas,paint);
            shield.draw(canvas,paint);
            barrier.draw(canvas,paint);
            joystick.draw(canvas,paint);

            drawSoundIcons(canvas);
            drawPlayerUI(canvas);
            drawSkillButtons(canvas);

            // === Game Over UI ===
            if (isGameOver) {
                paint.setColor(Color.argb(200,0,0,0));
                canvas.drawRect(0,0,screenWidth,screenHeight,paint);

                // === Scale & vẽ logo YOU LOSE! ===
                float logoW = screenWidth * 0.6f; // chiếm 60% màn hình ngang
                float yW = (bmpYouLose.getHeight() / (float)bmpYouLose.getWidth()) * logoW;
                float gW = (bmpGameOver.getHeight() / (float)bmpGameOver.getWidth()) * logoW;

                RectF dstYouLose = new RectF(
                        (screenWidth - logoW)/2,
                        screenHeight/5f,
                        (screenWidth - logoW)/2 + logoW,
                        screenHeight/5f + yW
                );
                canvas.drawBitmap(bmpYouLose, null, dstYouLose, paint);

                RectF dstGameOver = new RectF(
                        (screenWidth - logoW)/2,
                        dstYouLose.bottom + dp(10),
                        (screenWidth - logoW)/2 + logoW,
                        dstYouLose.bottom + dp(10) + gW
                );
                canvas.drawBitmap(bmpGameOver, null, dstGameOver, paint);

                // === Buttons ===
                float corner = dp(40);
                int btnColor = Color.rgb(68,68,68); // xám đậm

                // Replay
                paint.setColor(btnColor);
                canvas.drawRoundRect(buttonReplay, corner, corner, paint);
                if (iconReplay != null) {
                    RectF dst = new RectF(
                            buttonReplay.left+dp(12),
                            buttonReplay.top+dp(12),
                            buttonReplay.right-dp(12),
                            buttonReplay.bottom-dp(12));
                    canvas.drawBitmap(iconReplay, null, dst, paint);
                }

                // Home
                paint.setColor(btnColor);
                canvas.drawRoundRect(buttonHome, corner, corner, paint);
                if (iconHome != null) {
                    RectF dst = new RectF(
                            buttonHome.left+dp(12),
                            buttonHome.top+dp(12),
                            buttonHome.right-dp(12),
                            buttonHome.bottom-dp(12));
                    canvas.drawBitmap(iconHome, null, dst, paint);
                }

                // Exit
                paint.setColor(btnColor);
                canvas.drawRoundRect(buttonExit, corner, corner, paint);
                if (iconExit != null) {
                    RectF dst = new RectF(
                            buttonExit.left+dp(12),
                            buttonExit.top+dp(12),
                            buttonExit.right-dp(12),
                            buttonExit.bottom-dp(12));
                    canvas.drawBitmap(iconExit, null, dst, paint);
                }
            }

        } finally {
            if (canvas != null) holder.unlockCanvasAndPost(canvas);
        }
    }

    // === Skill button drawing with cooldown ===
    private void drawSkillButtons(Canvas canvas){
        drawButton(canvas, btnMissile, iconMissile, System.currentTimeMillis()-lastMissile, cdMissile);
        drawButton(canvas, btnLaser,   iconLaser,   System.currentTimeMillis()-lastLaser,   cdLaser);
        drawButton(canvas, btnShield,  iconShield,  System.currentTimeMillis()-lastShield,  cdShield);
        drawButton(canvas, btnArmor,   iconArmor,   System.currentTimeMillis()-lastArmor,   cdArmor);
        drawButton(canvas, btnBarrier, iconBarrier, System.currentTimeMillis()-lastBarrier, cdBarrier);
    }

    private void drawButton(Canvas c, RectF r, Bitmap icon, long elapsed, long cd){
        // Vẽ nền nút
        paint.setColor(Color.argb(200,50,50,50));
        c.drawRoundRect(r,10,10,paint);

        // Vẽ icon ở giữa nút
        if(icon != null){
            Rect dst = new Rect((int)r.left+10,(int)r.top+10,(int)r.right-10,(int)r.bottom-10);
            c.drawBitmap(icon,null,dst,paint);
        }

        // Overlay cooldown
        if(elapsed < cd){
            float ratio = 1f - (float)elapsed/cd;
            paint.setColor(Color.argb(160,0,0,0));
            c.drawRect(r.left, r.top, r.right, r.top + r.height()*ratio, paint);
        }
    }

    // ==== Fire ====
    private void fireProjectile(){
        float projX=charX+character.getFrameWidth()/2f-6;
        float projY=charY-20;
        int dmg=player.getCurrentDamage();
        int count=player.getBulletCount();
        if(count==1) projectiles.add(new Projectile(projX,projY,dmg));
        else if(count==2){
            projectiles.add(new Projectile(projX-15,projY,dmg));
            projectiles.add(new Projectile(projX+15,projY,dmg));
        } else {
            projectiles.add(new Projectile(projX-30,projY,dmg));
            projectiles.add(new Projectile(projX,projY,dmg));
            projectiles.add(new Projectile(projX+30,projY,dmg));
        }
        if(isAllSoundOn&&isSfxOn) soundPool.play(shootSoundId,1,1,0,0,1);
    }
    private void fireMissile(){
        float mx=charX+character.getFrameWidth()/2f;
        float my=charY;
        missiles.add(new Missile(mx,my));
    }
    private void fireLaser(){
        float lx=charX+character.getFrameWidth()/2f;
        float ly=charY;
        lasers.add(new Laser(lx,ly,screenHeight));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!initialized) return true;
        int action = event.getActionMasked();
        int pointerIndex = event.getActionIndex();
        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);

        // === Xử lý khi Game Over ===
        if (isGameOver && action == MotionEvent.ACTION_DOWN) {
            // --- Replay ---
            if (buttonReplay.contains(x, y)) {
                resetGame();  // Bắt đầu lại từ wave 1
                return true;
            }

            // --- Home ---
            if (buttonHome.contains(x, y)) {
                Context c = getContext();
                if (c instanceof android.app.Activity) {
                    android.content.Intent intent = new android.content.Intent(c, SplashActivity.class);
                    c.startActivity(intent);        // Quay về Splash
                    ((android.app.Activity)c).finish(); // Đóng GameActivity
                }
                return true;
            }

            // --- Exit ---
            if (buttonExit.contains(x, y)) {
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(1);
                return true;
            }
        }

        // === Xử lý khi đang chơi ===
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                long now = System.currentTimeMillis();

                // Skill buttons
                if (btnMissile.contains(x, y) && now - lastMissile > cdMissile) {
                    fireMissile(); lastMissile = now; return true;
                }
                if (btnLaser.contains(x, y) && now - lastLaser > cdLaser) {
                    fireLaser(); lastLaser = now; return true;
                }
                if (btnShield.contains(x, y) && now - lastShield > cdShield) {
                    shield.activate(charX+character.getFrameWidth()/2, charY+character.getFrameHeight()/2,
                            character.getFrameWidth());
                    lastShield = now; return true;
                }
                if (btnArmor.contains(x, y) && now - lastArmor > cdArmor) {
                    armorBuff.activate(); lastArmor = now; return true;
                }
                if (btnBarrier.contains(x, y) && now - lastBarrier > cdBarrier) {
                    barrier.activate(charX+character.getFrameWidth()/2, charY);
                    lastBarrier = now; return true;
                }

                // Toggle âm thanh
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
                    isSfxOn = !isSfxOn; return true;
                }

                // Joystick
                if (joystick.isPressed(x, y)) {
                    joystick.setPressed(true);
                    joystickPointerId = event.getPointerId(pointerIndex);
                    joystick.setActuator(x, y);
                }
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                if (joystickPointerId != -1) {
                    int idx = event.findPointerIndex(joystickPointerId);
                    if (idx != -1) {
                        joystick.setActuator(event.getX(idx), event.getY(idx));
                    }
                }
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (event.getPointerId(pointerIndex) == joystickPointerId) {
                    joystick.setPressed(false);
                    joystick.resetActuator();
                    joystickPointerId = -1;
                }
                break;
            }
        }
        return true;
    }

    // === UI helpers ===
    private void drawSoundIcons(Canvas canvas) {
        Bitmap musicIcon=isAllSoundOn?iconVolumeOn:iconVolumeOff;
        Rect musicDst=new Rect((int)iconVolumeRect.left,(int)iconVolumeRect.top,
                (int)iconVolumeRect.right,(int)iconVolumeRect.bottom);
        canvas.drawBitmap(musicIcon,null,musicDst,paint);

        Bitmap sfxIcon=isSfxOn?iconSfxOn:iconSfxOff;
        Rect sfxDst=new Rect((int)iconSfxRect.left,(int)iconSfxRect.top,
                (int)iconSfxRect.right,(int)iconSfxRect.bottom);
        canvas.drawBitmap(sfxIcon,null,sfxDst,paint);
    }
    private void drawPlayerUI(Canvas c){
        c.drawText("Wave: "+waveManager.getCurrentWave(), dp(20), dp(50), levelTextPaint);
        c.drawText("LV: "+player.getLevel(), dp(20), dp(80), levelTextPaint);
        if(player.isMaxLevel()) c.drawText("MAX LEVEL", dp(80), dp(80), levelTextPaint);
        else {
            float xpW=dp(150), xpH=dp(10), xpX=dp(80), xpY=dp(75);
            c.drawRect(xpX,xpY,xpX+xpW,xpY+xpH,xpBarBgPaint);
            float prog=player.getXPProgress();
            c.drawRect(xpX,xpY,xpX+xpW*prog,xpY+xpH,xpBarFillPaint);
        }
        c.drawText("HP: "+playerHp, dp(20), dp(120), levelTextPaint);
        float hpW=dp(150),hpH=dp(12), hpX=dp(80), hpY=dp(110);
        paint.setColor(Color.RED);
        c.drawRect(hpX,hpY,hpX+hpW,hpY+hpH,paint);
        paint.setColor(Color.GREEN);
        float hpProg=(float)playerHp/maxPlayerHp;
        c.drawRect(hpX,hpY,hpX+hpW*hpProg,hpY+hpH,paint);
    }

    private void resetGame() {
        // Reset HP và trạng thái
        playerHp = maxPlayerHp;
        isGameOver = false;
        player = new GamePlayer();

        // Dọn sạch mọi entity & effect
        projectiles.clear();
        missiles.clear();
        lasers.clear();
        explosions.clear();
        levelEffects.clear();

        if (star != null) star.respawn();

        // Reset wave
        waveManager = new WaveManager(getContext(), screenWidth, screenHeight);

        // === Reset vị trí nhân vật lại từ đầu (giữa, sát đáy) ===
        charX = (screenWidth - character.getFrameWidth()) / 2f;
        charY = screenHeight - character.getFrameHeight() - 30f;

        // Reset animation sprite nhân vật
        character.setRow(0);
        character.resetAnimation();

        // Bắt đầu thread game loop lại
        isPlaying = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    public void resume(){ if(isGameOver)return;
        isPlaying=true; gameThread=new Thread(this); gameThread.start();
        if(exoPlayer!=null && !exoPlayer.isPlaying())exoPlayer.play();
    }
    public void pause(){ isPlaying=false;
        try{if(gameThread!=null)gameThread.join();}catch(Exception ignored){}
        if(exoPlayer!=null&&exoPlayer.isPlaying())exoPlayer.pause();
    }
    public void release(){
        if(background!=null&&!background.isRecycled()){ background.recycle();background=null; }
        if(character!=null){ character.dispose(); character=null; }
        if(soundPool!=null){ soundPool.release(); soundPool=null; }
        if(exoPlayer!=null){ exoPlayer.release(); exoPlayer=null; }
    }

    // =========== Inner Joystick class ==========
    private class Joystick{
        private float centerX, centerY, baseRadius, hatRadius, actuatorX=0, actuatorY=0;
        private boolean isPressed = false;

        public Joystick(float cx, float cy, float br, float hr){
            centerX=cx; centerY=cy; baseRadius=br; hatRadius=hr;
        }

        public void draw(Canvas canvas,Paint paint){
            paint.setColor(Color.argb(100,200,200,200));
            canvas.drawCircle(centerX,centerY,baseRadius,paint);
            float innerX = centerX + actuatorX*baseRadius;
            float innerY = centerY + actuatorY*baseRadius;
            paint.setColor(Color.argb(200,100,200,255));
            canvas.drawCircle(innerX, innerY, hatRadius, paint);
        }

        public boolean isPressed(float tx,float ty){
            double d=Math.sqrt((tx-centerX)*(tx-centerX)+(ty-centerY)*(ty-centerY));
            return d<baseRadius;
        }

        public void setPressed(boolean p){ isPressed=p; }

        public void setActuator(float tx,float ty){
            float dx=tx-centerX, dy=ty-centerY;
            float d=(float)Math.sqrt(dx*dx+dy*dy);
            if(d>baseRadius){
                dx=(dx/d)*baseRadius;
                dy=(dy/d)*baseRadius;
            }
            actuatorX=dx/baseRadius;
            actuatorY=dy/baseRadius;
        }

        public void resetActuator(){ actuatorX=0; actuatorY=0; }
        public float getActuatorX(){return actuatorX;}
        public float getActuatorY(){return actuatorY;}
    }
}