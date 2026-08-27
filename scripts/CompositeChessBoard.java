import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** One-off: stitch oak/walnut textures into an even 8x8 chessboard PNG. */
public final class CompositeChessBoard {
  public static void main(String[] args) throws Exception {
    Path oakPath = Path.of(args[0]);
    Path walnutPath = Path.of(args[1]);
    Path outPath = Path.of(args[2]);

    int n = 8;
    int cell = 192;
    int size = n * cell;
    BufferedImage oak = scale(ImageIO.read(oakPath.toFile()), size + cell, size + cell);
    BufferedImage walnut = scale(ImageIO.read(walnutPath.toFile()), size + cell, size + cell);
    oak = contrast(oak, 1.08f, 1.04f);
    walnut = contrast(walnut, 1.06f, 1.28f);

    BufferedImage board = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = board.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

    for (int rank = 0; rank < n; rank++) {
      for (int file = 0; file < n; file++) {
        boolean light = (file + (n - 1 - rank)) % 2 == 1;
        BufferedImage src = light ? oak : walnut;
        int ox = (file * 73 + rank * 29) % cell;
        int oy = (file * 41 + rank * 67) % cell;
        BufferedImage tile = src.getSubimage(ox, oy, cell, cell);
        if (!light && (file + rank) % 3 == 0) {
          tile = rotate90(tile);
        }
        g.drawImage(tile, file * cell, rank * cell, null);
        g.setComposite(AlphaComposite.SrcOver.derive(0.22f));
        g.setColor(new Color(255, 255, 255));
        g.fillRect(file * cell, rank * cell, cell, 3);
        g.fillRect(file * cell, rank * cell, 3, cell);
        g.setColor(new Color(0, 0, 0));
        g.fillRect(file * cell, rank * cell + cell - 3, cell, 3);
        g.fillRect(file * cell + cell - 3, rank * cell, 3, cell);
        g.setComposite(AlphaComposite.SrcOver);
      }
    }

    g.setColor(new Color(28, 16, 10, 110));
    for (int i = 1; i < n; i++) {
      int p = i * cell;
      g.fillRect(0, p - 1, size, 3);
      g.fillRect(p - 1, 0, 3, size);
    }

    BufferedImage vignette = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
    Graphics2D vg = vignette.createGraphics();
    vg.setPaint(
        new java.awt.RadialGradientPaint(
            size / 2f,
            size / 2f,
            size * 0.72f,
            new float[] {0f, 0.72f, 1f},
            new Color[] {new Color(0, 0, 0, 0), new Color(0, 0, 0, 0), new Color(0, 0, 0, 70)}));
    vg.fillRect(0, 0, size, size);
    vg.dispose();
    g.drawImage(vignette, 0, 0, null);
    g.dispose();

    outPath.getParent().toFile().mkdirs();
    ImageIO.write(board, "png", outPath.toFile());
    System.out.println("Wrote " + outPath.toAbsolutePath() + " " + outPath.toFile().length());
  }

  private static BufferedImage scale(BufferedImage src, int w, int h) {
    BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = dst.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    g.drawImage(src, 0, 0, w, h, null);
    g.dispose();
    return dst;
  }

  private static BufferedImage contrast(BufferedImage src, float contrast, float brightness) {
    float offset = 128f * (1f - contrast) + (brightness - 1f) * 128f;
    RescaleOp op = new RescaleOp(contrast, offset, null);
    BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
    op.filter(src, dst);
    return dst;
  }

  private static BufferedImage rotate90(BufferedImage src) {
    int w = src.getWidth();
    int h = src.getHeight();
    BufferedImage dst = new BufferedImage(h, w, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = dst.createGraphics();
    g.translate(h, 0);
    g.rotate(Math.PI / 2);
    g.drawImage(src, 0, 0, null);
    g.dispose();
    return dst;
  }
}
