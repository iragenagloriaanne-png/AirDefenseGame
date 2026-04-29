import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

public class AirDefenseGame extends JPanel implements Runnable {

    int width = 900, height = 600;
    Thread gameThread;

    boolean running = true;
    boolean gameStarted = false;
    boolean levelSelected = false;

    int selectedLevel = 0;

    Rectangle easyButton = new Rectangle(330, 240, 240, 50);
    Rectangle mediumButton = new Rectangle(330, 305, 240, 50);
    Rectangle hardButton = new Rectangle(330, 370, 240, 50);
    Rectangle playButton = new Rectangle(350, 455, 200, 50);
    Rectangle restartButton = new Rectangle(350, 390, 200, 55);

    int playerX = 420, playerY = 455;
    int playerW = 45, playerH = 85;
    int playerSpeed = 8;
    boolean facingRight = true;

    int mouseX = 450, mouseY = 450;

    boolean left = false;
    boolean right = false;

    boolean playerDead = false;
    int deadTimer = 0;
    int muzzleFlashTimer = 0;

    int score = 0;
    int persons = 3;
    int currentPerson = 1;
    int nextExtraPersonScore = 100;

    int wave = 1;
    int planesToSpawn = 1;
    int planesDestroyedInWave = 0;

    String levelName = "";
    int enemySpeed = 3;
    int enemyShootChance = 2;

    boolean treeAlive = true;
    boolean houseAlive = true;
    boolean bunkerAlive = true;
    boolean stairsAlive = true;

    int treeHealth = 2;
    int houseHealth = 2;
    int bunkerHealth = 2;
    int stairsHealth = 2;

    ArrayList<Bullet> bullets = new ArrayList<>();
    ArrayList<Rectangle> enemyBullets = new ArrayList<>();
    ArrayList<Bomb> bombs = new ArrayList<>();
    ArrayList<Plane> planes = new ArrayList<>();
    ArrayList<KnifeEnemy> knifeEnemies = new ArrayList<>();
    ArrayList<Explosion> explosions = new ArrayList<>();

    Random rand = new Random();

    class Bullet {
        double x, y, dx, dy;
        int w = 8, h = 8;

        Bullet(double x, double y, double dx, double dy) {
            this.x = x;
            this.y = y;
            this.dx = dx;
            this.dy = dy;
        }

        void move() {
            x += dx;
            y += dy;
        }

        Rectangle getBox() {
            return new Rectangle((int) x, (int) y, w, h);
        }
    }

    class Plane {
        int x, y;
        int w = 100, h = 45;
        int direction;

        Plane(int direction) {
            this.direction = direction;
            x = direction == 1 ? -130 : width + 130;
            y = 50 + rand.nextInt(160);
        }

        void move() {
            x += enemySpeed * direction;
        }

        Rectangle getBox() {
            return new Rectangle(x, y, w, h);
        }

        boolean outOfScreen() {
            return x > width + 160 || x < -180;
        }
    }

    class Bomb {
        int x, y;
        int w = 16, h = 24;

        Bomb(int x, int y) {
            this.x = x;
            this.y = y;
        }

        void move() {
            y += 5;
        }

        Rectangle getBox() {
            return new Rectangle(x, y, w, h);
        }
    }

    class KnifeEnemy {
        int x, y;
        int w = 35, h = 60;
        boolean landed = false;
        int speed = 2;

        KnifeEnemy(int x, int y) {
            this.x = x;
            this.y = y;
        }

        void move() {
            if (!landed) {
                y += 4;
                if (y >= 480) {
                    y = 480;
                    landed = true;
                }
            } else {
                if (x < playerX) x += speed;
                else x -= speed;
            }
        }

        Rectangle getBox() {
            return new Rectangle(x, y, w, h);
        }
    }

    class Explosion {
        int x, y;
        int size = 10;
        int life = 22;

        Explosion(int x, int y) {
            this.x = x;
            this.y = y;
        }

        void update() {
            size += 3;
            life--;
        }

        boolean isDone() {
            return life <= 0;
        }
    }

    public AirDefenseGame() {
        setPreferredSize(new Dimension(width, height));
        setBackground(Color.CYAN);
        setFocusable(true);

        setupKeys();
        setupMouse();

        gameThread = new Thread(this);
        gameThread.start();
    }

    public void setupMouse() {
        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
            }

            public void mouseDragged(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
            }
        });

        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                Point p = e.getPoint();
                mouseX = e.getX();
                mouseY = e.getY();

                if (!running) {
                    if (restartButton.contains(p)) {
                        restartGame();
                    }
                    return;
                }

                if (!gameStarted) {
                    handleMenuClick(p);
                    return;
                }

                shootToMouse();
            }
        });
    }

    public void setupKeys() {
        InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        im.put(KeyStroke.getKeyStroke("pressed LEFT"), "leftPressed");
        im.put(KeyStroke.getKeyStroke("released LEFT"), "leftReleased");
        im.put(KeyStroke.getKeyStroke("pressed RIGHT"), "rightPressed");
        im.put(KeyStroke.getKeyStroke("released RIGHT"), "rightReleased");
        im.put(KeyStroke.getKeyStroke("pressed SPACE"), "shoot");

        am.put("leftPressed", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                left = true;
            }
        });

        am.put("leftReleased", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                left = false;
            }
        });

        am.put("rightPressed", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                right = true;
            }
        });

        am.put("rightReleased", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                right = false;
            }
        });

        am.put("shoot", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                shootToMouse();
            }
        });
    }

    public void handleMenuClick(Point p) {
        if (easyButton.contains(p)) {
            selectedLevel = 1;
            levelSelected = true;
            levelName = "EASY";
        } else if (mediumButton.contains(p)) {
            selectedLevel = 2;
            levelSelected = true;
            levelName = "MEDIUM";
        } else if (hardButton.contains(p)) {
            selectedLevel = 3;
            levelSelected = true;
            levelName = "HARD";
        } else if (playButton.contains(p) && levelSelected) {
            chooseLevel(selectedLevel);
        }
    }

    public void chooseLevel(int level) {
        gameStarted = true;

        if (level == 1) {
            enemySpeed = 2;
            enemyShootChance = 1;
        } else if (level == 2) {
            enemySpeed = 4;
            enemyShootChance = 2;
        } else {
            enemySpeed = 6;
            enemyShootChance = 4;
        }

        startWave();
    }

    public void startWave() {
        planes.clear();
        bullets.clear();
        enemyBullets.clear();
        bombs.clear();
        knifeEnemies.clear();

        planesDestroyedInWave = 0;
        planesToSpawn = wave;

        for (int i = 0; i < planesToSpawn; i++) {
            planes.add(new Plane(rand.nextBoolean() ? 1 : -1));
        }
    }

    public void shootToMouse() {
        if (!gameStarted || !running || playerDead) return;

        double gunX = facingRight ? playerX + playerW + 10 : playerX - 10;
        double gunY = playerY + 30;

        double diffX = mouseX - gunX;
        double diffY = mouseY - gunY;

        double distance = Math.sqrt(diffX * diffX + diffY * diffY);
        if (distance == 0) return;

        double speed = 12;
        double dx = (diffX / distance) * speed;
        double dy = (diffY / distance) * speed;

        bullets.add(new Bullet(gunX, gunY, dx, dy));
        muzzleFlashTimer = 6;
    }

    public void restartGame() {
        running = true;
        gameStarted = false;
        levelSelected = false;
        selectedLevel = 0;

        playerX = 420;
        playerY = 455;
        facingRight = true;

        left = false;
        right = false;
        playerDead = false;
        deadTimer = 0;
        muzzleFlashTimer = 0;

        score = 0;
        persons = 3;
        currentPerson = 1;
        nextExtraPersonScore = 100;

        wave = 1;
        planesToSpawn = 1;
        planesDestroyedInWave = 0;

        levelName = "";

        treeAlive = true;
        houseAlive = true;
        bunkerAlive = true;
        stairsAlive = true;

        treeHealth = 2;
        houseHealth = 2;
        bunkerHealth = 2;
        stairsHealth = 2;

        bullets.clear();
        enemyBullets.clear();
        bombs.clear();
        planes.clear();
        knifeEnemies.clear();
        explosions.clear();
    }

    @Override
    public void run() {
        while (true) {
            if (gameStarted && running) {
                updateGame();
            }

            updateExplosions();
            repaint();

            try {
                Thread.sleep(20);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void updateGame() {
        facingRight = mouseX >= playerX;

        if (muzzleFlashTimer > 0) muzzleFlashTimer--;

        if (persons <= 0 && !playerDead) {
            running = false;
            return;
        }

        if (playerDead) {
            deadTimer++;
            if (deadTimer > 80) {
                playerDead = false;
                deadTimer = 0;

                if (persons > 0) {
                    currentPerson++;
                    resetPlayer();
                } else {
                    running = false;
                }
            }
            return;
        }

        if (left && playerX > 0) playerX -= playerSpeed;
        if (right && playerX < width - playerW - 40) playerX += playerSpeed;

        movePlanes();
        moveBullets();
        moveBombs();
        moveKnifeEnemies();

        checkCollision();
        checkBombCollision();
    }

    public void updateExplosions() {
        for (int i = explosions.size() - 1; i >= 0; i--) {
            explosions.get(i).update();
            if (explosions.get(i).isDone()) {
                explosions.remove(i);
            }
        }
    }

    public void movePlanes() {
        for (int i = planes.size() - 1; i >= 0; i--) {
            Plane p = planes.get(i);
            p.move();

            if (rand.nextInt(100) < enemyShootChance) {
                enemyBullets.add(new Rectangle(p.x + p.w / 2, p.y + p.h, 8, 15));
            }

            if (rand.nextInt(1000) < 4) {
                bombs.add(new Bomb(p.x + p.w / 2, p.y + p.h));
            }

            if (rand.nextInt(3000) < 2 && knifeEnemies.size() < 2) {
                knifeEnemies.add(new KnifeEnemy(p.x + p.w / 2, p.y + p.h));
            }

            if (p.outOfScreen()) {
                planes.remove(i);
            }
        }

        while (planes.size() < planesToSpawn - planesDestroyedInWave) {
            planes.add(new Plane(rand.nextBoolean() ? 1 : -1));
        }
    }

    public void moveBullets() {
        for (int i = bullets.size() - 1; i >= 0; i--) {
            Bullet b = bullets.get(i);
            b.move();

            if (b.x < 0 || b.x > width || b.y < 0 || b.y > height) {
                bullets.remove(i);
            }
        }

        for (int i = enemyBullets.size() - 1; i >= 0; i--) {
            Rectangle b = enemyBullets.get(i);
            b.y += 6;

            if (b.y > height) {
                enemyBullets.remove(i);
            }
        }
    }

    public void moveBombs() {
        for (int i = bombs.size() - 1; i >= 0; i--) {
            Bomb b = bombs.get(i);
            b.move();

            if (b.y > height) {
                explosions.add(new Explosion(b.x, height - 40));
                bombs.remove(i);
            }
        }
    }

    public void moveKnifeEnemies() {
        for (KnifeEnemy k : knifeEnemies) {
            k.move();
        }
    }

    public void checkCollision() {
        Rectangle playerBox = new Rectangle(playerX, playerY, playerW, playerH);

        for (int i = bullets.size() - 1; i >= 0; i--) {
            Bullet bullet = bullets.get(i);

            for (int j = planes.size() - 1; j >= 0; j--) {
                Plane p = planes.get(j);

                if (bullet.getBox().intersects(p.getBox())) {
                    explosions.add(new Explosion(p.x + p.w / 2, p.y + p.h / 2));
                    bullets.remove(i);
                    planes.remove(j);

                    addScore(10);
                    planesDestroyedInWave++;

                    if (planesDestroyedInWave >= planesToSpawn) {
                        wave++;
                        startWave();
                    }

                    return;
                }
            }
        }

        for (int i = bullets.size() - 1; i >= 0; i--) {
            Bullet bullet = bullets.get(i);

            for (int j = knifeEnemies.size() - 1; j >= 0; j--) {
                KnifeEnemy k = knifeEnemies.get(j);

                if (bullet.getBox().intersects(k.getBox())) {
                    explosions.add(new Explosion(k.x + k.w / 2, k.y + k.h / 2));
                    bullets.remove(i);
                    knifeEnemies.remove(j);
                    addScore(15);
                    return;
                }
            }
        }

        for (int i = enemyBullets.size() - 1; i >= 0; i--) {
            if (enemyBullets.get(i).intersects(playerBox)) {
                explosions.add(new Explosion(enemyBullets.get(i).x, enemyBullets.get(i).y));
                enemyBullets.remove(i);
                killPlayer();
                return;
            }
        }

        for (int i = knifeEnemies.size() - 1; i >= 0; i--) {
            if (knifeEnemies.get(i).getBox().intersects(playerBox)) {
                explosions.add(new Explosion(knifeEnemies.get(i).x, knifeEnemies.get(i).y));
                knifeEnemies.remove(i);
                killPlayer();
                return;
            }
        }
    }

    public void checkBombCollision() {
        Rectangle treeBox = new Rectangle(85, 390, 140, 150);
        Rectangle houseBox = new Rectangle(610, 385, 180, 155);
        Rectangle bunkerBox = new Rectangle(390, 470, 130, 70);
        Rectangle stairsBox = new Rectangle(260, 465, 100, 75);
        Rectangle playerBox = new Rectangle(playerX, playerY, playerW, playerH);

        for (int i = bombs.size() - 1; i >= 0; i--) {
            Bomb bomb = bombs.get(i);

            if (treeAlive && bomb.getBox().intersects(treeBox)) {
                explosions.add(new Explosion(bomb.x, bomb.y));
                bombs.remove(i);
                treeHealth--;
                if (treeHealth <= 0) treeAlive = false;
                continue;
            }

            if (houseAlive && bomb.getBox().intersects(houseBox)) {
                explosions.add(new Explosion(bomb.x, bomb.y));
                bombs.remove(i);
                houseHealth--;
                if (houseHealth <= 0) houseAlive = false;
                continue;
            }

            if (bunkerAlive && bomb.getBox().intersects(bunkerBox)) {
                explosions.add(new Explosion(bomb.x, bomb.y));
                bombs.remove(i);
                bunkerHealth--;
                if (bunkerHealth <= 0) bunkerAlive = false;
                continue;
            }

            if (stairsAlive && bomb.getBox().intersects(stairsBox)) {
                explosions.add(new Explosion(bomb.x, bomb.y));
                bombs.remove(i);
                stairsHealth--;
                if (stairsHealth <= 0) stairsAlive = false;
                continue;
            }

            if (bomb.getBox().intersects(playerBox)) {
                explosions.add(new Explosion(bomb.x, bomb.y));
                bombs.remove(i);
                killPlayer();
                return;
            }
        }
    }

    public void addScore(int points) {
        score += points;

        if (score % 50 == 0) enemySpeed++;

        if (score >= nextExtraPersonScore) {
            persons++;
            nextExtraPersonScore += 100;

            if (enemySpeed > 2) enemySpeed--;
        }
    }

    public void killPlayer() {
        explosions.add(new Explosion(playerX + playerW / 2, playerY + playerH / 2));

        persons--;
        playerDead = true;

        bullets.clear();
        enemyBullets.clear();
        bombs.clear();
        knifeEnemies.clear();
    }

    public void resetPlayer() {
        playerX = 420;
        playerY = 455;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.setColor(new Color(135, 206, 235));
        g.fillRect(0, 0, width, height);

        if (!gameStarted && running) {
            drawMenu(g);
            return;
        }

        drawWorld(g);
        drawReservePeople(g);

        if (playerDead) {
            drawSleepingPerson(g);
        } else if (running) {
            drawPerson(g);
        }

        for (Plane p : planes) drawAirplane(g, p);

        drawKnifeEnemies(g);
        drawBullets(g);
        drawExplosions(g);
        drawAimPoint(g);
        drawText(g);
    }

    public void drawButton(Graphics g, Rectangle r, String text, boolean selected) {
        g.setColor(selected ? new Color(255, 190, 80) : new Color(240, 240, 240));
        g.fillRoundRect(r.x, r.y, r.width, r.height, 20, 20);

        g.setColor(Color.BLACK);
        g.drawRoundRect(r.x, r.y, r.width, r.height, 20, 20);

        g.setFont(new Font("Arial", Font.BOLD, 22));
        FontMetrics fm = g.getFontMetrics();

        int tx = r.x + (r.width - fm.stringWidth(text)) / 2;
        int ty = r.y + 32;

        g.drawString(text, tx, ty);
    }

    public void drawMenu(Graphics g) {
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 45));
        g.drawString("AIR DEFENSE SHOOTER", 170, 120);

        g.setFont(new Font("Arial", Font.BOLD, 25));
        g.drawString("Choose Difficulty", 340, 200);

        drawButton(g, easyButton, "EASY", selectedLevel == 1);
        drawButton(g, mediumButton, "MEDIUM", selectedLevel == 2);
        drawButton(g, hardButton, "HARD", selectedLevel == 3);
        drawButton(g, playButton, "PLAY", false);

        g.setFont(new Font("Arial", Font.PLAIN, 18));
        g.drawString("Move mouse to aim, click/SPACE to shoot, LEFT/RIGHT to move", 220, 540);
    }

    public void drawWorld(Graphics g) {
        if (!treeAlive && !houseAlive && !bunkerAlive && !stairsAlive) {
            g.setColor(new Color(210, 180, 120));
        } else {
            g.setColor(new Color(80, 180, 80));
        }

        g.fillRect(0, 540, width, 60);

        if (stairsAlive) drawStairs(g);
        if (treeAlive) drawTree(g);
        if (houseAlive) drawHouse(g);
        if (bunkerAlive) drawBunker(g);
    }

    public void drawTree(Graphics g) {
        g.setColor(new Color(100, 60, 20));
        g.fillRect(140, 440, 25, 100);

        g.setColor(new Color(20, 120, 40));
        g.fillOval(105, 390, 95, 70);
        g.fillOval(85, 420, 80, 65);
        g.fillOval(145, 420, 80, 65);

        g.setColor(Color.BLACK);
        g.drawString("Tree HP: " + treeHealth, 95, 380);
    }

    public void drawHouse(Graphics g) {
        g.setColor(new Color(180, 120, 70));
        g.fillRect(630, 455, 140, 85);

        g.setColor(new Color(120, 50, 30));
        int[] roofX = {610, 700, 790};
        int[] roofY = {455, 385, 455};
        g.fillPolygon(roofX, roofY, 3);

        g.setColor(Color.DARK_GRAY);
        g.fillRect(685, 500, 30, 40);

        g.setColor(Color.BLACK);
        g.drawString("House HP: " + houseHealth, 625, 375);
    }

    public void drawBunker(Graphics g) {
        g.setColor(new Color(120, 120, 120));
        g.fillRoundRect(390, 470, 130, 70, 20, 20);

        g.setColor(Color.DARK_GRAY);
        g.fillRect(420, 500, 70, 15);

        g.setColor(Color.BLACK);
        g.drawString("Bunker HP: " + bunkerHealth, 390, 460);
    }

    public void drawStairs(Graphics g) {
        g.setColor(new Color(150, 150, 150));
        g.fillRect(260, 515, 100, 25);
        g.fillRect(285, 490, 75, 25);
        g.fillRect(310, 465, 50, 25);

        g.setColor(Color.BLACK);
        g.drawString("Stairs HP: " + stairsHealth, 255, 455);
    }

    public void drawReservePeople(Graphics g) {
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("Reserve People", 710, 25);

        int reserve = persons;
        if (!playerDead && running && reserve > 0) reserve = persons - 1;

        for (int i = 0; i < reserve; i++) {
            drawSmallPerson(g, 720 + i * 45, 40);
        }
    }

    public void drawSmallPerson(Graphics g, int x, int y) {
        g.setColor(new Color(255, 220, 180));
        g.fillOval(x + 8, y, 18, 18);

        g.setColor(Color.BLUE);
        g.fillRect(x + 11, y + 18, 12, 25);

        g.setColor(Color.BLACK);
        g.drawLine(x + 11, y + 43, x + 4, y + 60);
        g.drawLine(x + 23, y + 43, x + 30, y + 60);
    }

    public void drawPerson(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        g.setColor(new Color(255, 220, 180));
        g.fillOval(playerX + 12, playerY, 25, 25);

        g.setColor(Color.BLACK);
        g.fillArc(playerX + 12, playerY - 2, 25, 18, 0, 180);

        g.setColor(Color.BLUE);
        g.fillRect(playerX + 15, playerY + 25, 20, 35);

        double gunX = facingRight ? playerX + playerW + 10 : playerX - 10;
        double gunY = playerY + 30;

        double angle = Math.atan2(mouseY - gunY, mouseX - gunX);
        int gunEndX = (int) (gunX + Math.cos(angle) * 42);
        int gunEndY = (int) (gunY + Math.sin(angle) * 42);

        g.setColor(new Color(255, 220, 180));

        if (facingRight) {
            g.drawLine(playerX + 15, playerY + 35, playerX + 2, playerY + 50);
            g.drawLine(playerX + 35, playerY + 35, (int) gunX, (int) gunY);
        } else {
            g.drawLine(playerX + 35, playerY + 35, playerX + 48, playerY + 50);
            g.drawLine(playerX + 15, playerY + 35, (int) gunX, (int) gunY);
        }

        g2.setStroke(new BasicStroke(6));
        g2.setColor(Color.BLACK);
        g2.drawLine((int) gunX, (int) gunY, gunEndX, gunEndY);
        g2.setStroke(new BasicStroke(1));

        if (muzzleFlashTimer > 0) {
            g.setColor(Color.YELLOW);
            g.fillOval(gunEndX - 6, gunEndY - 6, 12, 12);
        }

        g.setColor(Color.BLACK);
        g.drawLine(playerX + 20, playerY + 60, playerX + 10, playerY + 85);
        g.drawLine(playerX + 30, playerY + 60, playerX + 40, playerY + 85);

        g.fillRect(playerX + 4, playerY + 84, 14, 5);
        g.fillRect(playerX + 36, playerY + 84, 14, 5);
    }

    public void drawSleepingPerson(Graphics g) {
        int x = playerX;
        int y = 515;

        g.setColor(new Color(255, 220, 180));
        g.fillOval(x, y, 25, 25);

        g.setColor(Color.BLUE);
        g.fillRect(x + 25, y + 8, 45, 18);

        g.setColor(Color.BLACK);
        g.drawLine(x + 35, y + 25, x + 20, y + 40);
        g.drawLine(x + 55, y + 25, x + 75, y + 40);

        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString("X", x + 5, y + 18);
    }

    public void drawAirplane(Graphics g, Plane p) {
        int x = p.x;
        int y = p.y;

        g.setColor(Color.RED);
        g.fillOval(x, y + 10, p.w, 25);

        if (p.direction == 1) {
            int[] noseX = {x + p.w, x + p.w + 25, x + p.w};
            int[] noseY = {y + 10, y + 22, y + 35};
            g.fillPolygon(noseX, noseY, 3);
        } else {
            int[] noseX = {x, x - 25, x};
            int[] noseY = {y + 10, y + 22, y + 35};
            g.fillPolygon(noseX, noseY, 3);
        }

        g.setColor(Color.DARK_GRAY);
        int[] wingX = {x + 35, x + 70, x + 50};
        int[] wingY = {y + 20, y - 25, y + 20};
        g.fillPolygon(wingX, wingY, 3);

        g.setColor(Color.WHITE);
        g.fillOval(x + 60, y + 15, 15, 10);
    }

    public void drawKnifeEnemies(Graphics g) {
        for (KnifeEnemy k : knifeEnemies) {
            g.setColor(new Color(255, 220, 180));
            g.fillOval(k.x + 8, k.y, 20, 20);

            g.setColor(Color.BLACK);
            g.fillRect(k.x + 10, k.y + 20, 16, 30);

            g.setColor(Color.RED);
            g.drawLine(k.x + 26, k.y + 28, k.x + 38, k.y + 20);

            g.setColor(Color.LIGHT_GRAY);
            g.fillRect(k.x + 36, k.y + 16, 5, 16);
        }
    }

    public void drawBullets(Graphics g) {
        g.setColor(Color.YELLOW);
        for (Bullet b : bullets) {
            g.fillOval((int) b.x, (int) b.y, b.w, b.h);
        }

        g.setColor(Color.MAGENTA);
        for (Rectangle b : enemyBullets) {
            g.fillRect(b.x, b.y, b.width, b.height);
        }

        g.setColor(Color.BLACK);
        for (Bomb bomb : bombs) {
            g.fillOval(bomb.x, bomb.y, bomb.w, bomb.h);
        }
    }

    public void drawExplosions(Graphics g) {
        for (Explosion ex : explosions) {
            g.setColor(Color.ORANGE);
            g.fillOval(ex.x - ex.size / 2, ex.y - ex.size / 2, ex.size, ex.size);

            g.setColor(Color.RED);
            g.drawOval(ex.x - ex.size / 2, ex.y - ex.size / 2, ex.size, ex.size);

            g.setColor(Color.YELLOW);
            g.fillOval(ex.x - ex.size / 4, ex.y - ex.size / 4, ex.size / 2, ex.size / 2);
        }
    }

    public void drawAimPoint(Graphics g) {
        if (!playerDead && running && gameStarted) {
            g.setColor(Color.BLACK);
            g.drawOval(mouseX - 8, mouseY - 8, 16, 16);
            g.drawLine(mouseX - 12, mouseY, mouseX + 12, mouseY);
            g.drawLine(mouseX, mouseY - 12, mouseX, mouseY + 12);
        }
    }

    public void drawText(Graphics g) {
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 18));

        g.drawString("Level: " + levelName, 20, 30);
        g.drawString("Wave: " + wave, 20, 60);
        g.drawString("Planes left: " + (planesToSpawn - planesDestroyedInWave), 20, 90);
        g.drawString("Score: " + score, 20, 120);
        g.drawString("People Left: " + persons, 20, 150);
        g.drawString("Enemy Speed: " + enemySpeed, 20, 180);
        g.drawString("Click/SPACE to shoot", 20, 210);

        if (!running) {
            g.setFont(new Font("Arial", Font.BOLD, 50));
            g.setColor(Color.RED);
            g.drawString("GAME OVER", 290, 280);

            g.setFont(new Font("Arial", Font.BOLD, 25));
            g.setColor(Color.BLACK);
            g.drawString("Final Score: " + score, 360, 340);

            drawButton(g, restartButton, "RESTART", false);
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Air Defense Shooter");
        AirDefenseGame game = new AirDefenseGame();

        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}