package gane;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class SplashScreen {

    private static JWindow window;
    private static Timer timer;
    private static boolean showStatic = false;

    /**
     * Arka planda çalışacak şekilde çerçevesiz animasyonlu yükleme ekranını gösterir.
     */
    public static void show() {
        if (window != null) return; // Zaten açıksa çık

        try {
            // GIF ve statik dosya yolları
            File gifFile = new File("Engine/res/ENGINE_MEDIA/loading.gif");
            File staticFile = new File("Engine/res/ENGINE_MEDIA/static_splash.png");
            
            if (!gifFile.exists()) {
                System.err.println("SplashScreen: GIF dosyası bulunamadı! " + gifFile.getAbsolutePath());
                return;
            }

            window = new JWindow();
            window.setAlwaysOnTop(true);

            // Ekran boyutunu al
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int sw = screenSize.width;
            int sh = screenSize.height;

            // Orijinal medya dosyalarını yükle
            ImageIcon rawGif = new ImageIcon(gifFile.getAbsolutePath());
            ImageIcon rawStatic = new ImageIcon(staticFile.getAbsolutePath());

            // GIF animasyonunu bozmadan ve yüksek kalitede çizmek için özel bir Panel
            JPanel mainPanel = new JPanel(new BorderLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2d = (Graphics2D) g.create();
                    
                    // En yüksek kalitede çizim (Bilinear Interpolation)
                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    Image imgToDraw = showStatic ? rawStatic.getImage() : rawGif.getImage();
                    if (imgToDraw != null) {
                        g2d.drawImage(imgToDraw, 0, 0, getWidth(), getHeight(), this);
                    }
                    g2d.dispose();
                }
            };
            mainPanel.setPreferredSize(new Dimension(sw, sh));

            // Yükleme Dairesi (Loading Circle) Paneli
            JPanel loadingCirclePanel = new JPanel() {
                double angle = 0;
                {
                    setOpaque(false);
                    new Timer(16, e -> {
                        angle += 0.15;
                        repaint();
                    }).start();
                }
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    
                    int size = 50;
                    int x = getWidth() / 2 - size / 2;
                    int y = getHeight() - size - 40; // Alt kısma yerleştir
                    
                    // Dairenin gölgesi
                    g2d.setColor(new Color(0, 0, 0, 100));
                    g2d.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2d.drawArc(x+2, y+2, size, size, 0, 360);
                    
                    // Dönen daire
                    g2d.setColor(Color.WHITE);
                    g2d.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2d.drawArc(x, y, size, size, (int) Math.toDegrees(angle), 300);
                    
                    g2d.dispose();
                }
            };
            loadingCirclePanel.setVisible(false); // Başlangıçta gizli
            mainPanel.add(loadingCirclePanel, BorderLayout.CENTER);

            window.getContentPane().add(mainPanel);
            window.getContentPane().setBackground(Color.BLACK);
            
            // Pencereyi tam ekran yap
            window.setSize(sw, sh);
            window.setLocation(0, 0);
            window.setVisible(true);

            // 8 Saniye sonra statik resme geç (ekrana tam fit ederek) ve daireyi göster
            timer = new Timer(8000, e -> {
                if (staticFile.exists()) {
                    showStatic = true;
                    mainPanel.repaint(); // Paneli yenile ki yüksek kaliteli statik resim çizilsin
                }
                loadingCirclePanel.setVisible(true);
            });
            timer.setRepeats(false);
            timer.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Yükleme işlemi bitince ekranı kapatır.
     */
    public static void close() {
        if (window != null) {
            if (timer != null) timer.stop();
            window.setVisible(false);
            window.dispose();
            window = null;
        }
    }
}
